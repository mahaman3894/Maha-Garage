package maha.garage.ui.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.engine.ConsumableGuideProvider
import maha.garage.ui.theme.*

/**
 * 소모품 카드를 눌렀을 때 나타나는 입력 모달 내에서
 * [소모품교환정보] 버튼을 눌렀을 때 표시되는 상세 안내 다이얼로그
 * - 해당 부품을 교체해야 하는 이유
 * - 미교체 시 발생하는 문제 (치명적 고장/위험)
 * - 교체 시기 전조 증상
 * - 전기차, 하이브리드, 가솔린, 디젤/SUV, 버스/상용차 등 차량 기종별 맞춤 조언
 */
@Composable
fun ConsumableReplacementGuideDialog(
    car: CarEntity,
    itemCode: String,
    onDismiss: () -> Unit
) {
    val guide = ConsumableGuideProvider.getGuide(car, itemCode)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, WarmLine),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. 다이얼로그 헤더 (아이콘 + 부품명 + 차종 뱃지 + 닫기 버튼)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GarageAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = GarageAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = guide.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ToolYellowSoft,
                                border = BorderStroke(1.dp, ToolYellow.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = guide.vehicleCategoryLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftInk
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. 가이드 본문 스크롤 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 차량 식별 정보 서브 배너
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${car.make} ${car.modelName} (${car.carNumber}) • ${car.powertrain.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // A. 권장 교체 주기 카드
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = JackGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = JackGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "권장 교체 주기",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = guide.intervalText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftInk
                                )
                            }
                        }
                    }

                    // B. 교체 시기 전조 증상
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = ToolOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "교체 시기 전조 증상",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            guide.warningSymptoms.forEach { symptom ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(ToolOrange)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = symptom,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftInk,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // C. 교체해야 하는 이유 & 미교체 시 발생하는 문제 (경고 강조)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dangerous,
                                    contentDescription = null,
                                    tint = StatusOverdue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "교체 이유 & 미교체 시 발생하는 위험",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            guide.problemRisks.forEach { risk ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(StatusOverdue)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = risk,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = SoftInk,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // D. [차량 기종 맞춤] 전문가 정비 조언
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "마하개러지 기종별 맞춤 조언",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = guide.expertAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftInk,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 하단 확인 닫기 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GarageAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "확인",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
