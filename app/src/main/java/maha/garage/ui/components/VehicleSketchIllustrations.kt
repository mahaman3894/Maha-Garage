package maha.garage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import maha.garage.R
import maha.garage.data.model.enums.DueStatus
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass
import maha.garage.domain.model.ItemEvaluation
import maha.garage.ui.theme.*

/**
 * 차량 종류에 따른 대표 자동차 스케치 일러스트.
 * 카드 영역에서는 너무 기술 도면스럽지 않게, 차종 인지가 쉬운 사이드 뷰로 표현한다.
 */
@Composable
fun VehicleTypeIllustration(
    vehicleClass: VehicleClass,
    modifier: Modifier = Modifier
) {
    val strokeColor = GarageAccent.copy(alpha = 0.92f)
    val bodyFillColor = GaragePrimary.copy(alpha = 0.10f)
    val wheelColor = Color(0xFF64748B)
    val accentColor = GaragePrimaryLight

    Canvas(modifier = modifier) {
        when (vehicleClass) {
            VehicleClass.SEDAN -> drawSedanSketch(strokeColor, bodyFillColor, wheelColor, accentColor)
            VehicleClass.SUV -> drawSuvSketch(strokeColor, bodyFillColor, wheelColor, accentColor)
            VehicleClass.LIGHT_TRUCK, VehicleClass.MEDIUM_TRUCK,
            VehicleClass.HEAVY_TRUCK, VehicleClass.PICKUP -> drawTruckSketch(strokeColor, bodyFillColor, wheelColor, accentColor)
            VehicleClass.VAN, VehicleClass.BUS, VehicleClass.OTHER -> drawVanSketch(strokeColor, bodyFillColor, wheelColor, accentColor)
        }
    }
}

private fun DrawScope.drawSedanSketch(stroke: Color, fill: Color, wheelColor: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.80f

    val shadow = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(w * 0.16f, groundY - h * 0.05f, w * 0.86f, groundY + h * 0.05f), CornerRadius(w * 0.04f, w * 0.04f)))
    }
    drawPath(shadow, Color(0x33000000))

    val bodyPath = Path().apply {
        moveTo(w * 0.08f, groundY - h * 0.10f)
        cubicTo(w * 0.10f, groundY - h * 0.22f, w * 0.16f, groundY - h * 0.29f, w * 0.27f, groundY - h * 0.30f)
        lineTo(w * 0.36f, groundY - h * 0.31f)
        cubicTo(w * 0.42f, groundY - h * 0.47f, w * 0.47f, groundY - h * 0.58f, w * 0.53f, groundY - h * 0.60f)
        cubicTo(w * 0.64f, groundY - h * 0.61f, w * 0.73f, groundY - h * 0.56f, w * 0.78f, groundY - h * 0.46f)
        lineTo(w * 0.86f, groundY - h * 0.35f)
        cubicTo(w * 0.91f, groundY - h * 0.34f, w * 0.93f, groundY - h * 0.24f, w * 0.92f, groundY - h * 0.12f)
        lineTo(w * 0.88f, groundY - h * 0.10f)
        lineTo(w * 0.82f, groundY - h * 0.10f)
        arcTo(Rect(Offset(w * 0.75f, groundY - h * 0.10f), h * 0.15f), 0f, -180f, false)
        lineTo(w * 0.35f, groundY - h * 0.10f)
        arcTo(Rect(Offset(w * 0.27f, groundY - h * 0.10f), h * 0.15f), 0f, -180f, false)
        lineTo(w * 0.08f, groundY - h * 0.10f)
        close()
    }
    drawPath(bodyPath, brush = Brush.verticalGradient(listOf(Color(0xFF16344D), Color(0xFF0E2235))))
    drawPath(bodyPath, color = stroke, style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

    val cabin = Path().apply {
        moveTo(w * 0.39f, groundY - h * 0.31f)
        lineTo(w * 0.50f, groundY - h * 0.56f)
        cubicTo(w * 0.58f, groundY - h * 0.57f, w * 0.67f, groundY - h * 0.54f, w * 0.73f, groundY - h * 0.47f)
        lineTo(w * 0.80f, groundY - h * 0.31f)
        close()
    }
    drawPath(cabin, color = accent.copy(alpha = 0.16f), style = Fill)
    drawPath(cabin, color = accent.copy(alpha = 0.60f), style = Stroke(width = 1.2.dp.toPx()))
    drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.59f, groundY - h * 0.55f), Offset(w * 0.59f, groundY - h * 0.31f), 1.2f)
    drawLine(stroke.copy(alpha = 0.40f), Offset(w * 0.17f, groundY - h * 0.26f), Offset(w * 0.87f, groundY - h * 0.28f), 1.1f)
    drawLine(Color(0xFF67E8F9), Offset(w * 0.10f, groundY - h * 0.22f), Offset(w * 0.15f, groundY - h * 0.26f), 2.4f)
    drawLine(Color(0xFFEF4444), Offset(w * 0.90f, groundY - h * 0.27f), Offset(w * 0.92f, groundY - h * 0.22f), 2.4f)
    drawWheel(Offset(w * 0.27f, groundY - h * 0.10f), h * 0.12f, wheelColor, accent)
    drawWheel(Offset(w * 0.75f, groundY - h * 0.10f), h * 0.12f, wheelColor, accent)
}

