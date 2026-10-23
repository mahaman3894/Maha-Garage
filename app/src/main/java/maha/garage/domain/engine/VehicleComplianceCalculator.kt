package maha.garage.domain.engine

import androidx.compose.ui.graphics.Color
import maha.garage.data.local.entity.CarEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class InspectionStatusType(
    val title: String,
    val badgeColor: Color,
    val description: String
) {
    NEEDS_CHECK("공식 확인 필요", Color(0xFF8E8E93), "자동차등록증 또는 한국교통안전공단에서 공식 만료일을 확인해 주세요."),
    WAITING("다음 검사 대기", Color(0xFF34C759), "아직 검사 가능 기간이 아닙니다."),
    AVAILABLE("검사 가능", Color(0xFF30D158), "정기/종합 검사를 받을 수 있는 기간입니다."),
    RECOMMEND_RESERVE("예약 권장", Color(0xFFFF9F0A), "검사 만료 기한이 30일 이내입니다. 검사 예약을 권장합니다."),
    RETEST_DUE("재검사 대상", Color(0xFFFF9F0A), "정기/종합검사 부적합 판정 차량입니다. 재검사 기한 내에 수검하여야 합니다."),
    URGENT("기한 임박", Color(0xFFFF453A), "검사 만료 기한이 3일 이내로 임박했습니다!"),
    OVERDUE("검사기간 경과", Color(0xFFFF3B30), "검사 기간이 경과되었습니다. 지연 과태료가 발생할 수 있습니다.")
}

data class InspectionEvaluation(
    val status: InspectionStatusType,
    val expiryDateStr: String,          // 자동차등록증 상 검사유효기간 만료일 (예: 2026-10-20)
    val startDateStr: String,           // 검사 가능 시작일 (예: 2026-07-22, 만료일 전 90일)
    val endDateStr: String,             // 법정 최종 수검기한 (만료일+31일, 예: 2026-11-20)
    val finalDeadlineStr: String,       // 법정 최종 수검기한
    val daysToExpiry: Long,             // 유효기간 만료일까지 남은 일수
    val daysToDeadline: Long,           // 최종 법정 기한까지 남은 일수 (양수: 남은일수, 음수: 초과일수)
    val daysRemaining: Long,            // UI 호환: 최종 법정 기한까지 남은 일수 (= daysToDeadline)
    val isEstimated: Boolean,
    val inspectionType: String
)

data class InsuranceEvaluation(
    val company: String,
    val startDateStr: String,
    val endDateStr: String,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val isRegistered: Boolean,
    val emergencyCall: String?
)

object VehicleComplianceCalculator {

