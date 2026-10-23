package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "car_inspection_history",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("carId")
    ]
)
data class CarInspectionHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val inspectionDate: String,            // 실제 검사일 (yyyy-MM-dd)
    val inspectionType: String = "정기검사",   // "정기검사", "종합검사"
    val stationName: String? = null,       // 검사소 명칭
    val isPassed: Boolean = true,          // 적합 / 부적합
    val nextExpiryDate: String? = null,    // 차기 유효기간 만료일
    val retestDeadline: String? = null,    // 부적합 시 재검사 기한
    val cost: Int = 0,                     // 검사비
    val memo: String? = null,              // 특이사항 / 비고
    val certificatePhotoUri: String? = null, // 결과표 사진
    val createdAt: Long = System.currentTimeMillis()
)
