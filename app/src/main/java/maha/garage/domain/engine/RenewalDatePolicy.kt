package maha.garage.domain.engine

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.VehicleClass
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RenewalDateState { MISSING, NORMAL, WARNING, EXPIRED }
data class RenewalDateEvaluation(val date: LocalDate?, val state: RenewalDateState, val daysRemaining: Long?) {
    val badge: String get() = when {
        date == null -> "날짜 미등록"
        daysRemaining!! < 0 -> "${-daysRemaining}일 지남"
        daysRemaining == 0L -> "오늘 만료"
        else -> "D-$daysRemaining"
    }
}

data class InspectionPeriod(
    val baseExpiryDate: LocalDate,     // 검사 만료 기준일 (예: 최초등록일 + 4년)
    val validStartDate: LocalDate,     // 검사 가능 시작일 (기준 만료일 - 31일)
    val validEndDate: LocalDate,       // 검사 가능 만료일 (기준 만료일 + 31일)
    val cycleDescription: String       // 주기 안내 문구
)

object RenewalDatePolicy {
    fun parse(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    fun evaluate(value: String?, today: LocalDate = LocalDate.now()): RenewalDateEvaluation {
        val date = parse(value) ?: return RenewalDateEvaluation(null, RenewalDateState.MISSING, null)
        val state = when {
            today > date -> RenewalDateState.EXPIRED
            today >= date.minusMonths(1) -> RenewalDateState.WARNING
            else -> RenewalDateState.NORMAL
        }
        return RenewalDateEvaluation(date, state, ChronoUnit.DAYS.between(today, date))
    }
    fun insurance(car: CarEntity, today: LocalDate = LocalDate.now()) = evaluate(car.insuranceEndDate, today)
    fun inspection(car: CarEntity, today: LocalDate = LocalDate.now()) = evaluate(
        car.retestDeadline?.takeIf { parse(it) != null } ?: car.inspectionEndDate, today)
    fun display(value: String?): String = parse(value)?.toString()?.replace('-', '.') ?: "미등록"

    /**
     * 신차 최초 등록일 및 차종/용도 기준 첫 정기검사 기간 자동 산출
     * - 비사업용(개인 자가용) 승용차: 최초 검사 4년 (이후 매 2년)
     * - 비사업용 승합·화물차 (카니발 11인승 이상, 포터, 픽업트럭 등): 최초 검사 1년 (매년 1회)
     * - 사업용 승용차 (렌터카, 택시 등): 최초 검사 2년 (이후 매 1년)
     * - 검사 가능 기간: 검사 유효기간 만료일 전후 31일 (총 62일간)
     */
    fun calculateInitialInspection(
        registrationDateStr: String?,
        isCommercial: Boolean = false,
        vehicleClass: VehicleClass = VehicleClass.SEDAN,
        modelName: String = ""
    ): InspectionPeriod? {
        val regDate = parse(registrationDateStr?.trim()) ?: return null

        val isVanOrTruck = vehicleClass == VehicleClass.LIGHT_TRUCK ||
                vehicleClass == VehicleClass.MEDIUM_TRUCK ||
                vehicleClass == VehicleClass.HEAVY_TRUCK ||
                vehicleClass == VehicleClass.PICKUP ||
                vehicleClass == VehicleClass.VAN ||
                modelName.contains("포터") ||
                modelName.contains("봉고") ||
                modelName.contains("카니발 11") ||
                modelName.contains("11인승") ||
                modelName.contains("픽업")

        val (yearsToAdd, desc) = when {
            isCommercial -> 2L to "사업용 승용차: 최초 2년 (이후 매 1년마다)"
            isVanOrTruck -> 1L to "비사업용 승합·화물차: 최초 1년 (이후 매년 1회)"
            else -> 4L to "비사업용 자가용 승용차: 최초 4년 (이후 매 2년마다)"
        }

        val baseExpiry = regDate.plusYears(yearsToAdd)
        val validStart = baseExpiry.minusDays(31)
        val validEnd = baseExpiry.plusDays(31)

        return InspectionPeriod(
            baseExpiryDate = baseExpiry,
            validStartDate = validStart,
            validEndDate = validEnd,
            cycleDescription = desc
        )
    }
}
