package maha.garage.ui.vehicle

import maha.garage.ui.tutorial.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.theme.*

/**
 * 차량 현재 주행거리 갱신 모달
 * 1. 현재 주행거리: 상단에 읽기 전용으로 명확히 표시 (변경 불가).
 * 2. 새 주행거리: 계기판 누적 주행거리 입력 필드.
 */
@Composable
fun UpdateMileageDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        newMileage: Int,
        summerMileage: Int?,
        winterMileage: Int?,
        lastSwapMileage: Int?
    ) -> Unit,
    onSkip: (() -> Unit)? = null,
    isSaving: Boolean = false,
    saveError: String? = null
) {
    var mileageText by rememberSaveable(car.id) { mutableStateOf("") }
    val tutorial = LocalTutorial.current
    LaunchedEffect(tutorial?.index) { if (tutorial?.cue?.target == "mileage.save") mileageText = "50600" }
    var showLowerConfirmation by rememberSaveable(car.id) { mutableStateOf(false) }
    var showOdometerScanner by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val parsedMileage = mileageText.toIntOrNull()
    val newMileage = parsedMileage ?: -1
    val isDecreased = parsedMileage != null && newMileage < car.currentMileage
    val drivenDelta = if (parsedMileage != null && newMileage >= car.currentMileage) (newMileage - car.currentMileage) else 0

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ToolYellowSoft,
                border = BorderStroke(1.dp, ToolYellow.copy(alpha = 0.6f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ToolOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "현재 주행거리를 입력하세요",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${car.make} ${car.modelName} · ${car.carNumber}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 현재 기록된 주행거리 박스 (변경 불가, 읽기 전용)
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WarmLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WarmGray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "현재 기록된 주행거리 (변경 불가)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%,d km".format(car.currentMileage),
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarmGray.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "기록 기준",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. 새 누적 주행거리 입력창 (계기판 기준) + 원형 OCR 카메라 FAB
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = mileageText,
                            onValueChange = { input ->
                                if (input.all { it in '0'..'9' }) {
                                    mileageText = input
                                }
                            },
                            label = { Text("새 누적 주행거리 (km)") },
                            placeholder = { Text("예: %,d".format(car.currentMileage + 250)) },
                            singleLine = true,
                            enabled = !isSaving,
                            isError = mileageText.isNotEmpty() && (parsedMileage == null || isDecreased),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ToolOrange,
                                unfocusedBorderColor = WarmLine,
                                focusedTextColor = SoftInk,
                                unfocusedTextColor = SoftInk
                            ),
                            modifier = Modifier.weight(1f).tutorialTarget("mileage.input") { mileageText = "50600" }
                        )

                        // 계기판 OCR 카메라 원형 플로팅 액션 버튼 (FAB)
                        maha.garage.ui.components.CameraLensFab(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "차량 계기판의 누적 주행거리(ODO)를 촬영해 주세요.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                showOdometerScanner = true
                            },
                            size = 52.dp,
                            showBadge = true,
                            badgeText = "ODO",
                            contentDescription = "계기판 OCR 촬영"
                        )
                    }

                    if (drivenDelta > 0) {
                        Text(
                            text = "▲ 이번 주행: +%,d km 추가 누적".format(drivenDelta),
                            style = MaterialTheme.typography.labelSmall,
                            color = JackGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (isDecreased) {
                    Surface(
                        color = SketchRedSoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SketchRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "기존 주행거리(%,d km)보다 작습니다. 누적 주행거리를 확인하세요.".format(car.currentMileage),
                                style = MaterialTheme.typography.labelSmall,
                                color = SketchRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val tutorialAction5 = tutorialAction5@ {
                    if (isDecreased) {
                        showLowerConfirmation = true
                    } else {
                        onConfirm(newMileage, null, null, null)
                    }
                }
Button(
modifier = Modifier.tutorialTarget("mileage.save", tutorialAction5),
                onClick = tutorialAction5,
                enabled = parsedMileage != null && newMileage >= 0 && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isSaving) "저장 중…" else if (isDecreased) "낮은 거리 확인" else "저장 및 계속",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("취소", color = WarmGray)
                }
                TextButton(
                    onClick = { onSkip?.invoke() ?: onDismiss() },
                    enabled = !isSaving
                ) {
                    Text("건너뛰기", color = ToolOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    )

    if (showLowerConfirmation) {
        AlertDialog(
            onDismissRequest = { showLowerConfirmation = false },
            title = { Text("누적 주행거리를 확인해 주세요") },
            text = { Text("입력한 %,d km는 마지막 %,d km보다 작습니다. 정비·타이어 이력의 거리 계산을 보호하기 위해 감소한 값은 저장할 수 없습니다. 계기판의 누적 거리를 다시 입력해 주세요.".format(newMileage, car.currentMileage)) },
            confirmButton = { TextButton(onClick = { showLowerConfirmation = false }) { Text("다시 입력") } },
            dismissButton = { TextButton(onClick = { showLowerConfirmation = false; onDismiss() }) { Text("취소") } }
        )
    }

    if (showOdometerScanner) {
        OdometerScannerDialog(
            onOdometerParsed = { parsedKm ->
                mileageText = parsedKm.toString()
                showOdometerScanner = false
            },
            onDismiss = { showOdometerScanner = false }
        )
    }
}

/** 하위 호환성을 위한 단일 주행거리 콜백 오버로드 */
@Composable
fun UpdateMileageDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onConfirm: (newMileage: Int) -> Unit,
    onSkip: (() -> Unit)? = null,
    isSaving: Boolean = false,
    saveError: String? = null
) {
    UpdateMileageDialog(
        car = car,
        onDismiss = onDismiss,
        onConfirm = { km, _, _, _ -> onConfirm(km) },
        onSkip = onSkip,
        isSaving = isSaving,
        saveError = saveError
    )
}
