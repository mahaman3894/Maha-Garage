package maha.garage.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.ui.theme.ToolOrange

/**
 * 카메라 렌즈 형상의 원형 플로팅 액션 버튼 (Circular Camera Lens FAB)
 * - 광학 렌즈 멀티코팅 반사 글래스
 * - 조리개 메탈 림
 * - 호흡하는 렌즈 광택 애니메이션
 * - 'OCR' 배지 지원
 */
@Composable
fun CameraLensFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    icon: ImageVector = Icons.Default.PhotoCamera,
    showBadge: Boolean = true,
    badgeText: String = "OCR",
    contentDescription: String = "계기판 카메라 OCR 촬영"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lensGlowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lensGlowAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 원형 본체 버튼
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .shadow(elevation = 8.dp, shape = CircleShape)
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
                            Color(0xFFFFD54F),
                            ToolOrange,
                            Color(0xFFD84315),
                            ToolOrange
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            // 3. 광학 렌즈 글래스 코팅 반사 효과
            Canvas(modifier = Modifier.size(size * 0.82f)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.width / 2f

                // 심도 있는 딥 블루/시안 광학 유리 반사
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

                // 렌즈 조리개 가이드 링
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = 0.35f),
                    radius = radius * 0.72f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // 상단 하이라이트 글레어
                drawArc(
                    color = Color(0xFF67E8F9).copy(alpha = glowAlpha * 0.5f),
                    startAngle = 200f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 4. 중앙 카메라 아이콘
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.45f)
            )
        }

        // 5. 상단 OCR 배지
        if (showBadge) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ToolOrange,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-3).dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
