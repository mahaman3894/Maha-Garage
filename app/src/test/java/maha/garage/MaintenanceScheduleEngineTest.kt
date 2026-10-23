package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.engine.MaintenanceScheduleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceScheduleEngineTest {

    private val sampleCatalogOil = MaintenanceCatalogEntity(
        itemCode = "ENGINE_OIL",
        displayName = "엔진오일 & 필터",
        category = "엔진/구동",
        defaultActionType = ActionType.REPLACE,
        defaultIntervalKm = 10000,
        defaultIntervalMonths = 12
    )

    private val sampleCatalogPad = MaintenanceCatalogEntity(
        itemCode = "BRAKE_PAD_FRONT",
        displayName = "전륜 브레이크 패드",
        category = "제동계통",
        defaultActionType = ActionType.INSPECT,
        defaultIntervalKm = 20000,
        isConditionBased = true
    )

    @Test
    fun `주행거리 기준 - 교체 주기 여유 시 NORMAL 상태여야 한다`() {
        val car = CarEntity(
            id = "c1", carNumber = "12가 3456", modelName = "335i", currentMileage = 95000
        )
        val rule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            lastServiceMileage = 90000,
            lastServiceDate = "2026-08-01"
        )

        val eval = MaintenanceScheduleEngine.evaluateItem(
            car, rule, sampleCatalogOil, today = LocalDate.of(2026, 10, 16)
        )

        assertEquals(DueStatus.NORMAL, eval.status)
        assertEquals(5000, eval.remainingKm)
        assertEquals(100000, eval.nextDueKm)
    }

    @Test
    fun `주행거리 기준 - 잔여 1000km 이내 도래 시 DUE_SOON 상태여야 한다`() {
        val car = CarEntity(
            id = "c1", carNumber = "12가 3456", modelName = "335i", currentMileage = 99500
        )
        val rule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            lastServiceMileage = 90000,
            lastServiceDate = "2026-08-01"
        )

        val eval = MaintenanceScheduleEngine.evaluateItem(
            car, rule, sampleCatalogOil, today = LocalDate.of(2026, 10, 16)
        )

        assertEquals(DueStatus.DUE_SOON, eval.status)
        assertEquals(500, eval.remainingKm)
    }

    @Test
    fun `주행거리 기준 - 주기 초과 시 OVERDUE 상태여야 한다`() {
        val car = CarEntity(
            id = "c1", carNumber = "12가 3456", modelName = "335i", currentMileage = 101200
        )
        val rule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            lastServiceMileage = 90000,
            lastServiceDate = "2026-08-01"
        )

        val eval = MaintenanceScheduleEngine.evaluateItem(
            car, rule, sampleCatalogOil, today = LocalDate.of(2026, 10, 16)
        )

        assertEquals(DueStatus.OVERDUE, eval.status)
        assertEquals(-1200, eval.remainingKm)
        assertTrue(eval.progressRatio > 1.0f)
    }

    @Test
    fun `날짜 기준 - 12개월 초과 경과 시 거리가 남아있어도 OVERDUE 상태여야 한다`() {
        val car = CarEntity(
            id = "c1", carNumber = "12가 3456", modelName = "335i", currentMileage = 92000 // 거리는 2,000km만 탐
        )
        val rule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            lastServiceMileage = 90000,
            lastServiceDate = "2025-05-01" // 17개월 전 정비
        )

        val eval = MaintenanceScheduleEngine.evaluateItem(
            car, rule, sampleCatalogOil, today = LocalDate.of(2026, 10, 16)
        )

        assertEquals(DueStatus.OVERDUE, eval.status)
        assertTrue(eval.statusDescription.contains("일 초과"))
    }

    @Test
    fun `상태 기반 부품 - 잔여 3mm 이하 측정 시 즉시 교체 OVERDUE 상태여야 한다`() {
        val car = CarEntity(
            id = "c1", carNumber = "12가 3456", modelName = "335i", currentMileage = 80000
        )
        val rule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "BRAKE_PAD_FRONT",
            intervalKm = 20000,
            lastServiceMileage = 75000,
            lastConditionValue = "2.5mm"
        )

        val eval = MaintenanceScheduleEngine.evaluateItem(car, rule, sampleCatalogPad)

        assertEquals(DueStatus.OVERDUE, eval.status)
        assertTrue(eval.statusDescription.contains("마모 한계선"))
    }
}