private fun DrawScope.drawSuvSketch(stroke: Color, fill: Color, wheelColor: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.82f

    drawRoundRect(color = Color(0x22000000), topLeft = Offset(w * 0.17f, groundY - h * 0.01f), size = Size(w * 0.70f, h * 0.09f), cornerRadius = CornerRadius(w * 0.06f))
    drawLine(color = accent.copy(alpha = 0.65f), start = Offset(w * 0.44f, groundY - h * 0.67f), end = Offset(w * 0.73f, groundY - h * 0.67f), strokeWidth = 2.dp.toPx())

    val bodyPath = Path().apply {
        moveTo(w * 0.07f, groundY - h * 0.12f)
        lineTo(w * 0.07f, groundY - h * 0.27f)
        cubicTo(w * 0.10f, groundY - h * 0.38f, w * 0.16f, groundY - h * 0.42f, w * 0.25f, groundY - h * 0.43f)
        lineTo(w * 0.37f, groundY - h * 0.43f)
        lineTo(w * 0.48f, groundY - h * 0.62f)
        lineTo(w * 0.77f, groundY - h * 0.62f)
        lineTo(w * 0.84f, groundY - h * 0.56f)
        lineTo(w * 0.89f, groundY - h * 0.42f)
        lineTo(w * 0.91f, groundY - h * 0.12f)
        lineTo(w * 0.83f, groundY - h * 0.12f)
        arcTo(Rect(Offset(w * 0.74f, groundY - h * 0.12f), h * 0.16f), 0f, -180f, false)
        lineTo(w * 0.36f, groundY - h * 0.12f)
        arcTo(Rect(Offset(w * 0.27f, groundY - h * 0.12f), h * 0.16f), 0f, -180f, false)
        lineTo(w * 0.07f, groundY - h * 0.12f)
        close()
    }
    drawPath(bodyPath, brush = Brush.verticalGradient(listOf(Color(0xFF17364D), Color(0xFF0C2031))))
    drawPath(bodyPath, color = stroke, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    val glassPath = Path().apply {
        moveTo(w * 0.40f, groundY - h * 0.42f)
        lineTo(w * 0.49f, groundY - h * 0.60f)
        lineTo(w * 0.77f, groundY - h * 0.60f)
        lineTo(w * 0.82f, groundY - h * 0.42f)
        close()
    }
    drawPath(glassPath, color = accent.copy(alpha = 0.17f))
    drawPath(glassPath, color = accent.copy(alpha = 0.55f), style = Stroke(width = 1.2.dp.toPx()))
    drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.58f, groundY - h * 0.60f), Offset(w * 0.58f, groundY - h * 0.42f), 1.3f)
    drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.69f, groundY - h * 0.60f), Offset(w * 0.69f, groundY - h * 0.42f), 1.3f)
    drawLine(Color(0xFF38BDF8), Offset(w * 0.09f, groundY - h * 0.33f), Offset(w * 0.16f, groundY - h * 0.37f), 3f)
    drawLine(Color(0xFFEF4444), Offset(w * 0.88f, groundY - h * 0.38f), Offset(w * 0.90f, groundY - h * 0.32f), 3f)
    drawWheel(Offset(w * 0.27f, groundY - h * 0.12f), h * 0.13f, wheelColor, accent)
    drawWheel(Offset(w * 0.74f, groundY - h * 0.12f), h * 0.13f, wheelColor, accent)
}

