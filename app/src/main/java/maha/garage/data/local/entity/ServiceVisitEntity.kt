package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "service_visits",
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
data class ServiceVisitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val serviceDate: String,            // "yyyy-MM-dd"
    val odometerKm: Int,                // 정비 시점 누적 주행거리
    val engineHours: Double? = null,    // 정비 시점 엔진 가동시간
    val workshop: String? = null,       // 예: "BMW 서비스센터", "공임나라 역삼점"
    val totalCost: Long = 0L,           // 총 지출 금액 (KRW)
    val notes: String? = null,
    val receiptPhotoUri: String? = null,// 영수증/정비내역서 사진 URI
    val createdAt: Long = System.currentTimeMillis()
)
