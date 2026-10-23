package maha.garage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.engine.*
import maha.garage.ui.theme.*
import java.time.LocalDate

/** Refresh on resume and at midnight, even when the Room row has not changed. */
@Composable
fun rememberRenewalToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { delay(60_000); today = LocalDate.now() } }
    return today
}

@Composable
fun VehicleRenewalCards(car: CarEntity, onInsuranceClick: () -> Unit, onInspectionClick: () -> Unit,
    modifier: Modifier = Modifier, today: LocalDate = rememberRenewalToday()) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= 560.dp && fontScale <= 1.25f) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsuranceRenewalCard(car, today, onInsuranceClick, Modifier.weight(1f))
                InspectionRenewalCard(car, today, onInspectionClick, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InsuranceRenewalCard(car, today, onInsuranceClick, Modifier.fillMaxWidth())
                InspectionRenewalCard(car, today, onInspectionClick, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun InsuranceRenewalCard(car: CarEntity, today: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    RenewalCard("보험가입정보", car.insuranceCompany?.takeIf { it.isNotBlank() } ?: "보험사 미등록",
        "가입일", car.insuranceStartDate, "갱신일 · 만기일", car.insuranceEndDate,
        RenewalDatePolicy.insurance(car, today), "만기일 한 달 전부터 갱신을 준비해 주세요.", onClick, modifier)
}

@Composable
private fun InspectionRenewalCard(car: CarEntity, today: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    val retest = RenewalDatePolicy.parse(car.retestDeadline) != null
    val evaluation = RenewalDatePolicy.inspection(car, today)
    val legal = VehicleComplianceCalculator.evaluateInspection(car, today)
    val note = when {
        retest -> "재검사 기한 내에 검사 결과를 등록해 주세요."
        evaluation.date == null -> "공식 검사 날짜를 확인하고 등록해 주세요."
        car.isInspectionDateEstimated -> "예상 날짜입니다. 공식 검사 날짜를 확인해 주세요."
        else -> "최종 수검기한 ${RenewalDatePolicy.display(legal.finalDeadlineStr)}"
    }
    RenewalCard(if (retest) "자동차 재검사" else "자동차 정기검사", car.inspectionType.ifBlank { "검사 정보" },
        "최근 검사일", car.lastInspectionDate, if (retest) "재검사 기한" else "다음 검사 · 만료일",
        if (retest) car.retestDeadline else car.inspectionEndDate, evaluation, note, onClick, modifier)
}

@Composable
private fun RenewalCard(title: String, subtitle: String, startLabel: String, start: String?, dueLabel: String, due: String?,
    evaluation: RenewalDateEvaluation, note: String, onClick: () -> Unit, modifier: Modifier) {
    val color = when (evaluation.state) {
        RenewalDateState.MISSING -> TextSecondaryDark
        RenewalDateState.NORMAL -> GarageAccent
        RenewalDateState.WARNING -> StatusDueSoon
        RenewalDateState.EXPIRED -> StatusOverdue
    }
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                }
                Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                    Text(evaluation.badge, Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = color.copy(alpha = 0.2f))
            val scale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 260.dp && scale <= 1.25f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateField(startLabel, start, TextPrimaryDark, Modifier.weight(1f))
                        DateField(dueLabel, due, color, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DateField(startLabel, start, TextPrimaryDark)
                        DateField(dueLabel, due, color)
                    }
                }
            }
            Text(note, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            Text("눌러서 정보 변경", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun DateField(label: String, value: String?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
        Spacer(Modifier.height(4.dp))
        Text(RenewalDatePolicy.display(value), style = MaterialTheme.typography.titleMedium,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** Garage summary follows the same date and color policy as the detail cards. */
@Composable
fun VehicleRenewalSummary(car: CarEntity) {
    if (car.isArchived) return
    val today = rememberRenewalToday()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for ((label, value) in listOf("보험" to RenewalDatePolicy.insurance(car, today), "검사" to RenewalDatePolicy.inspection(car, today))) {
            val color = when (value.state) {
                RenewalDateState.MISSING -> TextSecondaryDark
                RenewalDateState.NORMAL -> GarageAccent
                RenewalDateState.WARNING -> StatusDueSoon
                RenewalDateState.EXPIRED -> StatusOverdue
            }
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                Text("$label ${value.date?.toString()?.replace('-', '.') ?: "날짜 미등록"}",
                    Modifier.padding(horizontal = 6.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
