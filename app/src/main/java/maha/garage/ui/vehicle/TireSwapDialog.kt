package maha.garage.ui.vehicle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.components.TireModeBadge
import maha.garage.ui.theme.*

@Composable
fun TireSwapDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onConfirmSwap: (
        targetMode: String,
        swapMileage: Int,
        storageLocation: String?,
        frontTread: Double?,
        rearTread: Double?,
        swapCost: Long,
        notes: String?
    ) -> Unit
) {
    val defaultCurrentMode = car.currentTireMode
    val defaultTargetMode = if (defaultCurrentMode == "SUMMER") "WINTER" else "SUMMER"

    // 클릭 시 자리가 바뀌는 스왑 토글 상태
    var isSwapped by remember { mutableStateOf(false) }

    // 화면에 표시되는 탈거 대상과 신규 장착 대상 모드
    val activeToMode = defaultTargetMode

    var swapMileageText by remember { mutableStateOf(car.currentMileage.toString()) }
    var storageLocation by remember { mutableStateOf("") }
    var frontTreadText by remember { mutableStateOf("") }
    var rearTreadText by remember { mutableStateOf("") }
    var swapCostText by remember { mutableStateOf("") } // 기본 빈칸 시작
    var notes by remember { mutableStateOf("") }

    val swapMileage = swapMileageText.toIntOrNull() ?: car.currentMileage
    val drivenDistance = (swapMileage - car.lastSwapMileage).coerceAtLeast(0)

    // 화살표 회전 애니메이션
    val arrowRotation by animateFloatAsState(
        targetValue = if (isSwapped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "arrowRotation"
    )

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color.White,
        modifier = Modifier.border(1.5.dp, WarmLine, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = GarageAccent,
                modifier = Modifier
                    .size(36.dp)
                    .rotate(arrowRotation)
            )
        },
        title = {
            Text(
                text = "타이어 스왑 (계절 교체)",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                // 클릭 가능한 썸머 <-> 윈터 인터랙티브 스왑 박스
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSwapped) GarageAccent.copy(alpha = 0.6f) else WarmLine
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isSwapped = !isSwapped
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "터치하여 타이어 표시 위치 변경 ⇄",
                                style = MaterialTheme.typography.labelSmall,
                                color = GarageAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isSwapped) "전환됨" else "기본",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 자리 교체 애니메이션 적용 영역
                        AnimatedContent(
                            targetState = isSwapped,
                            transitionSpec = {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> if (targetState) fullWidth / 2 else -fullWidth / 2 },
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                ) + fadeIn(animationSpec = tween(250)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> if (targetState) -fullWidth / 2 else fullWidth / 2 },
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeOut(animationSpec = tween(200))
                                    )
                            },
                            label = "tireModeSwapAnimation"
                        ) { swapped ->
                            val currentLeft = if (!swapped) defaultCurrentMode else defaultTargetMode
                            val currentRight = if (!swapped) defaultTargetMode else defaultCurrentMode

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (!swapped) "현재 (탈거 보관)" else "장착 대상",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TireModeBadge(mode = currentLeft)
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = GarageAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GarageAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "스왑 전환",
                                        tint = GarageAccent,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(24.dp)
                                            .rotate(arrowRotation)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (!swapped) "장착 대상" else "현재 (탈거 보관)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GarageAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TireModeBadge(mode = currentRight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "현재 장착 기간 동안 약 %,d km 주행했습니다. 스왑 확정 시 이전 타이어의 누적 거리에 자동 가산됩니다."
                                .format(drivenDistance),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                OutlinedTextField(
                    value = swapMileageText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) swapMileageText = it },
                    label = { Text("스왑 시점 주행거리 (km)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GarageAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    label = { Text("탈거 타이어 보관 장소") },
                    placeholder = { Text("예: 타이어프로 강남점, 자택 창고") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GarageAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = frontTreadText,
                        onValueChange = { frontTreadText = it },
                        label = { Text("전륜 잔여홈 (mm)") },
                        placeholder = { Text("6.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rearTreadText,
                        onValueChange = { rearTreadText = it },
                        label = { Text("후륜 잔여홈 (mm)") },
                        placeholder = { Text("6.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = swapCostText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) swapCostText = it },
                    label = { Text("교체 공임비 (KRW)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("특이사항 및 메모") },
                    placeholder = { Text("예: 휠 밸런스 조정 완료") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val toWinter = activeToMode == "WINTER"
            val confirmGrad = if (toWinter) {
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)))
            } else {
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C)))
            }
            val isEnabled = !isSaving && swapMileageText.toIntOrNull() != null && swapMileage >= car.lastSwapMileage && swapMileage >= 0 &&
                listOf(frontTreadText, rearTreadText).all { it.isBlank() || (it.toDoubleOrNull()?.let { n -> n.isFinite() && n >= 0 } == true) } &&
                (swapCostText.isBlank() || swapCostText.toLongOrNull() != null)

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isEnabled) {
                        onConfirmSwap(
                            activeToMode,
                            swapMileage,
                            storageLocation.ifBlank { null },
                            frontTreadText.toDoubleOrNull(),
                            rearTreadText.toDoubleOrNull(),
                            swapCostText.toLongOrNull() ?: 0L,
                            notes.ifBlank { null }
                        )
                        onDismiss()
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isEnabled) Color.Transparent else Color.LightGray.copy(alpha = 0.5f),
                shadowElevation = if (isEnabled) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .then(if (isEnabled) Modifier.background(confirmGrad) else Modifier.background(Color(0xFFCBD5E1)))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (toWinter) Icons.Default.AcUnit else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaving) "저장 중…" else "스왑 확정 및 기록",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소", color = TextSecondaryDark)
            }
        }
    )
}

