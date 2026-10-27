package maha.garage.ui.settings

import maha.garage.ui.tutorial.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.ui.theme.*

import maha.garage.util.NotificationSettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCarHistory: () -> Unit = {},
    onStartTutorial: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val settingsStore = remember { NotificationSettingsStore(context) }
    var notifyInspection by remember { mutableStateOf(settingsStore.isInspectionEnabled) }
    var notifyInsurance by remember { mutableStateOf(settingsStore.isInsuranceEnabled) }
    val tutorial = LocalTutorial.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(tutorial?.index) {
        if (tutorial?.cue?.target == "settings.history") listState.scrollToItem(2)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "환경설정 & 앱 정보",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedButton(onClick = onStartTutorial, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("튜토리얼 다시 보기")
                }
            }
            // 1. 앱 헤더 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GaragePrimaryLight.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = GaragePrimaryLight,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "마하개러지 (MahaGarage)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "차량 다중 관리 & 타이어 듀얼 마일리지 추적 차계부 v$versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // 1.5. 카 히스토리 출력 전용 섹션 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GaragePrimaryLight.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = GaragePrimaryLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "카 히스토리 (차계부 정비 이력서)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "전체 교체·정비 이력을 한눈에 조회하고 PDF로 전송",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "차량 정보, 조회 기간별 정비 횟수, 비용 합계 및 건별 상세 내역을 표 형태로 확인하고, 관공서·중고차 판매·보험 제출용 공식 1장 요약/전체 이력 PDF를 생성하여 전송할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val tutorialAction1 = onNavigateToCarHistory
