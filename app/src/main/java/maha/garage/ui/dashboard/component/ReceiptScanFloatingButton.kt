package maha.garage.ui.dashboard.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.ui.theme.*

/**
 * 연비/주유 페이지 전용 카메라 렌즈 모양 플로팅 자동등록 버튼 (Float Box)
 */
@Composable
fun ReceiptScanFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 렌즈 광택 미세 애니메이션 (호흡 효과)
    val infiniteTransition = rememberInfiniteTransition(label = "lensGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1E232A), // 세련된 다크 메탈릭 바디
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    ToolOrange.copy(alpha = 0.9f),
                    Color(0xFFFFB74D),
                    ToolOrange.copy(alpha = 0.6f)
                )
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .padding(bottom = 8.dp, end = 4.dp)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카메라 렌즈 형상 위젯 (Camera Lens Graphic)
            CameraLensGraphic(glowAlpha = glowAlpha)

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "카메라 자동등록",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ToolOrange
                    ) {
                        Text(
                            text = "OCR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "영수증 촬영으로 즉시 입력",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * 정교하게 렌더링된 카메라 렌즈 그래픽
 * - 외곽 메탈 배럴 림 (Metallic Knurling Ring)
 * - 조리개 링 (Aperture Accent Ring)
 * - 광학 멀티코팅 반사 글래스 (Optical Multi-Coated Glass Reflection)
 * - 중앙 렌즈 조리개/카메라 아이콘
 */
@Composable
private fun CameraLensGraphic(glowAlpha: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            // 1. 렌즈 외경 텍스처 (다크 흑색 메탈 배럴)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF334155),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            // 2. 조리개 금색/오렌지 알루미늄 가공 림
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ToolOrange,
                        Color(0xFFFFE082),
                        ToolOrange,
                        Color(0xFFD84315),
                        ToolOrange
                    )
                ),
                shape = CircleShape
            )
    ) {
        // 3. 광학 렌즈 글래스 코팅 반사 효과 (Canvas)
        Canvas(modifier = Modifier.size(36.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // 광학 유리 베이스 (심도 있는 딥 블루/시안 반사)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0B192C),
                        Color(0xFF000000)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // 렌즈 조리개 날개 가이드 링
            drawCircle(
                color = Color(0xFFFF9800).copy(alpha = 0.35f),
                radius = radius * 0.72f,
                style = Stroke(width = 1.dp.toPx())
            )

            // 렌즈 상단 하이라이트 글레어 (빛 반사 곡선)
            drawArc(
                color = Color(0xFF67E8F9).copy(alpha = glowAlpha * 0.45f),
                startAngle = 200f,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 4. 중앙 카메라 렌즈 코어 아이콘
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "카메라 렌즈",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
