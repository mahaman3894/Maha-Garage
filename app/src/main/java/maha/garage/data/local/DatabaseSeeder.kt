package maha.garage.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.SourceType
import maha.garage.data.model.enums.VehicleClass
import maha.garage.data.seed.DefaultCatalogData

class DatabaseSeeder(private val db: MahaGarageDatabase) {

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Refresh bundled applicability lists, including all HEV/PHEV engines.
            db.catalogDao().insertAll(DefaultCatalogData.items)
            val catalog = db.catalogDao().getAllCatalogDirect()
            db.carDao().getAllCarsDirect().forEach { car ->
                val allRules = db.maintenanceRuleDao().getAllRulesForCarDirect(car.id)
                // 점화코일 규칙이 null 주기였던 경우 100,000km/60개월 기본 주기로 자동 보정
                val rulesToUpdate = allRules.filter { it.itemCode == "IGNITION_COIL" && it.intervalKm == null }
                if (rulesToUpdate.isNotEmpty()) {
                    val updated = rulesToUpdate.map { r ->
                        r.copy(
                            intervalKm = if (car.isSevereCondition) 80000 else 100000,
                            intervalMonths = if (car.isSevereCondition) 48 else 60,
                            actionType = ActionType.REPLACE
                        )
                    }
                    db.maintenanceRuleDao().insertRules(updated)
                }

                val existing = allRules.map { it.itemCode }.toSet()
                val missing = ApplicabilityResolver.resolveDefaultRules(car, catalog)
                    .filter { it.itemCode !in existing }
                if (missing.isNotEmpty()) db.maintenanceRuleDao().insertRules(missing)
            }
        }
        // 출시 버전: 사용자가 자신의 실제 차량을 직접 등록할 수 있도록 자동 더미 차량 삽입 제거
    }

    /**
     * 테스트 및 시연용 데모 차량(BMW 335i, Honda Pilot)이 필요한 경우 수동 호출
     */
    suspend fun seedDemoCars() = withContext(Dispatchers.IO) {
        val cars = db.carDao().getAllActiveCarsSnapshot()
        if (cars.isEmpty()) {
            val bmw335i = CarEntity(
                id = "demo-bmw-335i",
                carNumber = "12가 3456",
                modelName = "335i Convertible",
                nickname = "컨버터블",
                make = "BMW",
                modelYear = 2012,
                powertrain = PowertrainType.GASOLINE,
                vehicleClass = VehicleClass.SEDAN,
                currentMileage = 98500,
                currentTireMode = "SUMMER",
                summerTireMileage = 14200,
                winterTireMileage = 8500,
                lastSwapMileage = 95000,
                summerTireName = "미쉐린 PS4 245/40R18",
                winterTireName = "한국타이어 윈터아이셉트 에보3"
            )
            db.carDao().insertCar(bmw335i)
            initializeDefaultRulesForCar(bmw335i)

            val hondaPilot = CarEntity(
                id = "demo-honda-pilot",
                carNumber = "34나 7890",
                modelName = "Pilot",
                nickname = "패밀리 SUV",
                make = "Honda",
                modelYear = 2021,
                powertrain = PowertrainType.GASOLINE,
                vehicleClass = VehicleClass.SUV,
                currentMileage = 48200,
                currentTireMode = "SUMMER",
                summerTireMileage = 32000,
                winterTireMileage = 0,
                lastSwapMileage = 0,
                summerTireName = "브리지스톤 듀얼러 H/L 245/60R18"
            )
            db.carDao().insertCar(hondaPilot)
            initializeDefaultRulesForCar(hondaPilot)
        }
    }

    suspend fun initializeDefaultRulesForCar(car: CarEntity) = withContext(Dispatchers.IO) {
        val catalog = db.catalogDao().getAllCatalogDirect()
        val rulesToInsert = catalog.filter { item ->
            val matchPowertrain = item.applicablePowertrains.isEmpty() ||
                    item.applicablePowertrains.contains(car.powertrain)
            val matchClass = item.applicableVehicleClasses.isEmpty() ||
                    item.applicableVehicleClasses.contains(car.vehicleClass)
            matchPowertrain && matchClass
        }.map { item ->
            val intervalKm = if (car.isSevereCondition && item.severeIntervalKm != null) {
                item.severeIntervalKm
            } else {
                item.defaultIntervalKm
            }
            val intervalMonths = if (car.isSevereCondition && item.severeIntervalMonths != null) {
                item.severeIntervalMonths
            } else {
                item.defaultIntervalMonths
            }

            MaintenanceRuleEntity(
                carId = car.id,
                itemCode = item.itemCode,
                actionType = item.defaultActionType,
                intervalKm = intervalKm,
                intervalMonths = intervalMonths,
                firstIntervalKm = item.firstIntervalKm,
                firstIntervalMonths = item.firstIntervalMonths,
                sourceType = SourceType.GENERIC_ESTIMATE,
                isVerified = false,
                isEnabled = true
            )
        }
        db.maintenanceRuleDao().insertRules(rulesToInsert)
    }
}
