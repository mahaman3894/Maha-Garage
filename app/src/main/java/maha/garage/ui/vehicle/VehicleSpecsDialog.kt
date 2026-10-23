package maha.garage.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.theme.*

@Composable
fun VehicleSpecsDialog(
    car: CarEntity,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. 다이얼로그 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GaragePrimaryLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = GaragePrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "차량 상세 제원",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${car.make} ${car.modelName} ${car.trimName ?: ""}".trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderDark)
                Spacer(modifier = Modifier.height(14.dp))

                // 스크롤 가능한 본문 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. 타이밍 구동계 (Timing Drive) 전용 하이라이트 카드
                    TimingSystemSpecCard(car)

                    // 3. 파워트레인 및 성능 제원 카드
                    PowertrainSpecCard(car)

                    // 4. 차체 치수 및 연비 카드
                    DimensionsAndEfficiencyCard(car)

                    // 5. 차량 식별 및 등록 정보 카드
                    IdentificationSpecCard(car)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 닫기 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("확인", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimingSystemSpecCard(car: CarEntity) {
    val (badgeText, badgeColor, title, desc) = when (car.timingType) {
        "CHAIN" -> Quad(
            "⛓️ 타이밍 체인 (Timing Chain)",
            StatusNormal,
            "금속 타이밍 체인 방식",
            car.timingDesc ?: "본 차량은 금속 타이밍체인 구동 방식입니다. 엔진오일로 상시 윤활되므로 체인 자체는 반영구적 부품입니다. 주기적인 엔진오일 관리와 함께, 외부 구동벨트(발전기/에어컨 겉벨트 및 오토텐셔너)를 8~10만km 주기로 점검 및 교환하시기 바랍니다."
        )
        "BELT" -> Quad(
            "🔄 타이밍 벨트 (Timing Belt)",
            StatusDueSoon,
            "고무 타이밍 벨트 방식 (정기교환 필수)",
            car.timingDesc ?: "본 차량은 고무 재질 타이밍벨트 구동 방식입니다. 80,000~100,000km 또는 5년 주기 교환이 필수적이며, 벨트 노후 파손 시 밸브/피스톤 충돌로 인한 치명적 엔진 손상이 발생하므로 워터펌프/텐셔너와 함께 주기 내 반드시 교환하세요."
        )
        "NA" -> Quad(
            "⚡ 해당없음 (BEV / Electric)",
            StatusNormal,
            "전기모터 구동 (벨트/체인 없음)",
            "내연기관이 없는 순수 전기차로 타이밍벨트 및 타이밍체인이 존재하지 않습니다."
        )
        else -> Quad(
            "⚙️ 타이밍 구동계",
            GaragePrimaryLight,
            "타이밍 시스템",
            car.timingDesc ?: "엔진 캠축 구동계 점검 및 겉벨트 8~10만km 교환 권장."
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "타이밍 구동 계통 (소모품 매칭)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f
            )
        }
    }
}

@Composable
private fun PowertrainSpecCard(car: CarEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = GaragePrimaryLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "파워트레인 및 동력 성능",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            SpecGridRow("엔진 / 동력계", car.powertrain.displayName, "배기량", car.displacement.orDash())
            Spacer(modifier = Modifier.height(8.dp))
            SpecGridRow("최고출력", car.power.orDash(), "최대토크", car.torque.orDash())
            Spacer(modifier = Modifier.height(8.dp))
            SpecGridRow("변속기", car.transmission.orDash(), "구동방식", car.driveSystem.orDash())
        }
    }
}

@Composable
private fun DimensionsAndEfficiencyCard(car: CarEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, tint = GarageAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "치수 및 에너지 효율",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            SpecItem("공인 연비", car.fuelEconomy.orDash())
            Spacer(modifier = Modifier.height(8.dp))
            SpecItem("차체 치수 (전장x전폭x전고)", car.dimensions.orDash())
        }
    }
}

@Composable
private fun IdentificationSpecCard(car: CarEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "차량 등록 및 관리 정보",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            SpecGridRow("차량번호", car.carNumber, "차종 분류", car.vehicleClass.displayName)
            Spacer(modifier = Modifier.height(8.dp))
            SpecGridRow("연식", "${car.modelYear}년식", "최초등록일", car.firstRegistrationDate.orDash())
            Spacer(modifier = Modifier.height(8.dp))
            SpecGridRow("현재 주행거리", "%,d km".format(car.currentMileage), "운행 조건", if (car.isSevereCondition) "가혹 조건" else "표준 조건")
        }
    }
}

@Composable
private fun SpecGridRow(label1: String, val1: String, label2: String, val2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            SpecItem(label1, val1)
        }
        Box(modifier = Modifier.weight(1f)) {
            SpecItem(label2, val2)
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
    }
}

private fun String?.orDash(): String = if (this.isNullOrBlank()) "-" else this

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
