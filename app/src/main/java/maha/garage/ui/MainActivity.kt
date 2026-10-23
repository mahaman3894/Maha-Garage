package maha.garage.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.compose.runtime.saveable.rememberSaveable
import maha.garage.MahaGarageApp
import maha.garage.ui.theme.*

/**
 * 인앱 업데이트 게이트웨이 상태 정의 (MahaGarage_수정가이드.md Section 3 준수)
 * 상태와 안내창을 분리하여, 사용자가 다이얼로그를 닫거나 조회가 실패해도 구버전 앱을 우회 사용하는 것을 원천 차단합니다.
 */
sealed interface UpdateGateState {
    data object Checking : UpdateGateState
    data object Ready : UpdateGateState
    data class Required(val targetVersionCode: Long) : UpdateGateState
    data class Launching(val targetVersionCode: Long) : UpdateGateState
    data class BlockedError(
        val targetVersionCode: Long,
        val message: String
    ) : UpdateGateState
}

/**
 * 앱의 진입점 Activity.
 * 1. Google Play In-App Updates 상태 기반 강제 차단(Immediate Update Gate) 구현
 * 2. 알림 권한 체크 및 딥링크/인텐트 라우팅 처리
 * 3. 앱 실행 시 웰컴 애니메이션 재생 (터치 시 즉시 스킵)
 */
