package maha.garage.domain.engine

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.SourceType
import maha.garage.data.model.enums.VehicleClass

object ApplicabilityResolver {

    /**
     * 특정 차량에 해당 소모품 항목이 유효하게 적용 가능한지 판별합니다.
     */
    fun isItemApplicable(car: CarEntity, catalogItem: MaintenanceCatalogEntity): Boolean {
        // 1. 순수 전기차(BEV)의 내연기관 전용 품목 배제 검증 (하이브리드는 엔진오일 등 내연기관 항목 필수 유지)
        if (car.powertrain == PowertrainType.BEV) {
            val iceOnlyCodes = setOf(
                "ENGINE_OIL",
                "OIL_FILTER",
                "ENGINE_AIR_FILTER",
                "SPARK_PLUG",
                "IGNITION_COIL",
                "TRANSMISSION_OIL",
                "FUEL_FILTER",
                "WATER_SEPARATOR",
                "TIMING_BELT_SET",
                "TIMING_BELT_WATER_PUMP",
                "COOLANT" // 전기차는 일반 부동액 대신 EV_COOLANT 또는 차종별 규격 냉각수 사용
            )
            if (iceOnlyCodes.contains(catalogItem.itemCode)) {
                return false
            }
        }

        // 1-1. 타이밍 N/A인 차량(순수 모터 구동 등)은 타이밍 벨트 관련 품목만 배제 (엔진오일은 배제하지 않음)
        if (car.timingType == "NA") {
            if (catalogItem.itemCode in setOf("TIMING_BELT_SET", "TIMING_BELT_WATER_PUMP")) {
                return false
            }
        }

        // 1-2. 초소형 공랭식 전기차(르노 트위지 등) 예외 처리: 공조장치 및 수랭식 냉각수가 아예 없음
        val isTwizy = car.modelName.contains("트위지", ignoreCase = true) || car.modelName.contains("Twizy", ignoreCase = true)
        if (isTwizy && catalogItem.itemCode in setOf("CABIN_FILTER", "COOLANT", "EV_COOLANT")) {
            return false
        }

        // 2. 가솔린 전용 품목 검증 (점화플러그 등은 디젤에 미적용)
        if (car.powertrain in setOf(PowertrainType.DIESEL, PowertrainType.HEV_DIESEL, PowertrainType.PHEV_DIESEL) && catalogItem.itemCode in setOf("SPARK_PLUG", "IGNITION_COIL")) {
            return false
        }

        // 3. 디젤 전용 품목 검증 (연료 수분분리기 등은 가솔린/BEV에 미적용)
        if (car.powertrain != PowertrainType.DIESEL && car.powertrain != PowertrainType.HEV_DIESEL && car.powertrain != PowertrainType.PHEV_DIESEL) {
            if (catalogItem.itemCode == "WATER_SEPARATOR") {
                return false
            }
        }

        // 4. 동력계(Powertrain) 매칭 검사
        if (catalogItem.applicablePowertrains.isNotEmpty()) {
            if (!catalogItem.applicablePowertrains.contains(car.powertrain)) {
                return false
            }
        }

        // 5. 차체/용도(VehicleClass) 매칭 검사 (상용차 전용 등)
        if (catalogItem.applicableVehicleClasses.isNotEmpty()) {
            if (!catalogItem.applicableVehicleClasses.contains(car.vehicleClass)) {
                return false
            }
        }

        return true
    }

