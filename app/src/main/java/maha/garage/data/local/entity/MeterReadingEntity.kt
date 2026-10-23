package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "meter_readings",
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
data class MeterReadingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val readingDate: String,            // "yyyy-MM-dd"
    val odometerKm: Int,
    val engineHours: Double? = null,
    val inputSource: String = "MANUAL", // "MANUAL", "SERVICE_LOG", "TIRE_SWAP"
    val createdAt: Long = System.currentTimeMillis()
)
