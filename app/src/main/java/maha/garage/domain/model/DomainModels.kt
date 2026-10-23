package maha.garage.domain.model

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.DueStatus
import maha.garage.data.model.enums.RulePriority
import maha.garage.data.model.enums.SourceType

data class ItemEvaluation(
    val itemCode: String,
    val displayName: String,
    val category: String,
    val priority: RulePriority,
    val actionType: ActionType,
    val status: DueStatus,
    val statusDescription: String,

    // 잔여 여유량 (음수이면 초과된 수치)
    val remainingKm: Int?,
    val remainingDays: Long?,
    val remainingHours: Double?,

    // 소모 진행 비율 (0.0: 신품 ~ 1.0: 만료 도래, 1.0 초과: 만기 초과)
    val progressRatio: Float,

    // 다음 예정 시점
    val nextDueKm: Int?,
    val nextDueDateString: String?,
    val nextDueHours: Double?,

    // 마지막 정비/교체 기록
    val lastServiceDate: String?,
    val lastServiceMileage: Int?,
    val lastServiceHours: Double?,
    val lastConditionValue: String?,

    // 적용된 주기 규칙
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val intervalHours: Double?,
    val sourceType: SourceType,
    val isConditionBased: Boolean,
    val guideNotes: String?,

    // 관리 시작 기준 여부 및 기준값
    val isBaseline: Boolean = false,
    val baselineDate: String? = null,
    val baselineMileage: Int? = null,
    val baselineHours: Double? = null
)

data class VehicleWithStatus(
    val car: CarEntity,
    val evaluations: List<ItemEvaluation>,
    val overdueCount: Int,
    val dueSoonCount: Int,
    val inspectionCount: Int,
    val normalCount: Int,
    val worstStatus: DueStatus
)
