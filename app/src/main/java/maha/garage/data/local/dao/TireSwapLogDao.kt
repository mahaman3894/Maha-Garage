package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.TireSwapLogEntity

@Dao
interface TireSwapLogDao {
    @Query("SELECT * FROM tire_swap_logs WHERE carId = :carId ORDER BY swapDate DESC, swapMileage DESC")
    fun getSwapLogsForCar(carId: String): Flow<List<TireSwapLogEntity>>

    @Query("SELECT * FROM tire_swap_logs WHERE carId = :carId ORDER BY swapDate DESC, swapMileage DESC")
    suspend fun getSwapLogsForCarDirect(carId: String): List<TireSwapLogEntity>

    @Query("SELECT * FROM tire_swap_logs ORDER BY swapDate DESC, swapMileage DESC")
    suspend fun getAllSwapLogsDirect(): List<TireSwapLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwapLog(log: TireSwapLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwapLogs(logs: List<TireSwapLogEntity>)

    @Delete
    suspend fun deleteSwapLog(log: TireSwapLogEntity)
}
