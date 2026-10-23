package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tire_swap_logs",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class TireSwapLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val swapDate: String,               // "yyyy-MM-dd"
    val swapMileage: Int,               // 스왑 시점 주행거리 (km)
    val fromMode: String,               // "SUMMER" or "WINTER"
    val toMode: String,                 // "WINTER" or "SUMMER"
    val drivenDistanceDuringMode: Int,  // 이전 모드 장착 기간 동안 주행한 거리
    val storageLocation: String? = null,// 탈거한 타이어 보관소 (예: "타이어뱅크 강남점 보관", "자택 창고")
    val frontTreadDepthMm: Double? = null,// 탈거 타이어 앞 트레드 잔여 홈 (mm)
    val rearTreadDepthMm: Double? = null, // 탈거 타이어 뒤 트레드 잔여 홈 (mm)
    val swapCost: Long = 0L,            // 스왑/장착 공임비 (KRW)
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
