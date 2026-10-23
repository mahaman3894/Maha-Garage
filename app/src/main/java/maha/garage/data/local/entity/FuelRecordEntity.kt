package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["refuelDate"]),
        Index(value = ["odometer"])
    ]
)
data class FuelRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: String,             // 소속 차량 고유 ID (CarEntity.id와 매핑)
    val odometer: Int,                 // 주유 시점 누적 주행거리 (km)
    val fuelAmount: Double,            // 주유량 (L)
    val unitPrice: Int,                // 리터당 유가 (원)
    val totalPrice: Int,               // 총 주유 금액 (원)
    val isFull: Boolean = true,        // 가득 주유 여부 (기본 true)
    val isMissedPrevious: Boolean = false, // 직전 주유 누락 여부
    val calculatedEconomy: Double? = null, // 자동 계산된 구간 실연비 (km/L)
    val gasStationName: String? = null,    // 주유소명 (예: SK에너지 안티그라비티점)
    val refuelDate: Long = System.currentTimeMillis(), // 주유 일시 (타임스탬프)
    val receiptImageUri: String? = null    // 영수증 사본 URI
)
