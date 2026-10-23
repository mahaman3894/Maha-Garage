package maha.garage.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.engine.TireSwapCalculator
import maha.garage.domain.model.ItemEvaluation
import maha.garage.domain.model.VehicleWithStatus
import maha.garage.domain.engine.*
import maha.garage.ui.components.*
import maha.garage.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate

/**
 * 소모품 표시 카드 모델 (14종 관리 항목)
 */
data class ConsumableCardData(
    val id: String,
    val title: String,
    val status: DueStatus,
    val statusDescription: String,
    val lastServiceText: String?,
    val nextDueText: String?,
    val remainingText: String?,
    val progressRatio: Float,
    val isNeedsData: Boolean,
    val rawItemCode: String,
    val isBaseline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAddService: (String) -> Unit
) {
    val vehicleFlow = remember(vehicleId) { viewModel.getVehicleWithStatus(vehicleId) }
    val vehicleWithStatus by vehicleFlow.collectAsState(initial = null)
    val mountedTiresFlow = remember(vehicleId) { viewModel.getMountedTires(vehicleId) }
    val mountedTires by mountedTiresFlow.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(vehicleWithStatus?.car) {
        vehicleWithStatus?.car?.let { viewModel.ensureInitialTires(it) }
    }

    var showMileageDialog by remember { mutableStateOf(false) }
    var savingMileage by remember { mutableStateOf(false) }
    var mileageError by remember { mutableStateOf<String?>(null) }
    var showTireSwapDialog by remember { mutableStateOf(false) }
    var showTireReplacementDialog by remember { mutableStateOf(false) }
    var showTireRotationDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSpecsDialog by remember { mutableStateOf(false) }
    var selectedConsumableItemCode by remember { mutableStateOf<String?>(null) }
    var showInspectionResultDialog by remember { mutableStateOf(false) }
    var showInsuranceEditDialog by remember { mutableStateOf(false) }
    var showInspectionGuideDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var editingTireMileageMode by remember { mutableStateOf<String?>(null) }
    var savingTireMileage by remember { mutableStateOf(false) }
    var tireMileageError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. 긴급출동 플로팅 버튼 (정비 기록 추가 버튼 바로 위에 위치, 스크롤해도 최상단 유지)
                ExtendedFloatingActionButton(
                    onClick = { showEmergencyDialog = true },
                    containerColor = SketchRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "긴급출동",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "긴급출동",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                )

                // 2. 정비 기록 추가 버튼
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAddService(vehicleId) },
                    containerColor = GaragePrimaryLight,
                    contentColor = TextPrimaryDark,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = { Icon(imageVector = Icons.Default.Build, contentDescription = null) },
                    text = { Text("정비 기록 추가", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        val vws = vehicleWithStatus
        if (vws == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GaragePrimaryLight)
            }
        } else {
            val car = vws.car
            val liveTire = TireSwapCalculator.calculateLiveTireDistances(car)

            // Section 5 순서에 맞춘 14종 관리 항목 리스트 구성 (타이밍 방식 반영)
            val orderedConsumables = remember(vws.evaluations, car) {
                buildOrderedConsumablesList(vws.evaluations, car)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val screenWidth = maxWidth
                val columns = when {
                    screenWidth >= 840.dp -> 4 // 데스크톱/넓은 화면 4열
                    screenWidth >= 560.dp -> 3 // 태블릿 가로 3열
                    else -> 2 // 모바일 세로 2열
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // 1. 대시보드 최상단 통합 차량 정보 박스 (중복 헤더 완전 제거)
                    item {
                        IntegratedVehicleInfoCard(
                            car = car,
                            onBackClick = onNavigateBack,
                            onUpdateMileageClick = { mileageError = null; showMileageDialog = true },
                            onEditClick = { onNavigateToEdit(vehicleId) },
                            onDeleteClick = { showDeleteConfirmDialog = true },
                            onRestoreClick = {
                                viewModel.unarchiveVehicle(vehicleId) {
                                    maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                }
                            },
                            onInspectionClick = { showInspectionResultDialog = true },
                            onInsuranceClick = { showInsuranceEditDialog = true },
                            onEmergencyClick = { showEmergencyDialog = true }
                        )
                    }

                    // 2. 차량 스케치 다이어그램 & 부품 위치별 상태 마커 (Section 4 & 5)
                    item {
                        // Narrow phones use a vertical stack. Expanded/foldable screens
                        // give 70% of the available width to the technical vehicle diagram.
                        if (screenWidth >= 700.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                VehicleConsumablesSketchMap(
                                    vehicleClass = car.vehicleClass,
                                    powertrain = car.powertrain,
                                    evaluations = vws.evaluations,
                                    onMarkerClick = { code -> selectedConsumableItemCode = code },
                                    onSpecsClick = { showSpecsDialog = true },
                                    modifier = Modifier.weight(7f),
                                    diagramHeight = 450.dp
                                )
                                ConsumableQuickPanel(
                                    consumables = orderedConsumables,
                                    onSelect = { selectedConsumableItemCode = it },
                                    modifier = Modifier.weight(3f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                VehicleConsumablesSketchMap(
                                    vehicleClass = car.vehicleClass,
                                    powertrain = car.powertrain,
                                    evaluations = vws.evaluations,
                                    onMarkerClick = { code -> selectedConsumableItemCode = code },
                                    onSpecsClick = { showSpecsDialog = true },
                                    diagramHeight = 390.dp
                                )
                                ConsumableQuickPanel(
                                    consumables = orderedConsumables,
                                    onSelect = { selectedConsumableItemCode = it }
                                )
                            }
                        }
                    }

                    // 3. 독립 타이어 전용 관리 섹션 (Section 6)
                    item {
                        DedicatedTireManagementSection(
                            car = car,
                            mountedTires = mountedTires,
                            liveTire = liveTire,
                            onReplaceTireClick = { showTireReplacementDialog = true },
                            onRotateClick = { showTireRotationDialog = true },
                            onSwapClick = { showTireSwapDialog = true },
                            onEditTireMileageClick = { mode ->
                                tireMileageError = null
                                editingTireMileageMode = mode
                            }
                        )
                    }

                    // 4. 소모품 교환주기 관리 헤더
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "소모품 교환주기 관리 목록 (${orderedConsumables.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val (badgeLabel, badgeBgColor, badgeTextColor) = when (car.maintenanceStandard) {
                                        "SEVERE" -> Triple("제조사 가혹", StatusDueSoon.copy(alpha = 0.2f), StatusDueSoon)
                                        "SHOP" -> Triple("정비소·오너 권장", Color(0xFFFF9800).copy(alpha = 0.2f), Color(0xFFFFB74D))
                                        else -> if (car.isSevereCondition) {
                                            Triple("제조사 가혹", StatusDueSoon.copy(alpha = 0.2f), StatusDueSoon)
                                        } else {
                                            Triple("제조사 공식 표준", GarageAccent.copy(alpha = 0.15f), GarageAccent)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeBgColor,
                                        border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    }
                                }
                                Text(
                                    text = "카드를 누르면 교체기록 등록 및 교체주기 설정이 가능합니다.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    // 5. 14개 소모품 2~4열 반응형 그리드 카드 (Section 4.1 & Section 8)
                    val chunkedList = orderedConsumables.chunked(columns)
                    items(chunkedList) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    UnifiedConsumableCard(
                                        cardData = item,
                                        onClick = { selectedConsumableItemCode = item.rawItemCode }
                                    )
                                }
                            }
                            val emptySlots = columns - rowItems.size
                            for (i in 0 until emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // 주행거리 변경 모달
    if (showMileageDialog && vehicleWithStatus != null) {
        UpdateMileageDialog(
            car = vehicleWithStatus!!.car,
            isSaving = savingMileage,
            saveError = mileageError,
            onDismiss = { showMileageDialog = false },
            onConfirm = { newMileage, summerMileage, winterMileage, lastSwapMileage ->
                savingMileage = true
                mileageError = null
                viewModel.updateMileage(
                    carId = vehicleId,
                    newMileage = newMileage,
                    summerTireMileage = summerMileage,
                    winterTireMileage = winterMileage,
                    lastSwapMileage = lastSwapMileage,
                    onComplete = { savingMileage = false; showMileageDialog = false },
                    onError = { savingMileage = false; mileageError = it }
                )
            }
        )
    }

    // 타이어 누적 주행거리 직접 수정 모달 (썸머/사계절 또는 윈터 박스 터치 시)
    if (editingTireMileageMode != null && vehicleWithStatus != null) {
        val car = vehicleWithStatus!!.car
        val targetMode = editingTireMileageMode!!
        val isSummer = targetMode == "SUMMER"
        val liveTire = TireSwapCalculator.calculateLiveTireDistances(car)
        val currentLiveKm = if (isSummer) liveTire.summerTotalKm else liveTire.winterTotalKm

        EditTireMileageDialog(
            car = car,
            tireMode = targetMode,
            currentLiveKm = currentLiveKm,
            isSaving = savingTireMileage,
            saveError = tireMileageError,
            onDismiss = {
                editingTireMileageMode = null
                tireMileageError = null
            },
            onConfirm = { newTargetKm ->
                savingTireMileage = true
                tireMileageError = null
                val (baseSummer, baseWinter, effectiveSwap) = TireSwapCalculator.computeBaseTireMileages(
                    car = car,
                    newCarMileage = car.currentMileage,
                    targetSummerTotal = if (isSummer) newTargetKm else null,
                    targetWinterTotal = if (!isSummer) newTargetKm else null,
                    targetSwapMileage = null
                )
                viewModel.updateMileage(
                    carId = car.id,
                    newMileage = car.currentMileage,
                    summerTireMileage = baseSummer,
                    winterTireMileage = baseWinter,
                    lastSwapMileage = effectiveSwap,
                    onComplete = {
                        savingTireMileage = false
                        editingTireMileageMode = null
                        tireMileageError = null
                    },
                    onError = { err ->
                        savingTireMileage = false
                        tireMileageError = err
                    }
                )
            }
        )
    }

    // 타이어 스왑 모달
    if (showTireSwapDialog && vehicleWithStatus != null) {
        val saving by viewModel.tireSwapSaving.collectAsState()
        val swapError by viewModel.tireSwapError.collectAsState()
        TireSwapDialog(
            car = vehicleWithStatus!!.car,
            isSaving = saving,
            errorMessage = swapError,
            onDismiss = { showTireSwapDialog = false; viewModel.clearTireSwapError() },
            onConfirmSwap = { targetMode, swapMileage, storage, front, rear, cost, notes ->
                viewModel.swapTires(
                    car = vehicleWithStatus!!.car,
                    targetMode = targetMode,
                    swapMileage = swapMileage,
                    storageLocation = storage,
                    frontTread = front,
                    rearTread = rear,
                    swapCost = cost,
                    notes = notes,
                    onComplete = { showTireSwapDialog = false }
                )
            }
        )
    }

    // 타이어 신규 교체 모달
    if (showTireReplacementDialog && vehicleWithStatus != null) {
        TireReplacementDialog(
            car = vehicleWithStatus!!.car,
            onDismiss = { showTireReplacementDialog = false },
            onConfirm = { posList, brand, prod, spec, dot, cost, date, mileage, shop, notes, isWheelAlignmentDone ->
                val visitId = java.util.UUID.randomUUID().toString()
                val lineDesc = "$brand $prod $spec (DOT: $dot) [${posList.joinToString("/")}]"
                val visit = maha.garage.data.local.entity.ServiceVisitEntity(
                    id = visitId,
                    carId = vehicleId,
                    serviceDate = date,
                    odometerKm = mileage,
                    workshop = shop,
                    totalCost = cost,
                    notes = notes
                )
                val lines = listOf(
                    maha.garage.data.local.entity.ServiceLineEntity(
                        visitId = visitId,
                        itemCode = "TIRE_REPLACEMENT",
                        actionType = maha.garage.data.model.enums.ActionType.REPLACE,
                        partsCost = cost,
                        partSpecification = lineDesc,
                        notes = notes
                    )
                )
                val newTires = posList.map { pos ->
                    maha.garage.data.local.entity.TireEntity(
                        carId = vehicleId,
                        position = pos,
                        season = vehicleWithStatus!!.car.currentTireMode,
                        brand = brand,
                        productName = prod,
                        spec = spec,
                        dot = dot,
                        cumulativeMileage = 0,
                        installedAtMileage = mileage,
                        installDate = date,
                        purchaseCost = if (posList.isNotEmpty()) cost / posList.size else cost,
                        isMounted = true,
                        notes = notes
                    )
                }
                val histories = newTires.map { t ->
                    maha.garage.data.local.entity.TireInstallationHistoryEntity(
                        carId = vehicleId,
                        tireId = t.id,
                        eventType = "NEW_INSTALL",
                        fromPosition = null,
                        toPosition = t.position,
                        eventDate = date,
                        odometerKm = mileage,
                        drivenDistanceSinceLastEvent = 0,
                        cost = t.purchaseCost,
                        notes = notes
                    )
                }
                val alignmentMileage = if (isWheelAlignmentDone) mileage else vehicleWithStatus!!.car.wheelAlignmentMileage
                viewModel.replaceTires(vehicleId, newTires, visit, lines, histories, alignmentMileage)
            }
        )
    }

    // 타이어 위치교환 (로테이션) 모달
    if (showTireRotationDialog && vehicleWithStatus != null) {
        val savingRotation by viewModel.tireRotationSaving.collectAsState()
        val rotationError by viewModel.tireRotationError.collectAsState()
        TireRotationDialog(
            car = vehicleWithStatus!!.car,
            mountedTires = mountedTires,
            isSaving = savingRotation,
            initialError = rotationError,
            onDismiss = { showTireRotationDialog = false; viewModel.clearTireRotationError() },
            onConfirmRotation = { swapMapping, mileage, date, cost, notes, onSuccess, onError ->
                viewModel.rotateTires(
                    carId = vehicleId,
                    currentMileage = mileage,
                    date = date,
                    swapMapping = swapMapping,
                    cost = cost,
                    notes = notes,
                    onSuccess = {
                        onSuccess()
                        showTireRotationDialog = false
                    },
                    onError = onError
                )
            }
        )
    }

    // 소모품 교체/점검/주기 설정 다이얼로그 (Section 5.1, 5.2, 7.3)
    selectedConsumableItemCode?.let { code ->
        val vws = vehicleWithStatus
        if (vws != null) {
            ConsumableActionDialog(
                car = vws.car,
                itemCode = code,
                evaluations = vws.evaluations,
                onDismiss = { selectedConsumableItemCode = null },
                onSaveVisitWithLines = { visit, lines, done, error ->
                    viewModel.recordServiceVisit(visit, lines, done, error)
                },
                onSaveCustomRules = { rules, done, error ->
                    viewModel.updateRules(rules, done, error)
                }
            )
        }
    }

    // 차량 삭제 / Rest Area 운용 관리 다이얼로그
    if (showDeleteConfirmDialog && vehicleWithStatus != null) {
        val car = vehicleWithStatus!!.car
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.5.dp, WarmLine, RoundedCornerShape(20.dp)),
            title = {
                Text(
                    text = if (car.isArchived) "차량 운용 관리" else "차량 운용 중단 및 삭제",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!car.isArchived) {
                        Text(
                            text = "차량의 운용 상태를 변경하거나 완전히 삭제할 수 있습니다.",
                            color = TextPrimaryDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ToolOrange.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "• Rest Area로 이동 (운용 중단)",
                                    fontWeight = FontWeight.Bold,
                                    color = ToolOrange,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "등록된 모든 정비 기록과 데이터가 안전하게 보존되며, 내 차고 하단 Rest Area(휴면 차고)로 이동합니다. 언제든 운용을 재개할 수 있습니다.",
                                    color = TextSecondaryDark,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusOverdue.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "• 영구 삭제",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOverdue,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "차량 정보와 모든 정비 기록을 데이터베이스에서 영구적으로 삭제합니다. 복구할 수 없습니다.",
                                    color = TextSecondaryDark,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "현재 이 차량은 Rest Area(휴면 차고)에 보관 중입니다.",
                            color = TextPrimaryDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = JackGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JackGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "• 운용 재개: 차량을 다시 활성화하여 내 차고 메인으로 복귀시킵니다.",
                                    color = JackGreen,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusOverdue.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "• 영구 삭제: 차량 정보와 모든 정비 기록을 데이터베이스에서 영구 삭제합니다.",
                                    color = StatusOverdue,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!car.isArchived) {
                        Button(
                            onClick = {
                                viewModel.archiveVehicle(vehicleId) {
                                    maha.garage.util.NotificationHelper.cancelAllForCar(context, vehicleId)
                                    maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                    showDeleteConfirmDialog = false
                                    onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ToolOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rest Area로 이동 (운용 중단)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.unarchiveVehicle(vehicleId) {
                                    maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                    showDeleteConfirmDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JackGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "운용 재개 (내 차고 복귀)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteVehiclePermanently(vehicleId) {
                                maha.garage.util.NotificationHelper.cancelAllForCar(context, vehicleId)
                                maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                showDeleteConfirmDialog = false
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("영구 삭제", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("취소", color = TextSecondaryDark, fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = null
        )
    }

    // 3. 차량 상세 제원 보기 다이얼로그 (스케치 다이어그램에서 호출)
    if (showSpecsDialog && vehicleWithStatus != null) {
        VehicleSpecsDialog(
            car = vehicleWithStatus!!.car,
            onDismiss = { showSpecsDialog = false }
        )
    }

    // 4. 자동차 정기검사 결과 등록 모달
    if (showInspectionResultDialog && vehicleWithStatus != null) {
        InspectionResultDialog(
            car = vehicleWithStatus!!.car,
            onDismiss = { showInspectionResultDialog = false },
            onConfirm = { date, type, station, isPassed, nextExpiry, retestDeadline, cost, memo ->
                viewModel.recordInspectionResult(
                    car = vehicleWithStatus!!.car,
                    inspectionDate = date,
                    inspectionType = type,
                    stationName = station,
                    isPassed = isPassed,
                    nextExpiryDate = nextExpiry,
                    retestDeadline = retestDeadline,
                    cost = cost,
                    memo = memo,
                    onComplete = {
                        showInspectionResultDialog = false
                        maha.garage.util.NotificationHelper.cancelInspection(context, vehicleId)
                        maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                    }
                )
            }
        )
    }

    // 5. 자동차 보험정보 수정 모달
    if (showInsuranceEditDialog && vehicleWithStatus != null) {
        EditInsuranceDialog(
            car = vehicleWithStatus!!.car,
            onDismiss = { showInsuranceEditDialog = false },
            onConfirm = { company, startDate, endDate, emergencyCall ->
                viewModel.updateCarInspectionAndInsurance(
                    car = vehicleWithStatus!!.car,
                    inspectionType = vehicleWithStatus!!.car.inspectionType,
                    inspectionStartDate = vehicleWithStatus!!.car.inspectionStartDate,
                    inspectionEndDate = vehicleWithStatus!!.car.inspectionEndDate,
                    isInspectionDateEstimated = vehicleWithStatus!!.car.isInspectionDateEstimated,
                    insuranceCompany = company,
                    insuranceStartDate = startDate,
                    insuranceEndDate = endDate,
                    insuranceEmergencyCall = emergencyCall,
                    onComplete = {
                        showInsuranceEditDialog = false
                        maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                    }
                )
            }
        )
    }

    // 6. 자동차 검사 법정 주기 안내 모달
    if (showInspectionGuideDialog) {
        InspectionGuideDialog(
            onDismiss = { showInspectionGuideDialog = false }
        )
    }

    // 7. 사고·고장 긴급출동 모달
    if (showEmergencyDialog && vehicleWithStatus != null) {
        EmergencyDispatchDialog(
            car = vehicleWithStatus!!.car,
            onDismiss = { showEmergencyDialog = false }
        )
    }
}

/**
 * 대시보드 최상단 통합 차량 정보 박스 (Section 3.2)
 * - 차량 이름
 * - 차량 번호판
 * - 차량 연식
 * - 현재 누적 주행거리 (수정 버튼 포함) (표준 주행 조건)
 * - 보험가입정보: 동부화재
 * - 정기검사 정보
 * - 우측 상단 수정 및 삭제 작은 아이콘
 */
@Composable
fun IntegratedVehicleInfoCard(
    car: CarEntity,
    onBackClick: () -> Unit,
    onUpdateMileageClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRestoreClick: () -> Unit = {},
    onInspectionClick: () -> Unit = {},
    onInsuranceClick: () -> Unit = {},
    onEmergencyClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (car.isArchived) Color(0xFAFFFFFF) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 상단 행: 뒤로가기 버튼 + 차량 이름/번호판 + 우측 수정/삭제 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${car.make} ${car.modelName}".trim(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (car.isArchived) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = ToolOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ToolOrange.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Rest Area",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ToolOrange,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${car.carNumber} • ${car.modelYear}년식 • ${car.powertrain.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // 우측 상단 수정/삭제 아이콘 메뉴
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (car.isArchived) {
                        IconButton(
                            onClick = onRestoreClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "운용 재개 (내 차고 복귀)",
                                tint = GaragePrimaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "차량 수정",
                            tint = GaragePrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "차량 운용 관리 및 삭제",
                            tint = StatusOverdue.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val hasIceDiscrepancy = car.powertrain == maha.garage.data.model.enums.PowertrainType.BEV && run {
                val fullText = "${car.modelName} ${car.trimName.orEmpty()} ${car.displacement.orEmpty()}".lowercase()
                val iceKeywords = listOf("가솔린", "디젤", "터보", "슈퍼차저", "t-gdi", "v6", "v8", "cc", "3.5", "2.0", "하이브리드", "일렉트릭 슈퍼차저", "g90", "gv80")
                val hasKeyword = iceKeywords.any { fullText.contains(it) }
                val hasDisplacement = !car.displacement.isNullOrBlank() && car.displacement != "0" && car.displacement != "0cc" && car.displacement != "0 cc"
                val hasTiming = car.timingType != "NA"
                hasKeyword || hasDisplacement || hasTiming
            }

            if (hasIceDiscrepancy) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusDueSoon.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusDueSoon.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEditClick)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusDueSoon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "동력계 확인 권장 (순수전기차 ↔ 가솔린/내연기관)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusDueSoon
                            )
                            Text(
                                text = "내연기관/슈퍼차저 사양이 감지되었으나 현재 BEV로 설정되어 있습니다. 탭하여 가솔린/HEV로 정정하면 엔진오일 등 정비 항목이 활성화됩니다.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextPrimaryDark.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = StatusDueSoon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            val today = rememberRenewalToday()
            val insuranceEval = RenewalDatePolicy.insurance(car, today)
            val insuranceStatusText = if (car.isArchived) {
                "(휴면)"
            } else when (insuranceEval.state) {
                RenewalDateState.MISSING -> "(미등록)"
                RenewalDateState.NORMAL -> "(정상)"
                RenewalDateState.WARNING -> "(만기임박)"
                RenewalDateState.EXPIRED -> "(만료됨)"
            }
            val insuranceColor = if (car.isArchived) {
                TextSecondaryDark
            } else when (insuranceEval.state) {
                RenewalDateState.MISSING -> TextSecondaryDark
                RenewalDateState.NORMAL -> StatusNormal
                RenewalDateState.WARNING -> StatusDueSoon
                RenewalDateState.EXPIRED -> StatusOverdue
            }
            val insuranceRangeText = when {
                !car.insuranceStartDate.isNullOrBlank() || !car.insuranceEndDate.isNullOrBlank() ->
                    formatKoreanDateRange(car.insuranceStartDate, car.insuranceEndDate)
                !car.insuranceCompany.isNullOrBlank() -> car.insuranceCompany
                else -> "미등록"
            }

            val inspectionEval = RenewalDatePolicy.inspection(car, today)
            val legal = VehicleComplianceCalculator.evaluateInspection(car, today)
            val retest = RenewalDatePolicy.parse(car.retestDeadline) != null
            val inspectionStatusText = if (car.isArchived) {
                "(휴면)"
            } else when {
                inspectionEval.state == RenewalDateState.MISSING -> "(미등록)"
                inspectionEval.state == RenewalDateState.EXPIRED -> "(만료일경과)"
                retest -> "(재검사)"
                legal.status == InspectionStatusType.AVAILABLE || legal.status == InspectionStatusType.RECOMMEND_RESERVE || legal.status == InspectionStatusType.URGENT || inspectionEval.state == RenewalDateState.WARNING -> "(검사기간)"
                else -> "(정상)"
            }
            val inspectionColor = if (car.isArchived) {
                TextSecondaryDark
            } else when {
                inspectionEval.state == RenewalDateState.MISSING -> TextSecondaryDark
                inspectionEval.state == RenewalDateState.EXPIRED -> StatusOverdue
                retest || legal.status == InspectionStatusType.URGENT || inspectionEval.state == RenewalDateState.WARNING -> StatusDueSoon
                else -> StatusNormal
            }
            val inspStart = car.lastInspectionDate ?: car.inspectionStartDate ?: legal.startDateStr.takeIf { it != "-" }
            val inspEnd = if (retest) car.retestDeadline else (car.inspectionEndDate ?: legal.endDateStr.takeIf { it != "-" })
            val inspectionRangeText = formatKoreanDateRange(inspStart, inspEnd)

            // 하단 행: 현재누적주행거리        보험가입정보 (만기임박)      정기검사 (검사기간,만료일경과)
            //          165000km                 24년1월1일~25년1월1일         24년1월1일~26년1월1일
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. 현재누적주행거리
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "현재누적주행거리",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val badgeText = when (car.maintenanceStandard) {
                            "SEVERE" -> "(가혹)"
                            "SHOP" -> "(예방정비)"
                            else -> if (car.isSevereCondition) "(가혹)" else "(공식)"
                        }
                        val badgeColor = when (car.maintenanceStandard) {
                            "SEVERE" -> StatusDueSoon
                            "SHOP" -> Color(0xFFFFB74D)
                            else -> if (car.isSevereCondition) StatusDueSoon else GarageAccent
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GaragePrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, GaragePrimaryLight.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onUpdateMileageClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "%,dkm".format(car.currentMileage),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                    color = GaragePrimaryLight,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "주행거리 수정",
                                    tint = GaragePrimaryLight.copy(alpha = 0.8f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // 2. 보험가입정보 (만기임박)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "보험가입정보",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = insuranceStatusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = insuranceColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = insuranceColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, insuranceColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onInsuranceClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = insuranceRangeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = if (insuranceEval.state == RenewalDateState.MISSING) TextSecondaryDark else TextPrimaryDark,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // 3. 정기검사 (검사기간, 만료일경과)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "정기검사",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = inspectionStatusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = inspectionColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = inspectionColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, inspectionColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onInspectionClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = inspectionRangeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = if (inspectionEval.state == RenewalDateState.MISSING) TextSecondaryDark else TextPrimaryDark,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKoreanDate(dateStr: String?): String? {
    if (dateStr.isNullOrBlank()) return null
    val parsed = runCatching { LocalDate.parse(dateStr.trim()) }.getOrNull() ?: return dateStr
    val yearShort = parsed.year % 100
    return "${yearShort}년${parsed.monthValue}월${parsed.dayOfMonth}일"
}

private fun formatKoreanDateRange(startStr: String?, endStr: String?): String {
    val s = formatKoreanDate(startStr)
    val e = formatKoreanDate(endStr)
    return when {
        s != null && e != null -> "$s~$e"
        e != null -> "~$e"
        s != null -> "$s~"
        else -> "미등록"
    }
}

/**
 * 독립 타이어 관리 전용 섹션 (Section 6)
 * - 4륜 배치도 (FL, FR, RL, RR)
 * - 각 위치별 타이어 식별(제조사, 제품, 규격, DOT) 및 개별 마일리지 실시간 표시
 * - 타이어 신규 교체, 위치교환, 스왑 3대 독립 액션 지원
 */
@Composable
fun DedicatedTireManagementSection(
    car: CarEntity,
    mountedTires: List<maha.garage.data.local.entity.TireEntity>,
    liveTire: TireSwapCalculator.LiveTireDistances,
    onReplaceTireClick: () -> Unit,
    onRotateClick: () -> Unit,
    onSwapClick: () -> Unit,
    onEditTireMileageClick: (mode: String) -> Unit = {}
) {
    val flTire = mountedTires.find { it.position == "FL" }
    val frTire = mountedTires.find { it.position == "FR" }
    val rlTire = mountedTires.find { it.position == "RL" }
    val rrTire = mountedTires.find { it.position == "RR" }
    val fallbackName = if (car.currentTireMode == "WINTER") car.winterTireName ?: "윈터 타이어" else car.summerTireName ?: "사계절 타이어"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 섹션 헤더 (좌측: 타이어 관리 / 우측: 휠얼라이먼트 점검기록)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = GarageAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "타이어 관리 섹션",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "4륜 개별 식별 · 위치교환 · 계절 스왑",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 휠얼라이먼트 점검기록 (옐로우 계열 라벨, 소프트잉크 숫자)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "휠얼라이먼트 점검기록",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706), // 앰버 계열
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (car.wheelAlignmentMileage != null) "%,d km".format(car.wheelAlignmentMileage) else "- km",
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 타이어 액션 버튼 (교체, 위치교환, 스왑 - 높이와 비례 일치 완벽 정렬)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReplaceTireClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = SoftInk
                    ),
                    border = BorderStroke(1.dp, WarmLine),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "교체",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onRotateClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = SoftInk
                    ),
                    border = BorderStroke(1.dp, WarmLine),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "위치교환",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val isSummer = car.currentTireMode == "SUMMER"
                val swapGrad = if (isSummer) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C)))
                } else {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)))
                }
                Button(
                    onClick = onSwapClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(swapGrad),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "스왑",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4륜 배치도 (FL, FR, RL, RR)
            Surface(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, WarmLine.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 전륜 (FL / FR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WheelTireItem(
                            position = "FL",
                            name = "전륜 좌측",
                            tire = flTire,
                            currentVehicleMileage = car.currentMileage,
                            fallbackName = fallbackName
                        )
                        WheelTireItem(
                            position = "FR",
                            name = "전륜 우측",
                            tire = frTire,
                            currentVehicleMileage = car.currentMileage,
                            fallbackName = fallbackName,
                            isRight = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 중앙 차체 축 연결선 및 현재 모드 배지
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark, thickness = 1.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        TireModeBadge(mode = car.currentTireMode)
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark, thickness = 1.dp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 후륜 (RL / RR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WheelTireItem(
                            position = "RL",
                            name = "후륜 좌측",
                            tire = rlTire,
                            currentVehicleMileage = car.currentMileage,
                            fallbackName = fallbackName
                        )
                        WheelTireItem(
                            position = "RR",
                            name = "후륜 우측",
                            tire = rrTire,
                            currentVehicleMileage = car.currentMileage,
                            fallbackName = fallbackName,
                            isRight = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 계절별 누적 주행거리 요약 바 (터치하여 누적 키로수 보정 가능)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 섬머 / 사계절 누적 카드
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (car.currentTireMode == "SUMMER") Color(0xFFFFFBEB) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (car.currentTireMode == "SUMMER") Color(0xFFF59E0B) else WarmLine
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditTireMileageClick("SUMMER") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "섬머/사계절",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (car.currentTireMode == "SUMMER") SoftInk else TextSecondaryDark,
                                    fontWeight = if (car.currentTireMode == "SUMMER") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Text(
                                text = "터치하여 수정 ✎",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                                color = WarmGray
                            )
                        }
                        Text(
                            text = "%,d km".format(liveTire.summerTotalKm),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                // 윈터(스노우) 누적 카드
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (car.currentTireMode == "WINTER") Color(0xFFEFF6FF) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (car.currentTireMode == "WINTER") Color(0xFF2563EB) else WarmLine
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditTireMileageClick("WINTER") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "윈터(스노우)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (car.currentTireMode == "WINTER") SoftInk else TextSecondaryDark,
                                    fontWeight = if (car.currentTireMode == "WINTER") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Text(
                                text = "터치하여 수정 ✎",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                                color = WarmGray
                            )
                        }
                        Text(
                            text = "%,d km".format(liveTire.winterTotalKm),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WheelTireItem(
    position: String,
    name: String,
    tire: maha.garage.data.local.entity.TireEntity?,
    currentVehicleMileage: Int,
    fallbackName: String,
    isRight: Boolean = false
) {
    val tireDisplayName = tire?.let { "${it.brand} ${it.productName}".trim() }
        ?.takeIf { it.isNotBlank() } ?: fallbackName
    val liveDistance = if (tire != null) {
        tire.cumulativeMileage + (currentVehicleMileage - tire.installedAtMileage).coerceAtLeast(0)
    } else {
        0
    }
    val specText = tire?.spec?.takeIf { it.isNotBlank() }
    val dotText = tire?.dot?.takeIf { it.isNotBlank() }?.let { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.length == 4 && trimmed.all { it.isDigit() } -> {
                val week = trimmed.substring(0, 2)
                val year = trimmed.substring(2, 4)
                "DOT $trimmed ('20${year}년 ${week}주)"
            }
            trimmed.all { it.isDigit() } && trimmed.length == 2 -> "20${trimmed}년식"
            trimmed.all { it.isDigit() } && trimmed.length == 4 && trimmed.startsWith("20") -> "${trimmed}년식"
            trimmed.contains("년") -> trimmed
            else -> "연식/DOT: $trimmed"
        }
    }
    val treadText = tire?.treadDepthMm?.let { "잔여 ${it}mm" }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isRight) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SoftInk.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                modifier = Modifier.size(width = 18.dp, height = 44.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isRight) Alignment.End else Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GarageAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = position,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GarageAccent,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = name, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tireDisplayName,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (specText != null || dotText != null) {
                Text(
                    text = listOfNotNull(specText, dotText).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark,
                    maxLines = 1
                )
            }
            Text(
                text = "누적 %,d km".format(liveDistance) + if (treadText != null) " ($treadText)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = GarageAccent,
                fontWeight = FontWeight.Bold
            )
        }

        if (isRight) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SoftInk.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                modifier = Modifier.size(width = 18.dp, height = 44.dp)
            ) {}
        }
    }
}

/**
 * 소모품 관리 카드 (Section 4.1 & Section 8)
 * - 한 줄에 4개/2개 그리드에 최적화된 간결한 디자인
 * - 미등록 시: "상태: 입력대기", "최근 교체정보 없음", "[정보 등록]" 버튼
 * - 등록 시: 상태 배지, 마지막 교체거리, 다음 교체 예정, 남은 거리, "[교체 기록]" 버튼
 */
@Composable
fun UnifiedConsumableCard(
    cardData: ConsumableCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (cardData.status) {
                DueStatus.OVERDUE, DueStatus.DUE_NOW -> StatusOverdue.copy(alpha = 0.5f)
                DueStatus.DUE_SOON -> StatusDueSoon.copy(alpha = 0.5f)
                else -> WarmLine
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // 헤더: 소모품 명칭 + 상태 배지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardData.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                StatusBadge(status = cardData.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (cardData.isNeedsData) {
                // 입력대기 상태 (임의 교체 예정거리나 진행률 바 미표시)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "교환 기록 없음",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cardData.statusDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ToolYellowSoft, contentColor = SoftInk),
                    border = BorderStroke(1.dp, ToolYellow.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = ToolOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("정보/기준 등록", style = MaterialTheme.typography.labelSmall, color = SoftInk, fontWeight = FontWeight.Bold)
                }
            } else {
                // 정상 등록된 상태: 교환 예정 기준이 있는 경우에만 진행률 게이지 표시 (기준 없으면 숨김)
                if (cardData.nextDueText != null && cardData.progressRatio > 0f) {
                    ProgressGauge(ratio = cardData.progressRatio, status = cardData.status)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (cardData.isBaseline) "기준: ${cardData.lastServiceText ?: "-"}" else "최근: ${cardData.lastServiceText ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cardData.isBaseline) GarageAccent else TextSecondaryDark,
                            maxLines = 1
                        )
                        Text(
                            text = "예정: ${cardData.nextDueText ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = cardData.remainingText ?: cardData.statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (cardData.status) {
                            DueStatus.OVERDUE, DueStatus.DUE_NOW -> StatusOverdue
                            DueStatus.DUE_SOON -> StatusDueSoon
                            else -> TextPrimaryDark
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = ToolOrange),
                    border = BorderStroke(1.dp, ToolOrange.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = ToolOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("교체 기록", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Section 5 순서에 맞춘 14개 관리 항목 리스트 정렬 & 통합 구성 함수
 */
fun buildOrderedConsumablesList(evaluations: List<ItemEvaluation>, car: CarEntity? = null): List<ConsumableCardData> {
    val evalMap = evaluations.associateBy { it.itemCode }
    val result = mutableListOf<ConsumableCardData>()

    // 1. 엔진오일 (오일필터와 하나의 관리 카드로 통합)
    evalMap["ENGINE_OIL"]?.let { eval ->
        result.add(createCardData("ENGINE_OIL", "엔진오일", eval))
    }

    // 2. 엔진흡기 에어클리너
    evalMap["ENGINE_AIR_FILTER"]?.let { eval ->
        result.add(createCardData("ENGINE_AIR_FILTER", "엔진흡기 에어클리너", eval))
    }

    // 3. 에어컨 필터
    evalMap["CABIN_FILTER"]?.let { eval ->
        result.add(createCardData("CABIN_FILTER", "에어컨 필터", eval))
    }

    // 4. 와이퍼
    evalMap["WIPER_BLADE"]?.let { eval ->
        result.add(createCardData("WIPER_BLADE", "와이퍼", eval))
    }

    // 5. 12V 시동 배터리 / 전기차 저전압 배터리
    evalMap["BATTERY_12V"]?.let { eval ->
        val batteryTitle = if (car?.powertrain == maha.garage.data.model.enums.PowertrainType.BEV) {
            val modelLower = car.modelName.lowercase()
            val makeLower = car.make.lowercase()
            if (modelLower.contains("cybertruck") || modelLower.contains("사이버트럭")) {
                "48V 저전압 배터리 (리튬이온)"
            } else if (makeLower.contains("tesla") || makeLower.contains("테슬라")) {
                if (car.modelYear >= 2022) "16V 저전압 배터리 (리튬이온)" else "12V 보조 배터리 (납산)"
            } else {
                "12V 보조 배터리"
            }
        } else {
            "12V 시동 배터리"
        }
        result.add(createCardData("BATTERY_12V", batteryTitle, eval))
    }

    // 6. 미션오일 / 전기차 감속기 오일
    (evalMap["TRANSMISSION_OIL"] ?: evalMap["REDUCTION_GEAR_OIL"])?.let { eval ->
        val gearTitle = if (eval.itemCode == "REDUCTION_GEAR_OIL") {
            val makeLower = car?.make?.lowercase().orEmpty()
            if (makeLower.contains("tesla") || makeLower.contains("테슬라") || makeLower.contains("chevrolet") || makeLower.contains("쉐보레")) {
                "드라이브 유닛 오일 (감속기)"
            } else {
                "감속기 오일"
            }
        } else {
            "미션오일"
        }
        result.add(createCardData(eval.itemCode, gearTitle, eval))
    }

    // 7. 점화코일
    evalMap["IGNITION_COIL"]?.let { eval ->
        result.add(createCardData("IGNITION_COIL", "점화코일", eval))
    }

    // 8. 점화플러그 세트
    evalMap["SPARK_PLUG"]?.let { eval ->
        result.add(createCardData("SPARK_PLUG", "점화플러그 세트", eval))
    }

    // 9. 냉각수 / 부동액 / 전기차 전용 냉각수
    (evalMap["COOLANT"] ?: evalMap["EV_COOLANT"])?.let { eval ->
        val coolantTitle = if (eval.itemCode == "EV_COOLANT") {
            val modelLower = car?.modelName?.lowercase().orEmpty()
            val makeLower = car?.make?.lowercase().orEmpty()
            if (modelLower.contains("코나") || modelLower.contains("니로") || modelLower.contains("포터") || modelLower.contains("봉고") || modelLower.contains("이모션")) {
                "저전도성 냉각수 (절연 전용)"
            } else if (makeLower.contains("tesla") || makeLower.contains("테슬라")) {
                "배터리 냉각수"
            } else if (makeLower.contains("chevrolet") || makeLower.contains("쉐보레")) {
                "냉각수 (DEX-COOL 3-Circuit)"
            } else {
                "전기차 냉각수 (고전압 배터리)"
            }
        } else {
            "냉각수 / 부동액"
        }
        result.add(createCardData(eval.itemCode, coolantTitle, eval))
    }

    // 10. 타이밍벨트(겉벨트) (버튼 1)
    evalMap["TIMING_BELT_SET"]?.let { eval ->
        result.add(createCardData("TIMING_BELT_SET", "타이밍벨트(겉벨트)", eval))
    }

    // 11. 타이밍벨트(워터펌프) (버튼 2 - 신규 신설)
    evalMap["TIMING_BELT_WATER_PUMP"]?.let { eval ->
        result.add(createCardData("TIMING_BELT_WATER_PUMP", "타이밍벨트(워터펌프)", eval))
    }

    // 하이브리드 구동벨트 (HSG) - 하이브리드 전용
    evalMap["HSG_BELT"]?.let { eval ->
        result.add(createCardData("HSG_BELT", "하이브리드 구동벨트 (HSG)", eval))
    }

    // 11. 브레이크 패드 (전륜 / 후륜 통합 카드 - 시급도 우선순위 및 개별 상태 반영)
    val frontPad = evalMap["BRAKE_PAD_FRONT"]
    val rearPad = evalMap["BRAKE_PAD_REAR"]
    if (frontPad != null || rearPad != null) {
        fun statusPriority(s: DueStatus?): Int = when (s) {
            DueStatus.OVERDUE -> 100
            DueStatus.DUE_NOW -> 90
            DueStatus.DUE_SOON -> 80
            DueStatus.INSPECTION_DUE -> 70
            DueStatus.NORMAL -> 50
            DueStatus.NEEDS_DATA -> 10
            null -> 0
        }

        val frontPrio = statusPriority(frontPad?.status)
        val rearPrio = statusPriority(rearPad?.status)
        val rep = if (frontPrio >= rearPrio) (frontPad ?: rearPad!!) else (rearPad ?: frontPad!!)
        val worstStatus = rep.status
        val isNeedsData = (frontPad == null || frontPad.status == DueStatus.NEEDS_DATA) &&
                          (rearPad == null || rearPad.status == DueStatus.NEEDS_DATA)

        val detailDesc = buildString {
            if (frontPad != null) {
                val fTxt = if (frontPad.status == DueStatus.NEEDS_DATA) "기록없음" else (frontPad.remainingKm?.let { "약 %,dkm".format(it) } ?: frontPad.status.displayName)
                append("전륜: $fTxt")
            }
            if (frontPad != null && rearPad != null) append(" / ")
            if (rearPad != null) {
                val rTxt = if (rearPad.status == DueStatus.NEEDS_DATA) "기록없음" else (rearPad.remainingKm?.let { "약 %,dkm".format(it) } ?: rearPad.status.displayName)
                append("후륜: $rTxt")
            }
        }

        val lastServiceStr = "전: ${frontPad?.lastServiceMileage?.let { "%,dkm".format(it) } ?: "-"} / 후: ${rearPad?.lastServiceMileage?.let { "%,dkm".format(it) } ?: "-"}"

        result.add(
            ConsumableCardData(
                id = "BRAKE_PAD",
                title = "브레이크 패드",
                status = worstStatus,
                statusDescription = detailDesc,
                lastServiceText = lastServiceStr,
                nextDueText = rep.nextDueKm?.let { "%,d km".format(it) },
                remainingText = if (rep.remainingKm != null) "약 %,d km 남음".format(rep.remainingKm) else detailDesc,
                progressRatio = rep.progressRatio,
                isNeedsData = isNeedsData,
                rawItemCode = "BRAKE_PAD",
                isBaseline = frontPad?.isBaseline == true || rearPad?.isBaseline == true
            )
        )
    }

    // 12. 브레이크 로터
    evalMap["BRAKE_DISC"]?.let { eval ->
        result.add(createCardData("BRAKE_DISC", "브레이크 로터", eval))
    }

    // 13. 디퍼런셜 오일
    evalMap["DIFFERENTIAL_OIL"]?.let { eval ->
        result.add(createCardData("DIFFERENTIAL_OIL", "디퍼런셜 오일", eval))
    }

    // 14. 그 외 각종 오일 (브레이크액, 워셔액)
    evalMap["BRAKE_FLUID"]?.let { eval ->
        result.add(createCardData("BRAKE_FLUID", "브레이크액 (DOT4)", eval))
    }

    // 추가 적용 품목 (예: 디젤 연료필터, 트럭 에어드라이어 등)
    evalMap["FUEL_FILTER"]?.let { eval ->
        result.add(createCardData("FUEL_FILTER", "연료필터", eval))
    }
    evalMap["AIR_DRYER_CARTRIDGE"]?.let { eval ->
        result.add(createCardData("AIR_DRYER_CARTRIDGE", "에어드라이어 카트리지", eval))
    }

    return result
}

private fun createCardData(id: String, displayName: String, eval: ItemEvaluation): ConsumableCardData {
    val isNeedsData = eval.status == DueStatus.NEEDS_DATA
    val lastText = if (eval.isBaseline) {
        if (eval.baselineMileage == 0) {
            "신차 출고 (0 km)"
        } else {
            val baseKmText = eval.baselineMileage?.let { "%,d km".format(it) }
            val baseDateText = eval.baselineDate
            listOfNotNull(baseKmText, baseDateText, eval.baselineHours?.let { "${it}시간" }).firstOrNull() ?: "관리 시작"
        }
    } else {
        eval.lastServiceMileage?.let { "%,d km".format(it) } ?: eval.lastServiceDate ?: eval.lastServiceHours?.let { "${it}시간" }
    }
    val nextText = eval.nextDueKm?.let { "%,d km".format(it) } ?: eval.nextDueDateString ?: eval.nextDueHours?.let { "${it}시간" }
    val remText = when {
        isNeedsData -> null
        eval.remainingKm != null && eval.remainingKm < 0 -> "%,d km 초과".format(-eval.remainingKm)
        eval.remainingKm != null -> "약 %,d km 남음".format(eval.remainingKm)
        eval.remainingDays != null && eval.remainingDays < 0 -> "${-eval.remainingDays}일 초과"
        eval.remainingDays != null -> "${eval.remainingDays}일 남음"
        else -> null
    }

    return ConsumableCardData(
        id = id,
        title = displayName,
        status = eval.status,
        statusDescription = eval.statusDescription,
        lastServiceText = lastText,
        nextDueText = nextText,
        remainingText = remText,
        progressRatio = eval.progressRatio,
        isNeedsData = isNeedsData,
        rawItemCode = eval.itemCode,
        isBaseline = eval.isBaseline
    )
}


/** Compact status panel accompanying the vehicle illustration. */
@Composable
private fun ConsumableQuickPanel(
    consumables: List<ConsumableCardData>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("소모품 교환주기", color = TextPrimaryDark, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            // The full consumable grid remains below this at all screen sizes.
            consumables.take(6).forEach { item ->
                val dotColor = when (item.status) {
                    DueStatus.OVERDUE, DueStatus.DUE_NOW -> StatusOverdue
                    DueStatus.DUE_SOON -> StatusDueSoon
                    DueStatus.NORMAL -> StatusNormal
                    else -> StatusNeutral
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(item.rawItemCode) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(modifier = Modifier.size(9.dp), shape = androidx.compose.foundation.shape.CircleShape,
                        color = dotColor) {}
                    Spacer(Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium, color = TextPrimaryDark)
                        Text(if (item.isNeedsData) "교환 기록 없음" else item.remainingText ?: item.statusDescription,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "상세", tint = TextSecondaryDark,
                        modifier = Modifier.size(15.dp))
                }
                HorizontalDivider(color = BorderDark.copy(alpha = 0.6f), thickness = 0.5.dp)
            }
            Text("아래 전체 소모품 목록에서 모든 항목 확인", color = TextSecondaryDark,
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
