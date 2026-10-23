package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 개별 타이어 엔티티 (FL, FR, RL, RR 및 보관 중인 타이어)
 * 각 타이어의 제조사, 제품명, 규격, DOT 제조주차 및 고유 누적 주행거리를 위치교환이나 계절 스왑과 무관하게 독립 보존합니다.
 */
@Entity(
    tableName = "tires",
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
data class TireEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val position: String, // "FL", "FR", "RL", "RR", "STORAGE"
    val season: String = "ALL_SEASON", // "SUMMER", "WINTER", "ALL_SEASON"
    val brand: String = "", // 제조사 (예: "한국타이어", "미쉐린", "금호")
    val productName: String = "", // 제품명 (예: "벤투스 S2 AS", "파일럿 스포츠 4")
    val spec: String = "", // 규격 (예: "245/45R18")
    val dot: String = "", // 제조연월 DOT (예: "2424" -> 2024년 24주차)
    val cumulativeMileage: Int = 0, // 해당 타이어 자체의 확정 누적 주행거리 (km)
    val installedAtMileage: Int = 0, // 현재 위치에 장착된 시점의 차량 주행거리 (km)
    val installDate: String = "", // 장착 일자 ("yyyy-MM-dd")
    val treadDepthMm: Double? = null, // 최근 측정 잔여 트레드 홈 깊이 (mm)
    val storageLocation: String? = null, // 보관 장소 (탈거 상태일 경우)
    val isMounted: Boolean = true, // 현재 차량 장착 여부
    val purchaseCost: Long = 0L, // 타이어 개당 또는 구매 비용 (KRW)
    val notes: String? = null,
    val setId: String? = null, // 세트 식별자 (예: "SUMMER_SET_1", "WINTER_SET_1")
    val assignedWheelPosition: String? = null, // 보관 시 지정/원래 장착 휠 위치 ("FL", "FR", "RL", "RR")
    val updatedAt: Long = System.currentTimeMillis()
)
