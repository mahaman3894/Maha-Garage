package maha.garage.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.local.entity.ServiceVisitEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.model.ItemEvaluation
import maha.garage.ui.components.StatusBadge
import maha.garage.ui.theme.*
import java.time.LocalDate
import java.util.UUID
import maha.garage.domain.engine.MaintenanceInputPolicy

/**
 * 소모품 정보 등록 / 교체 기록 / 주기 설정 통합 다이얼로그
 * - 엔진오일: 오일필터 동시/개별 교체 선택 기능
 * - 브레이크 패드: 전륜 / 후륜 / 전체 교체 선택 기능
 * - 미등록 품목: 입력대기 상태에서 교체이력 등록 또는 주기 설정
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumableActionDialog(
    car: CarEntity,
    itemCode: String,
    evaluations: List<ItemEvaluation>,
    onDismiss: () -> Unit,
    onSaveVisitWithLines: (ServiceVisitEntity, List<ServiceLineEntity>, () -> Unit, (String) -> Unit) -> Unit,
    onSaveCustomRules: (List<MaintenanceRuleEntity>, () -> Unit, (String) -> Unit) -> Unit
) {
    val isEngineOil = itemCode == "ENGINE_OIL"
    val isBrakePad = itemCode == "BRAKE_PAD" || itemCode == "BRAKE_PAD_FRONT" || itemCode == "BRAKE_PAD_REAR"

    val currentEval = evaluations.find { it.itemCode == itemCode }
        ?: evaluations.find { it.itemCode == "BRAKE_PAD_FRONT" }
        ?: evaluations.firstOrNull()

    var selectedTab by remember { mutableStateOf(0) } // 0: 교체/정비 기록, 1: 교체주기 설정

    // 공통 입력값
    var serviceDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var serviceMileage by remember { mutableStateOf(car.currentMileage.toString()) }
    var workshop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // 엔진오일 전용 상태
    var replaceEngineOil by remember { mutableStateOf(true) }
    var replaceOilFilter by remember { mutableStateOf(true) }
    var oilBrand by remember { mutableStateOf("") }
    var oilProductName by remember { mutableStateOf("") }
    var oilViscosity by remember { mutableStateOf("5W-30") }
    var oilCapacityL by remember { mutableStateOf("5.0") }
    var engineOilCost by remember { mutableStateOf("") }

    // 브레이크 패드 전용 상태
    var brakePadPosition by remember(car.id, itemCode) { mutableStateOf(MaintenanceInputPolicy.initialPadPosition(itemCode)) } // FRONT, REAR, BOTH
    var padThicknessMm by remember { mutableStateOf("") }
    var brakePadCost by remember { mutableStateOf("") }
    var brakePadProduct by remember { mutableStateOf("") }

    // 일반 소모품 상태
    var actionType by remember { mutableStateOf(ActionType.REPLACE) }
    var partSpecification by remember { mutableStateOf("") }
    var conditionValue by remember { mutableStateOf("") }
    var partsCost by remember { mutableStateOf("") }
    var laborCost by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val relevantEvals = if (isBrakePad) evaluations.filter { it.itemCode in MaintenanceInputPolicy.padCodes(brakePadPosition) }
        else listOfNotNull(currentEval)
    val hasActualService = relevantEvals.isNotEmpty() && relevantEvals.all { it.lastServiceDate != null || it.lastServiceMileage != null || it.lastServiceHours != null }
    val initialBaseline = relevantEvals.firstOrNull { it.isBaseline }
    fun common(values: List<String?>) = values.distinct().singleOrNull().orEmpty()
    var enableBaseline by remember(car.id, brakePadPosition) { mutableStateOf(initialBaseline != null) }
    var baselineDateInput by remember(car.id, brakePadPosition) { mutableStateOf(common(relevantEvals.map { it.baselineDate })) }
    var baselineMileageInput by remember(car.id, brakePadPosition) { mutableStateOf(common(relevantEvals.map { it.baselineMileage?.toString() })) }
    var customIntervalKm by remember(car.id, brakePadPosition) { mutableStateOf(common(relevantEvals.map { it.intervalKm?.toString() })) }
    var customIntervalMonths by remember(car.id, brakePadPosition) { mutableStateOf(common(relevantEvals.map { it.intervalMonths?.toString() })) }
    var kmEdited by remember(car.id, brakePadPosition) { mutableStateOf(false) }
    var monthsEdited by remember(car.id, brakePadPosition) { mutableStateOf(false) }
    var baselineEdited by remember(car.id, brakePadPosition) { mutableStateOf(false) }
    val intervalKmError = runCatching { MaintenanceInputPolicy.optionalPositive(customIntervalKm, 1_000_000) }.exceptionOrNull()?.message
    val intervalMonthsError = runCatching { MaintenanceInputPolicy.optionalPositive(customIntervalMonths, 240) }.exceptionOrNull()?.message
    val parsedBaselineDate = baselineDateInput.trim().takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val baselineDateError = if (enableBaseline && baselineEdited && baselineDateInput.isNotBlank() && (parsedBaselineDate == null || parsedBaselineDate > LocalDate.now())) "오늘 이하의 올바른 날짜를 입력해 주세요." else null
    val baselineMileageInt = baselineMileageInput.toIntOrNull()
    val baselineMileageError = if (enableBaseline && baselineEdited && baselineMileageInput.isNotBlank() && (baselineMileageInt == null || baselineMileageInt !in 0..car.currentMileage)) "관리 시작 거리는 0~현재 주행거리로 입력해 주세요." else null
    fun settings(): List<MaintenanceRuleEntity> = relevantEvals.map { e ->
        val hasRecord = e.lastServiceDate != null || e.lastServiceMileage != null || e.lastServiceHours != null
        val changeBase = baselineEdited && !hasRecord
        val input = MaintenanceRuleEntity(carId = car.id, itemCode = e.itemCode,
            intervalKm = if (kmEdited) MaintenanceInputPolicy.optionalPositive(customIntervalKm, 1_000_000) else e.intervalKm,
            intervalMonths = if (monthsEdited) MaintenanceInputPolicy.optionalPositive(customIntervalMonths, 240) else e.intervalMonths,
            intervalHours = e.intervalHours,
            baselineDate = if (changeBase) { if (enableBaseline) baselineDateInput.trim().ifBlank { null } else null } else e.baselineDate,
            baselineMileage = if (changeBase) { if (enableBaseline && baselineMileageInput.isNotBlank()) requireNotNull(baselineMileageInt) else null } else e.baselineMileage,
            baselineHours = if (changeBase && !enableBaseline) null else e.baselineHours)
        if (changeBase && enableBaseline) require(input.baselineDate != null || input.baselineMileage != null || input.baselineHours != null) { "관리 시작 날짜 또는 거리를 입력해 주세요." }
        MaintenanceInputPolicy.applySettings(input, input, car)
    }
    val settingsError = runCatching { require(relevantEvals.isNotEmpty()); settings() }.exceptionOrNull()?.message
    val isTab1Valid = intervalKmError == null && intervalMonthsError == null && baselineDateError == null && baselineMileageError == null && settingsError == null
    val serviceError = runCatching {
        MaintenanceInputPolicy.validateService(serviceDate, serviceMileage,
            when { isEngineOil -> listOf(engineOilCost); isBrakePad -> listOf(brakePadCost); else -> listOf(partsCost, laborCost) })
        require(!isEngineOil || replaceEngineOil || replaceOilFilter) { "교환할 항목을 선택해 주세요." }
        if (isBrakePad && padThicknessMm.isNotBlank()) require(padThicknessMm.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..50.0 } == true) { "패드 두께를 확인해 주세요." }
    }.exceptionOrNull()?.message

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isEngineOil -> "엔진오일 & 필터 관리"
                            isBrakePad -> "브레이크 패드 관리"
                            else -> currentEval?.displayName ?: "소모품 관리"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    currentEval?.status?.let { StatusBadge(status = it) }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 탭 전환 (교체/정비 기록 vs 교체주기 설정)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceVariantDark,
                    contentColor = GaragePrimaryLight,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GaragePrimaryLight
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { if (!saving) selectedTab = 0 },
                        text = { Text("정비/교체 기록", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { if (!saving) selectedTab = 1 },
                        text = { Text("교체주기 설정", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // TAB 0: 정비 및 교체 기록 입력

                    // 1. 엔진오일 특화 섹션
                    if (isEngineOil) {
                        Surface(
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("교체 대상 선택", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = replaceEngineOil,
                                            onCheckedChange = { replaceEngineOil = it },
                                            colors = CheckboxDefaults.colors(checkedColor = GaragePrimaryLight)
                                        )
                                        Text("엔진오일 교체", color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = replaceOilFilter,
                                            onCheckedChange = { replaceOilFilter = it },
                                            colors = CheckboxDefaults.colors(checkedColor = GaragePrimaryLight)
                                        )
                                        Text("오일필터 교체", color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = oilBrand,
                                onValueChange = { oilBrand = it },
                                label = { Text("제조사/브랜드") },
                                placeholder = { Text("모빌1, 킥스, 지크") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = oilProductName,
                                onValueChange = { oilProductName = it },
                                label = { Text("제품명") },
                                placeholder = { Text("ESP, TOP") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = oilViscosity,
                                onValueChange = { oilViscosity = it },
                                label = { Text("오일 점도") },
                                placeholder = { Text("0W-20, 5W-30") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = oilCapacityL,
                                onValueChange = { oilCapacityL = it },
                                label = { Text("교체량 (L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = engineOilCost,
                            onValueChange = { if (it.all { c -> c.isDigit() }) engineOilCost = it },
                            label = { Text("교체 총 비용 (KRW)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // 2. 브레이크 패드 특화 섹션
                    else if (isBrakePad) {
                        Surface(
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("교체 위치 선택", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(8.dp))

                                val positions = listOf(
                                    "FRONT" to "전륜 교체",
                                    "REAR" to "후륜 교체",
                                    "BOTH" to "전·후륜 모두 교체"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    positions.forEach { (pos, label) ->
                                        val isSel = brakePadPosition == pos
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { brakePadPosition = pos },
                                            color = if (isSel) GaragePrimaryLight.copy(alpha = 0.25f) else SurfaceDark,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSel) GaragePrimaryLight else BorderDark
                                            )
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) GaragePrimaryLight else TextSecondaryDark,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = brakePadProduct,
                                onValueChange = { brakePadProduct = it },
                                label = { Text("패드 제품/제조사") },
                                placeholder = { Text("상신 하드론, 브렘보") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = padThicknessMm,
                                onValueChange = { padThicknessMm = it },
                                label = { Text("잔여 두께 (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = brakePadCost,
                            onValueChange = { if (it.all { c -> c.isDigit() }) brakePadCost = it },
                            label = { Text("총 교체 비용 (KRW)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // 3. 일반 소모품
                    else {
                        val actionOptions = listOf(
                            ActionType.REPLACE to "교체",
                            ActionType.INSPECT to "점검",
                            ActionType.REFILL to "보충/교환",
                            ActionType.CLEAN to "세척"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            actionOptions.forEach { (type, label) ->
                                val isSel = actionType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { actionType = type },
                                    color = if (isSel) GaragePrimaryLight.copy(alpha = 0.25f) else SurfaceVariantDark
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) GaragePrimaryLight else TextSecondaryDark,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = partSpecification,
                            onValueChange = { partSpecification = it },
                            label = { Text("사용 부품 규격 / 제품명") },
                            placeholder = { Text("예: 순정 규격품, 보쉬, 델코") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = partsCost,
                                onValueChange = { if (it.all { c -> c.isDigit() }) partsCost = it },
                                label = { Text("부품비 (원)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = laborCost,
                                onValueChange = { if (it.all { c -> c.isDigit() }) laborCost = it },
                                label = { Text("공임비 (원)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    HorizontalDivider(color = BorderDark, thickness = 0.5.dp)

                    // 정비 일자 및 시점 주행거리
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = serviceDate,
                            onValueChange = { serviceDate = it },
                            label = { Text("정비 일자 (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = serviceMileage,
                            onValueChange = { if (it.all { c -> c.isDigit() }) serviceMileage = it },
                            label = { Text("주행거리 (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = workshop,
                        onValueChange = { workshop = it },
                        label = { Text("정비소 / 작업처 (선택)") },
                        placeholder = { Text("예: 마하개러지, 직영센터, 공임나라") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("메모 및 특이사항") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // TAB 1: 교체주기 및 관리 기준 설정
                    Surface(
                        color = SurfaceVariantDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "사용자 정의 교체주기",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GarageAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (hasActualService) {
                                    val lastInfo = listOfNotNull(
                                        currentEval?.lastServiceDate,
                                        currentEval?.lastServiceMileage?.let { "%,dkm".format(it) }
                                    ).joinToString(" / ")
                                    "최근 교환 기록($lastInfo)을 기준으로 다음 교환 시점을 계산합니다."
                                } else {
                                    "운전 습관이나 주행 환경에 맞추어 권장 주기(km 또는 개월)를 변경할 수 있습니다."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    if (isBrakePad) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("적용 위치 선택", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("BOTH" to "전/후륜 모두", "FRONT" to "전륜만", "REAR" to "후륜만").forEach { (pos, label) ->
                                FilterChip(
                                    selected = brakePadPosition == pos,
                                    onClick = { if (!saving) brakePadPosition = pos },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GarageAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = GarageAccent
                                    )
                                )
                            }
                        }
                    }

                    if (relevantEvals.size > 1) Text("서로 다른 값은 공란으로 표시됩니다. 수정한 값만 양쪽에 적용합니다.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customIntervalKm,
                        onValueChange = { if (it.all { c -> c.isDigit() }) customIntervalKm = it; kmEdited = true },
                        label = { Text("거리 주기 (선택, 비우면 사용 안 함)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = intervalKmError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (intervalKmError != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(intervalKmError, color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customIntervalMonths,
                        onValueChange = { if (it.all { c -> c.isDigit() }) customIntervalMonths = it; monthsEdited = true },
                        label = { Text("개월 주기 (선택, 비우면 사용 안 함)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = intervalMonthsError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (intervalMonthsError != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(intervalMonthsError, color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                    }

                    // 교환 기록이 없는 경우 '현재부터 관리 시작' 선택 기능 제공
                    if (!hasActualService) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "현재부터 관리 시작",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "실제 교환 기록은 없지만, 현재 시점을 기준으로 교환 주기를 계산합니다. (정비 내역이나 비용에 포함되지 않음)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark
                                        )
                                    }
                                    Switch(
                                        checked = enableBaseline,
                                        onCheckedChange = {
                                            enableBaseline = it; baselineEdited = true
                                            if (it) {
                                                if (baselineDateInput.isBlank() && customIntervalMonths.isNotBlank()) baselineDateInput = LocalDate.now().toString()
                                                if (baselineMileageInput.isBlank() && customIntervalKm.isNotBlank()) baselineMileageInput = car.currentMileage.toString()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GarageAccent,
                                            checkedTrackColor = GarageAccent.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (enableBaseline) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = baselineDateInput,
                                        onValueChange = { baselineDateInput = it; baselineEdited = true },
                                        label = { Text("관리 시작 날짜 (선택, YYYY-MM-DD)") },
                                        isError = baselineDateError != null,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    if (baselineDateError != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(baselineDateError, color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = baselineMileageInput,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) baselineMileageInput = it; baselineEdited = true },
                                        label = { Text("관리 시작 거리 (선택, km)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = baselineMileageError != null,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    if (baselineMileageError != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(baselineMileageError, color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (initialBaseline?.isBaseline == true) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = {
                                                enableBaseline = false
                                                baselineEdited = true
                                                baselineDateInput = ""
                                                baselineMileageInput = ""
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("관리 기준 해제", color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column {
                val errorText = saveError ?: if (selectedTab == 0) serviceError else settingsError
                errorText?.let { Text(it, color = StatusOverdue, style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = {
                    saveError = null
                    saving = true
                    val done: () -> Unit = { saving = false; onDismiss() }
                    val failed: (String) -> Unit = { saving = false; saveError = it }
                    if (selectedTab == 0) {
                        // 정비 기록 저장
                        val visitId = UUID.randomUUID().toString()
                        val odo = requireNotNull(serviceMileage.toIntOrNull())
                        val lines = mutableListOf<ServiceLineEntity>()

                        if (isEngineOil) {
                            val totalCostLong = MaintenanceInputPolicy.cost(engineOilCost)
                            val spec = listOfNotNull(
                                oilBrand.ifBlank { null },
                                oilProductName.ifBlank { null },
                                oilViscosity.ifBlank { null },
                                if (oilCapacityL.isNotBlank()) "${oilCapacityL}L" else null
                            ).joinToString(" ")

                            if (replaceEngineOil) {
                                lines.add(
                                    ServiceLineEntity(
                                        visitId = visitId,
                                        itemCode = "ENGINE_OIL",
                                        actionType = ActionType.REPLACE,
                                        partsCost = if (replaceOilFilter) totalCostLong * 4 / 5 else totalCostLong,
                                        laborCost = 0L,
                                        partSpecification = spec.ifBlank { null },
                                        notes = notes.ifBlank { null }
                                    )
                                )
                            }
                            if (replaceOilFilter) {
                                lines.add(
                                    ServiceLineEntity(
                                        visitId = visitId,
                                        itemCode = "OIL_FILTER",
                                        actionType = ActionType.REPLACE,
                                        partsCost = if (replaceEngineOil) totalCostLong - totalCostLong * 4 / 5 else totalCostLong,
                                        laborCost = 0L,
                                        partSpecification = "오일필터 카트리지",
                                        notes = notes.ifBlank { null }
                                    )
                                )
                            }
                        } else if (isBrakePad) {
                            val costLong = MaintenanceInputPolicy.cost(brakePadCost)
                            val spec = brakePadProduct.ifBlank { null }
                            val cond = if (padThicknessMm.isNotBlank()) "${padThicknessMm}mm" else null

                            if (brakePadPosition == "FRONT" || brakePadPosition == "BOTH") {
                                lines.add(
                                    ServiceLineEntity(
                                        visitId = visitId,
                                        itemCode = "BRAKE_PAD_FRONT",
                                        actionType = ActionType.REPLACE,
                                        partsCost = if (brakePadPosition == "BOTH") costLong / 2 else costLong,
                                        laborCost = 0L,
                                        partSpecification = spec,
                                        conditionValue = cond,
                                        notes = notes.ifBlank { null }
                                    )
                                )
                            }
                            if (brakePadPosition == "REAR" || brakePadPosition == "BOTH") {
                                lines.add(
                                    ServiceLineEntity(
                                        visitId = visitId,
                                        itemCode = "BRAKE_PAD_REAR",
                                        actionType = ActionType.REPLACE,
                                        partsCost = if (brakePadPosition == "BOTH") costLong - costLong / 2 else costLong,
                                        laborCost = 0L,
                                        partSpecification = spec,
                                        conditionValue = cond,
                                        notes = notes.ifBlank { null }
                                    )
                                )
                            }
                        } else {
                            val pCost = MaintenanceInputPolicy.cost(partsCost)
                            val lCost = MaintenanceInputPolicy.cost(laborCost)
                            lines.add(
                                ServiceLineEntity(
                                    visitId = visitId,
                                    itemCode = itemCode,
                                    actionType = actionType,
                                    partsCost = pCost,
                                    laborCost = lCost,
                                    partSpecification = partSpecification.ifBlank { null },
                                    conditionValue = conditionValue.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }

                        val visit = ServiceVisitEntity(
                            id = visitId,
                            carId = car.id,
                            serviceDate = serviceDate.trim(),
                            odometerKm = odo,
                            workshop = workshop.ifBlank { null },
                            totalCost = lines.sumOf { it.partsCost + it.laborCost },
                            notes = notes.ifBlank { null }
                        )

                        onSaveVisitWithLines(visit, lines, done, failed)
                    } else {
                        onSaveCustomRules(settings(), done, failed)
                    }
                },
                enabled = !saving && if (selectedTab == 0) serviceError == null else isTab1Valid,
                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
            ) {
                Text(
                    text = if (saving) "저장 중…" else if (selectedTab == 0) "정비 기록 저장" else "설정 저장",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("닫기", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * 신규 타이어 교체 다이얼로그 (Section 6.2)
 * FL, FR, RL, RR, 전륜, 후륜, 4륜 전체 교체 기록
 */
