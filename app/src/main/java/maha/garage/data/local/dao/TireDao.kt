package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.TireEntity
import maha.garage.data.local.entity.TireInstallationHistoryEntity

@Dao
interface TireDao {

    // === 타이어 본체 ===
    @Query("SELECT * FROM tires WHERE carId = :carId")
    fun getTiresForCar(carId: String): Flow<List<TireEntity>>

    @Query("SELECT * FROM tires WHERE carId = :carId")
    suspend fun getTiresForCarDirect(carId: String): List<TireEntity>

    @Query("SELECT * FROM tires WHERE carId = :carId AND isMounted = 1")
    fun getMountedTiresForCar(carId: String): Flow<List<TireEntity>>

    @Query("SELECT * FROM tires WHERE carId = :carId AND isMounted = 1")
    suspend fun getMountedTiresForCarDirect(carId: String): List<TireEntity>

    @Query("SELECT * FROM tires")
    suspend fun getAllTiresDirect(): List<TireEntity>

    @Query("SELECT * FROM tires WHERE id = :id")
    suspend fun getTireById(id: String): TireEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTire(tire: TireEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTires(tires: List<TireEntity>)

    @Update
    suspend fun updateTire(tire: TireEntity)

    @Update
    suspend fun updateTires(tires: List<TireEntity>)

    @Delete
    suspend fun deleteTire(tire: TireEntity)

    @Query("DELETE FROM tires WHERE carId = :carId")
    suspend fun deleteAllTiresForCar(carId: String)

    // === 타이어 장착/로테이션/스왑 이력 ===
    @Query("SELECT * FROM tire_installation_history WHERE carId = :carId ORDER BY odometerKm DESC, eventDate DESC, createdAt DESC")
    fun getHistoryForCar(carId: String): Flow<List<TireInstallationHistoryEntity>>

    @Query("SELECT * FROM tire_installation_history WHERE carId = :carId ORDER BY odometerKm DESC, eventDate DESC, createdAt DESC")
    suspend fun getHistoryForCarDirect(carId: String): List<TireInstallationHistoryEntity>

    @Query("SELECT * FROM tire_installation_history")
    suspend fun getAllHistoryDirect(): List<TireInstallationHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TireInstallationHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<TireInstallationHistoryEntity>)

    @Delete
    suspend fun deleteHistory(history: TireInstallationHistoryEntity)
}
