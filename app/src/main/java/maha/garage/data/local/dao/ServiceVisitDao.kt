package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.local.entity.ServiceVisitEntity

data class ServiceVisitWithLines(
    @Embedded val visit: ServiceVisitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "visitId"
    )
    val lines: List<ServiceLineEntity>
)

@Dao
interface ServiceVisitDao {
    @Query("SELECT EXISTS(SELECT 1 FROM service_visits WHERE id = :id)")
    suspend fun visitExists(id: String): Boolean

    @Transaction
    @Query("SELECT * FROM service_visits WHERE carId = :carId ORDER BY serviceDate DESC, odometerKm DESC")
    fun getVisitsForCar(carId: String): Flow<List<ServiceVisitWithLines>>

    @Transaction
    @Query("SELECT * FROM service_visits WHERE carId = :carId ORDER BY serviceDate DESC, odometerKm DESC")
    suspend fun getVisitsForCarDirect(carId: String): List<ServiceVisitWithLines>

    @Transaction
    @Query("SELECT * FROM service_visits ORDER BY serviceDate DESC")
    fun getAllVisits(): Flow<List<ServiceVisitWithLines>>

    @Transaction
    @Query("SELECT * FROM service_visits ORDER BY serviceDate DESC")
    suspend fun getAllVisitsDirect(): List<ServiceVisitWithLines>

    @Transaction
    @Query("SELECT * FROM service_visits WHERE carId = :carId AND EXISTS (SELECT 1 FROM service_lines WHERE visitId = service_visits.id AND itemCode IN (:itemCodes)) ORDER BY serviceDate, odometerKm, createdAt, id")
    suspend fun getVisitsForItemsDirect(carId: String, itemCodes: List<String>): List<ServiceVisitWithLines>

    @Transaction
    @Query("SELECT * FROM service_visits WHERE carId = :carId AND (:fromDate IS NULL OR serviceDate >= :fromDate) ORDER BY serviceDate DESC, odometerKm DESC")
    fun getVisitsForPeriod(carId: String, fromDate: String?): Flow<List<ServiceVisitWithLines>>

    @Upsert
    suspend fun insertVisit(visit: ServiceVisitEntity)

    @Upsert
    suspend fun insertVisits(visits: List<ServiceVisitEntity>)

    @Upsert
    suspend fun insertLines(lines: List<ServiceLineEntity>)

    @Transaction
    suspend fun recordFullService(visit: ServiceVisitEntity, lines: List<ServiceLineEntity>) {
        insertVisit(visit)
        deleteLinesForVisit(visit.id)
        insertLines(lines)
    }

    @Delete
    suspend fun deleteVisit(visit: ServiceVisitEntity)

    @Query("DELETE FROM service_lines WHERE visitId = :visitId")
    suspend fun deleteLinesForVisit(visitId: String)
}
