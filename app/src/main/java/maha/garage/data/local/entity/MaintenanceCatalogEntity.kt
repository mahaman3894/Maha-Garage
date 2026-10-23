package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.RulePriority
import maha.garage.data.model.enums.VehicleClass

@Entity(tableName = "maintenance_catalog")
data class MaintenanceCatalogEntity(
    @PrimaryKey
    val itemCode: String,                       // 예: "ENGINE_OIL", "CABIN_FILTER"
    val displayName: String,                    // 예: "엔진오일 & 필터", "에어컨 필터"
    val category: String,                       // "엔진/구동", "제동계통", "필터/공조", "섀시/조향"
    val priority: RulePriority = RulePriority.P1,
    val defaultActionType: ActionType = ActionType.REPLACE,
    val defaultIntervalKm: Int? = null,
    val defaultIntervalMonths: Int? = null,
    val severeIntervalKm: Int? = null,
    val severeIntervalMonths: Int? = null,
    val firstIntervalKm: Int? = null,          // 최초 교체 주기 km (예: EV 냉각수 100,000km)
    val firstIntervalMonths: Int? = null,      // 최초 교체 주기 개월 (예: EV 냉각수 60개월)
    val applicablePowertrains: List<PowertrainType> = emptyList(), // 빈 리스트면 전체 적용
    val applicableVehicleClasses: List<VehicleClass> = emptyList(), // 빈 리스트면 전체 적용
    val isConditionBased: Boolean = false,      // 수치 주기보다는 마모/상태 기반(예: 브레이크 패드)
    val guideNotes: String? = null
)