    /**
     * 카탈로그 전체에서 차량에 적합한 항목만 필터링하여 초기 MaintenanceRuleEntity 목록을 생성합니다.
     * OFFICIAL (공식 매뉴얼), SEVERE (공식 가혹), SHOP (정비소/커뮤니티 권장 예방정비) 기준 반영
     */
    fun resolveDefaultRules(car: CarEntity, catalog: List<MaintenanceCatalogEntity>): List<MaintenanceRuleEntity> {
        val standard = if (car.maintenanceStandard == "SHOP") {
            "SHOP"
        } else if (car.maintenanceStandard == "SEVERE" || car.isSevereCondition) {
            "SEVERE"
        } else {
            "OFFICIAL"
        }
        val isSevere = standard == "SEVERE"
        val isShop = standard == "SHOP"

        val carTxt = "${car.make} ${car.modelName} ${car.trimName.orEmpty()}".lowercase()
        val isTesla = car.make.contains("테슬라", ignoreCase = true) || car.make.contains("Tesla", ignoreCase = true) || carTxt.contains("model ") || carTxt.contains("cybertruck")
        val isTesla3Y = isTesla && (carTxt.contains("model 3") || carTxt.contains("model y") || carTxt.contains("모델 3") || carTxt.contains("모델 y") || carTxt.contains("모델3") || carTxt.contains("모델y"))
        val isTeslaSX = isTesla && (carTxt.contains("model s") || carTxt.contains("model x") || carTxt.contains("모델 s") || carTxt.contains("모델 x") || carTxt.contains("모델s") || carTxt.contains("모델x"))

        val isKiaEV6 = car.powertrain == PowertrainType.BEV && carTxt.contains("ev6")
        val isEGMP = car.powertrain == PowertrainType.BEV && (
            carTxt.contains("아이오닉 5") || carTxt.contains("아이오닉5") ||
            carTxt.contains("아이오닉 6") || carTxt.contains("아이오닉6") ||
            carTxt.contains("아이오닉 9") || carTxt.contains("아이오닉9") ||
            carTxt.contains("ioniq") ||
            carTxt.contains("ev3") || carTxt.contains("ev4") || isKiaEV6 || carTxt.contains("ev9") ||
            carTxt.contains("gv60")
        )
        val isChevyEV = car.powertrain == PowertrainType.BEV && (car.make.contains("쉐보레", ignoreCase = true) || car.make.contains("chevrolet", ignoreCase = true) || carTxt.contains("볼트") || carTxt.contains("bolt"))
        val isKgmEV = car.powertrain == PowertrainType.BEV && (car.make.contains("kg", ignoreCase = true) || car.make.contains("쌍용", ignoreCase = true) || carTxt.contains("토레스 evx") || carTxt.contains("코란도"))
        val isRenaultEV = car.powertrain == PowertrainType.BEV && (car.make.contains("르노", ignoreCase = true) || car.make.contains("renault", ignoreCase = true) || carTxt.contains("조에") || carTxt.contains("zoe") || carTxt.contains("sm3"))
        val isLegacyHmgEV = car.powertrain == PowertrainType.BEV && !isEGMP && (carTxt.contains("코나") || carTxt.contains("니로") || carTxt.contains("포터") || carTxt.contains("봉고"))

        return catalog
            .filter { item -> isItemApplicable(car, item) }
            .map { item ->
                var intervalKm: Int?
                var intervalMonths: Int?
                var firstIntervalKm: Int? = item.firstIntervalKm
                var firstIntervalMonths: Int? = item.firstIntervalMonths

                // 1. 순수 전기차(BEV) 전용 정밀 소모품 매칭
                if (car.powertrain == PowertrainType.BEV) {
                    when (item.itemCode) {
                        "CABIN_FILTER" -> {
                            if (isTesla3Y) {
                                intervalKm = if (isShop) 10000 else null
                                intervalMonths = if (isSevere) 12 else if (isShop) 12 else 24
                            } else if (isTeslaSX) {
                                intervalKm = if (isShop) 10000 else null
                                intervalMonths = if (isSevere) 24 else if (isShop) 12 else 36
                            } else {
                                intervalKm = if (isSevere || isShop) 10000 else 15000
                                intervalMonths = if (isChevyEV && isSevere) 6 else 12
                            }
                        }
                        "BRAKE_FLUID" -> {
                            if (isTesla3Y) {
                                intervalKm = if (isSevere || isShop) 40000 else null
                                intervalMonths = if (isSevere || isShop) 24 else 48
                            } else if (isTeslaSX) {
                                intervalKm = if (isSevere || isShop) 40000 else null
                                intervalMonths = 24
                            } else if (isChevyEV) {
                                intervalKm = if (isSevere || isShop) 40000 else 75000
                                intervalMonths = if (isSevere || isShop) 24 else 60
                            } else {
                                intervalKm = 40000
                                intervalMonths = 24
                            }
                        }
                        "REDUCTION_GEAR_OIL" -> {
                            if (isTesla) {
                                intervalKm = if (isSevere) 100000 else if (isShop) 80000 else null
                                intervalMonths = if (isSevere || isShop) 48 else null
                            } else if (isEGMP || isLegacyHmgEV) {
                                intervalKm = if (isSevere) 120000 else if (isShop) 50000 else 60000
                                intervalMonths = if (isSevere) 72 else if (isShop) 36 else 48
                            } else if (isChevyEV || isRenaultEV) {
                                intervalKm = if (isSevere) 80000 else if (isShop) 60000 else null
                                intervalMonths = if (isSevere || isShop) 48 else null
                            } else if (isKgmEV) {
                                intervalKm = if (isSevere) 60000 else if (isShop) 50000 else null
                                intervalMonths = if (isSevere || isShop) 36 else null
                            } else {
                                intervalKm = if (isSevere) 120000 else if (isShop) 50000 else 60000
                                intervalMonths = if (isSevere) 72 else if (isShop) 36 else 48
                            }
                        }
                        "EV_COOLANT" -> {
                            if (isTesla) {
                                intervalKm = if (isSevere || isShop) 160000 else null
                                intervalMonths = if (isSevere || isShop) 96 else null
                            } else if (isKiaEV6) {
                                intervalKm = if (isSevere || isShop) 40000 else 20000
                                intervalMonths = 24
                                firstIntervalKm = 100000
                                firstIntervalMonths = 60
                            } else if (isEGMP) {
                                intervalKm = 40000
                                intervalMonths = 24
                                firstIntervalKm = if (isSevere || isShop) 100000 else 200000
                                firstIntervalMonths = if (isSevere || isShop) 60 else 60
                            } else if (isLegacyHmgEV || carTxt.contains("이모션")) {
                                intervalKm = 60000
                                intervalMonths = 36
                                firstIntervalKm = 60000
                                firstIntervalMonths = 36
                            } else if (isChevyEV) {
                                intervalKm = if (isSevere || isShop) 100000 else 240000
                                intervalMonths = 60
                            } else if (isKgmEV) {
                                intervalKm = 40000
                                intervalMonths = 24
                                firstIntervalKm = if (isSevere || isShop) 100000 else 200000
                                firstIntervalMonths = if (isSevere || isShop) 60 else 120
                            } else if (isRenaultEV) {
                                intervalKm = if (isSevere || isShop) 80000 else 120000
                                intervalMonths = 48
                            } else {
                                intervalKm = if (isSevere && item.severeIntervalKm != null) item.severeIntervalKm else item.defaultIntervalKm
                                intervalMonths = if (isSevere && item.severeIntervalMonths != null) item.severeIntervalMonths else item.defaultIntervalMonths
                            }
                        }
                        "TIRE_ROTATION" -> {
                            intervalKm = 10000
                            intervalMonths = if (isSevere) 6 else 12
                        }
                        "BRAKE_PAD_FRONT", "BRAKE_PAD_REAR" -> {
                            intervalKm = 20000
                            intervalMonths = 12
                        }
                        "WIPER_BLADE" -> {
                            intervalKm = if (isSevere) 5000 else 10000
                            intervalMonths = if (isSevere || isShop) 6 else 12
                        }
                        "TIRE_REPLACEMENT" -> {
                            intervalKm = if (isSevere || isShop) 40000 else 50000
                            intervalMonths = if (isSevere) 36 else 48
                        }
                        else -> {
                            intervalKm = if (isSevere && item.severeIntervalKm != null) item.severeIntervalKm else item.defaultIntervalKm
                            intervalMonths = if (isSevere && item.severeIntervalMonths != null) item.severeIntervalMonths else item.defaultIntervalMonths
                        }
                    }
                } else {
                    // 2. 내연기관(가솔린, 디젤, LPG) 및 하이브리드(HEV, PHEV) 소모품 3단계(OFFICIAL, SEVERE, SHOP) 매칭
                    val isDiesel = car.powertrain in setOf(
                        PowertrainType.DIESEL,
                        PowertrainType.HEV_DIESEL,
                        PowertrainType.PHEV_DIESEL
                    )
                    when (item.itemCode) {
                        "ENGINE_OIL", "OIL_FILTER" -> {
                            if (isDiesel) {
                                intervalKm = if (isSevere) 7500 else if (isShop) 10000 else 15000
                                intervalMonths = if (isSevere || isShop) 6 else 12
                            } else {
                                // 가솔린, LPG, 가솔린 하이브리드
                                intervalKm = if (isSevere) 5000 else if (isShop) 7500 else 10000
                                intervalMonths = if (isSevere || isShop) 6 else 12
                            }
                        }
                        "ENGINE_AIR_FILTER" -> {
                            intervalKm = if (isSevere) 10000 else if (isShop) 10000 else 20000
                            intervalMonths = if (isSevere) 6 else 12
                        }
                        "CABIN_FILTER" -> {
                            intervalKm = if (isSevere) 7500 else if (isShop) 10000 else 15000
                            intervalMonths = if (isSevere || isShop) 6 else 12
                        }
                        "TRANSMISSION_OIL" -> {
                            intervalKm = if (isSevere) 50000 else if (isShop) 60000 else 100000
                            intervalMonths = if (isSevere) 36 else if (isShop) 48 else 60
                        }
                        "BRAKE_FLUID" -> {
                            intervalKm = if (isSevere) 30000 else 40000
                            intervalMonths = if (isSevere) 18 else 24
                        }
                        "SPARK_PLUG" -> {
                            intervalKm = if (isSevere) 50000 else if (isShop) 60000 else 80000
                            intervalMonths = if (isSevere) 36 else if (isShop) 36 else 48
                        }
                        "IGNITION_COIL" -> {
                            intervalKm = if (isSevere) 80000 else 100000
                            intervalMonths = if (isSevere) 48 else 60
                        }
                        "COOLANT" -> {
                            intervalKm = 40000
                            intervalMonths = 24
                            firstIntervalKm = if (isSevere) 40000 else if (isShop) 80000 else 100000
                            firstIntervalMonths = if (isSevere) 24 else if (isShop) 48 else 60
                        }
                        "FUEL_FILTER" -> {
                            intervalKm = if (isSevere) 30000 else if (isShop) 30000 else 40000
                            intervalMonths = if (isSevere) 18 else if (isShop) 12 else 24
                        }
                        "WATER_SEPARATOR" -> {
                            intervalKm = if (isSevere || isShop) 10000 else 20000
                            intervalMonths = if (isSevere || isShop) 6 else 12
                        }
                        "BATTERY_12V" -> {
                            intervalKm = if (isSevere || isShop) 50000 else 60000
                            intervalMonths = if (isSevere || isShop) 36 else 48
                        }
                        "TIMING_BELT_SET" -> {
                            intervalKm = if (isSevere || isShop) 80000 else 100000
                            intervalMonths = if (isSevere || isShop) 48 else 60
                        }
                        "TIMING_BELT_WATER_PUMP" -> {
                            intervalKm = if (isSevere || isShop) 100000 else 130000
                            intervalMonths = if (isSevere || isShop) 60 else 72
                        }
                        "HSG_BELT" -> {
                            intervalKm = if (isSevere) 20000 else 30000
                            intervalMonths = if (isSevere) 12 else 24
                            firstIntervalKm = if (isSevere) 20000 else if (isShop) 50000 else 60000
                            firstIntervalMonths = if (isSevere) 12 else if (isShop) 36 else 48
                        }
                        "BRAKE_PAD_FRONT" -> {
                            intervalKm = if (isSevere || isShop) 30000 else 40000
                            intervalMonths = if (isSevere || isShop) 18 else 24
                        }
                        "BRAKE_PAD_REAR" -> {
                            intervalKm = if (isSevere || isShop) 50000 else 70000
                            intervalMonths = if (isSevere || isShop) 24 else 36
                        }
                        "WIPER_BLADE" -> {
                            intervalKm = if (isSevere) 5000 else 10000
                            intervalMonths = if (isSevere || isShop) 6 else 12
                        }
                        "TIRE_ROTATION" -> {
                            intervalKm = 10000
                            intervalMonths = if (isSevere) 6 else 12
                        }
                        "TIRE_REPLACEMENT" -> {
                            intervalKm = if (isSevere || isShop) 40000 else 50000
                            intervalMonths = if (isSevere) 36 else 48
                        }
                        "AIR_DRYER_CARTRIDGE" -> {
                            intervalKm = if (isSevere || isShop) 40000 else 60000
                            intervalMonths = if (isSevere || isShop) 12 else 24
                        }
                        "CHASSIS_GREASE" -> {
                            intervalKm = if (isSevere || isShop) 5000 else 10000
                            intervalMonths = if (isSevere || isShop) 3 else 6
                        }
                        else -> {
                            intervalKm = if (isSevere && item.severeIntervalKm != null) item.severeIntervalKm else item.defaultIntervalKm
                            intervalMonths = if (isSevere && item.severeIntervalMonths != null) item.severeIntervalMonths else item.defaultIntervalMonths
                        }
                    }
                }

                if (isSevere && intervalKm != null) {
                    firstIntervalKm = firstIntervalKm?.coerceAtMost(intervalKm)
                }
                if (isSevere && intervalMonths != null) {
                    firstIntervalMonths = firstIntervalMonths?.coerceAtMost(intervalMonths)
                }

                val source = when (standard) {
                    "OFFICIAL" -> SourceType.VERIFIED_OEM
                    "SEVERE" -> SourceType.OEM_FAMILY
                    "SHOP" -> SourceType.GENERIC_ESTIMATE
                    else -> SourceType.GENERIC_ESTIMATE
                }

                MaintenanceRuleEntity(
                    carId = car.id,
                    itemCode = item.itemCode,
                    actionType = item.defaultActionType,
                    intervalKm = intervalKm,
                    intervalMonths = intervalMonths,
                    firstIntervalKm = firstIntervalKm,
                    firstIntervalMonths = firstIntervalMonths,
                    sourceType = source,
                    isVerified = (standard == "OFFICIAL"),
                    isEnabled = true
                )
            }
    }
}
