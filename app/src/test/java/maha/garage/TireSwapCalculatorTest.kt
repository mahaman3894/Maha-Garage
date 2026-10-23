package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.engine.TireSwapCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class TireSwapCalculatorTest {

    @Test
    fun `실시간 타이어 거리 계산 - 현재 장착 모드 타이어에만 미기록 주행거리가 합산되어야 한다`() {
        // 섬머 타이어 장착 상태 (스왑 시점 90,000km, 현재 94,500km -> +4,500km 주행 중)
        val car = CarEntity(
            id = "c1",
            carNumber = "12가 3456",
            modelName = "335i",
            currentMileage = 94500,
            lastSwapMileage = 90000,
            currentTireMode = "SUMMER",
            summerTireMileage = 10000,
            winterTireMileage = 8000
        )

        val live = TireSwapCalculator.calculateLiveTireDistances(car)

        assertEquals("섬머 타이어는 기존 10,000 + 주행 4,500 = 14,500km여야 함", 14500, live.summerTotalKm)
        assertEquals("보관 중인 윈터 타이어는 그대로 8,000km여야 함", 8000, live.winterTotalKm)
        assertEquals("현재 장착 주행거리는 4,500km여야 함", 4500, live.currentMountDrivenSinceSwapKm)
    }

    @Test
    fun `타이어 스왑 수행 - 섬머에서 윈터로 스왑 시 이전 주행거리가 누적되고 신규 모드로 정상 전환되어야 한다`() {
        val car = CarEntity(
            id = "c1",
            carNumber = "12가 3456",
            modelName = "335i",
            currentMileage = 96000,
            lastSwapMileage = 90000,
            currentTireMode = "SUMMER",
            summerTireMileage = 10000,
            winterTireMileage = 8000
        )

        val result = TireSwapCalculator.performTireSwap(
            car = car,
            targetMode = "WINTER",
            swapMileage = 96000,
            swapDate = "2026-11-15",
            storageLocation = "타이어프로 강남점",
            frontTreadDepthMm = 6.2,
            rearTreadDepthMm = 5.8,
            swapCost = 60000L,
            notes = "동절기 스노우타이어 교체 장착"
        )

        val updatedCar = result.updatedCar
        val swapLog = result.swapLog

        // 변경된 차량 상태 검증
        assertEquals("현재 타이어 모드는 WINTER여야 함", "WINTER", updatedCar.currentTireMode)
        assertEquals("섬머 타이어 누적 거리는 10,000 + 6,000 = 16,000km여야 함", 16000, updatedCar.summerTireMileage)
        assertEquals("윈터 타이어 누적 거리는 아직 8,000km여야 함", 8000, updatedCar.winterTireMileage)
        assertEquals("마지막 스왑 시점 주행거리는 96,000km로 갱신되어야 함", 96000, updatedCar.lastSwapMileage)

        // 생성된 스왑 로그 검증
        assertEquals("c1", swapLog.carId)
        assertEquals("SUMMER", swapLog.fromMode)
        assertEquals("WINTER", swapLog.toMode)
        assertEquals(6000, swapLog.drivenDistanceDuringMode)
        assertEquals(96000, swapLog.swapMileage)
        assertEquals("타이어프로 강남점", swapLog.storageLocation)
        assertEquals(60000L, swapLog.swapCost)
    }
}