class MainActivity : ComponentActivity() {
    private val targetVehicleId = mutableStateOf<String?>(null)
    private val updateGateState = mutableStateOf<UpdateGateState>(UpdateGateState.Checking)
    private val queryInProgress = mutableStateOf(false)
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var requirement: maha.garage.util.UpdateRequirementStore
    private var installedVersion = 0L
    private var requestId = 0L
    private var timeoutJob: kotlinx.coroutines.Job? = null
    private var flowInFlight = false
    private var firstResume = true

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        firstResume = false
        flowInFlight = false
        requestId++
        timeoutJob?.cancel()
        queryInProgress.value = false
        if (result.resultCode != Activity.RESULT_OK) {
            block("업데이트가 취소되었거나 완료되지 않았습니다. 다시 시도해 주세요.")
        } else {
            checkForAppUpdate(autoStart = false)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetVehicleId.value = if (savedInstanceState != null) savedInstanceState.getString("pending_vehicle")
            else intent.getStringExtra("target_vehicle_id")
        installedVersion = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
            packageManager.getPackageInfo(packageName, 0)
        )
        requirement = maha.garage.util.UpdateRequirementStore(this, installedVersion)
        if (requirement.target > 0) updateGateState.value = UpdateGateState.Required(requirement.target)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        val app = application as MahaGarageApp
        checkAndRequestNotificationPermission()
        setContent {
            var showWelcome by rememberSaveable { mutableStateOf(true) }

            if (showWelcome) {
                WelcomeAnimationScreen(
                    onDismiss = { showWelcome = false }
                )
            } else {
                MahaGarageTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (val state = updateGateState.value) {
                            is UpdateGateState.Ready -> MainAppScreen(
                                app = app,
                                targetVehicleId = targetVehicleId.value,
                                onTargetConsumed = { targetVehicleId.value = null }
                            )
                            is UpdateGateState.Checking -> UpdateCheckingScreen()
                            else -> RequiredUpdateScreen(
                                state = state,
                                onRetry = { checkForAppUpdate(autoStart = true) },
                                onOpenStore = { openPlayStore() },
                                onExit = { finishAffinity() },
                                isChecking = queryInProgress.value
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // One query path for startup, store return and interrupted update recovery.
        // Returning from a cancelled flow must not automatically prompt again.
        val autoStart = firstResume
        firstResume = false
        checkForAppUpdate(autoStart)
    }

    override fun onStop() {
        // Ignore callbacks belonging to an Activity which is no longer visible.
        requestId++
        timeoutJob?.cancel()
        queryInProgress.value = false
        flowInFlight = false
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_vehicle", targetVehicleId.value)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetVehicleId.value = intent.getStringExtra("target_vehicle_id")
    }

    private fun checkForAppUpdate(autoStart: Boolean) {
        if (queryInProgress.value || flowInFlight || isFinishing || isDestroyed) return
        queryInProgress.value = true
        val id = ++requestId
        timeoutJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(15_000)
            if (id == requestId && queryInProgress.value) {
                requestId++
                queryInProgress.value = false
                handleQueryFailure()
            }
        }
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (!finishQuery(id)) return@addOnSuccessListener
            val availability = when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> maha.garage.domain.engine.UpdateGatePolicy.Availability.NONE
                UpdateAvailability.UPDATE_AVAILABLE -> maha.garage.domain.engine.UpdateGatePolicy.Availability.AVAILABLE
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> maha.garage.domain.engine.UpdateGatePolicy.Availability.IN_PROGRESS
                else -> maha.garage.domain.engine.UpdateGatePolicy.Availability.UNKNOWN
            }
            val decision = maha.garage.domain.engine.UpdateGatePolicy.decide(
                installedVersion, requirement.target, info.availableVersionCode().toLong(),
                availability, info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            )
            try {
                when (decision.action) {
                    maha.garage.domain.engine.UpdateGatePolicy.Action.ALLOW -> {
                        requirement.clear()
                        updateGateState.value = UpdateGateState.Ready
                    }
                    maha.garage.domain.engine.UpdateGatePolicy.Action.START -> {
                        requirement.requireVersion(decision.target)
                        if (autoStart || availability == maha.garage.domain.engine.UpdateGatePolicy.Availability.IN_PROGRESS) {
                            requestImmediateUpdate(info)
                        } else if (updateGateState.value !is UpdateGateState.BlockedError) {
                            updateGateState.value = UpdateGateState.Required(requirement.target)
                        }
                    }
                    maha.garage.domain.engine.UpdateGatePolicy.Action.REQUIRE_STORE -> {
                        requirement.requireVersion(decision.target)
                        block("즉시 업데이트를 사용할 수 없습니다. Google Play 스토어에서 업데이트해 주세요.")
                    }
                    maha.garage.domain.engine.UpdateGatePolicy.Action.RETRY -> handleQueryFailure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update state could not be saved", e)
                block("업데이트 상태를 저장하지 못했습니다. 다시 시도해 주세요.")
            }
        }.addOnFailureListener { error ->
            if (!finishQuery(id)) return@addOnFailureListener
            Log.w(TAG, "Update query failed", error)
            handleQueryFailure()
        }
    }

    private fun finishQuery(id: Long): Boolean {
        if (id != requestId || !queryInProgress.value || isFinishing || isDestroyed) return false
        queryInProgress.value = false
        timeoutJob?.cancel()
        return true
    }

    private fun handleQueryFailure() {
        if (maha.garage.domain.engine.UpdateGatePolicy.failed(installedVersion, requirement.target).target > 0) {
            block("업데이트 정보를 확인할 수 없습니다. 연결을 확인하고 다시 시도해 주세요.")
        } else {
            // No confirmed requirement: keep local vehicle data usable offline.
            updateGateState.value = UpdateGateState.Ready
        }
    }

    private fun block(message: String) {
        updateGateState.value = UpdateGateState.BlockedError(requirement.target, message)
    }

    private fun requestImmediateUpdate(info: AppUpdateInfo) {
        if (flowInFlight || isFinishing || isDestroyed) return
        updateGateState.value = UpdateGateState.Launching(requirement.target)
        flowInFlight = true
        try {
            val started = appUpdateManager.startUpdateFlowForResult(
                info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
            if (!started) {
                flowInFlight = false
                block("업데이트를 시작하지 못했습니다. 다시 시도하거나 스토어에서 업데이트해 주세요.")
            }
        } catch (e: Exception) {
            flowInFlight = false
            Log.e(TAG, "Update launch failed", e)
            block("업데이트를 시작하지 못했습니다. 다시 시도해 주세요.")
        }
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } catch (e: Exception) {
                Log.w(TAG, "Store could not be opened", e)
                block("스토어를 열 수 없습니다. Google Play 스토어에서 마하개러지를 검색해 주세요.")
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object { private const val TAG = "MainActivity" }
}
/**
 * 초기 업데이트 확인 중 표시 화면
 */
@Composable
fun UpdateCheckingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GaragePrimaryLight, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "앱 버전을 확인하는 중입니다...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
    }
}

/**
 * 필수 즉시 업데이트 강제 차단 화면 (MahaGarage_수정가이드.md Section 3 준수)
 * 뒤로가기나 팝업 닫기로 메인 화면에 진입하는 것을 완벽히 방지합니다.
 */
@Composable
fun RequiredUpdateScreen(
    state: UpdateGateState,
    onRetry: () -> Unit,
    onOpenStore: () -> Unit,
    onExit: () -> Unit,
    isChecking: Boolean = false
) {
    val isLaunching = state is UpdateGateState.Launching || isChecking
    val errorMessage = (state as? UpdateGateState.BlockedError)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GaragePrimaryLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = GaragePrimaryLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "최신 버전 업데이트 안내",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "마하개러지의 최신 안정화 버전이 출시되었습니다.\n원활하고 안전한 이용을 위해 앱을 최신 버전으로 업데이트해야 합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = StatusOverdue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, StatusOverdue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusOverdue,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. 업데이트 다시 시도 버튼
                Button(
                    onClick = onRetry,
                    enabled = !isLaunching,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLaunching) {
                        CircularProgressIndicator(
                            color = TextPrimaryDark,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("업데이트 시작 중...", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextPrimaryDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("업데이트 다시 시도", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Google Play 스토어에서 직접 열기 버튼
                OutlinedButton(
                    onClick = onOpenStore,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GaragePrimaryLight.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GaragePrimaryLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Play 스토어로 이동", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. 앱 종료 버튼
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("앱 종료", color = TextSecondaryDark)
                }
            }
        }
    }
}
