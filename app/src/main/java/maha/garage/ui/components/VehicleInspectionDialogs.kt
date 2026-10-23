package maha.garage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.theme.*
import java.time.LocalDate

/**
 * 1. 자동차 검사 결과 등록 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionResultDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        inspectionDate: String,
        inspectionType: String,
        stationName: String?,
        isPassed: Boolean,
        nextExpiryDate: String?,
        retestDeadline: String?,
        cost: Int,
        memo: String?
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    var inspectionDate by remember { mutableStateOf(today.toString()) }
    var inspectionType by remember { mutableStateOf(car.inspectionType.ifBlank { "정기검사" }) }
    var stationName by remember { mutableStateOf("") }
    var isPassed by remember { mutableStateOf(true) }
    var nextExpiryDate by remember { mutableStateOf("") }
    var retestDeadline by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null, tint = GaragePrimaryLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("자동차 검사 결과 등록", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "실제 검사일과 결과표의 다음 유효기간 만료일을 저장하면 앱의 다음 알림 일정이 자동으로 갱신됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )

                // 검사일
                OutlinedTextField(
                    value = inspectionDate,
                    onValueChange = { inspectionDate = it },
                    label = { Text("실제 검사일 (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GaragePrimaryLight,
                        unfocusedBorderColor = BorderDark
                    )
                )

                // 검사 종류 (정기검사 vs 종합검사)
                Text("검사 종류", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("정기검사", "종합검사").forEach { type ->
                        val isSelected = inspectionType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) GaragePrimaryLight else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { inspectionType = type }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) GaragePrimaryLight else TextSecondaryDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 검사소 명칭
                OutlinedTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("검사소 명칭 (예: 강남검사소)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GaragePrimaryLight,
                        unfocusedBorderColor = BorderDark
                    )
                )

                // 적합 / 부적합 판정
                Text("판정 결과", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(true to "적합 (합격)", false to "부적합 (재검사 필요)").forEach { (passed, label) ->
                        val isSelected = isPassed == passed
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                if (passed) StatusNormal.copy(alpha = 0.2f) else StatusOverdue.copy(alpha = 0.2f)
                            } else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) {
                                if (passed) StatusNormal else StatusOverdue
                            } else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isPassed = passed }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) (if (passed) StatusNormal else StatusOverdue) else TextSecondaryDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // 판정 결과에 따른 기한 입력 (적합: 차기 만료일, 부적합: 재검사 기한)
                if (isPassed) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = nextExpiryDate,
                            onValueChange = { nextExpiryDate = it },
                            label = { Text("결과표의 다음 만료일 (필수, YYYY-MM-DD)") },
                            placeholder = { Text("검사결과표 확인 후 입력") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { nextExpiryDate = today.plusYears(2).toString() },
                                label = { Text("+2년 입력 (비사업용 승용 기본)", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = GaragePrimaryLight)
                            )
                            AssistChip(
                                onClick = { nextExpiryDate = today.plusYears(1).toString() },
                                label = { Text("+1년 입력 (사업용/화물 등)", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = TextSecondaryDark)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = retestDeadline,
                            onValueChange = { retestDeadline = it },
                            label = { Text("부적합 통지서 상 재검사 기한 (YYYY-MM-DD)") },
                            placeholder = { Text("재검사 기한 입력") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = StatusOverdue,
                                unfocusedBorderColor = BorderDark
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = { retestDeadline = today.plusDays(10).toString() },
                                label = { Text("+10일 입력 (통상 재검사 기간)", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = StatusOverdue)
                            )
                        }
                    }
                }

                // 검사비 (원)
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it.filter { c -> c.isDigit() }.take(9) },
                    label = { Text("검사비 (원)") },
                    placeholder = { Text("예: 56000 (최대 1억 원)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GaragePrimaryLight,
                        unfocusedBorderColor = BorderDark
                    )
                )

                // 비고
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모 (선택사항)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GaragePrimaryLight,
                        unfocusedBorderColor = BorderDark
                    )
                )

                val parsedInspectionDate = runCatching { LocalDate.parse(inspectionDate.trim()) }.getOrNull()
                val parsedNextExpiryDate = runCatching { LocalDate.parse(nextExpiryDate.trim()) }.getOrNull()
                val parsedRetestDeadline = runCatching { LocalDate.parse(retestDeadline.trim()) }.getOrNull()
                val parsedCost = costStr.toLongOrNull()

                val isInspectionDateFuture = parsedInspectionDate != null && parsedInspectionDate.isAfter(today)
                val isCostOverflow = costStr.isNotBlank() && (parsedCost == null || parsedCost > 100_000_000L)

                val dateErrorMessage = when {
                    inspectionDate.isNotBlank() && parsedInspectionDate == null -> "검사일을 올바른 형식(YYYY-MM-DD)으로 입력해 주세요."
                    isInspectionDateFuture -> "실제 검사일은 미래 날짜일 수 없습니다. 오늘(${today}) 또는 이전 날짜를 입력해 주세요."
                    isPassed && nextExpiryDate.isNotBlank() && parsedNextExpiryDate == null -> "차기 만료일을 올바른 형식(YYYY-MM-DD)으로 입력해 주세요."
                    isPassed && parsedInspectionDate != null && parsedNextExpiryDate != null && !parsedNextExpiryDate.isAfter(parsedInspectionDate) ->
                        "차기 만료일은 실제 검사일(${inspectionDate.trim()}) 이후 날짜여야 합니다."
                    !isPassed && retestDeadline.isNotBlank() && parsedRetestDeadline == null -> "재검사 기한을 올바른 형식(YYYY-MM-DD)으로 입력해 주세요."
                    !isPassed && parsedInspectionDate != null && parsedRetestDeadline != null && parsedRetestDeadline.isBefore(parsedInspectionDate) ->
                        "재검사 기한은 실제 검사일(${inspectionDate.trim()}) 이후 날짜여야 합니다."
                    isCostOverflow -> "검사비는 최대 1억 원 이하의 숫자로 입력해 주세요."
                    else -> null
                }

                if (dateErrorMessage != null) {
                    Text(
                        text = dateErrorMessage,
                        color = StatusOverdue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            val parsedInspectionDate = runCatching { LocalDate.parse(inspectionDate.trim()) }.getOrNull()
            val parsedNextExpiryDate = runCatching { LocalDate.parse(nextExpiryDate.trim()) }.getOrNull()
            val parsedRetestDeadline = runCatching { LocalDate.parse(retestDeadline.trim()) }.getOrNull()
            val parsedCost = costStr.toLongOrNull()

            val isInspectionDateValid = parsedInspectionDate != null && !parsedInspectionDate.isAfter(today)
            val isCostValid = costStr.isBlank() || (parsedCost != null && parsedCost in 0L..100_000_000L)
            val isTargetDateValid = if (isPassed) {
                parsedNextExpiryDate != null && parsedInspectionDate != null && parsedNextExpiryDate.isAfter(parsedInspectionDate)
            } else {
                parsedRetestDeadline != null && parsedInspectionDate != null && !parsedRetestDeadline.isBefore(parsedInspectionDate)
            }

            Button(
                onClick = {
                    val cost = if (isCostValid && costStr.isNotBlank()) {
                        (costStr.toLongOrNull() ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    } else 0
                    onConfirm(
                        inspectionDate.trim(),
                        inspectionType.trim(),
                        stationName.trim().ifEmpty { null },
                        isPassed,
                        if (isPassed) nextExpiryDate.trim().ifEmpty { null } else null,
                        if (!isPassed) retestDeadline.trim().ifEmpty { null } else null,
                        cost,
                        memo.trim().ifEmpty { null }
                    )
                },
                enabled = isInspectionDateValid && isTargetDateValid && isCostValid,
                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
            ) {
                Text("저장", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * 2. 자동차 보험 정보 수정 다이얼로그 (만료일 보존 및 유효성 검증)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInsuranceDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        company: String,
        startDate: String,
        endDate: String,
        emergencyCall: String
    ) -> Unit
) {
    EditInsuranceDialog(
        initialCompany = car.insuranceCompany.orEmpty(),
        initialStartDate = car.insuranceStartDate.orEmpty(),
        initialEndDate = car.insuranceEndDate.orEmpty(),
        initialEmergencyCall = car.insuranceEmergencyCall.orEmpty(),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInsuranceDialog(
    initialCompany: String = "",
    initialStartDate: String = "",
    initialEndDate: String = "",
    initialEmergencyCall: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        company: String,
        startDate: String,
        endDate: String,
        emergencyCall: String
    ) -> Unit
) {
    val commonInsurers = listOf(
        "동부화재" to "1588-0100",
        "삼성화재" to "1588-5114",
        "현대해상" to "1588-5656",
        "KB손해보험" to "1544-0114",
        "메리츠화재" to "1566-7711",
        "한화손해보험" to "1566-8000",
        "DB손해보험" to "1588-0100"
    )

    var selectedCompany by remember { mutableStateOf(initialCompany) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember {
        mutableStateOf(
            initialEndDate.ifEmpty {
                runCatching { LocalDate.parse(initialStartDate.trim()).plusYears(1).toString() }.getOrDefault("")
            }
        )
    }
    var emergencyCall by remember { mutableStateOf(initialEmergencyCall) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GarageAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("자동차 보험 가입정보 수정", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "보험 가입 정보 및 만료일을 수정합니다. 가입일로부터 1년 계산 버튼을 누르면 만료일이 자동 입력됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )

                Text("주요 보험사 빠른 선택", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonInsurers.take(3).forEach { (comp, num) ->
                        val isSelected = selectedCompany == comp
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GarageAccent.copy(alpha = 0.2f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) GarageAccent else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCompany = comp
                                    emergencyCall = num
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = comp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) GarageAccent else TextPrimaryDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonInsurers.drop(3).take(4).forEach { (comp, num) ->
                        val isSelected = selectedCompany == comp
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) GarageAccent.copy(alpha = 0.2f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) GarageAccent else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCompany = comp
                                    emergencyCall = num
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = comp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) GarageAccent else TextPrimaryDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 보험사명 입력 (직접 입력 가능)
                OutlinedTextField(
                    value = selectedCompany,
                    onValueChange = { selectedCompany = it },
                    label = { Text("보험사명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GarageAccent,
                        unfocusedBorderColor = BorderDark
                    )
                )

                // 가입 시작일
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { newStart ->
                            startDate = newStart
                            // 가입일이 YYYY-MM-DD 또는 YYYYMMDD 형식이면 만료일을 1년 뒤로 디폴트 자동 계산
                            val trimmed = newStart.trim()
                            val parsed = runCatching { LocalDate.parse(trimmed) }.getOrNull()
                            if (parsed != null) {
                                endDate = parsed.plusYears(1).toString()
                            } else if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
                                val formatted = "${trimmed.substring(0, 4)}-${trimmed.substring(4, 6)}-${trimmed.substring(6, 8)}"
                                val p = runCatching { LocalDate.parse(formatted) }.getOrNull()
                                if (p != null) {
                                    startDate = formatted
                                    endDate = p.plusYears(1).toString()
                                }
                            }
                        },
                        label = { Text("보험 가입일 (필수, YYYY-MM-DD)") },
                        placeholder = { Text("예: 2026-03-01") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = GarageAccent,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val today = LocalDate.now()
                                startDate = today.toString()
                                endDate = today.plusYears(1).toString()
                            },
                            label = { Text("오늘 날짜로 입력 (1년 자동)", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = GarageAccent)
                        )
                    }
                }

                // 만료일 (기존 만료일 보존 및 1년 자동 계산 기능)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("보험 만료일 (필수, YYYY-MM-DD)") },
                        placeholder = { Text("예: 2027-03-01") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = GarageAccent,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val p = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
                                if (p != null) {
                                    endDate = p.plusYears(1).toString()
                                }
                            },
                            label = { Text("가입일부터 1년 자동 계산", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = GarageAccent)
                        )
                    }
                }

                // 긴급출동 번호
                OutlinedTextField(
                    value = emergencyCall,
                    onValueChange = { emergencyCall = it },
                    label = { Text("긴급출동 전화번호 (선택사항)") },
                    placeholder = { Text("예: 1588-0000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = GarageAccent,
                        unfocusedBorderColor = BorderDark
                    )
                )

                val parsedStartDate = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
                val parsedEndDate = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()

                val insuranceDateErrorMessage = when {
                    startDate.isNotBlank() && parsedStartDate == null -> "보험 가입일을 올바른 형식(YYYY-MM-DD)으로 입력해 주세요."
                    endDate.isNotBlank() && parsedEndDate == null -> "보험 만료일을 올바른 형식(YYYY-MM-DD)으로 입력해 주세요."
                    parsedStartDate != null && parsedEndDate != null && !parsedEndDate.isAfter(parsedStartDate) ->
                        "보험 만료일은 가입일(${startDate.trim()}) 이후 날짜여야 합니다."
                    else -> null
                }

                if (insuranceDateErrorMessage != null) {
                    Text(
                        text = insuranceDateErrorMessage,
                        color = StatusOverdue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            val parsedStartDate = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
            val parsedEndDate = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()

            val isCompanyValid = selectedCompany.isNotBlank()
            val isStartDateValid = parsedStartDate != null
            val isEndDateValid = parsedEndDate != null && parsedStartDate != null && parsedEndDate.isAfter(parsedStartDate)

            Button(
                onClick = {
                    onConfirm(
                        selectedCompany.trim(),
                        startDate.trim(),
                        endDate.trim(),
                        emergencyCall.trim()
                    )
                },
                enabled = isCompanyValid && isStartDateValid && isEndDateValid,
                colors = ButtonDefaults.buttonColors(containerColor = GarageAccent)
            ) {
                Text("저장", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * 3. 대한민국 정기검사/종합검사 법정 주기 안내 다이얼로그 (한국교통안전공단 기준)
 */
