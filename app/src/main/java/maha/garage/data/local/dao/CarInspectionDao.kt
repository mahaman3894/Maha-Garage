package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.CarInspectionHistoryEntity

@Dao
interface CarInspectionDao {
    @Query("SELECT * FROM car_inspection_history WHERE carId = :carId ORDER BY inspectionDate DESC")
    fun getInspectionsForCar(carId: String): Flow<List<CarInspectionHistoryEntity>>

    @Query("SELECT * FROM car_inspection_history WHERE carId = :carId ORDER BY inspectionDate DESC")
    suspend fun getInspectionsForCarDirect(carId: String): List<CarInspectionHistoryEntity>

    @Query("SELECT * FROM car_inspection_history ORDER BY inspectionDate DESC")
    suspend fun getAllInspectionsDirect(): List<CarInspectionHistoryEntity>

    @Upsert
    suspend fun insertInspection(inspection: CarInspectionHistoryEntity)

    @Upsert
    suspend fun insertInspections(inspections: List<CarInspectionHistoryEntity>)

    @Delete
    suspend fun deleteInspection(inspection: CarInspectionHistoryEntity)
}
