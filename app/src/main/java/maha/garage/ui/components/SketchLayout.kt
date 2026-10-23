package maha.garage.ui.components

/** All coordinates are local dp in the same measured BoxWithConstraints. */
data class SketchRect(val x: Float, val y: Float, val width: Float, val height: Float)
data class SketchPoint(val x: Float, val y: Float)
data class SketchCallout(
    val code: String,
    val label: String,
    val box: SketchRect,
    val start: SketchPoint,
    val anchor: SketchPoint,
    val bends: List<SketchPoint> = emptyList()
)
data class SketchPin(
    val code: String,
    val label: String,
    val point: SketchPoint
)
data class SketchLayout(
    val car: SketchRect,
    val callouts: List<SketchCallout>,
    val pins: List<SketchPin> = emptyList()
)

fun sketchDiagramHeight(width: Float, fontScale: Float, preferredHeight: Float): Float {
    val font = fontScale.coerceAtLeast(1f)
    return maxOf(preferredHeight, 260f * font + 32f, 170f * font + width * .35f + 96f)
}

/**
 * 1620x725 mahagarage-parts.json 정규화 좌표계를 반영한 레이아웃 계산 함수
 */
fun calculateSketchLayout(
    width: Float,
    height: Float,
    fontScale: Float,
    electric: Boolean,
    differential: Boolean
): SketchLayout {
    val gap = 6f
    val boxHeight = 84f * fontScale.coerceAtLeast(1f)
    val cellWidth = (width - 3 * gap) / 4
    val carWidth = width * if (differential) .62f else .74f
    // mahagarage-car.webp 원본 비율 1620 x 725 (2.2345:1)
    val carHeight = carWidth * 725f / 1620f
    val car = SketchRect(width * .18f, (height - carHeight) / 2, carWidth, carHeight)

    fun point(nx: Float, ny: Float) = SketchPoint(car.x + car.width * nx, car.y + car.height * ny)

    val result = mutableListOf<SketchCallout>()
    fun row(code: String, label: String, column: Int, top: Boolean, nx: Float, ny: Float) {
        val box = SketchRect(column * (cellWidth + gap), if (top) 0f else height - boxHeight, cellWidth, boxHeight)
        val anchor = point(nx, ny)
        val corridor = if (top) (boxHeight + height / 2 - boxHeight / 2) / 2
        else (height - boxHeight + height / 2 + boxHeight / 2) / 2
        val bends = if (column == 0) listOf(SketchPoint(anchor.x, corridor)) else emptyList()
        result += SketchCallout(
            code = code,
            label = label,
            box = box,
            start = SketchPoint(box.x + box.width / 2, if (top) box.y + box.height else box.y),
            anchor = anchor,
            bends = bends
        )
    }

    // 상단 콜아웃 4개 (mahagarage-parts.json 좌표 정밀 반영)
    if (!electric) {
        row("ENGINE_OIL", "엔진오일", 0, true, 0.1944f, 0.4207f)
        row("ENGINE_AIR_FILTER", "에어클리너", 1, true, 0.2099f, 0.3172f)
    } else {
        row("BATTERY_12V", "저전압 배터리", 0, true, 0.1093f, 0.2483f)
        row("CABIN_FILTER", "에어컨필터", 1, true, 0.3765f, 0.3366f)
    }
    row("WIPER_BLADE", "와이퍼", 2, true, 0.2963f, 0.1821f)
    if (!electric) {
        row("CABIN_FILTER", "에어컨필터", 3, true, 0.3765f, 0.3366f)
    } else {
        row("BRAKE_FLUID", "브레이크액", 3, true, 0.2160f, 0.2138f)
    }

    // 좌측 냉각수 (coolant)
    val coolant = SketchRect(0f, height / 2 - boxHeight / 2, width * .17f, boxHeight)
    result += SketchCallout(
        code = if (electric) "EV_COOLANT" else "COOLANT",
        label = "냉각수",
        box = coolant,
        start = SketchPoint(coolant.width, height / 2),
        anchor = point(0.0735f, 0.4621f)
    )

    // 하단 콜아웃 4개
    if (!electric) {
        row("BATTERY_12V", "12V 배터리", 0, false, 0.1093f, 0.2483f)
    } else {
        row("BRAKE_PAD_FRONT", "전륜 패드", 0, false, 0.2037f, 0.7310f)
    }
    if (!electric) {
        row("BRAKE_PAD_FRONT", "전륜 패드", 1, false, 0.2037f, 0.7310f)
    } else {
        row("TIRE", "타이어", 1, false, 0.1975f, 0.8855f)
    }
    row(if (electric) "REDUCTION_GEAR_OIL" else "TRANSMISSION_OIL", if (electric) "감속기오일" else "미션오일", 2, false, 0.3265f, 0.5034f)
    row("BRAKE_PAD_REAR", "후륜 패드", 3, false, 0.7654f, 0.7103f)

    // 우측 디퍼렌셜 (differential)
    if (differential) {
        val box = SketchRect(width * .84f, height / 2 - boxHeight / 2, width * .16f, boxHeight)
        result += SketchCallout(
            code = "DIFFERENTIAL_OIL",
            label = "디퍼렌셜",
            box = box,
            start = SketchPoint(box.x, height / 2),
            anchor = point(0.7660f, 0.5034f)
        )
    }

    // 차량 휠 4개 타이어 상태 핀
    val pins = listOf(
        SketchPin("TIRE", "전륜좌측", point(0.1975f, 0.8855f)),
        SketchPin("TIRE", "전륜우측", point(0.1975f, 0.1297f)),
        SketchPin("TIRE", "후륜좌측", point(0.7562f, 0.8855f)),
        SketchPin("TIRE", "후륜우측", point(0.7562f, 0.1228f))
    )

    return SketchLayout(car, result, pins)
}

