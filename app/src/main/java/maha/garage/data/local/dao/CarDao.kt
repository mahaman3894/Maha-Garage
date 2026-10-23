package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.CarEntity

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE isArchived = 0 ORDER BY updatedAt DESC")
    suspend fun getAllActiveCarsSnapshot(): List<CarEntity>

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    fun getCarById(id: String): Flow<CarEntity?>

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    suspend fun getCarByIdDirect(id: String): CarEntity?

    @Query("SELECT * FROM cars ORDER BY updatedAt DESC")
    suspend fun getAllCarsDirect(): List<CarEntity>

    @Upsert
    suspend fun insertCar(car: CarEntity)

    @Upsert
    suspend fun insertCars(cars: List<CarEntity>)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Query("SELECT * FROM cars ORDER BY isArchived ASC, updatedAt DESC")
    fun getAllCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getAllArchivedCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE isArchived = 1 ORDER BY updatedAt DESC")
    suspend fun getAllArchivedCarsSnapshot(): List<CarEntity>

    @Query("UPDATE cars SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveCar(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE cars SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveCar(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCarById(id: String)

    @Query("""
        UPDATE cars 
        SET currentMileage = :newMileage, 
            engineHours = coalesce(:engineHours, engineHours),
            summerTireMileage = coalesce(:summerTireMileage, summerTireMileage),
            winterTireMileage = coalesce(:winterTireMileage, winterTireMileage),
            lastSwapMileage = coalesce(:lastSwapMileage, lastSwapMileage),
            updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateMileage(
        id: String,
        newMileage: Int,
        engineHours: Double? = null,
        summerTireMileage: Int? = null,
        winterTireMileage: Int? = null,
        lastSwapMileage: Int? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE cars 
        SET currentTireMode = :newMode,
            summerTireMileage = :summerMileage,
            winterTireMileage = :winterMileage,
            lastSwapMileage = :swapMileage,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateTireSwapState(
        id: String,
        newMode: String,
        summerMileage: Int,
        winterMileage: Int,
        swapMileage: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteCar(car: CarEntity)
}