private fun DrawScope.drawTruckSketch(stroke: Color, fill: Color, wheelColor: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.82f

    drawRoundRect(color = Color(0x22000000), topLeft = Offset(w * 0.14f, groundY - h * 0.01f), size = Size(w * 0.73f, h * 0.09f), cornerRadius = CornerRadius(w * 0.05f))
    val cabinPath = Path().apply {
        moveTo(w * 0.10f, groundY - h * 0.11f)
        lineTo(w * 0.10f, groundY - h * 0.39f)
        lineTo(w * 0.20f, groundY - h * 0.56f)
        lineTo(w * 0.42f, groundY - h * 0.56f)
        lineTo(w * 0.48f, groundY - h * 0.42f)
        lineTo(w * 0.48f, groundY - h * 0.11f)
        lineTo(w * 0.39f, groundY - h * 0.11f)
        arcTo(Rect(Offset(w * 0.31f, groundY - h * 0.11f), h * 0.14f), 0f, -180f, false)
        lineTo(w * 0.10f, groundY - h * 0.11f)
        close()
    }
    val bedPath = Path().apply {
        moveTo(w * 0.49f, groundY - h * 0.36f)
        lineTo(w * 0.86f, groundY - h * 0.36f)
        lineTo(w * 0.90f, groundY - h * 0.30f)
        lineTo(w * 0.90f, groundY - h * 0.11f)
        lineTo(w * 0.81f, groundY - h * 0.11f)
        arcTo(Rect(Offset(w * 0.73f, groundY - h * 0.11f), h * 0.14f), 0f, -180f, false)
        lineTo(w * 0.49f, groundY - h * 0.11f)
        close()
    }
    drawPath(cabinPath, brush = Brush.verticalGradient(listOf(Color(0xFF18364D), Color(0xFF0F2235))))
    drawPath(cabinPath, color = stroke, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    drawPath(bedPath, brush = Brush.verticalGradient(listOf(Color(0xFF17364B), Color(0xFF0E2030))))
    drawPath(bedPath, color = stroke, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    drawRoundRect(color = accent.copy(alpha = 0.16f), topLeft = Offset(w * 0.20f, groundY - h * 0.48f), size = Size(w * 0.17f, h * 0.16f), cornerRadius = CornerRadius(7f))
    drawRoundRect(color = accent.copy(alpha = 0.55f), topLeft = Offset(w * 0.20f, groundY - h * 0.48f), size = Size(w * 0.17f, h * 0.16f), cornerRadius = CornerRadius(7f), style = Stroke(width = 1.2.dp.toPx()))
    drawLine(Color(0xFF67E8F9), Offset(w * 0.11f, groundY - h * 0.28f), Offset(w * 0.16f, groundY - h * 0.33f), 3f)
    drawLine(Color(0xFFEF4444), Offset(w * 0.89f, groundY - h * 0.31f), Offset(w * 0.89f, groundY - h * 0.19f), 3f)
    drawWheel(Offset(w * 0.31f, groundY - h * 0.11f), h * 0.12f, wheelColor, accent)
    drawWheel(Offset(w * 0.73f, groundY - h * 0.11f), h * 0.12f, wheelColor, accent)
}

private fun DrawScope.drawVanSketch(stroke: Color, fill: Color, wheelColor: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.82f

    drawRoundRect(color = Color(0x22000000), topLeft = Offset(w * 0.13f, groundY - h * 0.01f), size = Size(w * 0.74f, h * 0.09f), cornerRadius = CornerRadius(w * 0.05f))
    val bodyPath = Path().apply {
        moveTo(w * 0.08f, groundY - h * 0.11f)
        lineTo(w * 0.08f, groundY - h * 0.29f)
        cubicTo(w * 0.12f, groundY - h * 0.46f, w * 0.20f, groundY - h * 0.62f, w * 0.29f, groundY - h * 0.62f)
        lineTo(w * 0.77f, groundY - h * 0.62f)
        cubicTo(w * 0.87f, groundY - h * 0.62f, w * 0.89f, groundY - h * 0.56f, w * 0.90f, groundY - h * 0.45f)
        lineTo(w * 0.90f, groundY - h * 0.11f)
        lineTo(w * 0.81f, groundY - h * 0.11f)
        arcTo(Rect(Offset(w * 0.73f, groundY - h * 0.11f), h * 0.15f), 0f, -180f, false)
        lineTo(w * 0.36f, groundY - h * 0.11f)
        arcTo(Rect(Offset(w * 0.27f, groundY - h * 0.11f), h * 0.15f), 0f, -180f, false)
        lineTo(w * 0.08f, groundY - h * 0.11f)
        close()
    }
    drawPath(bodyPath, brush = Brush.verticalGradient(listOf(Color(0xFF17354B), Color(0xFF0E2031))))
    drawPath(bodyPath, color = stroke, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    drawRoundRect(color = accent.copy(alpha = 0.18f), topLeft = Offset(w * 0.21f, groundY - h * 0.53f), size = Size(w * 0.58f, h * 0.16f), cornerRadius = CornerRadius(8f))
    drawRoundRect(color = accent.copy(alpha = 0.55f), topLeft = Offset(w * 0.21f, groundY - h * 0.53f), size = Size(w * 0.58f, h * 0.16f), cornerRadius = CornerRadius(8f), style = Stroke(width = 1.1.dp.toPx()))
    drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.42f, groundY - h * 0.53f), Offset(w * 0.42f, groundY - h * 0.37f), 1.2f)
    drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.63f, groundY - h * 0.53f), Offset(w * 0.63f, groundY - h * 0.37f), 1.2f)
    drawLine(Color(0xFF67E8F9), Offset(w * 0.09f, groundY - h * 0.30f), Offset(w * 0.15f, groundY - h * 0.34f), 3f)
    drawLine(Color(0xFFEF4444), Offset(w * 0.88f, groundY - h * 0.49f), Offset(w * 0.88f, groundY - h * 0.23f), 3f)
    drawWheel(Offset(w * 0.27f, groundY - h * 0.11f), h * 0.12f, wheelColor, accent)
    drawWheel(Offset(w * 0.73f, groundY - h * 0.11f), h * 0.12f, wheelColor, accent)
}

