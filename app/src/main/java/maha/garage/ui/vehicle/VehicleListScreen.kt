package maha.garage.ui.vehicle

import maha.garage.ui.tutorial.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import maha.garage.domain.model.VehicleWithStatus
import maha.garage.ui.components.VehicleTypeIllustration
import maha.garage.ui.components.getVehicleIllustrationRes
import maha.garage.ui.theme.*
import androidx.compose.ui.platform.LocalContext

/**
 * 내 차량 페이지 (VehicleListScreen)
 * ui.md v3 반영:
 * 1. 밝은 종이 배경 + 반투명 화이트 카드 레이아웃
 * 2. 8종 대표 스케치 WebP 이미지 (2:1 비율 보존) 적용
 * 3. 카드에 차량 이름, 번호판 배지, 주행거리, 정기 갱신 요약 표시
 * 4. 카드 클릭 → 주행거리 입력 → 저장 완료 후 상세 대시보드 이동
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit
) {
    val state by viewModel.listState.collectAsState()
    val context = LocalContext.current

    var selectedCarId by rememberSaveable { mutableStateOf<String?>(null) }
    var savingMileage by remember { mutableStateOf(false) }
    var mileageError by remember { mutableStateOf<String?>(null) }
    val tutorial = LocalTutorial.current
    LaunchedEffect(tutorial?.index) {
        if (tutorial != null) selectedCarId = if (tutorial.cue.target.startsWith("mileage.")) TutorialFixtures.CAR_ID else null
    }
    val selectedCar = state.vehiclesWithStatus.find { it.car.id == selectedCarId }?.car

    Scaffold(
        modifier = Modifier.blur(if (selectedCar != null) 8.dp else 0.dp),
        containerColor = Color.Transparent,
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
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = ToolOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "내 차고",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SoftInk
                                )
                                Text(
                                    text = "등록 차량 선택 및 통합 관리",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGray
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = SoftInk
                )
            )
        },
        floatingActionButton = {
            val tutorialAction2 = onNavigateToAddVehicle
ExtendedFloatingActionButton(
modifier = Modifier.tutorialTarget("garage.add", tutorialAction2),
                onClick = tutorialAction2,
                containerColor = ToolYellow,
                contentColor = SoftInk,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SoftInk) },
                text = { Text("신규 차량 등록", fontWeight = FontWeight.Bold, color = SoftInk) }
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
                CircularProgressIndicator(color = GaragePrimaryLight)
            }
        } else if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "등록된 차량이 없습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "신규 차량을 등록하여 차계부와 소모품 관리를 시작해 보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToAddVehicle,
                        colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("첫 번째 차량 등록하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // 운용 2대 이상이면 휴면도 같은 카드 너비. 운용 1대는 와이드, 휴면은 별도 분할.
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val activeColumns = garageColumnCount(maxWidth.value, state.activeVehicles.size)
            val restColumns = garageRestColumnCount(maxWidth.value, state.activeVehicles.size, state.dormantVehicles.size)
            LazyVerticalGrid(
                columns = GridCells.Fixed(12),
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 활성 운용 차량 영역
                if (state.activeVehicles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "현재 운용 중인 활성 차량이 없습니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "신규 차량을 등록하거나 하단 Rest Area에서 운용을 재개해 보세요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(state.activeVehicles, key = { it.car.id }, span = { GridItemSpan(12 / activeColumns) }) { vehicle ->
                        VehicleIllustrationCard(
                            vehicleWithStatus = vehicle,
                            modifier = Modifier.tutorialTarget("garage.car") { selectedCarId = vehicle.car.id; mileageError = null },
                            onClick = { selectedCarId = vehicle.car.id; mileageError = null }
                        )
                    }
                }

                // 2. Rest Area (휴면 차고) 섹션 - 항상 하단부에 위치하며 신차가 등록되면 자연스럽게 밀려서 내려감
                if (state.dormantVehicles.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        RestAreaSectionHeader(count = state.dormantVehicles.size)
                    }

                    items(state.dormantVehicles, key = { it.car.id }, span = { GridItemSpan(12 / restColumns) }) { vehicle ->
                        DormantVehicleCard(
                            vehicleWithStatus = vehicle,
                            onClick = { onNavigateToDetail(vehicle.car.id) },
                            onResumeOperation = {
                                viewModel.unarchiveVehicle(vehicle.car.id) {
                                    maha.garage.worker.WorkManagerScheduler.triggerImmediateCheck(context)
                                }
                            }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            }
        }
    }
    selectedCar?.let { car ->
        UpdateMileageDialog(
            car = car,
            isSaving = savingMileage,
            saveError = mileageError,
            onDismiss = { selectedCarId = null; mileageError = null },
            onSkip = {
                selectedCarId = null
                mileageError = null
                onNavigateToDetail(car.id)
            },
            onConfirm = { mileage, summerMileage, winterMileage, lastSwapMileage ->
                savingMileage = true
                mileageError = null
                viewModel.updateMileage(
                    carId = car.id,
                    newMileage = mileage,
                    summerTireMileage = summerMileage,
                    winterTireMileage = winterMileage,
                    lastSwapMileage = lastSwapMileage,
                    onComplete = {
                        savingMileage = false
                        selectedCarId = null
                        onNavigateToDetail(car.id)
                    },
                    onError = { savingMileage = false; mileageError = it }
                )
            }
        )
    }
}

/**
 * Rest Area (휴면 차고) 구분 헤더
 */
