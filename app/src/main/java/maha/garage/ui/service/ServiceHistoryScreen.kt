package maha.garage.ui.service

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.local.entity.TireSwapLogEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.ui.components.TireModeBadge
import maha.garage.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    viewModel: ServiceViewModel,
    onNavigateToAddService: (String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 정비 방문 이력, 1: 타이어 스왑 이력

    val selectedCar = state.cars.find { it.id == state.selectedCarId }
    val swapLogsFlow = remember(state.selectedCarId) {
        state.selectedCarId?.let { viewModel.loadSwapLogs(it) }
    }
    val swapLogs by (swapLogsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "정비 & 스왑 이력",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddService(state.selectedCarId) },
                containerColor = GaragePrimaryLight,
                contentColor = TextPrimaryDark,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("정비 기록 추가", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 탭 선택: 정비 이력 vs 타이어 스왑
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = GaragePrimaryLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GaragePrimaryLight
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "정비 내역 (${state.visits.size}건)",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) GaragePrimaryLight else TextSecondaryDark
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "타이어 스왑 로그",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) GaragePrimaryLight else TextSecondaryDark
                        )
                    }
                )
            }

            // 차량 필터 칩 목록
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCarId == null,
                        onClick = { viewModel.selectCar(null) },
                        label = { Text("전체 차량") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GaragePrimaryLight.copy(alpha = 0.25f),
                            selectedLabelColor = GaragePrimaryLight,
                            containerColor = SurfaceVariantDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
                items(state.cars) { car ->
                    val isSelected = state.selectedCarId == car.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCar(car.id) },
                        label = { Text("${car.make} ${car.modelName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GaragePrimaryLight.copy(alpha = 0.25f),
                            selectedLabelColor = GaragePrimaryLight,
                            containerColor = SurfaceVariantDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }

            if (selectedTab == 0) {
                // 1. 정비 방문 이력 탭
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        // 총 지출 요약 배너
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (selectedCar != null) "${selectedCar.modelName} 누적 정비비용" else "선택 차량 총 누적 정비비용",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "%,d 원".format(state.totalExpense),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GaragePrimaryLight
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = GaragePrimaryLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (state.visits.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "등록된 정비 이력이 없습니다.",
                                        color = TextSecondaryDark,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.visits) { visitWithLines ->
                            val car = state.cars.find { it.id == visitWithLines.visit.carId }
                            ServiceVisitCard(visitWithLines = visitWithLines, carName = car?.let { "${it.make} ${it.modelName}" } ?: "차량")
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            } else {
                // 2. 타이어 스왑 이력 탭
                if (state.selectedCarId == null && state.cars.isNotEmpty()) {
                    // 전체 차량일 때 안내
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "상단 칩에서 타이어 스왑 이력을 조회할 특정 차량을 선택해주세요.",
                            color = TextSecondaryDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${selectedCar?.make ?: ""} ${selectedCar?.modelName ?: ""} 타이어 스왑 기록 (${swapLogs.size}회)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        if (swapLogs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = GarageAccent,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "기록된 타이어 스왑 이력이 없습니다.",
                                            color = TextSecondaryDark,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        } else {
                            items(swapLogs) { log ->
                                TireSwapHistoryCard(log = log)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceVisitCard(
    visitWithLines: ServiceVisitWithLines,
    carName: String
) {
    val visit = visitWithLines.visit
    val lines = visitWithLines.lines
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더: 정비 일자, 차량명, 총 비용
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = visit.serviceDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GaragePrimaryLight
                    )
                    Text(
                        text = "$carName • %,d km".format(visit.odometerKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GaragePrimaryLight.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "%,d 원".format(visit.totalCost),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GaragePrimaryLight,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (visit.workshop != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visit.workshop,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 다중 작업 항목들 (1회 방문 N개 작업)
            Text(
                text = "수행된 작업 (${lines.size}건):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            lines.forEach { line ->
                ServiceLineItemRow(line = line)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (visit.notes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "메모: ${visit.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceLineItemRow(line: ServiceLineEntity) {
    val actionLabel = when (line.actionType) {
        ActionType.REPLACE -> "교체"
        ActionType.INSPECT -> "점검"
        ActionType.REFILL -> "보충/교환"
        ActionType.CLEAN -> "클리닝"
        ActionType.ROTATE -> "위치교환"
        ActionType.SWAP -> "스왑"
    }

    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GaragePrimaryLight.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = GaragePrimaryLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = line.itemCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                val lineCost = line.partsCost + line.laborCost
                if (lineCost > 0) {
                    Text(
                        text = "%,d 원".format(lineCost),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (line.partSpecification != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "규격: ${line.partSpecification}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }

            if (line.conditionValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "측정/상태: ${line.conditionValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusNormal
                )
            }
        }
    }
}

@Composable
fun TireSwapHistoryCard(log: TireSwapLogEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = GarageAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.swapDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                TireModeBadge(mode = log.toMode)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "스왑 시점 주행거리: %,d km".format(log.swapMileage),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )

            if (log.storageLocation != null) {
                Text(
                    text = "보관 장소: ${log.storageLocation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }

            if (log.frontTreadDepthMm != null || log.rearTreadDepthMm != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (log.frontTreadDepthMm != null) {
                        Text(
                            text = "전륜 잔여: %.1f mm".format(log.frontTreadDepthMm),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusNormal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (log.rearTreadDepthMm != null) {
                        Text(
                            text = "후륜 잔여: %.1f mm".format(log.rearTreadDepthMm),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusNormal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (log.swapCost > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "교체 비용: %,d 원".format(log.swapCost),
                    style = MaterialTheme.typography.labelSmall,
                    color = GaragePrimaryLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
