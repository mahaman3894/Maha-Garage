package maha.garage.domain.engine

import kotlin.math.roundToInt

data class FuelCalculationInput(
    val odometer: Int,
    val fuelAmount: Double,
    val isFull: Boolean,
    val isMissedPrevious: Boolean
)

data class FuelCalculationResult(
    val segmentEconomy: Double?,   // 구간 연비 (km/L)
    val distanceDriven: Int?,      // 주행 거리 (km)
    val totalFuelConsumed: Double? // 소모 주유량 (L)
)

object FuelCalculator {

    /**
     * 현재 주유 기록과 직전 기록 히스토리를 대조하여 실연비를 산출합니다.
     * Full-to-Full 방식: 직전 가득 주유 시점부터 이번 가득 주유 시점까지의 주행거리를
     * 그 사이에 주입된 모든 연료량(중간 부분 주유량 누적 + 이번 주유량)으로 나눕니다.
     */
    fun calculate(
        current: FuelCalculationInput,
        history: List<FuelCalculationInput> // 과거 -> 최신 순 정렬된 이전 주유 목록
    ): FuelCalculationResult {
        // 1. 이번 주유가 가득 주유가 아니거나, 이전 기록을 건너뛴 경우 연비 산출 불가
        if (!current.isFull || current.isMissedPrevious || history.isEmpty()) {
            return FuelCalculationResult(null, null, null)
        }

        // 2. 가장 최근의 '가득(Full)' 주유 시점을 탐색
        val lastFullIndex = history.indexOfLast { it.isFull }
        if (lastFullIndex == -1) {
            // 기준선이 없으므로 계산 불가 (첫 가득 주유는 시작 기준점)
            return FuelCalculationResult(null, null, null)
        }

        val lastFullRecord = history[lastFullIndex]
        val distance = current.odometer - lastFullRecord.odometer
        if (distance <= 0) {
            return FuelCalculationResult(null, null, null)
        }

        // 3. 직전 가득 주유 이후 투입된 모든 부분 주유량 합산 + 이번 주유량
        val intermediateFuel = history.subList(lastFullIndex + 1, history.size)
            .sumOf { it.fuelAmount }
        val totalFuel = intermediateFuel + current.fuelAmount

        if (totalFuel <= 0.0) {
            return FuelCalculationResult(null, null, null)
        }

        val rawEconomy = distance / totalFuel
        val roundedEconomy = (rawEconomy * 100.0).roundToInt() / 100.0

        return FuelCalculationResult(
            segmentEconomy = roundedEconomy,
            distanceDriven = distance,
            totalFuelConsumed = (totalFuel * 100.0).roundToInt() / 100.0
        )
    }

    /**
     * 1km당 유류비 산출 (원/km)
     */
    fun calculateCostPerKm(totalCost: Int, distanceDriven: Int): Int? {
        if (distanceDriven <= 0) return null
        return (totalCost.toDouble() / distanceDriven).roundToInt()
    }

    /**
     * 공인 연비 대비 효율 지수 (%)
     */
    fun calculateEfficiency(segmentEconomy: Double?, officialEconomy: Double?): Double? {
        if (segmentEconomy == null || officialEconomy == null || officialEconomy <= 0.0) return null
        val eff = (segmentEconomy / officialEconomy) * 100.0
        return (eff * 10.0).roundToInt() / 10.0
    }
}