@Composable
fun TireReplacementDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        positions: List<String>,
        brand: String,
        productName: String,
        spec: String,
        dot: String,
        totalCost: Long,
        serviceDate: String,
        mileage: Int,
        workshop: String?,
        notes: String?,
        isWheelAlignmentDone: Boolean
    ) -> Unit
) {
    var selectedPositionMode by remember { mutableStateOf("ALL") } // "FL", "FR", "RL", "RR", "FRONT", "REAR", "ALL"
    var isWheelAlignmentDone by remember { mutableStateOf(false) }
    var brand by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var spec by remember { mutableStateOf("245/45R18") }
    var dot by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var mileageStr by remember { mutableStateOf(car.currentMileage.toString()) }
    var workshop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = GarageAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "타이어 신규 교체 등록",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 교체 위치 선택
                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("교체 위치 선택", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(8.dp))

                        val modeOptions = listOf(
                            "ALL" to "4개 전체",
                            "FRONT" to "전륜 2개",
                            "REAR" to "후륜 2개",
                            "FL" to "FL (전좌)",
                            "FR" to "FR (전우)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            modeOptions.forEach { (mode, label) ->
                                val isSel = selectedPositionMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedPositionMode = mode },
                                    color = if (isSel) GarageAccent.copy(alpha = 0.25f) else SurfaceDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) GarageAccent else BorderDark
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) GarageAccent else TextSecondaryDark,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("제조사") },
                        placeholder = { Text("한국, 미쉐린, 금호") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("제품명") },
                        placeholder = { Text("벤투스 S2, PS4") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = spec,
                            onValueChange = { spec = it },
                            label = { Text("타이어 규격") },
                            placeholder = { Text("245/45R18") },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dot,
                            onValueChange = { dot = it },
                            label = { Text("타이어 연식 (DOT)") },
                            placeholder = { Text("2024년식 또는 2424") },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true
                        )
                    }
                    val dotHelperText = remember(dot) {
                        val trimmed = dot.trim()
                        when {
                            trimmed.isEmpty() -> null
                            trimmed.length == 4 && trimmed.all { it.isDigit() } -> {
                                val week = trimmed.substring(0, 2)
                                val year = trimmed.substring(2, 4)
                                "💡 20${year}년 ${week}주차 생산 타이어"
                            }
                            trimmed.length == 2 && trimmed.all { it.isDigit() } -> "💡 20${trimmed}년식 타이어"
                            trimmed.length == 4 && trimmed.startsWith("20") && trimmed.all { it.isDigit() } -> "💡 ${trimmed}년식 생산 타이어"
                            trimmed.contains("년") -> "💡 ${trimmed} 타이어"
                            else -> null
                        }
                    }
                    if (dotHelperText != null) {
                        Text(
                            text = dotHelperText,
                            style = MaterialTheme.typography.labelSmall,
                            color = GarageAccent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("교체일자") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mileageStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) mileageStr = it },
                        label = { Text("주행거리 (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) costStr = it },
                    label = { Text("총 구매 및 장착비용 (KRW)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = workshop,
                    onValueChange = { workshop = it },
                    label = { Text("정비업체 / 장착점") },
                    placeholder = { Text("타이어프로, 티스테이션") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 휠 얼라인먼트 동시 진행 여부 체크박스 카드
                Surface(
                    color = if (isWheelAlignmentDone) GarageAccent.copy(alpha = 0.12f) else SurfaceVariantDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isWheelAlignmentDone) GarageAccent else BorderDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isWheelAlignmentDone = !isWheelAlignmentDone }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isWheelAlignmentDone,
                            onCheckedChange = { isWheelAlignmentDone = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GarageAccent,
                                checkmarkColor = SurfaceDark,
                                uncheckedColor = TextSecondaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "휠 얼라인먼트 동시 진행",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isWheelAlignmentDone) GarageAccent else TextPrimaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "체크 시 현재 정비 키로수(${mileageStr.ifBlank { "0" }} km)가 휠 얼라인먼트 기준 정비 키로수로 등록됩니다.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val posList = when (selectedPositionMode) {
                        "ALL" -> listOf("FL", "FR", "RL", "RR")
                        "FRONT" -> listOf("FL", "FR")
                        "REAR" -> listOf("RL", "RR")
                        else -> listOf(selectedPositionMode)
                    }
                    onConfirm(
                        posList,
                        brand,
                        productName,
                        spec,
                        dot,
                        costStr.toLongOrNull() ?: 0L,
                        dateStr,
                        mileageStr.toIntOrNull() ?: car.currentMileage,
                        workshop.ifBlank { null },
                        notes.ifBlank { null },
                        isWheelAlignmentDone
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GarageAccent)
            ) {
                Text("교체 등록 완료", color = SurfaceDark, fontWeight = FontWeight.Bold)
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
 * 타이어 위치교환 (로테이션) 다이얼로그
 * 전후/대각선 위치교환 시 타이어의 고유 누적 거리를 보존하면서 장착 위치만 변경합니다.
 */
@Composable
fun TireRotationDialog(
    car: CarEntity,
    mountedTires: List<maha.garage.data.local.entity.TireEntity>,
    isSaving: Boolean = false,
    initialError: String? = null,
    onDismiss: () -> Unit,
    onConfirmRotation: (
        swapMapping: Map<String, String>,
        mileage: Int,
        date: String,
        cost: Long,
        notes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var selectedPattern by remember { mutableStateOf(0) }
    var dateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var mileageStr by remember { mutableStateOf(car.currentMileage.toString()) }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSavingLocal by remember { mutableStateOf(isSaving) }
    var errorMessage by remember { mutableStateOf(initialError) }

    val currentMapping = when (selectedPattern) {
        0 -> mapOf("FL" to "RL", "RL" to "FL", "FR" to "RR", "RR" to "FR")
        1 -> mapOf("FL" to "RR", "RR" to "FL", "FR" to "RL", "RL" to "FR")
        else -> mapOf("FL" to "RL", "FR" to "RR", "RL" to "FR", "RR" to "FL")
    }

    val patternNames = listOf(
        "전후 교환 (FL ↔ RL, FR ↔ RR)" to "방향성 타이어 기본 패턴",
        "대각 교환 (FL ↔ RR, FR ↔ RL)" to "비방향성 타이어 4륜 마모 균등화",
        "후륜 교차 (FL→RL, FR→RR, 후륜 교차 전진)" to "후륜/4WD 구동 권장 패턴"
    )

    val inputMileage = mileageStr.toIntOrNull()
    val isMileageValid = inputMileage != null && inputMileage >= car.currentMileage

    AlertDialog(
        onDismissRequest = { if (!isSavingLocal && !isSaving) onDismiss() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = GarageAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "타이어 위치교환 (로테이션)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ℹ️ 위치교환 마일리지 보존 안내",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GarageAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "각 바퀴의 기존 타이어 누적 주행거리는 초기화되지 않고 유지된 채 위치만 이동합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFF3B151A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusOverdue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusOverdue
                            )
                        }
                    }
                }

                Text(
                    text = "위치교환 패턴 선택",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )

                patternNames.forEachIndexed { idx, (title, desc) ->
                    val isSel = selectedPattern == idx
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSavingLocal && !isSaving) { selectedPattern = idx },
                        color = if (isSel) GarageAccent.copy(alpha = 0.2f) else SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSel) GarageAccent else BorderDark
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) GarageAccent else TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // 이동 미리보기
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "위치 이동 계획 (누적거리 보존)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        currentMapping.forEach { (from, to) ->
                            val tire = mountedTires.find { it.position == from }
                            val tireName = tire?.let { "${it.brand} ${it.productName}".trim() }.takeIf { !it.isNullOrBlank() } ?: "장착 타이어"
                            val effectiveMileage = inputMileage?.coerceAtLeast(car.currentMileage) ?: car.currentMileage
                            val currentDriven = (effectiveMileage - (tire?.installedAtMileage ?: car.currentMileage)).coerceAtLeast(0)
                            val totalDriven = (tire?.cumulativeMileage ?: 0) + currentDriven
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$from ➔ $to ($tireName)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "%,d km 유지".format(totalDriven),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GarageAccent
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("교환일자") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        enabled = !isSavingLocal && !isSaving
                    )
                    OutlinedTextField(
                        value = mileageStr,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) {
                                mileageStr = it
                                errorMessage = null
                            }
                        },
                        label = { Text("주행거리 (km)") },
                        supportingText = {
                            if (inputMileage != null && inputMileage < car.currentMileage) {
                                Text("현재 차량 거리(%,d km) 이상이어야 합니다.".format(car.currentMileage), color = StatusOverdue)
                            }
                        },
                        isError = inputMileage != null && inputMileage < car.currentMileage,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isSavingLocal && !isSaving
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) costStr = it },
                    label = { Text("공임비 (KRW)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSavingLocal && !isSaving
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("메모") },
                    placeholder = { Text("예: 휠 밸런스 조정 완료") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSavingLocal && !isSaving
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mileage = mileageStr.toIntOrNull()
                    if (mileage == null || mileage < car.currentMileage) {
                        errorMessage = "위치교환 주행거리는 현재 차량 거리(%,d km) 이상이어야 합니다.".format(car.currentMileage)
                        return@Button
                    }
                    isSavingLocal = true
                    errorMessage = null
                    onConfirmRotation(
                        currentMapping,
                        mileage,
                        dateStr,
                        costStr.toLongOrNull() ?: 0L,
                        notes.ifBlank { null },
                        {
                            isSavingLocal = false
                            onDismiss()
                        },
                        { err ->
                            isSavingLocal = false
                            errorMessage = err
                        }
                    )
                },
                enabled = !isSavingLocal && !isSaving && isMileageValid && (costStr.isBlank() || costStr.toLongOrNull() != null),
                colors = ButtonDefaults.buttonColors(containerColor = GarageAccent)
            ) {
                Text(
                    text = if (isSavingLocal || isSaving) "저장 중…" else "위치교환 완료",
                    color = SurfaceDark,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSavingLocal && !isSaving
            ) {
                Text("취소", color = TextSecondaryDark)
            }
        }
    )
}