@Composable
fun RestAreaSectionHeader(count: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = WarmLine, thickness = 1.dp)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ToolYellowSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ToolYellow.copy(alpha = 0.5f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = WarmGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rest Area",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftInk
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "( 휴면 차고 )",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGray
                        )
                    }
                    Text(
                        text = "운용이 중단되었거나 장기 보관 중인 차량",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ToolYellowSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolYellow.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "${count}대 보관",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftInk,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Rest Area (휴면 차고) 전용 카드
 * - 톤다운된 슬립 모드 비주얼 스타일
 * - 차량명, 번호판, 누적 주행거리 표시
 * - 운용 재개(내 차고 복귀) 버튼 원클릭 지원
 * - 클릭 시 대시보드 상세 조회
 */
@Composable
fun DormantVehicleCard(
    vehicleWithStatus: VehicleWithStatus,
    onClick: () -> Unit,
    onResumeOperation: () -> Unit
) {
    val car = vehicleWithStatus.car
    val vehicleImageRes = getVehicleIllustrationRes(
        vehicleClass = car.vehicleClass,
        powertrain = car.powertrain,
        modelName = car.modelName,
        make = car.make
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteSoft),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compactCard = maxWidth < 220.dp
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 일러스트 박스 (차분한 슬립 모드 톤)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDECE8)),
                contentAlignment = Alignment.Center
            ) {
                if (!car.photoUri.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(maha.garage.util.ImageStorageHelper.resolveImageModel(car.photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "${car.make} ${car.modelName}",
                        placeholder = painterResource(vehicleImageRes),
                        error = painterResource(vehicleImageRes),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.75f
                    )
                } else {
                    Image(
                        painter = painterResource(vehicleImageRes),
                        contentDescription = "${car.make} ${car.modelName}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentScale = ContentScale.Fit,
                        alpha = 0.70f
                    )
                }

                // 휴면 뱃지
                Surface(
                    color = SurfaceWhiteStrong,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, WarmLine),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "휴면 보관",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = WarmLine, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 차량 이름
            Text(
                text = "${car.make} ${car.modelName}".trim(),
                style = MaterialTheme.typography.titleSmall,
                color = SoftInk,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 차량 번호판 & 누적 주행거리
            Text(
                text = "${car.carNumber} • ${String.format("%,d", car.currentMileage)} km",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 원클릭 운용 재개(내 차고로 복귀) 버튼
            OutlinedButton(
                onClick = onResumeOperation,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = JackGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, JackGreen.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            ) {
                Text(
                    text = if (compactCard) "운용 재개" else "운용 재개 (차고 복귀)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = JackGreen
                )
            }
        }
        }
    }
}

/**
 * 8종 스케치 일러스트 중심 차량 선택 카드 (MahaGarage_Vehicle_Card_Images 준수)
 * - 2:1 투명 캔버스 비율 보존
 * - 차량 이름, 번호판 배지, 주행거리 배지 및 소모품 갱신 요약 표시
 */
@Composable
fun VehicleIllustrationCard(
    vehicleWithStatus: VehicleWithStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val car = vehicleWithStatus.car
    val vehicleImageRes = getVehicleIllustrationRes(
        vehicleClass = car.vehicleClass,
        powertrain = car.powertrain,
        modelName = car.modelName,
        make = car.make
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhiteStrong),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 차량 대표 스케치 WebP 이미지 (2:1 비율 보존, ContentScale.Fit)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ToolYellowSoft.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                if (!car.photoUri.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(maha.garage.util.ImageStorageHelper.resolveImageModel(car.photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "${car.make} ${car.modelName}",
                        placeholder = painterResource(vehicleImageRes),
                        error = painterResource(vehicleImageRes),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(vehicleImageRes),
                        contentDescription = "${car.make} ${car.modelName}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // 주행거리 배지
                Surface(
                    color = SurfaceWhiteStrong,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, WarmLine),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${String.format("%,d", car.currentMileage)} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftInk,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = WarmLine, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 2. 차량 이름
            Text(
                text = "${car.make} ${car.modelName}".trim(),
                style = MaterialTheme.typography.titleMedium,
                color = SoftInk,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 차량 번호판 배지 (스케치 옐로 크림톤 배지)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ToolYellowSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolYellow.copy(alpha = 0.7f))
            ) {
                Text(
                    text = car.carNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            maha.garage.ui.components.VehicleRenewalSummary(car)
        }
    }
}

