package maha.garage

import maha.garage.domain.engine.FuelCalculationInput
import maha.garage.domain.engine.FuelCalculator
import org.junit.Assert.*
import org.junit.Test

class FuelCalculatorTest {

    @Test
    fun testFirstFullRefuel_returnsNullEconomy() {
        // 첫 번째 가득 주유 (히스토리 없음)
        val current = FuelCalculationInput(
            odometer = 50000,
            fuelAmount = 50.0,
            isFull = true,
            isMissedPrevious = false
        )
        val history = emptyList<FuelCalculationInput>()

        val result = FuelCalculator.calculate(current, history)
        assertNull("첫 가득 주유는 기준선이므로 구간 연비는 null이어야 함", result.segmentEconomy)
    }

    @Test
    fun testSecondFullRefuel_calculatesCorrectEconomy() {
        // 첫 번째 가득 주유 (50,000 km)
        val first = FuelCalculationInput(50000, 50.0, isFull = true, isMissedPrevious = false)
        // 두 번째 가득 주유 (50,600 km, 50.0 L) -> 주행 600km / 50L = 12.00 km/L
        val second = FuelCalculationInput(50600, 50.0, isFull = true, isMissedPrevious = false)

        val result = FuelCalculator.calculate(second, listOf(first))
        assertEquals(12.00, result.segmentEconomy!!, 0.001)
        assertEquals(600, result.distanceDriven)
        assertEquals(50.0, result.totalFuelConsumed!!, 0.001)
    }

    @Test
    fun testPartialRefuel_accumulatesToNextFullRefuel() {
        // 1. 첫 가득 주유 (50,000 km)
        val first = FuelCalculationInput(50000, 50.0, isFull = true, isMissedPrevious = false)
        // 2. 중간 부분 주유 (50,300 km, 20.0 L - 부분 주유)
        val partial = FuelCalculationInput(50300, 20.0, isFull = false, isMissedPrevious = false)
        // 3. 다음 가득 주유 (50,700 km, 30.0 L - 가득)
        // 총 주행거리: 50,700 - 50,000 = 700 km
        // 총 투입 연료: 20.0 (부분) + 30.0 (현재) = 50.0 L
        // 연비 = 700 / 50 = 14.00 km/L
        val current = FuelCalculationInput(50700, 30.0, isFull = true, isMissedPrevious = false)

        val result = FuelCalculator.calculate(current, listOf(first, partial))
        assertEquals(14.00, result.segmentEconomy!!, 0.001)
        assertEquals(700, result.distanceDriven)
        assertEquals(50.0, result.totalFuelConsumed!!, 0.001)
    }

    @Test
    fun testMissedPrevious_resetsBaselineAndReturnsNull() {
        val first = FuelCalculationInput(50000, 50.0, isFull = true, isMissedPrevious = false)
        // 직전 주유를 기록하지 못해서 건너뛴 경우
        val current = FuelCalculationInput(51000, 50.0, isFull = true, isMissedPrevious = true)

        val result = FuelCalculator.calculate(current, listOf(first))
        assertNull("직전 주유 누락 시 연비 계산을 건너뛰어야 함", result.segmentEconomy)
    }

    @Test
    fun testNotFullTank_returnsNullEconomy() {
        val first = FuelCalculationInput(50000, 50.0, isFull = true, isMissedPrevious = false)
        // 이번 주유가 가득 주유가 아닌 경우
        val current = FuelCalculationInput(50500, 20.0, isFull = false, isMissedPrevious = false)

        val result = FuelCalculator.calculate(current, listOf(first))
        assertNull("부분 주유 시에는 연비를 산출하지 않음", result.segmentEconomy)
    }

    @Test
    fun testCalculateCostPerKm() {
        val costPerKm = FuelCalculator.calculateCostPerKm(totalCost = 80000, distanceDriven = 500)
        assertEquals(160, costPerKm)
    }

    @Test
    fun testCalculateEfficiency() {
        val efficiency = FuelCalculator.calculateEfficiency(segmentEconomy = 15.0, officialEconomy = 12.5)
        assertEquals(120.0, efficiency!!, 0.01)
    }
}
