package maha.garage.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.FuelRecordEntity
import maha.garage.ui.components.*
import maha.garage.ui.dashboard.component.AddFuelRecordDialog
import maha.garage.ui.dashboard.component.EmptyFuelRecordCard
import maha.garage.ui.dashboard.component.FuelRecordItemCard
import maha.garage.ui.dashboard.component.MichaelStyleFuelSummaryCard
import maha.garage.ui.dashboard.component.ReceiptScanFloatingButton
import maha.garage.ui.dashboard.component.ReceiptScannerDialog
import maha.garage.ui.dashboard.component.SmartFuelStatsCard
import maha.garage.ui.theme.*
import maha.garage.util.ParsedReceiptData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    vehicleViewModel: maha.garage.ui.vehicle.VehicleViewModel,
    onNavigateToVehicleDetail: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToAddService: (String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showAddFuelDialog by remember { mutableStateOf(false) }
    var showReceiptScanner by remember { mutableStateOf(false) }
    var currentReceiptData by remember { mutableStateOf<ParsedReceiptData?>(null) }
    var recordToDelete by remember { mutableStateOf<FuelRecordEntity?>(null) }
    val vehicleRowState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lastTappedVehicleId by remember { mutableStateOf<String?>(null) }
    var lastTapTimestamp by remember { mutableStateOf(0L) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (state.vehiclesWithStatus.isNotEmpty()) {
                ReceiptScanFloatingButton(
                    onClick = { showReceiptScanner = true }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                        shadowElevation = 3.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ToolYellowSoft,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ToolYellow.copy(alpha = 0.6f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalGasStation,
                                        contentDescription = null,
                                        tint = ToolOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "실주행 연비 & 주유",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SoftInk
                                )
                                Text(
                                    text = "Full-to-Full 오차보정 트래커",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGray
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = { showAddFuelDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "주유 기록 추가",
                                tint = ToolOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onNavigateToAddVehicle) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "차량 등록",
                                tint = WarmGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ToolOrange)
            }
        } else if (state.vehiclesWithStatus.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "등록된 차량이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftInk
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "차량을 먼저 등록하고 스마트 연비 분석을 시작해보세요.",
                            fontSize = 13.sp,
                            color = WarmGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToAddVehicle,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolOrange)
                        ) {
                            Text("차량 등록하기", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // 1. 차량 선택 탭 / 칩 셀렉터
                item {
                    LazyRow(
                        state = vehicleRowState,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(state.vehiclesWithStatus, key = { _, v -> v.car.id }) { index, vehicle ->
                            val isSelected = index == state.selectedVehicleIndex
                            val isPinned = vehicle.car.id == state.pinnedVehicleId
                            VehicleSelectorChip(
                                vehicle = vehicle.car,
                                isSelected = isSelected,
                                isPinned = isPinned,
                                worstStatus = vehicle.worstStatus,
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val isDoubleTap = (lastTappedVehicleId == vehicle.car.id && (now - lastTapTimestamp) < 600L)
                                    lastTappedVehicleId = vehicle.car.id
                                    lastTapTimestamp = now

                                    if (isSelected || isDoubleTap) {
                                        // 이미 선택된 상태에서 다시 탭하거나 연속 두 번 탭: 맨 왼쪽(0번 인덱스)으로 이동 및 핀 고정
                                        viewModel.pinAndMoveToFirst(vehicle.car.id)
                                        coroutineScope.launch {
                                            vehicleRowState.animateScrollToItem(0)
                                        }
                                    } else {
                                        // 1회 탭: 차량 선택
                                        viewModel.selectVehicle(index)
                                    }
                                },
                                onTogglePin = { viewModel.togglePin(vehicle.car.id) }
                            )
                        }
                    }
                }

                // 2. 선택된 차량 메인 연비 카드 (MichaelStyleFuelSummaryCard)
                state.selectedVehicle?.let { vehicleWithStatus ->
                    val car = vehicleWithStatus.car

                    item {
                        MichaelStyleFuelSummaryCard(
                            recentEconomy = state.recentEconomy,
                            averageEconomy = state.averageEconomy,
                            officialEconomy = state.officialEconomy,
                            recentEconomies = state.recentEconomies,
                            onAddRefuelClick = { showAddFuelDialog = true },
                            isPinned = state.isCurrentVehiclePinned,
                            onTogglePin = { viewModel.togglePinCurrentVehicle() }
                        )
                    }

                    // 3. 스마트 주유 통계 카드
                    item {
                        SmartFuelStatsCard(
                            monthlyExpense = state.monthlyFuelExpense,
                            monthlyFuelAmount = state.monthlyFuelAmount,
                            costPerKm = state.costPerKm
                        )
                    }

                    // 4. 주유 기록 섹션 헤더
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "주유 이력 (${state.fuelRecords.size}건)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftInk
                            )
                            TextButton(
                                onClick = { showAddFuelDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ToolOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "주유 입력",
                                    color = ToolOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 5. 주유 기록 리스트 (없을 경우 안내 카드)
                    if (state.fuelRecords.isEmpty()) {
                        item {
                            EmptyFuelRecordCard(
                                onAddRefuelClick = { showAddFuelDialog = true }
                            )
                        }
                    } else {
                        items(state.fuelRecords, key = { it.id }) { record ->
                            FuelRecordItemCard(
                                record = record,
                                onDeleteClick = { recordToDelete = record }
                            )
                        }
                    }

                    // 6. 소모품 정비 알림 배너 (만약 초과/임박 항목이 있을 시 컴팩트하게 노출)
                    if (state.totalOverdueCount > 0 || state.totalDueSoonCount > 0) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            AlertBanner(
                                overdueCount = state.totalOverdueCount,
                                dueSoonCount = state.totalDueSoonCount,
                                onClick = { onNavigateToVehicleDetail(car.id) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(28.dp)) }
            }
        }
    }

    // 영수증 카메라 OCR 스캐너 다이얼로그
    if (showReceiptScanner) {
        ReceiptScannerDialog(
            onDataParsed = { parsedData ->
                showReceiptScanner = false
                currentReceiptData = parsedData
                showAddFuelDialog = true
            },
            onDismiss = { showReceiptScanner = false }
        )
    }

    // 주유 기록 등록 모달
    if (showAddFuelDialog) {
        state.selectedVehicle?.car?.let { car ->
            AddFuelRecordDialog(
                carName = "${car.make} ${car.modelName} (${car.carNumber})",
                currentCarMileage = car.currentMileage,
                historyRecords = state.fuelRecords.reversed(), // 과거 -> 최신 순
                initialReceiptData = currentReceiptData,
                onScanReceiptClick = {
                    showAddFuelDialog = false
                    showReceiptScanner = true
                },
                onDismiss = {
                    showAddFuelDialog = false
                    currentReceiptData = null
                },
                onConfirm = { odo, amount, unit, total, isFull, isMissed, station ->
                    viewModel.addFuelRecord(
                        odometer = odo,
                        fuelAmount = amount,
                        unitPrice = unit,
                        totalPrice = total,
                        isFull = isFull,
                        isMissedPrevious = isMissed,
                        gasStationName = station
                    )
                    showAddFuelDialog = false
                    currentReceiptData = null
                }
            )
        }
    }

    // 주유 기록 삭제 확인 다이얼로그
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("주유 기록 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("선택한 주유 기록(${record.odometer}km, ${record.totalPrice}원)을 삭제하시겠습니까?\n삭제 시 이후 주유의 연비 계산에 영향이 있을 수 있습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFuelRecord(record)
                        recordToDelete = null
                    }
                ) {
                    Text("삭제", color = StatusOverdue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun AlertBanner(
    overdueCount: Int,
    dueSoonCount: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (overdueCount > 0) StatusOverdue.copy(alpha = 0.12f) else StatusDueSoon.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (overdueCount > 0) StatusOverdue.copy(alpha = 0.4f) else StatusDueSoon.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (overdueCount > 0) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (overdueCount > 0) StatusOverdue else StatusDueSoon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (overdueCount > 0) "정비 주기 초과 (${overdueCount}건)" else "정비 임박 (${dueSoonCount}건)",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (overdueCount > 0) StatusOverdue else StatusDueSoon,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "주행거리 또는 권장 교체 일수를 점검하세요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGray
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = WarmGray
            )
        }
    }
}

@Composable
fun VehicleSelectorChip(
    vehicle: CarEntity,
    isSelected: Boolean,
    isPinned: Boolean = false,
    worstStatus: maha.garage.data.model.enums.DueStatus,
    onClick: () -> Unit,
    onTogglePin: () -> Unit = {}
) {
    val bg = if (isSelected) ToolOrange else Color.White
    val border = if (isSelected) ToolOrange else if (isPinned) Color(0xFFFFB74D) else WarmLine
    val textColor = if (isSelected) Color.White else SoftInk

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(if (isPinned) 1.5.dp else 1.dp, border),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = if (isPinned || isSelected) 8.dp else 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 차량 고정 (Pin) 아이콘 버튼
            if (isPinned || isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePin),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "차량 고정 해제",
                            tint = if (isSelected) Color(0xFFFFE082) else ToolOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "차량 고정",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = "${vehicle.make} ${vehicle.modelName}".ifBlank { vehicle.carNumber },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = worstStatus)
        }
    }
}
