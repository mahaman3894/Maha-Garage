package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 타이어 장착/위치교환/스왑 이력 엔티티
 * 신규 장착, 위치교환(로테이션), 계절별 탈부착 이벤트를 명확히 구분하여 기록합니다.
 */
@Entity(
    tableName = "tire_installation_history",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("tireId")]
)
data class TireInstallationHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val tireId: String,
    val eventType: String, // "NEW_INSTALL" (신규 교체), "ROTATION" (위치교환), "SEASON_SWAP" (계절스왑), "DISMOUNT" (탈거)
    val fromPosition: String?, // "FL", "FR", "RL", "RR", "STORAGE", null
    val toPosition: String?, // "FL", "FR", "RL", "RR", "STORAGE", null
    val eventDate: String, // "yyyy-MM-dd"
    val odometerKm: Int, // 이벤트 발생 시점의 차량 주행거리 (km)
    val drivenDistanceSinceLastEvent: Int = 0, // 이전 위치 장착 기간 동안 주행한 거리 (km)
    val treadDepthMm: Double? = null, // 당시 잔여 트레드 깊이
    val cost: Long = 0L, // 공임비 또는 구매비용
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
