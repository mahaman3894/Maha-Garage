package maha.garage.domain.engine

import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.SourceType
import java.time.LocalDate

object MaintenanceInputPolicy {
    fun initialPadPosition(code: String) = when (code) {
        "BRAKE_PAD_REAR" -> "REAR"
        else -> "FRONT"
    }
    fun padCodes(position: String): List<String> = when (position) {
        "FRONT" -> listOf("BRAKE_PAD_FRONT")
        "REAR" -> listOf("BRAKE_PAD_REAR")
        "BOTH" -> listOf("BRAKE_PAD_FRONT", "BRAKE_PAD_REAR")
        else -> error("패드 위치를 선택해 주세요.")
    }
    fun optionalPositive(value: String, max: Int): Int? {
        if (value.isBlank()) return null
        return requireNotNull(value.toIntOrNull()?.takeIf { it in 1..max }) { "1~${max} 범위의 숫자를 입력해 주세요." }
    }
    fun cost(value: String): Long {
        if (value.isBlank()) return 0L
        return requireNotNull(value.toLongOrNull()?.takeIf { it in 0..100_000_000L }) { "비용은 0~100,000,000원으로 입력해 주세요." }
    }
    fun validateService(date: String, mileage: String, costs: List<String>, today: LocalDate = LocalDate.now()) {
        val parsed = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
        require(parsed != null && parsed <= today) { "정비 날짜는 오늘 이하의 올바른 날짜로 입력해 주세요." }
        require(mileage.toIntOrNull()?.let { it >= 0 } == true) { "올바른 주행거리를 입력해 주세요." }
        costs.forEach { cost(it) }
    }
    // Complete settings snapshot: null explicitly disables a criterion. Historical caches are never replaced.
    fun applySettings(existing: MaintenanceRuleEntity, input: MaintenanceRuleEntity, car: CarEntity, today: LocalDate = LocalDate.now()): MaintenanceRuleEntity {
        require(existing.carId == input.carId && input.carId == car.id && existing.itemCode == input.itemCode)
        require(input.intervalKm == null || input.intervalKm in 1..1_000_000) { "거리 주기 범위를 확인해 주세요." }
        require(input.intervalMonths == null || input.intervalMonths in 1..240) { "기간 주기 범위를 확인해 주세요." }
        require(input.intervalHours == null || (input.intervalHours.isFinite() && input.intervalHours in 0.1..50_000.0)) { "시간 주기 범위를 확인해 주세요." }
        require(input.intervalKm != null || input.intervalMonths != null || input.intervalHours != null) { "거리·기간·시간 중 하나의 주기를 입력해 주세요." }
        input.baselineDate?.let {
            val d = runCatching { LocalDate.parse(it) }.getOrNull()
            require(d != null && d <= today) { "관리 시작 날짜를 확인해 주세요." }
        }
        require(input.baselineMileage == null || input.baselineMileage in 0..car.currentMileage) { "관리 시작 거리는 0~현재 주행거리로 입력해 주세요." }
        require(input.baselineHours == null || (input.baselineHours.isFinite() && input.baselineHours >= 0 && (car.engineHours == null || input.baselineHours <= car.engineHours))) { "관리 시작 시간을 확인해 주세요." }
        return existing.copy(intervalKm = input.intervalKm, intervalMonths = input.intervalMonths, intervalHours = input.intervalHours,
            baselineDate = input.baselineDate, baselineMileage = input.baselineMileage, baselineHours = input.baselineHours,
            sourceType = SourceType.USER_OVERRIDE)
    }
}
