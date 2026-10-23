package maha.garage.ui.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
 * 타이어 관리 섹션에서 썸머/사계절 또는 윈터 타이어 박스를 클릭했을 때
 * 해당 타이어의 누적 주행거리(km)를 직접 수정/보정할 수 있는 다이얼로그
 */
@Composable
fun EditTireMileageDialog(
    car: CarEntity,
    tireMode: String, // "SUMMER" or "WINTER"
    currentLiveKm: Int,
    onDismiss: () -> Unit,
    onConfirm: (newTargetKm: Int) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null
) {
    val isSummer = tireMode == "SUMMER"
    val isMounted = car.currentTireMode == tireMode
    val titleText = if (isSummer) "☀️ 썸머 / 사계절 타이어 누적거리 수정" else "❄️ 윈터(스노우) 타이어 누적거리 수정"
    val accentColor = if (isSummer) ToolOrange else Color(0xFF1D4ED8)
    val iconVector = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit

    var inputKmText by rememberSaveable(car.id, tireMode) {
        mutableStateOf(currentLiveKm.toString())
    }

    val parsedKm = inputKmText.toIntOrNull()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isMounted) accentColor.copy(alpha = 0.12f) else WarmGray.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isMounted) "현재 차량에 장착 운용 중" else "탈거 보관 중",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMounted) accentColor else WarmGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "타이어를 교체하고 늦게 등록했거나 실제 주행거리와 차이가 있는 경우, 현재 타이어의 실제 총 주행거리(km)를 직접 입력하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )

                // 현재 기록된 거리 안내 카드
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, WarmLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "현재 계산된 누적거리",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray
                        )
                        Text(
                            text = "%,d km".format(currentLiveKm),
                            style = MaterialTheme.typography.titleSmall,
                            color = SoftInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 새 누적거리 입력 필드
                OutlinedTextField(
                    value = inputKmText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            inputKmText = input
                        }
                    },
                    label = { Text("실제 누적 주행거리 (km)") },
                    placeholder = { Text("예: %,d".format(currentLiveKm)) },
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = WarmLine,
                        focusedTextColor = SoftInk,
                        unfocusedTextColor = SoftInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (isMounted) {
                    Text(
                        text = "※ 현재 장착 중인 타이어는 수정된 거리를 기준으로 앞으로 차량이 주행할 때마다 자동 가산됩니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedKm != null && parsedKm >= 0) {
                        onConfirm(parsedKm)
                    }
                },
                enabled = parsedKm != null && parsedKm >= 0 && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isSaving) "저장 중…" else "수정 저장",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소", color = WarmGray)
            }
        }
    )
}
