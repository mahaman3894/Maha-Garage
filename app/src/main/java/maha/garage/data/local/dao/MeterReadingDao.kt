package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.MeterReadingEntity

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings WHERE carId = :carId ORDER BY readingDate DESC, odometerKm DESC")
    fun getReadingsForCar(carId: String): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE carId = :carId ORDER BY readingDate DESC, odometerKm DESC LIMIT 2")
    suspend fun getRecentTwoReadings(carId: String): List<MeterReadingEntity>

    @Query("SELECT * FROM meter_readings ORDER BY readingDate DESC, odometerKm DESC")
    suspend fun getAllReadingsDirect(): List<MeterReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<MeterReadingEntity>)
}