private fun DrawScope.drawWheel(center: Offset, radius: Float, wheelColor: Color, accent: Color) {
    drawCircle(color = Color(0xFF182334), radius = radius, center = center)
    drawCircle(color = wheelColor, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
    drawCircle(color = Color(0xFF0F172A), radius = radius * 0.65f, center = center)
    drawCircle(color = accent.copy(alpha = 0.82f), radius = radius * 0.65f, center = center, style = Stroke(width = 1.4.dp.toPx()))
    drawCircle(color = accent, radius = radius * 0.22f, center = center)
}


/**
 * 8종 대표 차량 스케치 일러스트 WebP 리소스 매핑 (MahaGarage_Vehicle_Card_Images)
 */
fun getVehicleIllustrationRes(
    vehicleClass: VehicleClass,
    powertrain: PowertrainType,
    modelName: String = "",
    make: String = ""
): Int {
    val lower = "$make $modelName".lowercase()
    return when {
        lower.contains("cyber") || lower.contains("사이버") -> R.drawable.vehicle_cyber
        lower.contains("sport") || lower.contains("스포츠") || lower.contains("페라리") || lower.contains("포르쉐") || lower.contains("ferrari") || lower.contains("porsche") -> R.drawable.vehicle_sport
        powertrain == PowertrainType.BEV || powertrain == PowertrainType.HEV_GASOLINE || powertrain == PowertrainType.HEV_DIESEL || powertrain == PowertrainType.PHEV_GASOLINE || powertrain == PowertrainType.PHEV_DIESEL -> {
            when (vehicleClass) {
                VehicleClass.SEDAN -> R.drawable.vehicle_ev_hybrid
                VehicleClass.SUV -> R.drawable.vehicle_suv
                VehicleClass.PICKUP, VehicleClass.LIGHT_TRUCK, VehicleClass.MEDIUM_TRUCK, VehicleClass.HEAVY_TRUCK -> R.drawable.vehicle_pickup
                VehicleClass.VAN -> R.drawable.vehicle_mpv
                VehicleClass.BUS -> R.drawable.vehicle_van_bus
                else -> R.drawable.vehicle_ev_hybrid
            }
        }
        vehicleClass == VehicleClass.SEDAN -> R.drawable.vehicle_sedan
        vehicleClass == VehicleClass.SUV -> R.drawable.vehicle_suv
        vehicleClass == VehicleClass.PICKUP || vehicleClass == VehicleClass.LIGHT_TRUCK || vehicleClass == VehicleClass.MEDIUM_TRUCK || vehicleClass == VehicleClass.HEAVY_TRUCK -> R.drawable.vehicle_pickup
        vehicleClass == VehicleClass.VAN -> R.drawable.vehicle_mpv
        vehicleClass == VehicleClass.BUS -> R.drawable.vehicle_van_bus
        else -> R.drawable.vehicle_sedan
    }
}

/** The measured image rectangle, callout bounds and leader lines share one layout. */
@Composable
fun VehicleConsumablesSketchMap(
    vehicleClass: VehicleClass,
    powertrain: PowertrainType,
    evaluations: List<ItemEvaluation>,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSpecsClick: (() -> Unit)? = null,
    diagramHeight: Dp = 450.dp
) {
    val evalMap = evaluations.associateBy { it.itemCode }
    val density = LocalDensity.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, WarmLine),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "차량 부품 위치별 상태 점검도",
                        color = SoftInk,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendItem(JackGreen, "양호")
                        LegendItem(ToolYellow, "교체 예정")
                        LegendItem(SketchRed, "교체 필요")
                        LegendItem(StatusNeutral, "대기")
                    }
                }
                if (onSpecsClick != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ToolYellowSoft,
                        border = BorderStroke(1.dp, ToolYellow.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable(onClick = onSpecsClick)
                    ) {
                        Text(
                            "제원 보기",
                            color = SoftInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val h = sketchDiagramHeight(maxWidth.value, density.fontScale, diagramHeight.value)
                val layout = calculateSketchLayout(
                    maxWidth.value,
                    h,
                    density.fontScale,
                    powertrain == PowertrainType.BEV,
                    evalMap.containsKey("DIFFERENTIAL_OIL")
                )

                Box(Modifier.fillMaxWidth().height(h.dp)) {
                    // 1. 차량 탑뷰 연필 스케치 투명 이미지 (MahaGarage_Car_Inspection_Assets/mahagarage_car.webp)
                    Image(
                        painter = painterResource(R.drawable.mahagarage_car),
                        contentDescription = "차량 부품 위치 점검도 탑뷰",
                        modifier = Modifier
                            .absoluteOffset(layout.car.x.dp, layout.car.y.dp)
                            .size(layout.car.width.dp, layout.car.height.dp),
                        contentScale = ContentScale.Fit
                    )

                    // 2. 부품 위치 연결선 및 상태 포인트 (Canvas)
                    Canvas(Modifier.fillMaxSize()) {
                        layout.callouts.filter { evalMap.containsKey(it.code) }.forEach { marker ->
                            val evaluation = evalMap[marker.code]
                            val markerColor = when (evaluation?.status) {
                                DueStatus.OVERDUE, DueStatus.DUE_NOW -> SketchRed
                                DueStatus.DUE_SOON -> ToolYellow
                                DueStatus.INSPECTION_DUE -> ToolOrange
                                DueStatus.NORMAL -> JackGreen
                                null, DueStatus.NEEDS_DATA -> StatusNeutral
                            }

                            val start = Offset(marker.start.x.dp.toPx(), marker.start.y.dp.toPx())
                            val end = Offset(marker.anchor.x.dp.toPx(), marker.anchor.y.dp.toPx())
                            val path = Path().apply {
                                moveTo(start.x, start.y)
                                marker.bends.forEach { lineTo(it.x.dp.toPx(), it.y.dp.toPx()) }
                                lineTo(end.x, end.y)
                            }

                            // 부드러운 연결선
                            drawPath(
                                path,
                                color = ToolOrange.copy(alpha = 0.20f),
                                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path,
                                color = ToolOrange.copy(alpha = 0.85f),
                                style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // 차량 위 상태 포인트 (고대비 화이트 외곽선 + 상태 컬러)
                            drawCircle(Color.White, 7.dp.toPx(), end)
                            drawCircle(markerColor, 5.5.dp.toPx(), end)
                            drawCircle(Color.White, 2.dp.toPx(), end)

                            if (evaluation?.status == DueStatus.OVERDUE || evaluation?.status == DueStatus.DUE_NOW) {
                                drawCircle(SketchRed.copy(alpha = 0.35f), 10.dp.toPx(), end, style = Stroke(1.5.dp.toPx()))
                            }
                        }

                        // 휠 핀 표시
                        layout.pins.forEach { pin ->
                            val eval = evalMap[pin.code]
                            val pinColor = when (eval?.status) {
                                DueStatus.OVERDUE, DueStatus.DUE_NOW -> SketchRed
                                DueStatus.DUE_SOON -> ToolYellow
                                DueStatus.INSPECTION_DUE -> ToolOrange
                                DueStatus.NORMAL -> JackGreen
                                null, DueStatus.NEEDS_DATA -> StatusNeutral
                            }
                            val p = Offset(pin.point.x.dp.toPx(), pin.point.y.dp.toPx())
                            drawCircle(Color.White, 6.dp.toPx(), p)
                            drawCircle(pinColor, 4.5.dp.toPx(), p)
                        }
                    }

                    // 3. 차체 위 상태 핀 직접 터치 타겟 영역 (최소 44dp 확보)
                    layout.callouts.filter { evalMap.containsKey(it.code) }.forEach { marker ->
                        Box(
                            modifier = Modifier
                                .absoluteOffset {
                                    IntOffset(
                                        with(density) { (marker.anchor.x - 22f).dp.roundToPx() },
                                        with(density) { (marker.anchor.y - 22f).dp.roundToPx() }
                                    )
                                }
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onMarkerClick(marker.code) }
                        )
                    }

                    // 4. 상태 콜아웃 박스 (상/하/좌/우 반응형 카드)
                    layout.callouts.forEach { marker ->
                        val evaluation = evalMap[marker.code] ?: return@forEach
                        val color = when (evaluation.status) {
                            DueStatus.OVERDUE, DueStatus.DUE_NOW -> SketchRed
                            DueStatus.DUE_SOON -> ToolYellow
                            DueStatus.INSPECTION_DUE -> ToolOrange
                            DueStatus.NORMAL -> JackGreen
                            DueStatus.NEEDS_DATA -> StatusNeutral
                        }

                        Surface(
                            modifier = Modifier
                                .absoluteOffset {
                                    IntOffset(
                                        with(density) { marker.box.x.dp.roundToPx() },
                                        with(density) { marker.box.y.dp.roundToPx() }
                                    )
                                }
                                .requiredSize(marker.box.width.dp, marker.box.height.dp)
                                .clickable { onMarkerClick(marker.code) },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceWhiteStrong,
                            border = BorderStroke(1.2.dp, color.copy(alpha = 0.7f)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    marker.label,
                                    color = SoftInk,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        evaluation.status.displayName,
                                        color = color,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = WarmGray, fontWeight = FontWeight.Medium)
    }
}

