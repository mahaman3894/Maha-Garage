package maha.garage.ui.service

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.local.entity.ServiceVisitEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.ui.theme.*
import java.time.LocalDate
import java.util.UUID

data class ServiceLineDraft(
    val id: String = UUID.randomUUID().toString(),
    var itemCode: String = "ENGINE_OIL",
    var itemName: String = "엔진오일 및 오일필터",
    var actionType: ActionType = ActionType.REPLACE,
    var partsCostStr: String = "0",
    var laborCostStr: String = "0",
    var partSpecification: String = "",
    var conditionValue: String = "",
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceVisitScreen(
    preselectedCarId: String? = null,
    viewModel: ServiceViewModel,
    onNavigateBack: () -> Unit
) {
    val cars by viewModel.cars.collectAsState()
    val catalog by viewModel.catalog.collectAsState()

    var selectedCarId by remember { mutableStateOf(preselectedCarId) }
    val currentSelectedCar = if (selectedCarId == null) cars.firstOrNull() else cars.find { it.id == selectedCarId }
    val applicableCatalog = catalog.filter { currentSelectedCar != null && maha.garage.domain.engine.ApplicabilityResolver.isItemApplicable(currentSelectedCar, it) }

    // 차량 선택 시 기본 주행거리 자동 반영
    var odometerStr by remember { mutableStateOf(currentSelectedCar?.currentMileage?.toString() ?: "0") }
    var engineHoursStr by remember { mutableStateOf(currentSelectedCar?.engineHours?.toString() ?: "") }
    var serviceDateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var workshop by remember { mutableStateOf("") }
    var visitNotes by remember { mutableStateOf("") }
    var receiptPhotoPath by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val receiptImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                receiptPhotoPath = maha.garage.util.ImageStorageHelper.saveImageToInternalStorage(context, uri)
            }
        }
    }

    // 1회 방문 다중 정비 작업 목록 (Multi-line service lines)
    val serviceLines = remember {
        mutableStateListOf(
            ServiceLineDraft(
                itemCode = "ENGINE_OIL",
                itemName = "엔진오일 및 오일필터",
                actionType = ActionType.REPLACE
            )
        )
    }

    LaunchedEffect(currentSelectedCar?.id) {
        if (currentSelectedCar != null) {
            selectedCarId = currentSelectedCar.id
            odometerStr = currentSelectedCar.currentMileage.toString()
            engineHoursStr = currentSelectedCar.engineHours?.toString().orEmpty()
            val byCode = maha.garage.data.seed.DefaultCatalogData.items.associateBy { it.itemCode }
            serviceLines.removeAll { draft -> byCode[draft.itemCode]?.let {
                !maha.garage.domain.engine.ApplicabilityResolver.isItemApplicable(currentSelectedCar, it)
            } ?: false }
        }
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 총 견적비 계산 (부품비 + 공임비 합산)
    val calculatedTotalCost = remember(serviceLines.map { it.partsCostStr to it.laborCostStr }) {
        serviceLines.sumOf { (it.partsCostStr.toLongOrNull() ?: 0L) + (it.laborCostStr.toLongOrNull() ?: 0L) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "정비 기록 등록 (1회 방문 다중 작업)",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "총 정비 비용",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "%,d 원".format(calculatedTotalCost),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GaragePrimaryLight
                        )
                    }

                    Button(
                        onClick = {
                            val carId = currentSelectedCar?.id
                            if (carId == null) {
                                errorMessage = "대상 차량을 선택해주세요."
                                return@Button
                            }
                            val odo = odometerStr.toIntOrNull()
                            if (odo == null || odo < 0) {
                                errorMessage = "올바른 정비 시점 주행거리를 입력해주세요."
                                return@Button
                            }
                            if (runCatching { LocalDate.parse(serviceDateStr.trim()) <= LocalDate.now() }.getOrDefault(false).not()) {
                                errorMessage = "정비 일자는 YYYY-MM-DD 형식의 과거 또는 오늘 날짜로 입력해 주세요."
                                return@Button
                            }
                            val hours = engineHoursStr.toDoubleOrNull()
                            if (engineHoursStr.isNotBlank() && (hours == null || !hours.isFinite() || hours < 0)) {
                                errorMessage = "올바른 가동시간을 입력해 주세요."
                                return@Button
                            }
                            if (serviceLines.any { draft -> applicableCatalog.none { it.itemCode == draft.itemCode } }) {
                                errorMessage = "선택한 차량에 적용되지 않는 정비 항목을 삭제해 주세요."
                                return@Button
                            }
                            if (serviceLines.isEmpty()) {
                                errorMessage = "최소 1개 이상의 정비 작업 항목을 추가해주세요."
                                return@Button
                            }

                            val visitId = UUID.randomUUID().toString()
                            val visitEntity = ServiceVisitEntity(
                                id = visitId,
                                carId = carId,
                                serviceDate = serviceDateStr.trim(),
                                odometerKm = odo,
                                engineHours = engineHoursStr.toDoubleOrNull(),
                                workshop = workshop.trim().ifEmpty { null },
                                totalCost = calculatedTotalCost,
                                notes = visitNotes.trim().ifEmpty { null },
                                receiptPhotoUri = receiptPhotoPath
                            )

                            val lineEntities = serviceLines.map { draft ->
                                ServiceLineEntity(
                                    id = UUID.randomUUID().toString(),
                                    visitId = visitId,
                                    itemCode = draft.itemCode,
                                    actionType = draft.actionType,
                                    partsCost = draft.partsCostStr.toLongOrNull() ?: 0L,
                                    laborCost = draft.laborCostStr.toLongOrNull() ?: 0L,
                                    conditionValue = draft.conditionValue.trim().ifEmpty { null },
                                    partSpecification = draft.partSpecification.trim().ifEmpty { null },
                                    notes = draft.notes.trim().ifEmpty { null }
                                )
                            }

                            viewModel.recordVisit(visitEntity, lineEntities, onError = { errorMessage = it }) {
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GaragePrimaryLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = TextPrimaryDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "정비 기록 저장",
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

            // 에러 배너
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

            // 1. 차량 선택 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "대상 차량 선택",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(cars) { car ->
                                val isSelected = (selectedCarId ?: currentSelectedCar?.id) == car.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedCarId = car.id
                                            odometerStr = car.currentMileage.toString()
                                            engineHoursStr = car.engineHours?.toString() ?: ""
                                        },
                                    color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) GaragePrimaryLight else BorderDark
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${car.make} ${car.modelName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) GaragePrimaryLight else TextPrimaryDark
                                        )
                                        Text(
                                            text = "${car.carNumber} • %,d km".format(car.currentMileage),
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

            // 2. 방문 기본 정보
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "방문 정보",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = serviceDateStr,
                                onValueChange = { serviceDateStr = it },
                                label = { Text("정비 일자 (YYYY-MM-DD)") },
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
                                value = odometerStr,
                                onValueChange = { odometerStr = it },
                                label = { Text("주행거리 (km) *") },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = workshop,
                                onValueChange = { workshop = it },
                                label = { Text("정비소 / 개러지명") },
                                placeholder = { Text("예: 마하개러지 성수점, 공임나라, 직영센터") },
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
                                value = engineHoursStr,
                                onValueChange = { engineHoursStr = it },
                                label = { Text("엔진시간 (hr)") },
                                placeholder = { Text("선택") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.7f),
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
                            value = visitNotes,
                            onValueChange = { visitNotes = it },
                            label = { Text("방문 특이사항 및 메모") },
                            placeholder = { Text("예: 정기 4만km 종합 정비, 타이어 공기압 36psi 세팅") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GaragePrimaryLight,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 영수증 사진 첨부
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "정비 영수증 / 정비내역서 사진",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (receiptPhotoPath != null) "영수증 사진이 앱 내부 저장소에 안전하게 보관되었습니다." else "영수증 또는 정비명세서를 사진으로 첨부하세요.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (receiptPhotoPath != null) StatusNormal else TextSecondaryDark
                                )
                            }

                            if (receiptPhotoPath == null) {
                                OutlinedButton(
                                    onClick = { receiptImagePicker.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GaragePrimaryLight)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("사진 첨부", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { receiptImagePicker.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GaragePrimaryLight)
                                    ) {
                                        Text("변경", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { receiptPhotoPath = null }, modifier = Modifier.size(32.dp)) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "삭제", tint = StatusOverdue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. 다중 정비 작업 상세 목록 (1회 방문 N개 작업)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "정비 작업 항목 (${serviceLines.size}건)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "각 항목별 교체/점검 시 해당 정비 주기가 자동 갱신됩니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }

                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GaragePrimaryLight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = GaragePrimaryLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "항목 추가", color = GaragePrimaryLight, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 정비 라인 아이템 카드들
            items(serviceLines) { draft ->
                ServiceLineDraftCard(
                    draft = draft,
                    onDelete = {
                        if (serviceLines.size > 1) {
                            serviceLines.remove(draft)
                        }
                    },
                    canDelete = serviceLines.size > 1
                )
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // 항목 추가 다이얼로그 (카탈로그 목록에서 선택)
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Text(
                    text = "정비 소모품 항목 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(applicableCatalog) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    serviceLines.add(
                                        ServiceLineDraft(
                                            itemCode = item.itemCode,
                                            itemName = item.displayName,
                                            actionType = ActionType.REPLACE
                                        )
                                    )
                                    showAddItemDialog = false
                                },
                            color = SurfaceVariantDark
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "${item.category} • 기본 ${item.defaultIntervalKm?.let { "${it / 10000}만" } ?: "1만"} km 주기",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = GaragePrimaryLight
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("닫기", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun ServiceLineDraftCard(
    draft: ServiceLineDraft,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var partsCost by remember { mutableStateOf(draft.partsCostStr) }
    var laborCost by remember { mutableStateOf(draft.laborCostStr) }
    var partSpec by remember { mutableStateOf(draft.partSpecification) }
    var condition by remember { mutableStateOf(draft.conditionValue) }
    var selectedAction by remember { mutableStateOf(draft.actionType) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = GaragePrimaryLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = draft.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "삭제", tint = StatusOverdue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 작업 유형 선택 (교체 / 점검 / 보충 / 세척 / 위치교환 / 스왑)
            val actionTypes: List<Pair<ActionType, String>> = listOf(
                ActionType.REPLACE to "교체",
                ActionType.INSPECT to "점검",
                ActionType.REFILL to "보충/교환",
                ActionType.CLEAN to "클리닝",
                ActionType.ROTATE to "위치교환",
                ActionType.SWAP to "스왑"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(actionTypes) { (type, label) ->
                    val isSelected = selectedAction == type
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedAction = type
                                draft.actionType = type
                            },
                        color = if (isSelected) GaragePrimaryLight.copy(alpha = 0.25f) else SurfaceVariantDark,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GaragePrimaryLight else TextSecondaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 사용 부품 규격
            OutlinedTextField(
                value = partSpec,
                onValueChange = {
                    partSpec = it
                    draft.partSpecification = it
                },
                label = { Text("사용 부품/규격") },
                placeholder = { Text("예: 모빌1 ESP 0W-30 6L, 보쉬 정품 필터") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GaragePrimaryLight,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 측정값 / 상태값
            OutlinedTextField(
                value = condition,
                onValueChange = {
                    condition = it
                    draft.conditionValue = it
                },
                label = { Text("점검/상태 측정치 (선택)") },
                placeholder = { Text("예: 전륜 패드 잔여 7.5mm, 배터리 SOH 96%") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GaragePrimaryLight,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 부품비 & 공임비
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = partsCost,
                    onValueChange = {
                        partsCost = it
                        draft.partsCostStr = it
                    },
                    label = { Text("부품비 (원)") },
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
                    value = laborCost,
                    onValueChange = {
                        laborCost = it
                        draft.laborCostStr = it
                    },
                    label = { Text("공임비 (원)") },
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