@Composable
fun InspectionGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GaragePrimaryLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("자동차 검사 법정 주기 안내", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "대한민국 한국교통안전공단 기준 (2026년 10월 확인)\n" +
                            "※ 검사 유효기간 만료일 전 90일부터 후 31일까지 검사가 가능합니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = GaragePrimaryLight
                )

                val guideRows = listOf(
                    "일반 자가용 승용차 (신차)" to "공단 안내상 최초 5년, 이후 2년마다",
                    "일반 자가용 승용차 (중고차)" to "구매 시점과 관계없이 기존 검사 만료일을 승계, 이후 통상 2년마다",
                    "자가용 전기 승용차" to "같은 용도의 승용차 주기 동일 적용 (전기차 별도 면제 없음)",
                    "사업용 승용차" to "최초 2년, 이후 1년마다",
                    "비사업용 경형·소형 승합/화물차" to "차령 4년 이하 2년, 4년 초과 1년마다",
                    "중형·대형 승합차" to "통상 차령 8년 이하 1년, 초과 6개월마다",
                    "중형·대형 화물차·특수차" to "용도·규모·차령에 따라 1년 또는 6개월마다"
                )

                guideRows.forEach { (category, interval) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = interval,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "중고차는 구매일 기준으로 검사 주기가 다시 시작되지 않습니다. 자동차등록증 또는 한국교통안전공단에서 기존 검사 만료일을 꼭 확인해 주세요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDueSoon
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
            ) {
                Text("확인", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    )
}
