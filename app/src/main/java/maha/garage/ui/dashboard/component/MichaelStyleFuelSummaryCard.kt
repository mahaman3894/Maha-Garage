package maha.garage.ui.dashboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import java.util.Locale

@Composable
fun MichaelStyleFuelSummaryCard(
    recentEconomy: Double?,        // 최근 구간 실연비 (km/L)
    averageEconomy: Double?,       // 누적 평균 실연비 (km/L)
    officialEconomy: Double?,      // 공인 연비 (km/L)
    recentEconomies: List<Double>, // 최근 연비 변동 추이 (과거 -> 최신)
    onAddRefuelClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. 헤더 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⛽ 실주행 연비",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Full-to-Full",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (onTogglePin != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "차량 고정 해제" else "차량 고정",
                                tint = if (isPinned) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                FilledTonalButton(
                    onClick = onAddRefuelClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF60A5FA)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ 주유 기록", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 주요 3대 메트릭 (최근 연비 / 누적 평균 / 공인 대비)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(
                    title = "최근 연비",
                    value = recentEconomy?.let { String.format(Locale.KOREA, "%.2f km/L", it) } ?: "- km/L",
                    highlight = true
                )
                HorizontalDivider(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .height(38.dp)
                        .width(1.dp)
                )
                MetricColumn(
                    title = "누적 평균",
                    value = averageEconomy?.let { String.format(Locale.KOREA, "%.2f km/L", it) } ?: "- km/L"
                )
                HorizontalDivider(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .height(38.dp)
                        .width(1.dp)
                )
                MetricColumn(
                    title = "공인 대비",
                    value = if (recentEconomy != null && officialEconomy != null && officialEconomy > 0.0) {
                        val diff = recentEconomy - officialEconomy
                        val prefix = if (diff >= 0) "+" else ""
                        String.format(Locale.KOREA, "%s%.1f km/L", prefix, diff)
                    } else if (officialEconomy != null && officialEconomy > 0.0) {
                        String.format(Locale.KOREA, "공인 %.1f", officialEconomy)
                    } else {
                        "-"
                    },
                    subtext = if (recentEconomy != null && officialEconomy != null && officialEconomy > 0.0) {
                        val eff = (recentEconomy / officialEconomy) * 100.0
                        String.format(Locale.KOREA, "효율 %.0f%%", eff)
                    } else null
                )
            }

            // 3. 미니 연비 추이 네온 차트
            if (recentEconomies.size >= 2) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "최근 연비 추이 (${recentEconomies.size}회)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.KOREA, "최고 %.1f / 최저 %.1f", recentEconomies.maxOrNull() ?: 0.0, recentEconomies.minOrNull() ?: 0.0),
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NeonFuelTrendChart(
                    economies = recentEconomies,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 '가득(Full)' 주유 2회 이상 기록 시 실연비 추이 그래프가 나타납니다.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    highlight: Boolean = false,
    subtext: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = if (highlight) Color(0xFF38BDF8) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtext != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NeonFuelTrendChart(
    economies: List<Double>,
    modifier: Modifier = Modifier
) {
    if (economies.isEmpty()) return

    val minVal = (economies.minOrNull() ?: 0.0) * 0.9
    val maxVal = ((economies.maxOrNull() ?: 20.0) * 1.1).coerceAtLeast(minVal + 1.0)
    val valRange = (maxVal - minVal).coerceAtLeast(0.1)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingX = 16f
        val paddingY = 14f
        val chartW = width - (paddingX * 2)
        val chartH = height - (paddingY * 2)

        val points = economies.mapIndexed { index, value ->
            val x = paddingX + (index.toFloat() / (economies.size - 1).coerceAtLeast(1)) * chartW
            val normalizedY = ((value - minVal) / valRange).toFloat().coerceIn(0f, 1f)
            val y = paddingY + chartH * (1f - normalizedY)
            Offset(x, y)
        }

        // 1. 하단 영역 그라데이션 필 (Glow Fill)
        val fillPath = Path().apply {
            moveTo(points.first().x, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.25f),
                    Color(0xFF38BDF8).copy(alpha = 0.0f)
                ),
                startY = paddingY,
                endY = height
            )
        )

        // 2. 부드러운 라인 그리기
        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }
        drawPath(
            path = strokePath,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. 각 데이터 포인트 점 그리기
        points.forEachIndexed { i, pt ->
            // 외부 은은한 글로우 원
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                radius = 5.dp.toPx(),
                center = pt
            )
            // 내부 하얀/스카이블루 코어 점
            drawCircle(
                color = if (i == points.lastIndex) Color(0xFF38BDF8) else Color.White,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}
