package maha.garage.domain.repository

import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.model.VehicleWithStatus

interface VehicleRepository {
    fun getAllActiveVehicles(): Flow<List<CarEntity>>
    fun getVehicleById(id: String): Flow<CarEntity?>
    fun getVehicleWithStatus(carId: String): Flow<VehicleWithStatus?>
    fun getAllVehiclesWithStatus(): Flow<List<VehicleWithStatus>>
    suspend fun saveVehicle(car: CarEntity, isNewCar: Boolean = false)
    suspend fun updateMileage(
        carId: String,
        newMileage: Int,
        engineHours: Double? = null,
        summerTireMileage: Int? = null,
        winterTireMileage: Int? = null,
        lastSwapMileage: Int? = null
    )
    suspend fun swapTires(
        carId: String,
        targetMode: String,
        swapMileage: Int? = null,
        storageLocation: String? = null,
        frontTread: Double? = null,
        rearTread: Double? = null,
        swapCost: Long = 0L,
        notes: String? = null
    )
    suspend fun archiveVehicle(carId: String)
    suspend fun unarchiveVehicle(carId: String)
    suspend fun deleteVehiclePermanently(carId: String)

    /**
     * 차량번호 기준 자동차 기본 제원 자동 조회 (공공데이터포털 API 연동)
     */
    suspend fun fetchVehicleSpecByPlate(plateNumber: String): maha.garage.data.remote.dto.VehicleSpecDto?
}
