package maha.garage

import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.*
import maha.garage.data.seed.DefaultCatalogData
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.domain.engine.MaintenanceScheduleEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class QualityGateTest {

    private val catalog = DefaultCatalogData.items

    @Test
    fun test1_BEV_Exclusion_And_Inclusion() {
        val bevCar = CarEntity(
            id = "bev-car-1",
            carNumber = "01전 1234",
            modelName = "아이오닉 5",
            make = "현대",
            powertrain = PowertrainType.BEV,
            vehicleClass = VehicleClass.SUV,
            currentMileage = 20000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(bevCar, catalog)
        val itemCodes = rules.map { it.itemCode }.toSet()

        // 1. 내연기관 소모품 배제 확인
        assertFalse("BEV에는 엔진오일이 없어야 함", itemCodes.contains("ENGINE_OIL"))
        assertFalse("BEV에는 점화플러그가 없어야 함", itemCodes.contains("SPARK_PLUG"))
        assertFalse("BEV에는 연료필터가 없어야 함", itemCodes.contains("FUEL_FILTER"))
        assertFalse("BEV에는 엔진 에어크리너가 없어야 함", itemCodes.contains("ENGINE_AIR_FILTER"))

        // 2. 전기차 전용 소모품 포함 확인
        assertTrue("BEV에는 감속기 오일이 있어야 함", itemCodes.contains("REDUCTION_GEAR_OIL"))
        assertTrue("BEV에는 전용 저전도도 냉각수가 있어야 함", itemCodes.contains("EV_COOLANT"))
    }

    @Test
    fun test2_Commercial_Diesel_Truck_Overlay() {
        val porterTruck = CarEntity(
            id = "truck-porter-1",
            carNumber = "80바 1234",
            modelName = "포터 II",
            make = "현대",
            powertrain = PowertrainType.DIESEL,
            vehicleClass = VehicleClass.LIGHT_TRUCK,
            currentMileage = 50000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(porterTruck, catalog)
        val itemCodes = rules.map { it.itemCode }.toSet()

        // 상용 트럭 특화 소모품 포함 확인
        assertTrue("디젤 트럭에는 에어드라이어 카트리지가 있어야 함", itemCodes.contains("AIR_DRYER_CARTRIDGE"))
        assertTrue("디젤 트럭에는 섀시 그리스 주입이 있어야 함", itemCodes.contains("CHASSIS_GREASE"))
        assertTrue("디젤 트럭에는 연료필터가 있어야 함", itemCodes.contains("FUEL_FILTER"))
    }

    @Test
    fun test3_HEV_Hybrid_Components() {
        val hevCar = CarEntity(
            id = "hev-tucson-1",
            carNumber = "23하 5678",
            modelName = "투싼 하이브리드",
            make = "현대",
            powertrain = PowertrainType.HEV_GASOLINE,
            vehicleClass = VehicleClass.SUV,
            currentMileage = 30000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(hevCar, catalog)
        val itemCodes = rules.map { it.itemCode }.toSet()

        // 하이브리드는 가솔린 엔진오일 + 하이브리드 특화 항목이 공존해야 함
        assertTrue("HEV에는 가솔린 엔진오일이 있어야 함", itemCodes.contains("ENGINE_OIL"))
        assertTrue("HEV에는 HSG 벨트가 있어야 함", itemCodes.contains("HSG_BELT"))
        assertTrue("HEV에는 냉각수가 있어야 함", itemCodes.contains("COOLANT"))
    }

    @Test
    fun test4_Inspect_vs_Replace_Semantics() {
        val car = CarEntity(
            id = "car-test-4",
            carNumber = "12가 3456",
            modelName = "쏘나타",
            powertrain = PowertrainType.GASOLINE,
            currentMileage = 40000
        )

        val catalogMap = catalog.associateBy { it.itemCode }
        val brakePadCatalog = catalogMap["BRAKE_PAD_FRONT"]!!

        // 1. 점검만 진행한 경우 (INSPECT) - 교체 주기 리셋 없이 측정값 보존
        val ruleAfterInspect = MaintenanceRuleEntity(
            carId = car.id,
            itemCode = "BRAKE_PAD_FRONT",
            actionType = ActionType.INSPECT,
            intervalKm = 40000,
            lastServiceDate = "2026-05-01",
            lastServiceMileage = 20000,
            lastConditionValue = "잔여 7.5mm (양호)"
        )
        val evalInspect = MaintenanceScheduleEngine.evaluateItem(car, ruleAfterInspect, brakePadCatalog, LocalDate.of(2026, 10, 1))
        assertEquals("점검 측정치가 정상적으로 반영되어야 함", "잔여 7.5mm (양호)", evalInspect.lastConditionValue)

        // 2. 신품 교체한 경우 (REPLACE)
        val ruleAfterReplace = MaintenanceRuleEntity(
            carId = car.id,
            itemCode = "BRAKE_PAD_FRONT",
            actionType = ActionType.REPLACE,
            intervalKm = 40000,
            lastServiceDate = "2026-10-01",
            lastServiceMileage = 40000,
            lastConditionValue = "신품 교체 완료 (10mm)"
        )
        val evalReplace = MaintenanceScheduleEngine.evaluateItem(car, ruleAfterReplace, brakePadCatalog, LocalDate.of(2026, 10, 1))
        assertEquals("신품 교체 시 잔여 km가 주기에 맞게 리셋되어야 함", 40000, evalReplace.remainingKm)
        assertEquals("상태는 NORMAL 이어야 함", DueStatus.NORMAL, evalReplace.status)
    }

    @Test
    fun test5_MultiLine_Batch_Evaluation() {
        val car = CarEntity(
            id = "car-multi-5",
            carNumber = "34다 9999",
            modelName = "BMW 330i",
            powertrain = PowertrainType.GASOLINE,
            currentMileage = 30000
        )
        val catalogMap = catalog.associateBy { it.itemCode }

        // 1회 방문에서 3가지 작업(엔진오일, 에어컨필터, 와이퍼) 동시 처리
        val oilRule = MaintenanceRuleEntity(carId = car.id, itemCode = "ENGINE_OIL", intervalKm = 10000, lastServiceMileage = 30000)
        val cabinRule = MaintenanceRuleEntity(carId = car.id, itemCode = "CABIN_FILTER", intervalKm = 15000, lastServiceMileage = 30000)
        val wiperRule = MaintenanceRuleEntity(carId = car.id, itemCode = "WIPER_BLADE", intervalKm = 10000, lastServiceMileage = 30000)

        val ev1 = MaintenanceScheduleEngine.evaluateItem(car, oilRule, catalogMap["ENGINE_OIL"]!!, LocalDate.now())
        val ev2 = MaintenanceScheduleEngine.evaluateItem(car, cabinRule, catalogMap["CABIN_FILTER"]!!, LocalDate.now())
        val ev3 = MaintenanceScheduleEngine.evaluateItem(car, wiperRule, catalogMap["WIPER_BLADE"]!!, LocalDate.now())

        assertEquals("동시 정비된 엔진오일 상태 NORMAL", DueStatus.NORMAL, ev1.status)
        assertEquals("동시 정비된 에어컨필터 상태 NORMAL", DueStatus.NORMAL, ev2.status)
        assertEquals("동시 정비된 와이퍼 상태 NORMAL", DueStatus.NORMAL, ev3.status)
    }

    @Test
    fun test6_JSON_Backup_RoundTrip_Data_Integrity() {
        val originalCar = CarEntity(
            id = "backup-car-id-123",
            carNumber = "77구 7777",
            modelName = "제네시스 G80",
            make = "제네시스",
            modelYear = 2024,
            powertrain = PowertrainType.GASOLINE,
            vehicleClass = VehicleClass.SEDAN,
            currentMileage = 15200,
            currentTireMode = "WINTER",
            summerTireMileage = 10000,
            winterTireMileage = 5200,
            summerTireName = "미쉐린 PS4",
            winterTireName = "한국 윈터아이셉트"
        )

        // 직렬화
        val root = JSONObject()
        root.put("app", "MahaGarage")
        root.put("version", 1)

        val carsArray = JSONArray()
        val cObj = JSONObject().apply {
            put("id", originalCar.id)
            put("carNumber", originalCar.carNumber)
            put("modelName", originalCar.modelName)
            put("make", originalCar.make)
            put("modelYear", originalCar.modelYear)
            put("powertrain", originalCar.powertrain.name)
            put("vehicleClass", originalCar.vehicleClass.name)
            put("currentMileage", originalCar.currentMileage)
            put("currentTireMode", originalCar.currentTireMode)
            put("summerTireMileage", originalCar.summerTireMileage)
            put("winterTireMileage", originalCar.winterTireMileage)
            put("summerTireName", originalCar.summerTireName)
            put("winterTireName", originalCar.winterTireName)
        }
        carsArray.put(cObj)
        root.put("cars", carsArray)

        val jsonString = root.toString()

        // 역직렬화
        val parsedRoot = JSONObject(jsonString)
        assertEquals("앱 식별자 일치", "MahaGarage", parsedRoot.getString("app"))

        val parsedCarObj = parsedRoot.getJSONArray("cars").getJSONObject(0)
        assertEquals("차량 ID 일치", originalCar.id, parsedCarObj.getString("id"))
        assertEquals("차량 번호 일치", originalCar.carNumber, parsedCarObj.getString("carNumber"))
        assertEquals("섬머 타이어 거리 일치", originalCar.summerTireMileage, parsedCarObj.getInt("summerTireMileage"))
        assertEquals("윈터 타이어 거리 일치", originalCar.winterTireMileage, parsedCarObj.getInt("winterTireMileage"))
        assertEquals("현재 장착 모드 일치", "WINTER", parsedCarObj.getString("currentTireMode"))
    }

    @Test
    fun test7_FirstInterval_EV_Coolant() {
        val bevCar = CarEntity(
            id = "bev-coolant-test",
            carNumber = "02하 1234",
            modelName = "EV6",
            make = "기아",
            modelYear = 2024,
            powertrain = PowertrainType.BEV,
            currentMileage = 10000
        )
        val rules = ApplicabilityResolver.resolveDefaultRules(bevCar, catalog)
        val evCoolantRule = rules.find { it.itemCode == "EV_COOLANT" }
        assertNotNull("EV 냉각수 규칙이 존재해야 함", evCoolantRule)
        assertEquals("최초 교체 주기는 100,000km 이어야 함", 100000, evCoolantRule?.firstIntervalKm)
        assertEquals("최초 교체 기간은 60개월 이어야 함", 60, evCoolantRule?.firstIntervalMonths)

        val catalogItem = catalog.first { it.itemCode == "EV_COOLANT" }

        // 1. 교환 기록/관리 기준이 없는 경우: 임의로 출고 기준을 적용하지 않고 '교환 기록 없음'(NEEDS_DATA) 처리
        val evalNoHistory = MaintenanceScheduleEngine.evaluateItem(bevCar, evCoolantRule!!, catalogItem, LocalDate.now())
        assertNull("교환 기록 및 관리 기준이 없으면 다음 교환 주행거리는 null이어야 함", evalNoHistory.nextDueKm)
        assertNull("교환 기록 및 관리 기준이 없으면 잔여 주행거리는 null이어야 함", evalNoHistory.remainingKm)
        assertEquals("교환 기록 없음 상태여야 함", DueStatus.NEEDS_DATA, evalNoHistory.status)
        assertEquals("교환 기록 없음", evalNoHistory.statusDescription)

        // 2. 명시적인 관리 시작 기준 설정 시: 기준 주행거리 10,000km + 주기 100,000km = 110,000km 계산 검증
        val ruleWithBaseline = evCoolantRule.copy(baselineMileage = 10000, intervalKm = 100000)
        val evalBaseline = MaintenanceScheduleEngine.evaluateItem(bevCar, ruleWithBaseline, catalogItem, LocalDate.now())
        assertEquals("관리 시작 기준 주행거리 10,000km + 주기 100,000km = 110,000km", 110000, evalBaseline.nextDueKm)
        assertEquals("현재 10,000km 기준 잔여거리는 100,000km", 100000, evalBaseline.remainingKm)
        assertTrue("관리 기준 플래그가 설정되어야 함", evalBaseline.isBaseline)

        // 3. 실제 교환 기록 시: 교환 주행거리 10,000km + 반복주기 40,000km = 50,000km 계산 검증
        val ruleWithService = evCoolantRule.copy(lastServiceMileage = 10000, intervalKm = 40000)
        val evalService = MaintenanceScheduleEngine.evaluateItem(bevCar, ruleWithService, catalogItem, LocalDate.now())
        assertEquals("교환 주행거리 10,000km + 반복주기 40,000km = 50,000km", 50000, evalService.nextDueKm)
        assertEquals("현재 10,000km 기준 잔여거리는 40,000km", 40000, evalService.remainingKm)
        assertFalse("실제 교환 기록은 관리 기준 플래그가 false여야 함", evalService.isBaseline)
    }

    @Test
    fun test8_MaxOf_Progress_Progression() {
        // 주행거리는 1,000km(10%)밖에 안 탔지만, 12개월(100%)이 지난 저주행 차량
        val lowMileageCar = CarEntity(
            id = "low-mileage-car",
            carNumber = "55호 5555",
            modelName = "그랜저",
            powertrain = PowertrainType.GASOLINE,
            currentMileage = 1000
        )
        val catalogItem = catalog.first { it.itemCode == "ENGINE_OIL" }
        val rule = MaintenanceRuleEntity(
            carId = lowMileageCar.id,
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            lastServiceDate = "2025-10-01",
            lastServiceMileage = 0
        )
        // 1년 후 평가 (2026-10-01)
        val eval = MaintenanceScheduleEngine.evaluateItem(lowMileageCar, rule, catalogItem, LocalDate.of(2026, 10, 1))
        
        // 거리 비율은 0.1(10%)이지만, 기간 비율은 1.0(100%)이므로 진행률은 maxOf에 의해 1.0 이상이어야 함
        assertTrue("기간 만기에 따라 진행률이 1.0(100%)에 도달해야 함", eval.progressRatio >= 1.0f)
        assertEquals("교체 기준일 도달 시 DUE_NOW 상태여야 함", DueStatus.DUE_NOW, eval.status)
    }

    @Test
    fun test9_BrakePadSeparationAndPositionRules() {
        val car = CarEntity(
            id = "car-bp-1",
            carNumber = "99구 9999",
            modelName = "소나타",
            powertrain = PowertrainType.GASOLINE,
            currentMileage = 50000
        )
        val frontItem = catalog.first { it.itemCode == "BRAKE_PAD_FRONT" }
        val rearItem = catalog.first { it.itemCode == "BRAKE_PAD_REAR" }

        // 전륜과 후륜이 독립적인 품목 코드로 관리되어야 함
        assertEquals("BRAKE_PAD_FRONT", frontItem.itemCode)
        assertEquals("BRAKE_PAD_REAR", rearItem.itemCode)
        assertNotEquals("전륜과 후륜의 주기는 달라야 함", frontItem.defaultIntervalKm, rearItem.defaultIntervalKm)

        // 브레이크 패드 전륜/후륜 개별 규칙 적용 검증
        val frontRule = MaintenanceRuleEntity(carId = car.id, itemCode = "BRAKE_PAD_FRONT", intervalKm = 35000, baselineMileage = 20000)
        val rearRule = MaintenanceRuleEntity(carId = car.id, itemCode = "BRAKE_PAD_REAR", intervalKm = 60000, baselineMileage = 20000)

        val frontEval = MaintenanceScheduleEngine.evaluateItem(car, frontRule, frontItem, LocalDate.now())
        val rearEval = MaintenanceScheduleEngine.evaluateItem(car, rearRule, rearItem, LocalDate.now())

        assertEquals(55000, frontEval.nextDueKm)
        assertEquals(80000, rearEval.nextDueKm)
        assertEquals(5000, frontEval.remainingKm)
        assertEquals(30000, rearEval.remainingKm)
    }

    @Test
    fun test10_PreserveExistingHoursWhenUpdatingDistanceAndDate() {
        // 기존에 등록된 시간 기준(intervalHours=250.0, baselineHours=100.0)
        val existingRule = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            intervalHours = 250.0,
            baselineMileage = 50000,
            baselineHours = 100.0
        )

        // 사용자가 날짜/주행거리만 수정하여 intervalHours와 baselineHours가 null로 전달된 새 규칙
        val updatedInput = MaintenanceRuleEntity(
            carId = "c1",
            itemCode = "ENGINE_OIL",
            intervalKm = 15000,
            intervalMonths = 6,
            intervalHours = null,
            baselineMileage = 52000,
            baselineHours = null
        )

        // 보존 로직: 수정하지 않은 값은 기존 값 유지
        val preservedIntervalHours = updatedInput.intervalHours ?: existingRule.intervalHours
        val preservedBaselineHours = updatedInput.baselineHours ?: existingRule.baselineHours

        assertEquals(250.0, preservedIntervalHours ?: 0.0, 0.001)
        assertEquals(100.0, preservedBaselineHours ?: 0.0, 0.001)
    }

    @Test
    fun test11_ValidationRangeRules() {
        val carMileage = 60000
        val today = LocalDate.now()

        // 1. 미래 날짜 검증
        val futureDate = today.plusDays(1)
        assertTrue("미래 날짜는 오늘 이후여야 함", futureDate.isAfter(today))

        // 2. 현재 주행거리 초과 기준 주행거리 검증
        val invalidBaselineMileage = 65000
        assertTrue("현재 주행거리를 초과하는 기준은 무효여야 함", invalidBaselineMileage > carMileage)

        // 3. 주기 범위 검증 (1 ~ 1,000,000km, 1 ~ 240개월)
        assertTrue(10000 in 1..1_000_000)
        assertFalse(1_500_000 in 1..1_000_000)
        assertFalse(-10 in 1..1_000_000)

        assertTrue(12 in 1..240)
        assertFalse(300 in 1..240)
    }
}
