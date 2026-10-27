package maha.garage.ui.vehicle

import maha.garage.ui.tutorial.*

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
                    contentDescription = "차량번호조회",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "차량번호조회",
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("해당 서비스는 준비중 입니다.", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.tutorialTarget("plate.notice")) },
        text = { Text("개인정보 수집관련 이슈", fontSize = 13.sp, color = TextSecondaryDark) },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.tutorialTarget("plate.close", onDismiss)) {
                Text("확인")
            }
        }
    )
}