    /**
     * 자동차 정기/종합 검사 상태 계산 (한국교통안전공단 자동차관리법 기준)
     * - 검사유효기간 만료일: 자동차등록증 기재일 (expiryDate)
     * - 수검 가능 기간: 만료일 전 90일(또는 시작일) ~ 만료일 후 31일 (finalDeadline)
     * - 법정 최종 수검기한(과태료 부과 기준일): expiryDate.plusDays(31)
     */
    fun evaluateInspection(car: CarEntity, today: LocalDate = LocalDate.now()): InspectionEvaluation {
        val inspectionType = car.inspectionType.ifBlank { "정기검사" }

        // 1. 재검사 대상 차량인 경우 (부적합 판정 및 재검사 기한 존재 시 우선 평가)
        if (!car.retestDeadline.isNullOrBlank()) {
            val retestDate = runCatching { LocalDate.parse(car.retestDeadline) }.getOrNull()
            if (retestDate != null) {
                val daysToRetest = ChronoUnit.DAYS.between(today, retestDate)
                val status = when {
                    today.isAfter(retestDate) -> InspectionStatusType.OVERDUE
                    daysToRetest <= 3 -> InspectionStatusType.URGENT
                    else -> InspectionStatusType.RETEST_DUE
                }
                return InspectionEvaluation(
                    status = status,
                    expiryDateStr = retestDate.toString(),
                    startDateStr = car.lastInspectionDate ?: today.toString(),
                    endDateStr = retestDate.toString(),
                    finalDeadlineStr = retestDate.toString(),
                    daysToExpiry = daysToRetest,
                    daysToDeadline = daysToRetest,
                    daysRemaining = daysToRetest,
                    isEstimated = false,
                    inspectionType = "재검사 (부적합)"
                )
            }
        }

        // 2. 미등록 상태일 때
        if (car.inspectionEndDate.isNullOrBlank()) {
            return InspectionEvaluation(
                status = InspectionStatusType.NEEDS_CHECK,
                expiryDateStr = "미등록",
                startDateStr = "-",
                endDateStr = "-",
                finalDeadlineStr = "-",
                daysToExpiry = 0,
                daysToDeadline = 0,
                daysRemaining = 0,
                isEstimated = true,
                inspectionType = inspectionType
            )
        }

        val expiryStr = car.inspectionEndDate
        val expiryDate = runCatching { LocalDate.parse(expiryStr) }.getOrNull()
        if (expiryDate == null) {
            return InspectionEvaluation(
                status = InspectionStatusType.NEEDS_CHECK,
                expiryDateStr = expiryStr,
                startDateStr = "-",
                endDateStr = "-",
                finalDeadlineStr = "-",
                daysToExpiry = 0,
                daysToDeadline = 0,
                daysRemaining = 0,
                isEstimated = true,
                inspectionType = inspectionType
            )
        }

        // 검사 가능 시작일 (입력값이 있으면 사용, 없으면 교통안전공단 규정대로 만료일 전 90일)
        val startDate = car.inspectionStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: expiryDate.minusDays(90)

        // 최종 법정 기한: 만료일 후 31일까지 (자동차관리법 시행규칙 기준 수검 가능 최종일)
        val finalDeadline = expiryDate.plusDays(31)

        val daysToExpiry = ChronoUnit.DAYS.between(today, expiryDate)
        val daysToDeadline = ChronoUnit.DAYS.between(today, finalDeadline)

        val status = when {
            car.isInspectionDateEstimated -> InspectionStatusType.NEEDS_CHECK
            today.isBefore(startDate) -> InspectionStatusType.WAITING
            today.isAfter(finalDeadline) -> InspectionStatusType.OVERDUE
            daysToDeadline <= 3 -> InspectionStatusType.URGENT
            daysToDeadline <= 30 -> InspectionStatusType.RECOMMEND_RESERVE
            else -> InspectionStatusType.AVAILABLE
        }

        return InspectionEvaluation(
            status = status,
            expiryDateStr = expiryDate.toString(),
            startDateStr = startDate.toString(),
            endDateStr = finalDeadline.toString(),
            finalDeadlineStr = finalDeadline.toString(),
            daysToExpiry = daysToExpiry,
            daysToDeadline = daysToDeadline,
            daysRemaining = daysToDeadline,
            isEstimated = car.isInspectionDateEstimated,
            inspectionType = inspectionType
        )
    }

    /**
     * 자동차 보험 가입 상태 계산 (가입일로부터 1년 만료)
     */
    fun evaluateInsurance(car: CarEntity, today: LocalDate = LocalDate.now()): InsuranceEvaluation {
        val hasInsurance = !car.insuranceCompany.isNullOrBlank() || 
                           !car.insuranceStartDate.isNullOrBlank() || 
                           !car.insuranceEndDate.isNullOrBlank()

        if (!hasInsurance) {
            return InsuranceEvaluation(
                company = "미등록",
                startDateStr = "-",
                endDateStr = "-",
                daysRemaining = 0,
                isExpired = false,
                isRegistered = false,
                emergencyCall = null
            )
        }

        val company = car.insuranceCompany?.ifBlank { "보험사 미지정" } ?: "보험사 미지정"
        val startDate = RenewalDatePolicy.parse(car.insuranceStartDate)
        val endDate = RenewalDatePolicy.parse(car.insuranceEndDate)
        val daysRemaining = endDate?.let { ChronoUnit.DAYS.between(today, it) } ?: 0L
        val emergencyCall = car.insuranceEmergencyCall

        return InsuranceEvaluation(
            company = company,
            startDateStr = startDate?.toString() ?: "-",
            endDateStr = endDate?.toString() ?: "-",
            daysRemaining = daysRemaining,
            isExpired = endDate != null && daysRemaining < 0,
            isRegistered = true,
            emergencyCall = emergencyCall
        )
    }
}
