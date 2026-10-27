package maha.garage.ui.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.data.local.entity.FuelRecordEntity
import maha.garage.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmartFuelStatsCard(
    monthlyExpense: Int,
    monthlyFuelAmount: Double,
    costPerKm: Int?,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ToolYellowSoft,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = ToolOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "스마트 주유 통계",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SoftInk
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "이달 주유비",
                    value = "${numberFormat.format(monthlyExpense)}원",
                    sub = String.format(Locale.KOREA, "총 %.1f L", monthlyFuelAmount),
                    color = ToolOrange
                )
                StatItem(
                    label = "1km당 유류비",
                    value = if (costPerKm != null) "${numberFormat.format(costPerKm)}원/km" else "- 원",
                    sub = "주행 가성비",
                    color = Color(0xFF2563EB)
                )
                StatItem(
                    label = "평균 단가",
                    value = if (monthlyFuelAmount > 0) "${numberFormat.format((monthlyExpense / monthlyFuelAmount).toInt())}원" else "- 원",
                    sub = "리터당",
                    color = SoftInk
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    sub: String,
    color: Color
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = WarmGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, fontSize = 10.sp, color = WarmGray.copy(alpha = 0.8f))
    }
}

@Composable
fun FuelRecordItemCard(
    record: FuelRecordEntity,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    val dateString = dateFormat.format(Date(record.refuelDate))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 상단 행: 날짜, 주유소, 연비 뱃지 & 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.gasStationName ?: "일반 주유소",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftInk
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.isMissedPrevious) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF7ED)
                        ) {
                            Text(
                                text = "직전 주유 누락",
                                color = Color(0xFFC2410C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (record.calculatedEconomy != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0F2FE)
                        ) {
                            Text(
                                text = String.format(Locale.KOREA, "%.2f km/L", record.calculatedEconomy),
                                color = Color(0xFF0369A1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (!record.isFull) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "부분 주유",
                                color = Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "기준점 생성",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "기록 삭제",
                            tint = WarmGray.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 하단 행: 주행거리, 주유량 & 단가, 총 금액
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = WarmGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${numberFormat.format(record.odometer)} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftInk
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.KOREA, "%.2fL (@%s원)", record.fuelAmount, numberFormat.format(record.unitPrice)),
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${numberFormat.format(record.totalPrice)}원",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ToolOrange
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFuelRecordCard(
    onAddRefuelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ToolYellowSoft,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = ToolOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "등록된 주유 기록이 없습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "주유 시 '가득 주유'를 체크하고 기록해보세요.\n연비가 자동으로 산출되고 계측학적으로 누적 분석됩니다.",
                fontSize = 12.sp,
                color = WarmGray,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddRefuelClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange)
            ) {
                Text("+ 첫 주유 기록 등록하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}
