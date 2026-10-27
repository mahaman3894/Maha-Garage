package maha.garage.domain.engine

import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.ActionType

object ServiceHistoryReducer {
    fun reduce(rule: MaintenanceRuleEntity, records: List<Pair<ServiceVisitEntity, ServiceLineEntity>>): MaintenanceRuleEntity {
        var result = rule
        for ((visit, line) in records.sortedWith(compareBy({ it.first.serviceDate }, { it.first.odometerKm }, { it.first.createdAt }, { it.first.id }))) {
            require(visit.carId == rule.carId && line.visitId == visit.id && line.itemCode == rule.itemCode)
            if (line.actionType == ActionType.REPLACE) {
                if (result.lastServiceDate == null || visit.serviceDate > result.lastServiceDate!! ||
                    (visit.serviceDate == result.lastServiceDate && visit.odometerKm >= (result.lastServiceMileage ?: 0))) {
                    result = result.copy(
                        lastServiceDate = visit.serviceDate,
                        lastServiceMileage = visit.odometerKm,
                        lastServiceHours = visit.engineHours,
                        lastConditionValue = line.conditionValue,
                        userCustomNotes = null
                    )
                }
            } else {
                if (line.conditionValue != null && (result.lastServiceDate == null || visit.serviceDate >= result.lastServiceDate!!)) {
                    result = result.copy(lastConditionValue = line.conditionValue)
                }
                // 점검, 정비, 보충, 세척 등 비교체 액션 기록 시
                if (result.lastServiceDate == null || visit.serviceDate >= result.lastServiceDate!!) {
                    val actionLabel = when (line.actionType) {
                        ActionType.INSPECT -> "점검"
                        ActionType.REFILL -> "교환/보충"
                        ActionType.CLEAN -> "정비"
                        else -> "점검/정비"
                    }
                    result = result.copy(
                        userCustomNotes = "INSPECTED:${line.actionType.name}:$actionLabel:${visit.serviceDate}:${visit.odometerKm}"
                    )
                }
            }
        }
        return result
    }
}
