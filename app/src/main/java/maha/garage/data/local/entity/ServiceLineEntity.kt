package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import maha.garage.data.model.enums.ActionType
import java.util.UUID

@Entity(
    tableName = "service_lines",
    foreignKeys = [
        ForeignKey(
            entity = ServiceVisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visitId")]
)
data class ServiceLineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val visitId: String,
    val itemCode: String,               // 예: "ENGINE_OIL", "BRAKE_PAD_FRONT"
    val actionType: ActionType = ActionType.REPLACE,
    val partsCost: Long = 0L,           // 부품비 (KRW)
    val laborCost: Long = 0L,           // 공임비 (KRW)
    val conditionValue: String? = null, // 상태/잔여량 (예: "브레이크 패드 잔여 7mm", "냉각수 비중 양호")
    val partSpecification: String? = null, // 사용 부품 규격 (예: "모빌1 ESP 0W-30 6.5L")
    val notes: String? = null
)
