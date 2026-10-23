package maha.garage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import maha.garage.data.model.enums.DueStatus
import maha.garage.data.model.enums.SourceType
import maha.garage.ui.theme.*

@Composable
fun StatusBadge(status: DueStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        DueStatus.OVERDUE, DueStatus.DUE_NOW -> Triple(StatusOverdue.copy(alpha = 0.15f), StatusOverdue, "교체 필요")
        DueStatus.DUE_SOON -> Triple(StatusDueSoon.copy(alpha = 0.15f), StatusDueSoon, "교체 예정")
        DueStatus.INSPECTION_DUE -> Triple(StatusInspection.copy(alpha = 0.15f), StatusInspection, "점검 필요")
        DueStatus.NORMAL -> Triple(StatusNormal.copy(alpha = 0.15f), StatusNormal, "정상")
        DueStatus.NEEDS_DATA -> Triple(StatusNeutral.copy(alpha = 0.15f), StatusNeutral, "기록 없음")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SourceBadge(sourceType: SourceType, modifier: Modifier = Modifier) {
    val (label, tint) = when (sourceType) {
        SourceType.VERIFIED_OEM -> "OEM 공시 정비표" to StatusNormal
        SourceType.USER_OVERRIDE -> "사용자 직접 설정" to GarageAccent
        SourceType.OEM_FAMILY -> "동일 파워트레인 기준" to GaragePrimaryLight
        SourceType.GENERIC_ESTIMATE -> "표준 추천치" to TextSecondaryDark
        SourceType.NOT_CONFIGURED -> "기준 미확인" to StatusDueSoon
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = SurfaceVariantDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sourceType == SourceType.VERIFIED_OEM) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ProgressGauge(
    ratio: Float,
    status: DueStatus,
    modifier: Modifier = Modifier
) {
    val progressColor = when (status) {
        DueStatus.OVERDUE, DueStatus.DUE_NOW -> StatusOverdue
        DueStatus.DUE_SOON -> StatusDueSoon
        DueStatus.INSPECTION_DUE -> StatusInspection
        DueStatus.NORMAL -> GaragePrimaryLight
        DueStatus.NEEDS_DATA -> StatusNeutral
    }

    val displayRatio = ratio.coerceIn(0f, 1f)

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { displayRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = BorderDark
        )
    }
}

@Composable
fun TireModeBadge(mode: String, modifier: Modifier = Modifier) {
    val isSummer = mode == "SUMMER"
    // 윈터/스노우: 라이트스카이블루(0xFF38BDF8) -> 로얄블루(0xFF1D4ED8)
    // 썸머: 대칭되는 앰버(0xFFF59E0B) -> 선셋오렌지(0xFFEA580C)
    val gradientBrush = if (isSummer) {
        androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
        )
    } else {
        androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8))
        )
    }
    val text = if (isSummer) "섬머 / 사계절" else "윈터 (스노우)"

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }
        }
        action?.invoke()
    }
}
