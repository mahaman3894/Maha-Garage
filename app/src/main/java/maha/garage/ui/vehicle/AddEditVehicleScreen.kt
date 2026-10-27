package maha.garage.ui.vehicle

import maha.garage.ui.tutorial.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import maha.garage.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass
import maha.garage.ui.theme.*
import maha.garage.ui.components.EditInsuranceDialog
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditVehicleScreen(
    vehicleId: String? = null,
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val isEditMode = vehicleId != null

    // 기존 데이터 로드 (수정 모드인 경우)
    var isLoaded by remember { mutableStateOf(!isEditMode) }
    var existingCar by remember { mutableStateOf<CarEntity?>(null) }

    var make by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var carNumber by remember { mutableStateOf("") }
    var firstRegistrationDate by remember { mutableStateOf("") }
    var modelYearStr by remember { mutableStateOf("2023") }
    var powertrain by remember { mutableStateOf(PowertrainType.GASOLINE) }
    var vehicleClass by remember { mutableStateOf(VehicleClass.SEDAN) }
    var currentMileageStr by remember { mutableStateOf("0") }
    var engineHoursStr by remember { mutableStateOf("") }
    var maintenanceStandard by remember { mutableStateOf("OFFICIAL") }
    var isSevereCondition by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<String?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 갤러리 사진 선택 런처 (앱 내부 저장소 복사 후 file:/// URI 영구 보존)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingPhoto = true
                val persistentPath = maha.garage.util.ImageStorageHelper.saveVehicleImageToInternalStorage(context, uri)
                isSavingPhoto = false
                if (persistentPath != null) {
                    if (photoUri != persistentPath) {
                        maha.garage.util.ImageStorageHelper.deleteInternalPhotoIfPresent(context, photoUri)
                    }
                    photoUri = persistentPath
                } else {
                    Toast.makeText(context, "차량 사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 상단 번호판 자동 제원 조회 상태
    var searchPlateNumber by remember { mutableStateOf("") }
    var isSearchingPlate by remember { mutableStateOf(false) }
    var searchPlateError by remember { mutableStateOf<String?>(null) }
    var searchPlateSuccessMessage by remember { mutableStateOf<String?>(null) }
    val plateRegex = remember { Regex("^[0-9]{2,3}[가-힣]{1}[0-9]{4}$|^[가-힣]{2}[0-9]{2}[가-힣]{1}[0-9]{4}$") }

    // 타이어 설정
    var currentTireMode by remember { mutableStateOf("SUMMER") }
    var summerTireName by remember { mutableStateOf("") }
    var winterTireName by remember { mutableStateOf("") }
    var summerMileageStr by remember { mutableStateOf("0") }
    var winterMileageStr by remember { mutableStateOf("0") }

    // 카탈로그 제원 및 타이밍 구동계 상태
    var timingType by remember { mutableStateOf("CHAIN") }
    var timingDesc by remember { mutableStateOf<String?>(null) }
    var trimName by remember { mutableStateOf("") }
    var displacement by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var torque by remember { mutableStateOf("") }
    var transmission by remember { mutableStateOf("") }
    var driveSystem by remember { mutableStateOf("") }
    var fuelEconomy by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf("") }
    var showVehicleSelectDialog by remember { mutableStateOf(false) }

    // 정기검사 및 보험 정보
    var isCommercial by remember { mutableStateOf(false) }
    var inspectionType by remember { mutableStateOf("정기검사") }
    var inspectionStartDate by remember { mutableStateOf("") }
    var inspectionEndDate by remember { mutableStateOf("") }
    var isUsedCar by remember { mutableStateOf(true) }
    var insuranceCompany by remember { mutableStateOf("") }
    var insuranceStartDate by remember { mutableStateOf("") }
    var insuranceEndDate by remember { mutableStateOf("") }
    var insuranceEmergencyCall by remember { mutableStateOf("") }
    var showInsuranceDialog by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPlateSearchDialog by remember { mutableStateOf(false) }
    val tutorial = LocalTutorial.current
    LaunchedEffect(tutorial?.index) {
        if (tutorial != null) {
            currentMileageStr = "50000"
            showVehicleSelectDialog = tutorial.cue.target.startsWith("catalog.")
            showPlateSearchDialog = tutorial.cue.target.startsWith("plate.")
            if (tutorial.cue.target == "vehicle.save") {
                make = "현대"; modelName = "그랜저 하이브리드"; carNumber = "12가3456"
                powertrain = PowertrainType.HEV_GASOLINE
                firstRegistrationDate = "2023-04-10"
            }
        }
    }

    // 신차 정기검사 기간 자동 산출 함수
    fun applyInitialInspectionCalculation(
        regDate: String = firstRegistrationDate,
        commercial: Boolean = isCommercial,
        vClass: VehicleClass = vehicleClass,
        mName: String = modelName
    ) {
        if (!isUsedCar) {
            val period = maha.garage.domain.engine.RenewalDatePolicy.calculateInitialInspection(
                registrationDateStr = regDate,
                isCommercial = commercial,
                vehicleClass = vClass,
                modelName = mName
            )
            if (period != null) {
                inspectionStartDate = period.validStartDate.toString()
                inspectionEndDate = period.baseExpiryDate.toString()
            }
        }
    }

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            viewModel.getVehicleWithStatus(vehicleId).collect { vws ->
                if (vws != null && !isLoaded) {
                    val car = vws.car
                    existingCar = car
                    photoUri = car.photoUri
                    make = car.make
                    modelName = car.modelName
                    nickname = car.nickname ?: ""
                    carNumber = car.carNumber
                    firstRegistrationDate = car.firstRegistrationDate.orEmpty()
                    modelYearStr = car.modelYear.toString()
                    powertrain = car.powertrain
                    vehicleClass = car.vehicleClass
                    currentMileageStr = car.currentMileage.toString()
                    engineHoursStr = car.engineHours?.toString() ?: ""
                    maintenanceStandard = car.maintenanceStandard.ifBlank { if (car.isSevereCondition) "SEVERE" else "OFFICIAL" }
                    isSevereCondition = (maintenanceStandard == "SEVERE")
                    isArchived = car.isArchived
                    currentTireMode = car.currentTireMode
                    summerTireName = car.summerTireName ?: ""
                    winterTireName = car.winterTireName ?: ""
                    summerMileageStr = car.summerTireMileage.toString()
                    winterMileageStr = car.winterTireMileage.toString()
                    timingType = car.timingType
                    timingDesc = car.timingDesc
                    trimName = car.trimName.orEmpty()
                    displacement = car.displacement.orEmpty()
                    power = car.power.orEmpty()
                    torque = car.torque.orEmpty()
                    transmission = car.transmission.orEmpty()
                    driveSystem = car.driveSystem.orEmpty()
                    fuelEconomy = car.fuelEconomy.orEmpty()
                    dimensions = car.dimensions.orEmpty()

                    // 정기검사 및 보험 로드
                    isCommercial = car.isCommercial
                    inspectionType = car.inspectionType.ifBlank { "정기검사" }
                    inspectionStartDate = car.inspectionStartDate.orEmpty()
                    inspectionEndDate = car.inspectionEndDate.orEmpty()
                    insuranceCompany = car.insuranceCompany.orEmpty()
                    insuranceStartDate = car.insuranceStartDate.orEmpty()
                    insuranceEndDate = car.insuranceEndDate.orEmpty()
                    insuranceEmergencyCall = car.insuranceEmergencyCall.orEmpty()

                    isLoaded = true
                }
            }
        }
    }

    val quickMakes = listOf("현대", "기아", "제네시스", "BMW", "벤츠", "아우디", "테슬라", "볼보", "포르쉐", "쉐보레", "KG모빌리티", "르노코리아")

    Scaffold(
        floatingActionButton = {
            PlateLookupFloatingButton(
                onClick = { showPlateSearchDialog = true },
                modifier = Modifier.tutorialTarget("vehicle.plate") { showPlateSearchDialog = true }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "차량 정보 수정" else "신규 차량 등록",
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
            Surface(
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    val tutorialAction3 = tutorialAction3@ {
                            if (make.isBlank()) {
                                errorMessage = "제조사를 입력하거나 선택해주세요."
                                return@tutorialAction3
                            }
                            if (modelName.isBlank()) {
                                errorMessage = "모델명을 입력해주세요."
                                return@tutorialAction3
                            }
                            if (carNumber.isBlank()) {
                                errorMessage = "차량번호(예: 12가 3456)를 입력해주세요."
                                return@tutorialAction3
                            }
                            if (firstRegistrationDate.isNotBlank() && runCatching {
                                java.time.LocalDate.parse(firstRegistrationDate) <= java.time.LocalDate.now()
                            }.getOrDefault(false).not()) {
                                errorMessage = "최초 등록일은 YYYY-MM-DD 형식의 과거 또는 오늘 날짜로 입력해 주세요."
                                return@tutorialAction3
                            }
                            val dateValues = listOf(inspectionStartDate, inspectionEndDate, insuranceStartDate, insuranceEndDate)
                            if (dateValues.any { it.isNotBlank() && maha.garage.domain.engine.RenewalDatePolicy.parse(it.trim()) == null }) {
                                errorMessage = "검사·보험 날짜를 YYYY-MM-DD 형식으로 확인해 주세요."
                                return@tutorialAction3
                            }
                            val insuranceStart = maha.garage.domain.engine.RenewalDatePolicy.parse(insuranceStartDate.trim())
                            val insuranceEnd = maha.garage.domain.engine.RenewalDatePolicy.parse(insuranceEndDate.trim())
                            if (insuranceStart != null && insuranceEnd != null && insuranceEnd < insuranceStart) {
                                errorMessage = "보험 만기일은 가입일 이후여야 합니다."
                                return@tutorialAction3
                            }
                            val modelYear = modelYearStr.toIntOrNull() ?: 2023
                            val currentMileage = currentMileageStr.toIntOrNull() ?: 0
                            val engineHours = engineHoursStr.toDoubleOrNull()
                            val summerMileage = summerMileageStr.toIntOrNull() ?: 0
                            val winterMileage = winterMileageStr.toIntOrNull() ?: 0

                            if (currentMileage < (existingCar?.currentMileage ?: 0) ||
                                (engineHours != null && (!engineHours.isFinite() || engineHours < (existingCar?.engineHours ?: 0.0)))) {
                                errorMessage = "누적 주행거리와 가동시간은 이전 값보다 작을 수 없습니다."
                                return@tutorialAction3
                            }
                            val carToSave = existingCar?.copy(
                                make = make.trim(),
                                modelName = modelName.trim(),
                                nickname = nickname.trim().ifEmpty { null },
                                carNumber = carNumber.trim(),
                                modelYear = modelYear,
                                firstRegistrationDate = firstRegistrationDate.trim().ifEmpty { null },
                                powertrain = powertrain,
                                vehicleClass = vehicleClass,
                                currentMileage = currentMileage,
                                engineHours = engineHours,
                                maintenanceStandard = maintenanceStandard,
                                isSevereCondition = (maintenanceStandard == "SEVERE"),
                                currentTireMode = currentTireMode,
                                summerTireName = summerTireName.trim().ifEmpty { null },
                                winterTireName = winterTireName.trim().ifEmpty { null },
                                summerTireMileage = summerMileage,
                                winterTireMileage = winterMileage,
                                timingType = timingType,
                                timingDesc = timingDesc,
                                trimName = trimName.trim().ifEmpty { null },
                                displacement = displacement.trim().ifEmpty { null },
                                power = power.trim().ifEmpty { null },
                                torque = torque.trim().ifEmpty { null },
                                transmission = transmission.trim().ifEmpty { null },
                                driveSystem = driveSystem.trim().ifEmpty { null },
                                fuelEconomy = fuelEconomy.trim().ifEmpty { null },
                                dimensions = dimensions.trim().ifEmpty { null },
                                isCommercial = isCommercial,
                                inspectionType = inspectionType.trim(),
                                inspectionStartDate = inspectionStartDate.trim().ifEmpty { null },
                                inspectionEndDate = inspectionEndDate.trim().ifEmpty { null },
                                isInspectionDateEstimated = inspectionEndDate.trim().isEmpty() || (existingCar?.isInspectionDateEstimated ?: true),
                                insuranceCompany = insuranceCompany.trim().ifEmpty { null },
                                insuranceStartDate = insuranceStartDate.trim().ifEmpty { null },
                                insuranceEndDate = insuranceEndDate.trim().ifEmpty { null },
                                insuranceEmergencyCall = insuranceEmergencyCall.trim().ifEmpty { null },
                                isArchived = isArchived,
                                photoUri = photoUri,
                                updatedAt = System.currentTimeMillis()
                            ) ?: CarEntity(
                                id = UUID.randomUUID().toString(),
                                make = make.trim(),
                                modelName = modelName.trim(),
                                nickname = nickname.trim().ifEmpty { null },
                                carNumber = carNumber.trim(),
                                modelYear = modelYear,
                                firstRegistrationDate = firstRegistrationDate.trim().ifEmpty { null },
                                powertrain = powertrain,
                                vehicleClass = vehicleClass,
                                currentMileage = currentMileage,
                                engineHours = engineHours,
                                maintenanceStandard = maintenanceStandard,
                                isSevereCondition = (maintenanceStandard == "SEVERE"),
                                currentTireMode = currentTireMode,
                                lastSwapMileage = currentMileage,
                                summerTireName = summerTireName.trim().ifEmpty { null },
                                winterTireName = winterTireName.trim().ifEmpty { null },
                                summerTireMileage = summerMileage,
                                winterTireMileage = winterMileage,
                                timingType = timingType,
                                timingDesc = timingDesc,
                                trimName = trimName.trim().ifEmpty { null },
                                displacement = displacement.trim().ifEmpty { null },
                                power = power.trim().ifEmpty { null },
                                torque = torque.trim().ifEmpty { null },
                                transmission = transmission.trim().ifEmpty { null },
                                driveSystem = driveSystem.trim().ifEmpty { null },
                                fuelEconomy = fuelEconomy.trim().ifEmpty { null },
                                dimensions = dimensions.trim().ifEmpty { null },
                                isCommercial = isCommercial,
                                inspectionType = inspectionType.trim(),
                                inspectionStartDate = inspectionStartDate.trim().ifEmpty { null },
                                inspectionEndDate = inspectionEndDate.trim().ifEmpty { null },
                                isInspectionDateEstimated = inspectionEndDate.trim().isEmpty(),
                                insuranceCompany = insuranceCompany.trim().ifEmpty { null },
                                insuranceStartDate = insuranceStartDate.trim().ifEmpty { null },
                                insuranceEndDate = insuranceEndDate.trim().ifEmpty { null },
                                insuranceEmergencyCall = insuranceEmergencyCall.trim().ifEmpty { null },
                                photoUri = photoUri,
                                updatedAt = System.currentTimeMillis()
                            )

                            val effectiveNewCar = (!isUsedCar) || (currentMileage <= 1000)
                            viewModel.saveVehicle(if (tutorial != null) carToSave.copy(id = TutorialFixtures.CAR_ID, insuranceCompany = "예시 보험사", insuranceEmergencyCall = "000-0000-0000") else carToSave, isNewCar = effectiveNewCar) {
                                onNavigateBack()
                            }
                        }
Button(
                        onClick = tutorialAction3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp).tutorialTarget("vehicle.save", tutorialAction3),
                        colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = TextPrimaryDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "수정사항 저장" else "차량 등록 완료",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 에러 메시지
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusOverdue.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusOverdue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = StatusOverdue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 1. 기본 제원 정보 섹션 (가장 상단에 배치)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1. 차량 기본 제원",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 카탈로그 선택 버튼 왼쪽에 내 차 사진 등록 버튼 신설
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (photoUri != null) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    border = BorderStroke(1.dp, if (photoUri != null) GaragePrimaryLight else BorderDark),
                                    modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSavingPhoto) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = GaragePrimaryLight,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "저장 중...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GaragePrimaryLight
                                            )
                                        } else if (!photoUri.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(maha.garage.util.ImageStorageHelper.resolveImageModel(photoUri))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "선택된 차량 사진",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "사진 변경",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GaragePrimaryLight
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "내 차 사진 등록",
                                                tint = TextSecondaryDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "내 차 사진",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                }

                                AnimatedMaskCatalogButton(
                                    onClick = { showVehicleSelectDialog = true },
                                    modifier = Modifier.tutorialTarget("vehicle.catalog") { showVehicleSelectDialog = true }
                                )
                            }
                        }

                        // 등록된 사진 미리보기 및 삭제 옵션 (등록 시 노출)
                        if (!photoUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantDark,
                                border = BorderStroke(0.8.dp, GaragePrimaryLight.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        coil.compose.AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(maha.garage.util.ImageStorageHelper.resolveImageModel(photoUri))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "차량 카드에 내 사진이 적용됩니다 (영구 저장)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GaragePrimaryLight,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            maha.garage.util.ImageStorageHelper.deleteInternalPhotoIfPresent(context, photoUri)
                                            photoUri = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("사진 제거", color = StatusOverdue, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        if (isEditMode) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isArchived) Color(0xFF202634) else SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isArchived) Color(0xFF3B4861) else BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArchived) "운용 중단 (Rest Area 보관)" else "정상 운용 중 (내 차고)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isArchived) Color(0xFFA0AEC0) else GaragePrimaryLight
                                        )
                                        Text(
                                            text = if (isArchived) "차고 하단 Rest Area 섹션에 보관됩니다." else "내 차고 메인 활성 목록에 표시됩니다.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark
                                        )
                                    }
                                    Switch(
                                        checked = !isArchived,
                                        onCheckedChange = { isArchived = !it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GarageAccent,
                                            checkedTrackColor = GaragePrimaryLight.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 제조사 선택 칩
                        Text(
                            text = "주요 제조사 추천",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickMakes) { m ->
                                val isSelected = make == m
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { make = m },
                                    label = { Text(m) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GaragePrimaryLight.copy(alpha = 0.25f),
                                        selectedLabelColor = GaragePrimaryLight,
                                        containerColor = SurfaceVariantDark,
                                        labelColor = TextSecondaryDark
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = firstRegistrationDate,
                            onValueChange = {
                                firstRegistrationDate = it
                                if (it.trim().length == 10) {
                                    applyInitialInspectionCalculation(regDate = it.trim())
                                }
                            },
                            label = { Text("최초 등록일 (선택, YYYY-MM-DD)") },
                            placeholder = { Text("예: 2026-05-10") },
                            supportingText = {
                                Text(
                                    if (!isUsedCar) "신차 출고 시 만 4년 등 첫 정기검사 기간이 자동 계산됩니다."
                                    else "자동차등록증 상의 최초등록일을 입력해 주세요."
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it; errorMessage = null },
                            label = { Text("제조사 (직접 입력 또는 카탈로그 선택)") },
                            placeholder = { Text("예: 현대, BMW, 테슬라") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it; errorMessage = null },
                            label = { Text("모델명 *") },
                            placeholder = { Text("예: 팰리세이드 3.8, 아반떼 N, 포터 2, Model Y") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        // 선택된 차량 제원 및 타이밍 방식 피드백 카드
                        if (modelName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (tBadge, tCol) = when (timingType) {
                                            "CHAIN" -> "⛓️ 타이밍 체인 (반영구)" to StatusNormal
                                            "BELT" -> "🔄 고무 타이밍벨트 (정기교체)" to StatusDueSoon
                                            "NA" -> "⚡ 전기구동 (해당없음)" to StatusNormal
                                            else -> "타이밍 미정" to TextSecondaryDark
                                        }
                                        Text("차량 소모품 매칭 제원", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = tCol.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = tBadge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = tCol
                                            )
                                        }
                                    }

                                    if (trimName.isNotBlank() || displacement.isNotBlank() || power.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val summary = listOf(trimName, displacement, power, fuelEconomy).filter { it.isNotBlank() }.joinToString(" · ")
                                        Text(summary, style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = carNumber,
                                onValueChange = { carNumber = it; errorMessage = null },
                                label = { Text("차량번호 *") },
                                placeholder = { Text("12가 3456") },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            OutlinedTextField(
                                value = modelYearStr,
                                onValueChange = { modelYearStr = it },
                                label = { Text("연식") },
                                placeholder = { Text("2023") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("차량 별칭 (선택)") },
                            placeholder = { Text("예: 패밀리카, 출퇴근 머신") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )
                    }
                }
            }

            // 2. 핵심 2축 독립 분류 (파워트레인 × 차종)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = GaragePrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. 2축 독립 분류 (정비 규칙 자동 결정)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // 축 1: 파워트레인
                        Text(
                            text = "파워트레인 선택",
                            style = MaterialTheme.typography.titleSmall,
                            color = GaragePrimaryLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val powertrains = PowertrainType.values()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(powertrains) { pt ->
                                val isSelected = powertrain == pt
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { powertrain = pt },
                                    color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) GaragePrimaryLight else BorderDark
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = pt.displayName,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GaragePrimaryLight else TextSecondaryDark
                                    )
                                }
                            }
                        }

                        // 파워트레인 특이사항 힌트
                        if (powertrain == PowertrainType.BEV) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚡ 순수 전기차: 엔진오일, 점화플러그, 연료필터 등이 자동 제외되고, 감속기 오일 및 절연형 냉각수 규칙이 적용됩니다.",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusNormal
                            )
                        } else if (powertrain == PowertrainType.DIESEL) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🛢️ 디젤 엔진: DPF 클리닝, 디젤 연료필터(카트리지/어셈블리), 요소수 규칙이 자동 적용됩니다.",
                                style = MaterialTheme.typography.labelSmall,
                                color = GarageAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // 축 2: 차종
                        Text(
                            text = "차종 (Vehicle Class) 선택",
                            style = MaterialTheme.typography.titleSmall,
                            color = GaragePrimaryLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val vehicleClasses = VehicleClass.values()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vehicleClasses) { vc ->
                                val isSelected = vehicleClass == vc
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { vehicleClass = vc },
                                    color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) GaragePrimaryLight else BorderDark
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = vc.displayName,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GaragePrimaryLight else TextSecondaryDark
                                    )
                                }
                            }
                        }

                        if (vehicleClass == VehicleClass.LIGHT_TRUCK || vehicleClass == VehicleClass.MEDIUM_TRUCK || vehicleClass == VehicleClass.HEAVY_TRUCK) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🚛 상용 화물 트럭: 에어브레이크 에어드라이어 카트리지 및 섀시 그리스 주입 규칙이 추가 적용됩니다.",
                                style = MaterialTheme.typography.labelSmall,
                                color = GarageAccent
                            )
                        }
                    }
                }
            }

            // 3. 소모품 교환 관리 기준 (OFFICIAL / SEVERE / SHOP)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = GarageAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "소모품 교환 관리 기준",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "차량 특성 및 주행 환경에 맞추어 소모품 점검·교환 주기를 계산하는 기준입니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val standardOptions = listOf(
                            Triple(
                                "OFFICIAL",
                                "제조사 공식 표준 (OFFICIAL)",
                                "제조사 정비 매뉴얼 또는 공식 정비 주기표에 명시된 기본 기준입니다."
                            ),
                            Triple(
                                "SEVERE",
                                "제조사 가혹 조건 (SEVERE)",
                                "잦은 단거리, 도심 정체, 험로 등 제조사가 명시한 가혹 주행 조건 기준입니다."
                            ),
                            Triple(
                                "SHOP",
                                "정비소·오너 권장 (SHOP)",
                                "정비소 및 오너 커뮤니티 기반의 예방 정비 권장값입니다. (공식 의무 교체 아님)"
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            standardOptions.forEach { (code, title, desc) ->
                                val isSelected = maintenanceStandard == code
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            maintenanceStandard = code
                                            isSevereCondition = (code == "SEVERE")
                                        },
                                    color = if (isSelected) GarageAccent.copy(alpha = 0.12f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) GarageAccent else BorderDark
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                maintenanceStandard = code
                                                isSevereCondition = (code == "SEVERE")
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = GarageAccent,
                                                unselectedColor = TextSecondaryDark
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) GarageAccent else TextPrimaryDark
                                                )
                                                if (code == "SHOP") {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFFF9800).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "예방정비",
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFFFB74D),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 주행거리 및 엔진 가동시간
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "3. 주행거리 및 가동시간",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = currentMileageStr,
                                onValueChange = { currentMileageStr = it },
                                label = { Text("현재 누적 주행거리 (km) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            OutlinedTextField(
                                value = engineHoursStr,
                                onValueChange = { engineHoursStr = it },
                                label = { Text("엔진 가동시간 (hr)") },
                                placeholder = { Text("선택 입력") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }
                    }
                }
            }

            // 5. 마하개러지 전용: 타이어 스왑 & 듀얼 마일리지 추적 설정
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = GarageAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4. 타이어 스왑 & 듀얼 주행거리 추적",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "계절별 섬머/윈터 타이어 교체 시 각각의 누적 주행거리를 별도로 정밀 누적 추적합니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // 현재 장착 모드 선택
                        Text(
                            text = "현재 차량에 장착된 타이어 모드",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isSummer = currentTireMode == "SUMMER"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { currentTireMode = "SUMMER" },
                                color = if (isSummer) StatusNormal.copy(alpha = 0.2f) else SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSummer) 1.5.dp else 0.5.dp,
                                    color = if (isSummer) StatusNormal else BorderDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.WbSunny, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "섬머 / 사계절",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSummer) StatusNormal else TextSecondaryDark
                                    )
                                }
                            }

                            val isWinter = currentTireMode == "WINTER"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { currentTireMode = "WINTER" },
                                color = if (isWinter) GarageAccent.copy(alpha = 0.2f) else SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isWinter) 1.5.dp else 0.5.dp,
                                    color = if (isWinter) GarageAccent else BorderDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AcUnit, contentDescription = null, tint = GarageAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "윈터 (스노우)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWinter) GarageAccent else TextSecondaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = summerTireName,
                            onValueChange = { summerTireName = it },
                            label = { Text("섬머/사계절 타이어 제품명") },
                            placeholder = { Text("예: 미쉐린 Pilot Sport 4 (245/45R19)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = winterTireName,
                            onValueChange = { winterTireName = it },
                            label = { Text("윈터 타이어 제품명") },
                            placeholder = { Text("예: 금호 윈터크래프트 WP72") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = summerMileageStr,
                                onValueChange = { summerMileageStr = it },
                                label = { Text("섬머 누적주행 (km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            OutlinedTextField(
                                value = winterMileageStr,
                                onValueChange = { winterMileageStr = it },
                                label = { Text("윈터 누적주행 (km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }
                    }
                }
            }

            // 6. 자동차 정기검사 & 보험가입 정보 설정
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = GaragePrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5. 정기검사 & 보험가입 정보",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "한국교통안전공단 정기/종합 검사 유효기간과 보험가입 정보를 등록하여 주기적인 알림을 받습니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // 신차 / 중고차 등록 구분
                        Text("차량 취득 구분", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isUsedCar = true },
                                color = if (isUsedCar) GarageAccent.copy(alpha = 0.2f) else SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isUsedCar) 1.5.dp else 0.5.dp,
                                    color = if (isUsedCar) GarageAccent else BorderDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("중고차 구매 (기본)", fontWeight = FontWeight.Bold, color = if (isUsedCar) GarageAccent else TextSecondaryDark)
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        isUsedCar = false
                                        applyInitialInspectionCalculation()
                                    },
                                color = if (!isUsedCar) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (!isUsedCar) 1.5.dp else 0.5.dp,
                                    color = if (!isUsedCar) GaragePrimaryLight else BorderDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("신차 출고", fontWeight = FontWeight.Bold, color = if (!isUsedCar) GaragePrimaryLight else TextSecondaryDark)
                                }
                            }
                        }

                        // 중고차 등록 시 공단 공식 안내 및 소모품 자동 초기화 안내
                        if (isUsedCar) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2A2318),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusDueSoon.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = StatusDueSoon, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "중고차 등록 관리 기준 안내",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusDueSoon
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• 주행거리 1,000km 이하는 자동으로 신차 출고 기준(0km 신품 기산점)이 적용됩니다.\n• 5만km 이상 장주기 부품(타이밍벨트, 워터펌프, 미션오일, 부동액 등): 출고(0km)부터 주행된 것으로 자동 측정됩니다.\n• 1.6만~5만km 부품(브레이크 패드, 오일 등): 절반 주기 경과로 기본 간주됩니다.\n• 1.5만km 이하 부품(엔진오일, 에어컨필터 등): 정보없음 상태로 등록 대기됩니다.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        } else {
                            // 신차 출고 정기검사 주기 안내 및 자동 계산 배너
                            val period = maha.garage.domain.engine.RenewalDatePolicy.calculateInitialInspection(
                                registrationDateStr = firstRegistrationDate,
                                isCommercial = isCommercial,
                                vehicleClass = vehicleClass,
                                modelName = modelName
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = period?.cycleDescription ?: "비사업용 승용차: 최초 등록 후 만 4년 시점 첫 정기검사 (이후 2년 주기)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "정기검사는 만료일 전후 각각 31일(총 62일간) 이내에 받으셔야 지연 과태료가 발생하지 않습니다.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                    if (period != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GaragePrimaryLight.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "▶ 1회차 검사 가능 기간: ${period.validStartDate} ~ ${period.validEndDate}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = GaragePrimaryLight,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 사업용 / 비사업용
                        Text("용도 구분", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(false to "비사업용 (자가용)", true to "사업용 (영업/화물/법인)").forEach { (commercial, label) ->
                                val isSelected = isCommercial == commercial
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isCommercial = commercial
                                            applyInitialInspectionCalculation(commercial = commercial)
                                        },
                                    color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) GaragePrimaryLight else BorderDark
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) GaragePrimaryLight else TextSecondaryDark, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 검사 종류 및 만료일
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inspectionType,
                                onValueChange = { inspectionType = it },
                                label = { Text("검사 종류") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )

                            OutlinedTextField(
                                value = inspectionEndDate,
                                onValueChange = { inspectionEndDate = it },
                                label = { Text("검사 만료일") },
                                placeholder = { Text("YYYY-MM-DD") },
                                supportingText = {
                                    if (firstRegistrationDate.isNotBlank() && !isUsedCar) {
                                        Text("등록일 기준 자동 추산됨")
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GaragePrimaryLight,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 보험 가입정보 (VehicleDetailScreen 보험박스와 동일한 모달 호출 방식)
                        val isInsuranceSet = insuranceCompany.isNotBlank() && insuranceStartDate.isNotBlank()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = GarageAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "자동차 보험 가입정보",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isInsuranceSet) {
                                    Text(
                                        text = "초기화",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusOverdue.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .clickable {
                                                insuranceCompany = ""
                                                insuranceStartDate = ""
                                                insuranceEndDate = ""
                                                insuranceEmergencyCall = ""
                                            }
                                            .padding(end = 8.dp)
                                    )
                                }
                                Text(
                                    text = if (isInsuranceSet) "(등록완료)" else "(터치하여 등록)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isInsuranceSet) GarageAccent else TextSecondaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // 보험 카드 박스 (터치 시 EditInsuranceDialog 모달 오픈)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInsuranceSet) GarageAccent.copy(alpha = 0.08f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isInsuranceSet) GarageAccent.copy(alpha = 0.5f) else BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showInsuranceDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isInsuranceSet) GarageAccent.copy(alpha = 0.2f) else BorderDark.copy(alpha = 0.3f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (isInsuranceSet) GarageAccent else TextSecondaryDark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isInsuranceSet) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = insuranceCompany,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            if (insuranceEmergencyCall.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = GarageAccent.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "긴급 $insuranceEmergencyCall",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = GarageAccent,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "가입기간: $insuranceStartDate ~ ${insuranceEndDate.ifBlank { "만기 미지정" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = "자동차 보험 정보 등록",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "터치하여 주요 보험사 선택 및 1년 자동 계산 등록",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isInsuranceSet) GarageAccent.copy(alpha = 0.15f) else GaragePrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (isInsuranceSet) GarageAccent else GaragePrimaryLight.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isInsuranceSet) Icons.Default.Edit else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (isInsuranceSet) GarageAccent else GaragePrimaryLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isInsuranceSet) "수정" else "입력",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInsuranceSet) GarageAccent else GaragePrimaryLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    if (showVehicleSelectDialog) {
        VehicleSelectDialog(
            initialBrand = make.ifBlank { null },
            onDismiss = { showVehicleSelectDialog = false },
            onSelectVehicle = { item ->
                make = item.brand
                modelName = item.modelName
                trimName = item.lineup.ifEmpty { item.trim }
                modelYearStr = item.modelYear.toString()
                powertrain = runCatching { PowertrainType.valueOf(item.powertrain) }.getOrDefault(PowertrainType.GASOLINE)
                vehicleClass = runCatching { VehicleClass.valueOf(item.vehicleClass) }.getOrDefault(VehicleClass.SEDAN)
                timingType = item.timingType
                timingDesc = item.timingDesc
                displacement = item.displacement
                power = item.power
                torque = item.torque
                transmission = item.transmission
                driveSystem = item.driveSystem
                fuelEconomy = item.fuelEconomy
                dimensions = item.dimensions
                errorMessage = null
                showVehicleSelectDialog = false
            }
        )
    }

    if (showPlateSearchDialog) {
        PlateLookupDialog(
            initialPlate = carNumber,
            onDismiss = { showPlateSearchDialog = false },
            onLookup = { plate, onComplete ->
                viewModel.lookupVehicleSpecs(
                    plateNumber = plate,
                    onResult = { spec ->
                        if (spec != null) {
                            carNumber = spec.plateNumber
                            spec.manufacturer?.let { if (it.isNotBlank()) make = it }
                            spec.modelName?.let { if (it.isNotBlank()) modelName = it }
                            spec.modelYear?.let { modelYearStr = it.toString() }
                            spec.displacement?.let { displacement = it }
                            spec.powertrain?.let { powertrain = it }
                            spec.vehicleClass?.let { vehicleClass = it }
                            spec.registrationDate?.let { regDate ->
                                if (regDate.isNotBlank()) {
                                    firstRegistrationDate = regDate
                                    applyInitialInspectionCalculation(
                                        regDate = regDate,
                                        commercial = isCommercial,
                                        vClass = spec.vehicleClass ?: vehicleClass,
                                        mName = spec.modelName ?: modelName
                                    )
                                }
                            }
                            spec.nextInspectionDate?.let { nextInsp ->
                                if (nextInsp.isNotBlank()) {
                                    inspectionEndDate = nextInsp
                                }
                            }
                            errorMessage = null
                            onComplete(true)
                        } else {
                            onComplete(false)
                        }
                    },
                    onError = {
                        onComplete(false)
                    }
                )
            }
        )
    }

    if (showInsuranceDialog) {
        EditInsuranceDialog(
            initialCompany = insuranceCompany,
            initialStartDate = insuranceStartDate,
            initialEndDate = insuranceEndDate,
            initialEmergencyCall = insuranceEmergencyCall,
            onDismiss = { showInsuranceDialog = false },
            onConfirm = { company, start, end, emergency ->
                insuranceCompany = company
                insuranceStartDate = start
                insuranceEndDate = end
                insuranceEmergencyCall = emergency
                showInsuranceDialog = false
            }
        )
    }
}

/**
 * MASK1 느낌의 중앙 잉크 확산 애니메이션 카탈로그 선택 버튼.
 * 클릭이나 호버를 하지 않아도 22단계(steps(22)) 유기적 마스크가 중앙에서 퍼지고 돌아오며,
 * 마스크가 덮는 영역의 색상이 다크/네온 대비로 실시간 반전됩니다.
 */
@Composable
fun AnimatedMaskCatalogButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CatalogMask1Anim")

    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "maskProgress"
    )

    // CSS steps(22) 효과를 동일하게 구현한 22개 이산 스텝
    val stepCount = 22
    val steppedProgress = (animProgress * stepCount).toInt().toFloat() / stepCount

    val inkMask = ImageBitmap.imageResource(R.drawable.catalog_ink_mask)
    val frame = (steppedProgress * stepCount).toInt().coerceIn(0, stepCount)
    val buttonShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(buttonShape)
            .border(
                1.5.dp,
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(ToolYellow, ToolOrange, ToolYellow)
                ),
                buttonShape
            )
            .clickable(onClick = onClick)
    ) {
        // [베이스 레이어] 웜 크림 배경 + 소프트 잉크/툴 오렌지 텍스트/아이콘 (ui.md 팔레트)
        Box(
            modifier = Modifier
                .background(ToolYellowSoft)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ToolOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "카탈로그에서 선택",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftInk
                )
            }
        }

        // [마스크 오버레이 레이어] 툴 옐로-오렌지 그라데이션 + 딥 잉크 텍스트
        // 22-step ink mask animation 유지
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // MASK1 uses mask-size: 2300% and steps(22): 23 frames.
                    val left = (inkMask.width * frame / 23f).roundToInt()
                    val right = (inkMask.width * (frame + 1) / 23f).roundToInt()
                    drawImage(
                        inkMask,
                        srcOffset = IntOffset(left, 0),
                        srcSize = IntSize(right - left, inkMask.height),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        blendMode = BlendMode.DstIn
                    )
                }
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            ToolYellow,
                            ToolOrange,
                            Color(0xFFE59850)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SoftInk,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "카탈로그에서 선택",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftInk
                )
            }
        }
    }
}

