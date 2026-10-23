package maha.garage.domain.repository

import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.*

interface MaintenanceRepository {
    fun getRulesForCar(carId: String): Flow<List<MaintenanceRuleEntity>>
    fun getCatalog(): Flow<List<MaintenanceCatalogEntity>>
    fun getVisitsForCar(carId: String): Flow<List<ServiceVisitWithLines>>
    fun getVisitsForPeriod(carId: String, fromDate: String?): Flow<List<ServiceVisitWithLines>>
    fun getAllVisits(): Flow<List<ServiceVisitWithLines>>
    fun getSwapLogsForCar(carId: String): Flow<List<TireSwapLogEntity>>
    fun getTiresForCar(carId: String): Flow<List<TireEntity>>
    fun getMountedTiresForCar(carId: String): Flow<List<TireEntity>>
    fun getTireHistoryForCar(carId: String): Flow<List<TireInstallationHistoryEntity>>
    suspend fun recordServiceVisit(visit: ServiceVisitEntity, lines: List<ServiceLineEntity>)
    suspend fun updateCustomRule(rule: MaintenanceRuleEntity)
    suspend fun updateCustomRules(rules: List<MaintenanceRuleEntity>)
    suspend fun saveTires(tires: List<TireEntity>)
    suspend fun replaceTires(
        carId: String,
        newTires: List<TireEntity>,
        visit: ServiceVisitEntity,
        lines: List<ServiceLineEntity>,
        histories: List<TireInstallationHistoryEntity>,
        wheelAlignmentMileage: Int? = null
    )
    suspend fun rotateTires(
        carId: String,
        currentMileage: Int,
        date: String,
        swapMapping: Map<String, String>,
        cost: Long,
        notes: String?
    )
    suspend fun swapSeasonTires(
        car: CarEntity,
        targetMode: String,
        swapMileage: Int,
        swapDate: String,
        storageLocation: String?,
        frontTread: Double?,
        rearTread: Double?,
        cost: Long,
        notes: String?
    )
    fun getInspectionHistoryForCar(carId: String): Flow<List<CarInspectionHistoryEntity>>
    suspend fun recordInspectionResult(car: CarEntity, history: CarInspectionHistoryEntity)
}