Button(
                            onClick = tutorialAction1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp).tutorialTarget("settings.history", tutorialAction1),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "카 히스토리 출력 및 조회",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }

            // 2. 알림 설정 섹션
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = GaragePrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "필수 법정 및 보험 알림 설정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. 법정 자동차 검사 기한 알림
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "자동차 정기·종합·재검사 기한 알림",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "법정 검사 유효기간 만료 임박 및 부적합 판정 시 재검사 기한을 안내하여 과태료 발생을 방지합니다.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = notifyInspection,
                                onCheckedChange = {
                                    notifyInspection = it
                                    settingsStore.isInspectionEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GaragePrimaryLight,
                                    checkedTrackColor = GaragePrimaryLight.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. 자동차 보험 만기일 알림
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "자동차 보험 갱신 및 만기 알림",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "자동차 보험 만료 30일 전, 7일 전, 만료 당일에 갱신 안내 알림을 전송하여 무보험 상태를 방지합니다.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = notifyInsurance,
                                onCheckedChange = {
                                    notifyInsurance = it
                                    settingsStore.isInsuranceEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GarageAccent,
                                    checkedTrackColor = GarageAccent.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 알림 테스트 & 즉시 점검 실행 버튼 영역
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var testAlertSent by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    maha.garage.util.NotificationHelper.showTestNotification(context)
                                    testAlertSent = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GaragePrimaryLight)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (testAlertSent) "발송 완료!" else "알림 테스트", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "즉시 만기점검", color = TextPrimaryDark, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. OEM 계층 아키텍처 설명 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = GarageAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "마하개러지 OEM 계층 아키텍처",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "각 소모품의 권장 주기는 5단계 우선순위 엔진을 통해 엄격하게 산출됩니다:\n\n" +
                                    "1. USER_OVERRIDE (사용자 직접 수정값 최우선)\n" +
                                    "2. VERIFIED_OEM (차량 공식 매뉴얼 공시 주기)\n" +
                                    "3. OEM_FAMILY (동일 파워트레인 제조사 표준)\n" +
                                    "4. GENERIC_ESTIMATE (국내외 통상 권장 기준)\n" +
                                    "5. NOT_CONFIGURED (초기 미설정 상태)\n\n" +
                                    "* 가혹 운행 조건 활성화 시 표준 엔진오일 7,500km 등 실차 매뉴얼에 의거하여 단축 계산됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                        )
                    }
                }
            }

            // 4. 로컬 데이터베이스 및 파일 백업/복원
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val app = context.applicationContext as maha.garage.MahaGarageApp
                val db = app.database
                val scope = rememberCoroutineScope()
                var backupStatusMessage by remember { mutableStateOf<String?>(null) }
                var isProcessing by remember { mutableStateOf(false) }

                // 기기 변경 클라우드 전송 상태
                var showKeyResultDialog by remember { mutableStateOf(false) }
                var generatedTransferInfo by remember { mutableStateOf<maha.garage.util.CloudTransferInfo?>(null) }
                var showInputKeyDialog by remember { mutableStateOf(false) }
                var inputKeyText by remember { mutableStateOf("") }
                var transferErrorMessage by remember { mutableStateOf<String?>(null) }

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                val importJsonLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        isProcessing = true
                        scope.launch {
                            try {
                                val content = context.contentResolver.openInputStream(uri)?.use {
                                    it.reader().readText()
                                }
                                if (content != null) {
                                    val result = maha.garage.util.BackupRestoreManager.importBackup(context, db, content)
                                    result.onSuccess { summary ->
                                        backupStatusMessage = "✅ 파일 복원 완료: 차량 ${summary.carsCount}대, 규칙 ${summary.rulesCount}건, 정비 ${summary.visitsCount}건, 스왑 ${summary.swapLogsCount}건" + if (summary.missingPhotos > 0) "\n사진 ${summary.missingPhotos}개는 원본 파일이 없어 복원되지 않았습니다." else ""
                                    }.onFailure { err ->
                                        backupStatusMessage = "❌ 복원 실패: ${err.message}"
                                    }
                                }
                            } catch (e: Exception) {
                                backupStatusMessage = "❌ 오류 발생: ${e.localizedMessage}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                }

                // ==================== A. 기기 변경 데이터 이전 (6자리 전송 키) 카드 ====================
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GaragePrimaryLight.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GaragePrimaryLight.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, tint = GaragePrimaryLight, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "새 휴대폰으로 데이터 옮기기 (기기 변경)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "6자리 전송 키를 통한 간편 클라우드 마이그레이션",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GaragePrimaryLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "• 평상시 모든 데이터는 스마트폰 내부 로컬 DB에만 100% 안전하게 저장됩니다.\n" +
                                    "• 핸드폰을 바꿀 때 아래 [데이터 옮기기]를 누르면 임시 전송 키(6자리)가 발급되며, 새 폰에서 해당 키를 입력하면 모든 차량과 정비 기록이 그대로 불러와집니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.35f
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (transferErrorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusOverdue.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = transferErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusOverdue,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. 내보내기 (키 발급)
                            Button(
                                onClick = {
                                    isProcessing = true
                                    transferErrorMessage = null
                                    scope.launch {
                                        val result = maha.garage.util.CloudTransferManager.uploadTransferData(context, db)
                                        result.onSuccess { info ->
                                            generatedTransferInfo = info
                                            showKeyResultDialog = true
                                        }.onFailure { err ->
                                            transferErrorMessage = "❌ 전송 실패: ${err.localizedMessage ?: "알 수 없는 오류"}"
                                        }
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight),
                                enabled = !isProcessing
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "데이터 옮기기 (키 발급)", color = TextPrimaryDark, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            // 2. 가져오기 (키 입력)
                            OutlinedButton(
                                onClick = {
                                    transferErrorMessage = null
                                    inputKeyText = ""
                                    showInputKeyDialog = true
                                },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GarageAccent),
                                enabled = !isProcessing
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "전송 키로 가져오기", color = GarageAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==================== B. 오프라인 파일 백업/복원 카드 ====================
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = StatusNormal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "오프라인 파일 백업 & 복원",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "차계부 전체 데이터를 JSON 파일 형태로 직접 내보내거나 기기에 저장된 백업 파일을 불러옵니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        if (backupStatusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = backupStatusMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (backupStatusMessage?.startsWith("✅") == true) StatusNormal else StatusOverdue,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    isProcessing = true
                                    scope.launch {
                                        try {
                                            val backup = maha.garage.util.BackupRestoreManager.exportBackup(context, db)
                                            val file = backup.file
                                            maha.garage.util.BackupRestoreManager.shareBackup(context, file)
                                            backupStatusMessage = "✅ 백업 파일 생성 완료 (${file.name})" + if (backup.missingPhotos > 0) "\n원본이 없는 사진 ${backup.missingPhotos}개는 포함되지 않았습니다." else ""
                                        } catch (e: Exception) {
                                            backupStatusMessage = "❌ 백업 실패: ${e.localizedMessage}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                                enabled = !isProcessing
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "파일로 내보내기", color = TextPrimaryDark, style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    importJsonLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                                enabled = !isProcessing
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "파일에서 불러오기", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // ------------------ 팝업 다이얼로그 1: 전송 키 발급 완료 ------------------
                if (showKeyResultDialog && generatedTransferInfo != null) {
                    val info = generatedTransferInfo!!
                    AlertDialog(
                        onDismissRequest = { showKeyResultDialog = false },
                        containerColor = SurfaceDark,
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("데이터 전송 키 발급 완료", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "새 휴대폰에서 아래 6자리 키를 입력하면\n모든 차량 및 정비 데이터가 복원됩니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                // 키 카드
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GaragePrimaryLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "전송 인증 키 (6자리)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${info.transferKey.substring(0, 3)} - ${info.transferKey.substring(3, 6)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GaragePrimaryLight,
                                            letterSpacing = 3.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "• 이전 데이터: 차량 ${info.carsCount}대 / 정비 ${info.visitsCount}건\n• 두 휴대폰 모두 최신 버전에서 이용해 주세요.\n• 유효 기간: 발급 후 48시간, 한 번 복원하면 사용 완료" + if (info.missingPhotos > 0) "\n• 원본이 없는 사진 ${info.missingPhotos}개는 포함되지 않았습니다." else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(info.transferKey))
                                    showKeyResultDialog = false
                                    backupStatusMessage = "✅ 전송 키(${info.transferKey})가 클립보드에 복사되었습니다."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("키 복사 및 닫기", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKeyResultDialog = false }) {
                                Text("닫기", color = TextSecondaryDark)
                            }
                        }
                    )
                }

                // ------------------ 팝업 다이얼로그 2: 전송 키 입력하여 복원 ------------------
                if (showInputKeyDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isProcessing) showInputKeyDialog = false },
                        containerColor = SurfaceDark,
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = GaragePrimaryLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("전송 키로 데이터 가져오기", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "이전 휴대폰의 마하개러지에서 발급받은 6자리 전송 키를 입력하세요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = inputKeyText,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                            inputKeyText = it
                                        }
                                    },
                                    placeholder = { Text("6자리 숫자 (예: 123456)") },
                                    label = { Text("전송 키 입력") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GaragePrimaryLight,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )

                                if (isProcessing) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(color = GaragePrimaryLight, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("클라우드에서 데이터 복원 중...", style = MaterialTheme.typography.bodySmall, color = GaragePrimaryLight)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (inputKeyText.trim().length != 6) {
                                        transferErrorMessage = "6자리 전송 키를 모두 입력해주세요."
                                        return@Button
                                    }
                                    isProcessing = true
                                    transferErrorMessage = null
                                    scope.launch {
                                        val result = maha.garage.util.CloudTransferManager.downloadAndRestore(context, db, inputKeyText)
                                        result.onSuccess { summary ->
                                            showInputKeyDialog = false
                                            backupStatusMessage = "✅ 데이터 이전 성공! 차량 ${summary.carsCount}대, 정비 내역 ${summary.visitsCount}건이 복원되었습니다." + if (summary.missingPhotos > 0) "\n사진 ${summary.missingPhotos}개는 원본 파일이 없어 복원되지 않았습니다." else ""
                                        }.onFailure { err ->
                                            transferErrorMessage = "❌ 가져오기 실패: ${err.localizedMessage ?: "키를 확인해주세요"}"
                                        }
                                        isProcessing = false
                                    }
                                },
                                enabled = !isProcessing && inputKeyText.length == 6,
                                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
                            ) {
                                Text("데이터 불러오기", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            if (!isProcessing) {
                                TextButton(onClick = { showInputKeyDialog = false }) {
                                    Text("취소", color = TextSecondaryDark)
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

