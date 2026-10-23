package maha.garage.data.local.dao

import androidx.room.*
import maha.garage.data.local.entity.FuelRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FuelRecordEntity): Long

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY odometer ASC, refuelDate ASC")
    suspend fun getRecordsForVehicleAsc(vehicleId: String): List<FuelRecordEntity>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY odometer DESC, refuelDate DESC")
    fun getRecordsForVehicleDescFlow(vehicleId: String): Flow<List<FuelRecordEntity>>

    @Query("SELECT AVG(calculatedEconomy) FROM fuel_records WHERE vehicleId = :vehicleId AND calculatedEconomy IS NOT NULL")
    fun getAverageEconomyFlow(vehicleId: String): Flow<Double?>

    @Query("SELECT MAX(odometer) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun getLatestOdometer(vehicleId: String): Int?

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY odometer DESC, refuelDate DESC LIMIT 1")
    suspend fun getLatestRecord(vehicleId: String): FuelRecordEntity?

    @Delete
    suspend fun deleteRecord(record: FuelRecordEntity)

    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
