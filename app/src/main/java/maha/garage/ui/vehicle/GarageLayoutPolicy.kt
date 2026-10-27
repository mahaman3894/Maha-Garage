package maha.garage.ui.vehicle

/** Single active car stays wide; multiple active cars use two to four columns. */
fun garageColumnCount(widthDp: Float, vehicleCount: Int): Int {
    if (vehicleCount <= 1) return 1
    val capacity = when {
        widthDp >= 1000f -> 4
        widthDp >= 600f -> 3
        else -> 2
    }
    return minOf(vehicleCount, capacity)
}

/** Rest cards match active card/image width unless there is only one (or no) active car. */
fun garageRestColumnCount(widthDp: Float, activeCount: Int, restCount: Int): Int =
    if (activeCount >= 2) garageColumnCount(widthDp, activeCount)
    else garageColumnCount(widthDp, restCount)
