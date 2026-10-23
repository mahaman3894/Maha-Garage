package maha.garage.ui.vehicle

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import maha.garage.ui.theme.*

/**
 * 차량번호로 제원 자동조회 API를 호출하는 흔들리는 원형 플로팅 버튼
 */
@Composable
fun PlateLookupFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 사용자의 시선을 끌 수 있는 미세한 흔들림(Wobble/Shake) 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "plateShake")
    val shakeAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 0
                -12f at 160
                12f at 320
                -9f at 480
                9f at 640
                -5f at 800
                5f at 960
                0f at 1120
                0f at 2600 // 잠깐 멈춘 후 다시 흔들림 반복
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shakeAngle"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 0
                -4f at 160
                4f at 320
                -3f at 480
                3f at 640
                0f at 960
                0f at 2600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shakeOffset"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF0284C7),
        border = BorderStroke(2.dp, Color(0xFFBAE6FD)),
        shadowElevation = 10.dp,
        modifier = modifier
            .padding(bottom = 60.dp) // 하단 저장 버튼과 겹치지 않게 여유
            .size(64.dp)
            .graphicsLayer {
                rotationZ = shakeAngle
                translationX = shakeOffset
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF0284C7),
                            Color(0xFF0369A1)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "차량번호 제원 자동조회",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "제원조회",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 차량번호 입력 후 공공데이터 API를 통해 제원을 자동 조회하는 다이얼로그
 */
@Composable
fun PlateLookupDialog(
    initialPlate: String = "",
    onDismiss: () -> Unit,
    onLookup: (plateNumber: String, onComplete: (Boolean) -> Unit) -> Unit
) {
    var plateInput by remember { mutableStateOf(initialPlate) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val plateRegex = remember { Regex("^[0-9]{2,3}[가-힣]{1}[0-9]{4}$|^[가-힣]{2}[0-9]{2}[가-힣]{1}[0-9]{4}$") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDark,
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // 상단 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "차량번호 제원 자동 조회",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "공공데이터 API 실시간 자동 매칭",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "번호판만 입력하면 모델명, 배기량, 유종, 검사일이 자동 입력됩니다.",
                    fontSize = 12.5.sp,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = plateInput,
                    onValueChange = {
                        plateInput = it
                        errorMessage = null
                    },
                    label = { Text("차량 번호판 (예: 12가 3456)") },
                    placeholder = { Text("12가 3456 또는 123가 4567", color = TextSecondaryDark) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = StatusOverdue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val target = plateInput.replace(" ", "").trim()
                        if (!target.matches(plateRegex)) {
                            errorMessage = "올바른 번호판 형식이 아닙니다. (예: 12가 3456, 123가 4567)"
                        } else {
                            isSearching = true
                            errorMessage = null
                            onLookup(target) { success ->
                                isSearching = false
                                if (!success) {
                                    errorMessage = "조회된 차량 정보가 없습니다. 번호판을 확인해주세요."
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    },
                    enabled = !isSearching && plateInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = SurfaceVariantDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("공공데이터 조회 중...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("제원 조회 및 자동 입력", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
