package maha.garage.domain.repository

import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.FuelRecordEntity

interface FuelRepository {
    fun getRecordsForVehicle(vehicleId: String): Flow<List<FuelRecordEntity>>
    suspend fun getRecordsForVehicleAsc(vehicleId: String): List<FuelRecordEntity>
    fun getAverageEconomy(vehicleId: String): Flow<Double?>
    suspend fun getLatestOdometer(vehicleId: String): Int?
    suspend fun getLatestRecord(vehicleId: String): FuelRecordEntity?
    suspend fun addFuelRecord(
        vehicleId: String,
        odometer: Int,
        fuelAmount: Double,
        unitPrice: Int,
        totalPrice: Int,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        gasStationName: String?,
        refuelDate: Long = System.currentTimeMillis(),
        receiptImageUri: String? = null
    ): FuelRecordEntity
    suspend fun deleteRecord(record: FuelRecordEntity)
    suspend fun deleteById(id: Long)
}
