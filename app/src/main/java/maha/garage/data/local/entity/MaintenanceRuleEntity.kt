package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.SourceType
import java.util.UUID

@Entity(
    tableName = "maintenance_rules",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index(value = ["carId", "itemCode"], unique = true)]
)
data class MaintenanceRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val itemCode: String,
    val actionType: ActionType = ActionType.REPLACE,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val intervalHours: Double? = null,
    
    // 최초 vs 반복 주기 분리 (예: 최초 10만km 후 4만km마다)
    val firstIntervalKm: Int? = null,
    val firstIntervalMonths: Int? = null,
    
    val sourceType: SourceType = SourceType.GENERIC_ESTIMATE,
    val isVerified: Boolean = false,
    val isEnabled: Boolean = true,
    
    // 마지막 작업 이력 (캐시 및 산출 기준)
    val lastServiceDate: String? = null,        // yyyy-MM-dd
    val lastServiceMileage: Int? = null,        // 정비 시점 km
    val lastServiceHours: Double? = null,
    val lastConditionValue: String? = null,     // 예: 패드 잔여 "6mm"
    
    // 관리 시작 기준 (실제 교환 이력이 없을 때 사용자가 설정한 기산점)
    val baselineDate: String? = null,           // yyyy-MM-dd
    val baselineMileage: Int? = null,           // 관리 시작 시점 km
    val baselineHours: Double? = null,          // 관리 시작 시점 가동시간
    
    val userCustomNotes: String? = null
)
