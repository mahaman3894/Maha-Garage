package maha.garage.ui.history

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.ui.theme.*
import maha.garage.util.CarHistoryPdfGenerator
import java.time.LocalDate

enum class HistoryPeriodOption(val title: String, val months: Long?) {
    LAST_6_MONTHS("최근 6개월", 6L),
    LAST_1_YEAR("최근 1년", 12L),
    LAST_2_YEARS("최근 2년", 24L),
    ALL_TIME("전체 기간", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarHistoryScreen(
    cars: List<CarEntity>,
    loadVisits: (String, String?) -> kotlinx.coroutines.flow.Flow<List<ServiceVisitWithLines>>,
    initialCarId: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedCarId by remember {
        mutableStateOf(initialCarId ?: cars.firstOrNull()?.id.orEmpty())
    }
    val currentCar = remember(selectedCarId, cars) {
        cars.find { it.id == selectedCarId } ?: cars.firstOrNull()
    }

    var selectedPeriod by remember { mutableStateOf(HistoryPeriodOption.LAST_1_YEAR) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showPdfOptionDialog by remember { mutableStateOf(false) }
    var selectedDetailVisit by remember { mutableStateOf<ServiceVisitWithLines?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // 선택된 차량 및 기간에 따른 필터링 (기본 최근 1년)
    val today = remember { LocalDate.now() }
    val visitsFlow = remember(currentCar?.id, selectedPeriod, today) {
        currentCar?.let { loadVisits(it.id, selectedPeriod.months?.let { months -> today.minusMonths(months).toString() }) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList<ServiceVisitWithLines>())
    }
    val filteredVisits by visitsFlow.collectAsState(initial = emptyList())

    val periodSummaryText = remember(selectedPeriod) {
        val months = selectedPeriod.months
        if (months != null) {
            "${selectedPeriod.title} (${today.minusMonths(months)} ~ $today)"
        } else {
            "전체 기간"
        }
    }

    val totalCost = remember(filteredVisits) {
        filteredVisits.sumOf { it.visit.totalCost }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "카 히스토리 (정비 이력서)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            // 하단 버튼 바: [기간 선택] [PDF로 보내기]
            Surface(
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPeriodDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "기간 선택", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (currentCar != null) {
                                showPdfOptionDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "PDF로 보내기", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (cars.isEmpty() || currentCar == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("등록된 차량이 없습니다.", color = TextSecondaryDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                // 차량 선택 칩 (다중 차량 지원)
                if (cars.size > 1) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(cars) { carItem ->
                                val isSelected = carItem.id == currentCar.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceDark,
                                    border = BorderStroke(1.dp, if (isSelected) GaragePrimaryLight else BorderDark),
                                    modifier = Modifier.clickable { selectedCarId = carItem.id }
                                ) {
                                    Text(
                                        text = "${carItem.make} ${carItem.modelName} (${carItem.carNumber})",
                                        color = if (isSelected) GaragePrimaryLight else TextSecondaryDark,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. 차량 정보 영역
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = GaragePrimaryLight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "차량 정보",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GarageAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = currentCar.carNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GarageAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("차량명", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentCar.make} ${currentCar.modelName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("연식 / 파워트레인", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentCar.modelYear}년식 • ${currentCar.powertrain.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimaryDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("현재 누적 주행거리", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "%,d km".format(currentCar.currentMileage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GaragePrimaryLight
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("보험사 / 정기검사", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val insComp = currentCar.insuranceCompany ?: "보험 미등록"
                                    val inspEnd = currentCar.inspectionEndDate?.let { "$it 만료" } ?: "검사일 미등록"
                                    Text(
                                        text = "$insComp • $inspEnd",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. 요약 영역 (조회 기간 · 정비 횟수 · 기록된 비용 합계)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Summarize, contentDescription = null, tint = GarageAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "정비 이력 요약",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 조회 기간
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Text("조회 기간", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = selectedPeriod.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }

                                // 정비 횟수
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Text("정비 횟수", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${filteredVisits.size} 회",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GaragePrimaryLight
                                    )
                                }

                                // 비용 합계
                                Column(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Text("기록된 비용 합계", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "%,d 원".format(totalCost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GarageAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 정비 내역 (방문 1건당 1줄 표 형식)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "정비 내역 목록 (${filteredVisits.size}건)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "항목을 누르면 상세 내용을 확인합니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // 테이블 헤더 (날짜 / 주행거리 / 작업 내용 / 비용)
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(0.5.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("날짜", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                            Text("주행거리", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                            Text("작업 내용", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                            Text("비용", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                }

                if (filteredVisits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "선택된 기간(${selectedPeriod.title}) 내 정비 기록이 없습니다.",
                                color = TextSecondaryDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(filteredVisits) { visitItem ->
                        CarHistoryRowItem(
                            item = visitItem,
                            onClick = { selectedDetailVisit = visitItem }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    // 기간 선택 다이얼로그
    if (showPeriodDialog) {
        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("조회 기간 선택", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryPeriodOption.values().forEach { option ->
                        val isSelected = selectedPeriod == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) GaragePrimaryLight else BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPeriod = option
                                    showPeriodDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.title,
                                    color = if (isSelected) GaragePrimaryLight else TextPrimaryDark,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GaragePrimaryLight, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPeriodDialog = false }) {
                    Text("닫기", color = TextSecondaryDark)
                }
            }
        )
    }

    // PDF 출력 형식 선택 다이얼로그 (한 장 요약 vs 전체 이력)
    if (showPdfOptionDialog && currentCar != null) {
        AlertDialog(
            onDismissRequest = { showPdfOptionDialog = false },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = GaragePrimaryLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF 정비 이력서 출력", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "출력할 PDF 문서 형식을 선택해 주세요.\n공유하기를 누르면 카카오톡, 이메일, 프린트 등으로 즉시 보낼 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPdfOptionDialog = false
                                if (currentCar != null) {
                                    coroutineScope.launch {
                                        isGeneratingPdf = true
                                        try {
                                            val pdfFile = CarHistoryPdfGenerator.generatePdf(
                                                context = context,
                                                car = currentCar,
                                                periodText = periodSummaryText,
                                                visits = filteredVisits,
                                                isSummaryOnly = true
                                            )
                                            CarHistoryPdfGenerator.sharePdf(context, currentCar, pdfFile)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "PDF 생성 중 오류 발생: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📄 한 장 요약 PDF 출력 (권장)", fontWeight = FontWeight.Bold, color = GaragePrimaryLight)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("차량 정보, 정비 요약, 최근 주요 이력을 깔끔하게 1장에 담아 제출용으로 적합합니다.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPdfOptionDialog = false
                                if (currentCar != null) {
                                    coroutineScope.launch {
                                        isGeneratingPdf = true
                                        try {
                                            val pdfFile = CarHistoryPdfGenerator.generatePdf(
                                                context = context,
                                                car = currentCar,
                                                periodText = periodSummaryText,
                                                visits = filteredVisits,
                                                isSummaryOnly = false
                                            )
                                            CarHistoryPdfGenerator.sharePdf(context, currentCar, pdfFile)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "PDF 생성 중 오류 발생: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📑 전체 정비 이력 PDF 출력", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("조회된 모든 정비 기록을 세부 작업별로 완전 전개하여 누락 없이 출력합니다.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPdfOptionDialog = false }) {
                    Text("취소", color = TextSecondaryDark)
                }
            }
        )
    }

    // PDF 생성 로딩 인디케이터 다이얼로그 (메인 스레드 멈춤 방지 및 진행 표시)
    if (isGeneratingPdf) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = GaragePrimaryLight,
                        strokeWidth = 3.dp
                    )
                    Column {
                        Text(
                            text = "카 히스토리 PDF 생성 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "정비 기록을 페이지별로 구성하고 있습니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }

    // 정비 방문 1건 상세 모달 다이얼로그 (영수증 사진 및 세부 부품 규격 표시)
    selectedDetailVisit?.let { detail ->
        CarHistoryDetailDialog(
            visitWithLines = detail,
            car = currentCar,
            onDismiss = { selectedDetailVisit = null }
        )
    }
}

/**
 * 정비 내역 한 건당 한 줄 행 아이템 (날짜 / 주행거리 / 작업 내용 / 비용)
 */
@Composable
fun CarHistoryRowItem(
    item: ServiceVisitWithLines,
    onClick: () -> Unit
) {
    val visit = item.visit
    val lines = item.lines

    val workSummary = remember(lines, visit) {
        if (lines.isNotEmpty()) {
            val first = lines.first()
            val itemName = maha.garage.data.seed.DefaultCatalogData.items.find { it.itemCode == first.itemCode }?.displayName ?: first.itemCode
            val actionName = when (first.actionType) {
                ActionType.REPLACE -> "교체"
                ActionType.INSPECT -> "점검"
                ActionType.REFILL -> "보충"
                ActionType.CLEAN -> "클리닝"
                ActionType.ROTATE -> "위치교환"
                ActionType.SWAP -> "스왑"
            }
            val desc = "$itemName $actionName"
            if (lines.size > 1) "$desc 외 ${lines.size - 1}건" else desc
        } else {
            visit.notes ?: "정비 점검"
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(0.5.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visit.serviceDate,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
            Text(
                text = "%,d km".format(visit.odometerKm),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = GaragePrimaryLight,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = workSummary,
                modifier = Modifier.weight(1.8f),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "%,d원".format(visit.totalCost),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * 한 건당 상세 화면 연결 다이얼로그 (영수증 사진, 세부 부품 규격, 메모 등)
 */
@Composable
fun CarHistoryDetailDialog(
    visitWithLines: ServiceVisitWithLines,
    car: CarEntity?,
    onDismiss: () -> Unit
) {
    val visit = visitWithLines.visit
    val lines = visitWithLines.lines

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("정비 상세 내역", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GaragePrimaryLight.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "%,d 원".format(visit.totalCost),
                        color = GaragePrimaryLight,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .padding(10.dp)
                    ) {
                        Text("정비 일자: ${visit.serviceDate} (%,d km)".format(visit.odometerKm), style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        if (visit.workshop != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("정비소: ${visit.workshop}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                    }
                }

                item {
                    Text("세부 작업 내역 (${lines.size}건)", style = MaterialTheme.typography.labelMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }

                items(lines) { line ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(0.5.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val itemName = maha.garage.data.seed.DefaultCatalogData.items.find { it.itemCode == line.itemCode }?.displayName ?: line.itemCode
                                val lineCost = line.partsCost + line.laborCost
                                Text(itemName, fontWeight = FontWeight.Bold, color = TextPrimaryDark, style = MaterialTheme.typography.bodySmall)
                                Text("%,d 원".format(lineCost), fontWeight = FontWeight.Bold, color = GaragePrimaryLight, style = MaterialTheme.typography.bodySmall)
                            }
                            if (line.partSpecification != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("규격/부품: ${line.partSpecification}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            }
                        }
                    }
                }

                if (visit.notes != null) {
                    item {
                        Column {
                            Text("메모", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(visit.notes, style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                        }
                    }
                }

                // 영수증 사진 (상세 화면에서만 보여줌)
                if (visit.receiptPhotoUri != null) {
                    item {
                        Column {
                            Text("영수증 / 명세서 사진", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(
                                model = Uri.parse(visit.receiptPhotoUri),
                                contentDescription = "영수증 사진",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight)
            ) {
                Text("닫기", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    )
}
