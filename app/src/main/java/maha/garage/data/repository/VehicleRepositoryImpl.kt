package maha.garage.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MeterReadingEntity
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.domain.engine.MaintenanceScheduleEngine
import maha.garage.domain.engine.TireSwapCalculator
import maha.garage.domain.model.VehicleWithStatus
import maha.garage.domain.repository.VehicleRepository
import java.time.LocalDate

class VehicleRepositoryImpl(
    private val db: MahaGarageDatabase
) : VehicleRepository {

    override fun getAllActiveVehicles(): Flow<List<CarEntity>> {
        return db.carDao().getAllActiveCars()
    }

    override fun getVehicleById(id: String): Flow<CarEntity?> {
        return db.carDao().getCarById(id)
    }

    override fun getVehicleWithStatus(carId: String): Flow<VehicleWithStatus?> {
        return combine(
            db.carDao().getCarById(carId),
            db.catalogDao().getAllCatalog(),
            db.maintenanceRuleDao().getRulesForCar(carId)
        ) { car, catalog, rules ->
            if (car == null) return@combine null

            val catalogMap = catalog.associateBy { it.itemCode }
            val evaluations = rules.mapNotNull { rule ->
                val catalogItem = catalogMap[rule.itemCode] ?: return@mapNotNull null
                if (ApplicabilityResolver.isItemApplicable(car, catalogItem)) MaintenanceScheduleEngine.evaluateItem(car, rule, catalogItem, LocalDate.now()) else null
            }

            val overdueCount = evaluations.count { it.status == DueStatus.OVERDUE }
            val dueSoonCount = evaluations.count { it.status == DueStatus.DUE_SOON || it.status == DueStatus.DUE_NOW }
            val inspectionCount = evaluations.count { it.status == DueStatus.INSPECTION_DUE }
            val normalCount = evaluations.count { it.status == DueStatus.NORMAL }

            val worstStatus = when {
                overdueCount > 0 -> DueStatus.OVERDUE
                dueSoonCount > 0 -> DueStatus.DUE_SOON
                inspectionCount > 0 -> DueStatus.INSPECTION_DUE
                normalCount > 0 -> DueStatus.NORMAL
                else -> DueStatus.NEEDS_DATA
            }

            VehicleWithStatus(
                car = car,
                evaluations = evaluations,
                overdueCount = overdueCount,
                dueSoonCount = dueSoonCount,
                inspectionCount = inspectionCount,
                normalCount = normalCount,
                worstStatus = worstStatus
            )
        }.flowOn(Dispatchers.Default)
    }

    override fun getAllVehiclesWithStatus(): Flow<List<VehicleWithStatus>> {
        return combine(
            db.carDao().getAllCars(),
            db.catalogDao().getAllCatalog(),
            db.maintenanceRuleDao().observeAllRules()
        ) { cars, catalog, allRules ->
            val catalogMap = catalog.associateBy { it.itemCode }

            cars.map { car ->
                val rules = allRules.filter { it.carId == car.id }
                val evaluations = rules.filter { it.isEnabled }.mapNotNull { rule ->
                    val catalogItem = catalogMap[rule.itemCode] ?: return@mapNotNull null
                    if (ApplicabilityResolver.isItemApplicable(car, catalogItem)) MaintenanceScheduleEngine.evaluateItem(car, rule, catalogItem, LocalDate.now()) else null
                }

                val overdueCount = evaluations.count { it.status == DueStatus.OVERDUE }
                val dueSoonCount = evaluations.count { it.status == DueStatus.DUE_SOON || it.status == DueStatus.DUE_NOW }
                val inspectionCount = evaluations.count { it.status == DueStatus.INSPECTION_DUE }
                val normalCount = evaluations.count { it.status == DueStatus.NORMAL }

                val worstStatus = when {
                    overdueCount > 0 -> DueStatus.OVERDUE
                    dueSoonCount > 0 -> DueStatus.DUE_SOON
                    inspectionCount > 0 -> DueStatus.INSPECTION_DUE
                    normalCount > 0 -> DueStatus.NORMAL
                    else -> DueStatus.NEEDS_DATA
                }

                VehicleWithStatus(
                    car = car,
                    evaluations = evaluations,
                    overdueCount = overdueCount,
                    dueSoonCount = dueSoonCount,
                    inspectionCount = inspectionCount,
                    normalCount = normalCount,
                    worstStatus = worstStatus
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveVehicle(car: CarEntity, isNewCar: Boolean) = withContext(Dispatchers.IO) {
        db.withTransaction {
        val previous = db.carDao().getCarByIdDirect(car.id)
        car.firstRegistrationDate?.let { require(java.time.LocalDate.parse(it) <= LocalDate.now()) }
        require(car.engineHours == null || (car.engineHours.isFinite() && car.engineHours >= (previous?.engineHours ?: 0.0)))
        require(car.currentMileage >= (previous?.currentMileage ?: 0)) { "누적 주행거리는 감소할 수 없습니다." }
        db.carDao().insertCar(car)
        val catalogForUpdate = db.catalogDao().getAllCatalogDirect().ifEmpty {
            maha.garage.data.seed.DefaultCatalogData.items.also { db.catalogDao().insertAll(it) }
        }
        if (previous != null) {
            val standardChanged = (previous.maintenanceStandard != car.maintenanceStandard) ||
                (previous.isSevereCondition != car.isSevereCondition) ||
                (previous.powertrain != car.powertrain)
            val oldDefaults = ApplicabilityResolver.resolveDefaultRules(previous, catalogForUpdate).associateBy { it.itemCode }
            val existing = db.maintenanceRuleDao().getAllRulesForCarDirect(car.id).associateBy { it.itemCode }
            val updated = ApplicabilityResolver.resolveDefaultRules(car, catalogForUpdate).map { fresh ->
                val old = existing[fresh.itemCode]
                val baseline = oldDefaults[fresh.itemCode]
                val ruleToSave = if (old == null) {
                    fresh
                } else if (standardChanged || (baseline != null &&
                    old.intervalKm == baseline.intervalKm && old.intervalMonths == baseline.intervalMonths &&
                    old.firstIntervalKm == baseline.firstIntervalKm && old.firstIntervalMonths == baseline.firstIntervalMonths)) {
                    old.copy(
                        intervalKm = fresh.intervalKm,
                        intervalMonths = fresh.intervalMonths,
                        firstIntervalKm = fresh.firstIntervalKm,
                        firstIntervalMonths = fresh.firstIntervalMonths,
                        sourceType = fresh.sourceType,
                        isVerified = fresh.isVerified
                    )
                } else old

                // 신차 출고 설정이거나 1,000km 이하 차량: 기존에 교환 이력 및 관리 기준이 없던 항목은 0km 기산점 부여
                val isEffectivelyNew = isNewCar || car.currentMileage <= 1000
                if (isEffectivelyNew && ruleToSave.lastServiceMileage == null && (ruleToSave.baselineMileage == null || ruleToSave.baselineMileage == previous.currentMileage)) {
                    ruleToSave.copy(
                        baselineMileage = 0,
                        baselineDate = car.firstRegistrationDate ?: LocalDate.now().toString(),
                        baselineHours = 0.0
                    )
                } else {
                    ruleToSave
                }
            }
            db.maintenanceRuleDao().insertRules(updated)
        }

        // 신규 차량인 경우 적합한 기본 규칙 자동 생성 및 주행거리 이력 기록
        val existingRules = db.maintenanceRuleDao().getAllRulesForCarDirect(car.id)
        if (existingRules.isEmpty()) {
            val catalog = db.catalogDao().getAllCatalogDirect()
            val isEffectivelyNew = isNewCar || car.currentMileage <= 1000
            val newRules = ApplicabilityResolver.resolveDefaultRules(car, catalog).map { rule ->
                if (isEffectivelyNew) {
                    // 신차 출고 등록 시 또는 1,000km 이하 차량: 모든 소모품이 0km 신품에서 시작하므로 0km 기산점 설정
                    rule.copy(
                        baselineMileage = 0,
                        baselineDate = car.firstRegistrationDate ?: LocalDate.now().toString(),
                        baselineHours = 0.0
                    )
                } else {
                    // 중고차 등록 시 (1,000km 초과)
                    val effectiveKm = rule.firstIntervalKm ?: rule.intervalKm ?: 0
                    when {
                        // 1. 5만km 이상 장주기 부품 (타이밍벨트, 워터펌프, 미션오일, 점화플러그, 냉각수/부동액 등): 출고 0km부터 누적 측정
                        effectiveKm >= 50000 -> {
                            rule.copy(
                                baselineMileage = 0,
                                baselineDate = car.firstRegistrationDate ?: LocalDate.now().toString(),
                                baselineHours = 0.0
                            )
                        }
                        // 2. 1.6만km 이상 ~ 5만km 미만 부품 (브레이크 패드, 브레이크액, 연료필터 등): 절반 정도의 교환주기 경과로 간주
                        effectiveKm >= 16000 -> {
                            val halfKm = effectiveKm / 2
                            val halfMonths = rule.intervalMonths?.let { (it / 2).coerceAtLeast(1) }
                            rule.copy(
                                baselineMileage = (car.currentMileage - halfKm).coerceAtLeast(0),
                                baselineDate = halfMonths?.let { LocalDate.now().minusMonths(it.toLong()).toString() },
                                baselineHours = null
                            )
                        }
                        // 3. 1.5만km 이하 부품 (엔진오일, 에어컨필터, 와이퍼 등): 정보없음 (교환 기록 등록 대기)
                        else -> {
                            rule.copy(
                                baselineMileage = null,
                                baselineDate = null,
                                baselineHours = null
                            )
                        }
                    }
                }
            }
            db.maintenanceRuleDao().insertRules(newRules)

            db.meterReadingDao().insertReading(
                MeterReadingEntity(
                    carId = car.id,
                    readingDate = LocalDate.now().toString(),
                    odometerKm = car.currentMileage,
                    engineHours = car.engineHours,
                    inputSource = "REGISTRATION"
                )
            )
        }
        }
    }

    override suspend fun updateMileage(
        carId: String,
        newMileage: Int,
        engineHours: Double?,
        summerTireMileage: Int?,
        winterTireMileage: Int?,
        lastSwapMileage: Int?
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val car = requireNotNull(db.carDao().getCarByIdDirect(carId))
            require(newMileage >= car.currentMileage)
            require(engineHours == null || (engineHours.isFinite() && engineHours >= (car.engineHours ?: 0.0)))
            db.carDao().updateMileage(
                id = carId,
                newMileage = newMileage,
                engineHours = engineHours,
                summerTireMileage = summerTireMileage,
                winterTireMileage = winterTireMileage,
                lastSwapMileage = lastSwapMileage
            )
            db.meterReadingDao().insertReading(
                MeterReadingEntity(
                    carId = carId,
                    readingDate = LocalDate.now().toString(),
                    odometerKm = newMileage,
                    engineHours = engineHours,
                    inputSource = "MANUAL_UPDATE"
                )
            )
        }
    }

    override suspend fun swapTires(
        carId: String,
        targetMode: String,
        swapMileage: Int?,
        storageLocation: String?,
        frontTread: Double?,
        rearTread: Double?,
        swapCost: Long,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
        val car = requireNotNull(db.carDao().getCarByIdDirect(carId))
        val effectiveSwapMileage = swapMileage ?: car.currentMileage

        val (updatedCar, swapLog) = TireSwapCalculator.performTireSwap(
            car = car,
            targetMode = targetMode,
            swapMileage = effectiveSwapMileage,
            storageLocation = storageLocation,
            frontTreadDepthMm = frontTread,
            rearTreadDepthMm = rearTread,
            swapCost = swapCost,
            notes = notes
        )

        db.carDao().updateCar(updatedCar)
        db.tireSwapLogDao().insertSwapLog(swapLog)
        }
    }

    override suspend fun archiveVehicle(carId: String) = withContext(Dispatchers.IO) {
        db.carDao().archiveCar(carId)
    }

    override suspend fun unarchiveVehicle(carId: String) = withContext(Dispatchers.IO) {
        db.carDao().unarchiveCar(carId)
    }

    override suspend fun deleteVehiclePermanently(carId: String) = withContext(Dispatchers.IO) {
        db.carDao().deleteCarById(carId)
    }

    override suspend fun fetchVehicleSpecByPlate(plateNumber: String): maha.garage.data.remote.dto.VehicleSpecDto? = withContext(Dispatchers.IO) {
        val cleanPlate = plateNumber.replace(" ", "").trim()
        if (cleanPlate.isBlank()) return@withContext null

        // 1. 공공데이터포털(TS) 실제 API 통신 시도 (인증키 설정 시)
        if (maha.garage.data.remote.NetworkModule.isApiKeyConfigured) {
            try {
                val response = maha.garage.data.remote.NetworkModule.vehicleApiService.getVehicleSpec(plateNumber = cleanPlate)
                val rawItem = response.response?.body?.items?.itemList?.firstOrNull()
                if (rawItem != null) {
                    val rawFuel = rawItem.fuelType?.trim()
                    val rawCategory = rawItem.carCategory?.trim()
                    val regDateStr = formatRawDate(rawItem.regDate)
                    val inspectDateStr = formatRawDate(rawItem.nextInspectDate)
                    val modelYearInt = rawItem.regDate?.take(4)?.toIntOrNull() ?: 2023

                    return@withContext maha.garage.data.remote.dto.VehicleSpecDto(
                        plateNumber = rawItem.plateNumber ?: plateNumber,
                        modelName = rawItem.carModelName?.trim(),
                        manufacturer = rawItem.manufacturer?.trim(),
                        fuelType = rawFuel,
                        displacement = rawItem.displacement?.let { if (it.endsWith("cc")) it else "$it cc" },
                        registrationDate = regDateStr,
                        nextInspectionDate = inspectDateStr,
                        carCategory = rawCategory,
                        powertrain = mapFuelToPowertrain(rawFuel),
                        vehicleClass = mapCategoryToVehicleClass(rawCategory),
                        modelYear = modelYearInt
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Mock Fallback (API 키 미설정 또는 네트워크 오류 시 개발/체험용 자동 매칭)
        getMockSpecForPlate(cleanPlate)
    }

    private fun formatRawDate(rawDate: String?): String? {
        if (rawDate.isNullOrBlank() || rawDate.length != 8) return rawDate
        return try {
            val y = rawDate.substring(0, 4)
            val m = rawDate.substring(4, 6)
            val d = rawDate.substring(6, 8)
            "$y-$m-$d"
        } catch (e: Exception) {
            rawDate
        }
    }

    private fun mapFuelToPowertrain(fuel: String?): maha.garage.data.model.enums.PowertrainType {
        val f = fuel.orEmpty()
        return when {
            f.contains("하이브리드") || f.contains("HEV") || f.contains("PHEV") -> maha.garage.data.model.enums.PowertrainType.HEV_GASOLINE
            f.contains("전기") || f.contains("EV") -> maha.garage.data.model.enums.PowertrainType.BEV
            f.contains("디젤") || f.contains("경유") -> maha.garage.data.model.enums.PowertrainType.DIESEL
            f.contains("LPG") || f.contains("가스") -> maha.garage.data.model.enums.PowertrainType.LPG
            else -> maha.garage.data.model.enums.PowertrainType.GASOLINE
        }
    }

    private fun mapCategoryToVehicleClass(category: String?): maha.garage.data.model.enums.VehicleClass {
        val c = category.orEmpty()
        return when {
            c.contains("화물") || c.contains("픽업") || c.contains("트럭") -> maha.garage.data.model.enums.VehicleClass.PICKUP
            c.contains("승합") || c.contains("밴") || c.contains("카니발") || c.contains("스타리아") -> maha.garage.data.model.enums.VehicleClass.VAN
            c.contains("SUV") || c.contains("RV") -> maha.garage.data.model.enums.VehicleClass.SUV
            else -> maha.garage.data.model.enums.VehicleClass.SEDAN
        }
    }

    private fun getMockSpecForPlate(plate: String): maha.garage.data.remote.dto.VehicleSpecDto? {
        return when {
            plate.contains("33가4455") || plate.contains("캠리") -> maha.garage.data.remote.dto.VehicleSpecDto(
                plateNumber = plate,
                modelName = "캠리 하이브리드 (XV70)",
                manufacturer = "토요타",
                fuelType = "가솔린 하이브리드",
                displacement = "2,487 cc",
                registrationDate = "2021-06-15",
                nextInspectionDate = "2025-06-14",
                carCategory = "중형 승용",
                powertrain = maha.garage.data.model.enums.PowertrainType.HEV_GASOLINE,
                vehicleClass = maha.garage.data.model.enums.VehicleClass.SEDAN,
                modelYear = 2021
            )
            plate.contains("12가3456") || plate.contains("그랜저") -> maha.garage.data.remote.dto.VehicleSpecDto(
                plateNumber = plate,
                modelName = "그랜저 하이브리드 (GN7)",
                manufacturer = "현대",
                fuelType = "가솔린 하이브리드",
                displacement = "1,598 cc",
                registrationDate = "2023-04-10",
                nextInspectionDate = "2027-04-09",
                carCategory = "대형 승용",
                powertrain = maha.garage.data.model.enums.PowertrainType.HEV_GASOLINE,
                vehicleClass = maha.garage.data.model.enums.VehicleClass.SEDAN,
                modelYear = 2023
            )
            plate.contains("77하7777") || plate.contains("아이오닉") -> maha.garage.data.remote.dto.VehicleSpecDto(
                plateNumber = plate,
                modelName = "아이오닉 5 Long Range",
                manufacturer = "현대",
                fuelType = "전기(EV)",
                displacement = "0 cc",
                registrationDate = "2022-09-20",
                nextInspectionDate = "2026-09-19",
                carCategory = "중형 승용",
                powertrain = maha.garage.data.model.enums.PowertrainType.BEV,
                vehicleClass = maha.garage.data.model.enums.VehicleClass.SUV,
                modelYear = 2022
            )
            plate.contains("58러9123") || plate.contains("쏘렌토") -> maha.garage.data.remote.dto.VehicleSpecDto(
                plateNumber = plate,
                modelName = "쏘렌토 2.2 디젤 (MQ4)",
                manufacturer = "기아",
                fuelType = "디젤",
                displacement = "2,151 cc",
                registrationDate = "2022-01-18",
                nextInspectionDate = "2026-01-17",
                carCategory = "중형 RV",
                powertrain = maha.garage.data.model.enums.PowertrainType.DIESEL,
                vehicleClass = maha.garage.data.model.enums.VehicleClass.SUV,
                modelYear = 2022
            )
            plate.contains("11가1111") || plate.contains("아반떼") -> maha.garage.data.remote.dto.VehicleSpecDto(
                plateNumber = plate,
                modelName = "아반떼 1.6 가솔린 (CN7)",
                manufacturer = "현대",
                fuelType = "가솔린",
                displacement = "1,598 cc",
                registrationDate = "2023-05-12",
                nextInspectionDate = "2027-05-11",
                carCategory = "준중형 승용",
                powertrain = maha.garage.data.model.enums.PowertrainType.GASOLINE,
                vehicleClass = maha.garage.data.model.enums.VehicleClass.SEDAN,
                modelYear = 2023
            )
            else -> null
        }
    }
}

