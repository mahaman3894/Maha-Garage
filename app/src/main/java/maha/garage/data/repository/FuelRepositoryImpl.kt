package maha.garage.data.repository

import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.dao.CarDao
import maha.garage.data.local.dao.FuelRecordDao
import maha.garage.data.local.entity.FuelRecordEntity
import maha.garage.domain.engine.FuelCalculationInput
import maha.garage.domain.engine.FuelCalculator
import maha.garage.domain.repository.FuelRepository

class FuelRepositoryImpl(
    private val fuelDao: FuelRecordDao,
    private val carDao: CarDao
) : FuelRepository {

    override fun getRecordsForVehicle(vehicleId: String): Flow<List<FuelRecordEntity>> {
        return fuelDao.getRecordsForVehicleDescFlow(vehicleId)
    }

    override suspend fun getRecordsForVehicleAsc(vehicleId: String): List<FuelRecordEntity> {
        return fuelDao.getRecordsForVehicleAsc(vehicleId)
    }

    override fun getAverageEconomy(vehicleId: String): Flow<Double?> {
        return fuelDao.getAverageEconomyFlow(vehicleId)
    }

    override suspend fun getLatestOdometer(vehicleId: String): Int? {
        return fuelDao.getLatestOdometer(vehicleId)
    }

    override suspend fun getLatestRecord(vehicleId: String): FuelRecordEntity? {
        return fuelDao.getLatestRecord(vehicleId)
    }

    override suspend fun addFuelRecord(
        vehicleId: String,
        odometer: Int,
        fuelAmount: Double,
        unitPrice: Int,
        totalPrice: Int,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        gasStationName: String?,
        refuelDate: Long,
        receiptImageUri: String?
    ): FuelRecordEntity {
        // 1. 과거 히스토리 조회 (과거 -> 최신 순)
        val historyEntities = fuelDao.getRecordsForVehicleAsc(vehicleId)
        val historyInputs = historyEntities.map {
            FuelCalculationInput(
                odometer = it.odometer,
                fuelAmount = it.fuelAmount,
                isFull = it.isFull,
                isMissedPrevious = it.isMissedPrevious
            )
        }

        // 2. 현재 입력 데이터
        val currentInput = FuelCalculationInput(
            odometer = odometer,
            fuelAmount = fuelAmount,
            isFull = isFull,
            isMissedPrevious = isMissedPrevious
        )

        // 3. Full-to-Full 연비 엔진 구동
        val calcResult = FuelCalculator.calculate(currentInput, historyInputs)

        // 4. 엔티티 생성 및 Room DB 저장
        val entity = FuelRecordEntity(
            vehicleId = vehicleId,
            odometer = odometer,
            fuelAmount = fuelAmount,
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            isFull = isFull,
            isMissedPrevious = isMissedPrevious,
            calculatedEconomy = calcResult.segmentEconomy,
            gasStationName = gasStationName?.ifBlank { null },
            refuelDate = refuelDate,
            receiptImageUri = receiptImageUri
        )

        val insertedId = fuelDao.insertRecord(entity)

        // 5. 차량 주행거리 동기화 (주유 시점 주행거리가 더 높으면 차량 주행거리 갱신)
        val car = carDao.getCarByIdDirect(vehicleId)
        if (car != null && odometer > car.currentMileage) {
            carDao.updateCar(car.copy(currentMileage = odometer, updatedAt = System.currentTimeMillis()))
        }

        return entity.copy(id = insertedId)
    }

    override suspend fun deleteRecord(record: FuelRecordEntity) {
        fuelDao.deleteRecord(record)
    }

    override suspend fun deleteById(id: Long) {
        fuelDao.deleteById(id)
    }
}
