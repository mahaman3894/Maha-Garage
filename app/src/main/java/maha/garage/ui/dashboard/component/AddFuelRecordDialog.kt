package maha.garage.ui.dashboard.component

import maha.garage.ui.tutorial.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import maha.garage.data.local.entity.FuelRecordEntity
import maha.garage.domain.engine.FuelCalculationInput
import maha.garage.domain.engine.FuelCalculator
import maha.garage.ui.components.CameraLensFab
import maha.garage.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelRecordDialog(
    carName: String,
    currentCarMileage: Int,
    historyRecords: List<FuelRecordEntity>, // 과거 기록 (과거 -> 최신)
    initialReceiptData: maha.garage.util.ParsedReceiptData? = null,
    onScanReceiptClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        odometer: Int,
        fuelAmount: Double,
        unitPrice: Int,
        totalPrice: Int,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        gasStationName: String?
    ) -> Unit
) {
    var odometerText by remember { mutableStateOf(if (currentCarMileage > 0) currentCarMileage.toString() else "") }
    var fuelAmountText by remember {
        mutableStateOf(
            initialReceiptData?.fuelAmount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.2f", it)
            } ?: ""
        )
    }
    var unitPriceText by remember {
        mutableStateOf(initialReceiptData?.unitPrice?.toString() ?: "1650")
    }
    var totalPriceText by remember {
        mutableStateOf(initialReceiptData?.totalPrice?.toString() ?: "")
    }
    var gasStationName by remember {
        mutableStateOf(initialReceiptData?.gasStationName ?: "")
    }
    var isFull by remember { mutableStateOf(true) }
    var isMissedPrevious by remember { mutableStateOf(false) }

    // 추천 주유소 브랜드 칩 목록
    val brandChips = listOf("GS칼텍스", "SK에너지", "현대오일뱅크", "S-OIL", "알뜰/기타")

    // 주유량/단가/총액 상호 동기화 로직
    fun recalculateTotal(amountStr: String, unitStr: String): String? {
        val amount = amountStr.toDoubleOrNull() ?: return null
        val unit = unitStr.toIntOrNull() ?: return null
        return (amount * unit).toInt().toString()
    }

    fun recalculateAmount(totalStr: String, unitStr: String): String? {
        val total = totalStr.toIntOrNull() ?: return null
        val unit = unitStr.toIntOrNull() ?: return null
        if (unit <= 0) return null
        return String.format(Locale.US, "%.2f", total.toDouble() / unit)
    }

    // 영수증 OCR 파싱 데이터가 전달되거나 갱신될 때 입력 필드 자동 채우기 및 상호 보정
    LaunchedEffect(initialReceiptData) {
        initialReceiptData?.let { data ->
            data.fuelAmount?.let { amount ->
                fuelAmountText = if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format(Locale.US, "%.2f", amount)
            }
            data.unitPrice?.let { unit ->
                unitPriceText = unit.toString()
            }
            data.totalPrice?.let { total ->
                totalPriceText = total.toString()
            }
            data.gasStationName?.let { station ->
                if (station.isNotBlank()) gasStationName = station
            }

            // 상호 보완 자동 채우기 (리터가 비어있고 총액과 단가가 있으면 리터 계산)
            if (fuelAmountText.isBlank() && totalPriceText.isNotBlank() && unitPriceText.isNotBlank()) {
                recalculateAmount(totalPriceText, unitPriceText)?.let { fuelAmountText = it }
            } else if (totalPriceText.isBlank() && fuelAmountText.isNotBlank() && unitPriceText.isNotBlank()) {
                recalculateTotal(fuelAmountText, unitPriceText)?.let { totalPriceText = it }
            }
        }
    }

    // 예상 연비 프리뷰 계산
    val previewResult = remember(odometerText, fuelAmountText, isFull, isMissedPrevious) {
        val odo = odometerText.toIntOrNull()
        val amount = fuelAmountText.toDoubleOrNull()
        if (odo != null && amount != null && amount > 0.0) {
            val input = FuelCalculationInput(
                odometer = odo,
                fuelAmount = amount,
                isFull = isFull,
                isMissedPrevious = isMissedPrevious
            )
            val historyInputs = historyRecords.map {
                FuelCalculationInput(it.odometer, it.fuelAmount, it.isFull, it.isMissedPrevious)
            }
            FuelCalculator.calculate(input, historyInputs)
        } else {
            null
        }
    }

    val isValid = remember(odometerText, fuelAmountText, totalPriceText) {
        val odo = odometerText.toIntOrNull() ?: 0
        val amount = fuelAmountText.toDoubleOrNull() ?: 0.0
        val total = totalPriceText.toIntOrNull() ?: 0
        odo > 0 && amount > 0.0 && total > 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 다이얼로그 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ToolYellowSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = ToolOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "주유 기록 등록",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftInk
                        )
                        Text(
                            text = carName,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray
                        )
                    }
                    if (onScanReceiptClick != null) {
                        CameraLensFab(
                            size = 40.dp,
                            modifier = Modifier.tutorialTarget("fuel.camera") { onScanReceiptClick() },
                            onClick = onScanReceiptClick
                        )
                    }
                }

                val hasOcrAutoFilled = initialReceiptData != null && (fuelAmountText.isNotBlank() || totalPriceText.isNotBlank())
                if (hasOcrAutoFilled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ToolYellowSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = ToolOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "영수증 OCR로 주유량/금액이 자동 입력되었습니다. 계기판 주행거리(km)를 확인해주세요.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = SoftInk,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. 누적 주행거리 입력
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it.filter { c -> c.isDigit() } },
                    label = { Text("주유 시점 주행거리 (km)") },
                    placeholder = { Text("예: 54300") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ToolOrange,
                        focusedLabelColor = ToolOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 단가 / 주유량 / 총 결제금액 (3자 실시간 연동)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            unitPriceText = filtered
                            val newTotal = recalculateTotal(fuelAmountText, filtered)
                            if (newTotal != null) totalPriceText = newTotal
                        },
                        label = { Text("리터당 단가 (원)") },
                        placeholder = { Text("1650") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ToolOrange,
                            focusedLabelColor = ToolOrange
                        )
                    )

                    OutlinedTextField(
                        value = fuelAmountText,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() || c == '.' }
                            fuelAmountText = filtered
                            val newTotal = recalculateTotal(filtered, unitPriceText)
                            if (newTotal != null) totalPriceText = newTotal
                        },
                        label = { Text("주유량 (L)") },
                        placeholder = { Text("45.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ToolOrange,
                            focusedLabelColor = ToolOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalPriceText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }
                        totalPriceText = filtered
                        val newAmount = recalculateAmount(filtered, unitPriceText)
                        if (newAmount != null) fuelAmountText = newAmount
                    },
                    label = { Text("총 결제 금액 (원)") },
                    placeholder = { Text("75000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ToolOrange,
                        focusedLabelColor = ToolOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 주유소명 및 추천 칩
                OutlinedTextField(
                    value = gasStationName,
                    onValueChange = { gasStationName = it },
                    label = { Text("주유소명 (선택)") },
                    placeholder = { Text("예: SK에너지 강남셀프주유소") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ToolOrange,
                        focusedLabelColor = ToolOrange
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 브랜드 칩
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    brandChips.forEach { brand ->
                        val isSelected = when (brand) {
                            "S-OIL" -> gasStationName.contains("S-OIL", ignoreCase = true) ||
                                       gasStationName.contains("SOIL", ignoreCase = true) ||
                                       gasStationName.contains("에쓰오일") ||
                                       gasStationName.contains("에스오일")
                            "알뜰/기타" -> gasStationName.contains("알뜰") ||
                                         gasStationName.contains("기타") ||
                                         gasStationName.contains("ex-OIL", ignoreCase = true) ||
                                         gasStationName.contains("NH-OIL", ignoreCase = true)
                            else -> gasStationName.contains(brand, ignoreCase = true)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ToolOrange.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ToolOrange else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable {
                                    gasStationName = when (brand) {
                                        "알뜰/기타" -> "알뜰주유소"
                                        else -> brand
                                    }
                                }
                        ) {
                            Text(
                                text = brand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) ToolOrange else SoftInk,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 가득 주유(Full Tank) 토글
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "가득 주유 (Full Tank)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftInk
                        )
                        Text(
                            text = "Full-to-Full 연비 산출에 필수입니다.",
                            fontSize = 11.sp,
                            color = WarmGray
                        )
                    }
                    Switch(
                        checked = isFull,
                        onCheckedChange = { isFull = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ToolOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. 직전 주유 누락 여부 토글
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "직전 주유 기록 누락",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoftInk
                        )
                        Text(
                            text = "이전 주유를 건너뛴 경우 새 기준선 생성",
                            fontSize = 11.sp,
                            color = WarmGray
                        )
                    }
                    Switch(
                        checked = isMissedPrevious,
                        onCheckedChange = { isMissedPrevious = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WarmGray
                        )
                    )
                }

                // 6. 실시간 예상 연비 피드백 박스
                if (isMissedPrevious) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF7ED),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDBA74)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "직전 주유 누락: 이번 주유는 새 연비 측정 기준선으로 설정됩니다 (구간 연비 미계산)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }
                } else if (previewResult != null && previewResult.segmentEconomy != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(
                                    Locale.KOREA,
                                    "예상 구간 연비: %.2f km/L (주행 %d km / 소모 %.1f L)",
                                    previewResult.segmentEconomy,
                                    previewResult.distanceDriven ?: 0,
                                    previewResult.totalFuelConsumed ?: 0.0
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 하단 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }

                    val tutorialAction7 = tutorialAction7@ {
                            val odo = odometerText.toIntOrNull() ?: 0
                            val amount = fuelAmountText.toDoubleOrNull() ?: 0.0
                            val unit = unitPriceText.toIntOrNull() ?: 0
                            val total = totalPriceText.toIntOrNull() ?: (amount * unit).toInt()
                            onConfirm(odo, amount, unit, total, isFull, isMissedPrevious, gasStationName)
                        }
Button(
                        onClick = tutorialAction7,
                        enabled = isValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                        modifier = Modifier.weight(1f).tutorialTarget("fuel.save", tutorialAction7)) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
