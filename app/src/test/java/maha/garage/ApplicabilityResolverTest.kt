package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass
import maha.garage.data.seed.DefaultCatalogData
import maha.garage.domain.engine.ApplicabilityResolver
import org.junit.Assert.*
import org.junit.Test

class ApplicabilityResolverTest {

    @Test
    fun `전기차(BEV)에는 엔진오일, 점화플러그가 배제되고 감속기오일이 포함되어야 한다`() {
        val bevCar = CarEntity(
            id = "bev-1",
            carNumber = "12전 3456",
            modelName = "아이오닉 5",
            powertrain = PowertrainType.BEV,
            vehicleClass = VehicleClass.SUV,
            currentMileage = 30000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(bevCar, DefaultCatalogData.items)
        val itemCodes = rules.map { it.itemCode }.toSet()

        // 배제 검증
        assertFalse("전기차에는 엔진오일이 없어야 함", itemCodes.contains("ENGINE_OIL"))
        assertFalse("전기차에는 흡기에어크리너가 없어야 함", itemCodes.contains("ENGINE_AIR_FILTER"))
        assertFalse("전기차에는 점화플러그가 없어야 함", itemCodes.contains("SPARK_PLUG"))
        assertFalse("전기차에는 타이밍벨트가 없어야 함", itemCodes.contains("TIMING_BELT_SET"))
        assertFalse("전기차에는 디젤 연료필터가 없어야 함", itemCodes.contains("FUEL_FILTER"))

        // 포함 검증
        assertTrue("전기차 전용 감속기 오일이 포함되어야 함", itemCodes.contains("REDUCTION_GEAR_OIL"))
        assertTrue("전기차 전용 저전도도 냉각수가 포함되어야 함", itemCodes.contains("EV_COOLANT"))
        assertTrue("공통 소모품 에어컨필터가 포함되어야 함", itemCodes.contains("CABIN_FILTER"))
        assertTrue("공통 소모품 브레이크패드가 포함되어야 함", itemCodes.contains("BRAKE_PAD_FRONT"))
    }

    @Test
    fun `디젤 차량에는 점화플러그가 배제되고 연료필터 및 수분분리기가 포함되어야 한다`() {
        val dieselCar = CarEntity(
            id = "diesel-1",
            carNumber = "34경 7890",
            modelName = "쏘렌토 디젤",
            powertrain = PowertrainType.DIESEL,
            vehicleClass = VehicleClass.SUV,
            currentMileage = 50000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(dieselCar, DefaultCatalogData.items)
        val itemCodes = rules.map { it.itemCode }.toSet()

        assertFalse("디젤에는 점화플러그가 없어야 함", itemCodes.contains("SPARK_PLUG"))
        assertTrue("디젤에는 연료필터가 포함되어야 함", itemCodes.contains("FUEL_FILTER"))
        assertTrue("디젤에는 수분분리기가 포함되어야 함", itemCodes.contains("WATER_SEPARATOR"))
    }

    @Test
    fun `상용 트럭에는 에어드라이어 및 섀시 그리스 주입이 오버레이 적용되어야 한다`() {
        val truck = CarEntity(
            id = "truck-1",
            carNumber = "80바 1234",
            modelName = "포터2 1톤",
            powertrain = PowertrainType.DIESEL,
            vehicleClass = VehicleClass.LIGHT_TRUCK,
            currentMileage = 60000
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(truck, DefaultCatalogData.items)
        val itemCodes = rules.map { it.itemCode }.toSet()

        assertTrue("상용차 전용 에어드라이어 카트리지가 포함되어야 함", itemCodes.contains("AIR_DRYER_CARTRIDGE"))
        assertTrue("상용차 전용 섀시 그리스 주입이 포함되어야 함", itemCodes.contains("CHASSIS_GREASE"))
    }

    @Test
    fun `가혹 운행조건 선택 시 가혹 주기값으로 우선 설정되어야 한다`() {
        val severeCar = CarEntity(
            id = "severe-1",
            carNumber = "11가 1111",
            modelName = "아반떼",
            powertrain = PowertrainType.GASOLINE,
            vehicleClass = VehicleClass.SEDAN,
            currentMileage = 20000,
            isSevereCondition = true
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(severeCar, DefaultCatalogData.items)
        val oilRule = rules.first { it.itemCode == "ENGINE_OIL" }

        assertEquals("가혹조건 엔진오일 교환주기는 5,000km여야 함", 5000, oilRule.intervalKm)
        assertEquals("가혹조건 엔진오일 교환개월은 6개월이어야 함", 6, oilRule.intervalMonths)
    }

    @Test
    fun `가솔린 하이브리드(HEV_GASOLINE) 차량에는 엔진오일, 오일필터, 흡기에어크리너가 반드시 포함되어야 한다`() {
        val hybridCar = CarEntity(
            id = "hev-1",
            carNumber = "56하 1234",
            make = "현대",
            modelName = "그랜저 하이브리드",
            powertrain = PowertrainType.HEV_GASOLINE,
            vehicleClass = VehicleClass.SEDAN,
            currentMileage = 15000,
            timingType = "CHAIN"
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(hybridCar, DefaultCatalogData.items)
        val itemCodes = rules.map { it.itemCode }.toSet()

        assertTrue("하이브리드 차량에는 엔진오일이 반드시 포함되어야 함", itemCodes.contains("ENGINE_OIL"))
        assertTrue("하이브리드 차량에는 흡기에어크리너가 포함되어야 함", itemCodes.contains("ENGINE_AIR_FILTER"))

        val oilRule = rules.first { it.itemCode == "ENGINE_OIL" }
        assertEquals("일반차량과 동일하게 기본 엔진오일 교환주기는 10,000km여야 함", 10000, oilRule.intervalKm)
        assertEquals("일반차량과 동일하게 기본 엔진오일 교환개월은 12개월이어야 함", 12, oilRule.intervalMonths)
    }

    @Test
    fun `전기차 테슬라 모델Y는 OFFICIAL, SEVERE, SHOP 기준에 따라 권장주기가 차등 적용되어야 한다`() {
        val teslaOfficial = CarEntity(
            id = "tesla-1",
            carNumber = "01전 1234",
            make = "테슬라",
            modelName = "Model Y Long Range",
            powertrain = PowertrainType.BEV,
            vehicleClass = VehicleClass.SUV,
            maintenanceStandard = "OFFICIAL"
        )
        val teslaSevere = teslaOfficial.copy(id = "tesla-2", maintenanceStandard = "SEVERE", isSevereCondition = true)
        val teslaShop = teslaOfficial.copy(id = "tesla-3", maintenanceStandard = "SHOP")

        val officialRules = ApplicabilityResolver.resolveDefaultRules(teslaOfficial, DefaultCatalogData.items)
        val severeRules = ApplicabilityResolver.resolveDefaultRules(teslaSevere, DefaultCatalogData.items)
        val shopRules = ApplicabilityResolver.resolveDefaultRules(teslaShop, DefaultCatalogData.items)

        val offCabin = officialRules.first { it.itemCode == "CABIN_FILTER" }
        val sevCabin = severeRules.first { it.itemCode == "CABIN_FILTER" }
        val shopCabin = shopRules.first { it.itemCode == "CABIN_FILTER" }

        assertEquals("테슬라 OFFICIAL 캐빈필터는 24개월이어야 함", 24, offCabin.intervalMonths)
        assertEquals("테슬라 SEVERE 캐빈필터는 12개월이어야 함", 12, sevCabin.intervalMonths)
        assertEquals("테슬라 SHOP 캐빈필터는 10,000km / 12개월이어야 함", 10000, shopCabin.intervalKm)
        assertEquals(12, shopCabin.intervalMonths)

        val offBrake = officialRules.first { it.itemCode == "BRAKE_FLUID" }
        val sevBrake = severeRules.first { it.itemCode == "BRAKE_FLUID" }
        val shopBrake = shopRules.first { it.itemCode == "BRAKE_FLUID" }

        assertEquals("테슬라 OFFICIAL 브레이크액은 48개월이어야 함", 48, offBrake.intervalMonths)
        assertEquals("테슬라 SEVERE 브레이크액은 24개월이어야 함", 24, sevBrake.intervalMonths)
        assertEquals("테슬라 SHOP 브레이크액은 40,000km / 24개월이어야 함", 40000, shopBrake.intervalKm)
    }

    @Test
    fun `초소형 공랭식 전기차 트위지는 캐빈필터와 냉각수가 배제되어야 한다`() {
        val twizy = CarEntity(
            id = "twizy-1",
            carNumber = "99전 9999",
            make = "르노코리아",
            modelName = "트위지 (Twizy)",
            powertrain = PowertrainType.BEV,
            vehicleClass = VehicleClass.OTHER
        )

        val rules = ApplicabilityResolver.resolveDefaultRules(twizy, DefaultCatalogData.items)
        val itemCodes = rules.map { it.itemCode }.toSet()

        assertFalse("트위지는 에어컨/캐빈필터가 없음", itemCodes.contains("CABIN_FILTER"))
        assertFalse("트위지는 공랭식이므로 냉각수가 없음", itemCodes.contains("COOLANT"))
        assertFalse("트위지는 저전도도 냉각수가 없음", itemCodes.contains("EV_COOLANT"))
        assertTrue("트위지 감속기 오일은 유지되어야 함", itemCodes.contains("REDUCTION_GEAR_OIL"))
    }

    @Test
    fun `가솔린 일반차량도 OFFICIAL, SEVERE, SHOP 3가지 기준이 소모품 전반에 차등 적용되어야 한다`() {
        val gasOfficial = CarEntity(
            id = "gas-1",
            carNumber = "10가 1001",
            make = "현대",
            modelName = "아반떼 가솔린",
            powertrain = PowertrainType.GASOLINE,
            maintenanceStandard = "OFFICIAL"
        )
        val gasSevere = gasOfficial.copy(id = "gas-2", maintenanceStandard = "SEVERE", isSevereCondition = true)
        val gasShop = gasOfficial.copy(id = "gas-3", maintenanceStandard = "SHOP")

        val offRules = ApplicabilityResolver.resolveDefaultRules(gasOfficial, DefaultCatalogData.items)
        val sevRules = ApplicabilityResolver.resolveDefaultRules(gasSevere, DefaultCatalogData.items)
        val shopRules = ApplicabilityResolver.resolveDefaultRules(gasShop, DefaultCatalogData.items)

        // 엔진오일
        assertEquals(10000, offRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)
        assertEquals(12, offRules.first { it.itemCode == "ENGINE_OIL" }.intervalMonths)
        assertEquals(5000, sevRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)
        assertEquals(6, sevRules.first { it.itemCode == "ENGINE_OIL" }.intervalMonths)
        assertEquals(7500, shopRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)
        assertEquals(6, shopRules.first { it.itemCode == "ENGINE_OIL" }.intervalMonths)

        // 미션오일
        assertEquals(100000, offRules.first { it.itemCode == "TRANSMISSION_OIL" }.intervalKm)
        assertEquals(50000, sevRules.first { it.itemCode == "TRANSMISSION_OIL" }.intervalKm)
        assertEquals(60000, shopRules.first { it.itemCode == "TRANSMISSION_OIL" }.intervalKm)

        // 점화플러그
        assertEquals(80000, offRules.first { it.itemCode == "SPARK_PLUG" }.intervalKm)
        assertEquals(50000, sevRules.first { it.itemCode == "SPARK_PLUG" }.intervalKm)
        assertEquals(60000, shopRules.first { it.itemCode == "SPARK_PLUG" }.intervalKm)
    }

    @Test
    fun `디젤 차량도 OFFICIAL, SEVERE, SHOP 3가지 기준에 따라 연료필터 및 엔진오일이 차등 적용되어야 한다`() {
        val dieselOfficial = CarEntity(
            id = "dsl-1",
            carNumber = "20구 2002",
            make = "기아",
            modelName = "쏘렌토 디젤",
            powertrain = PowertrainType.DIESEL,
            maintenanceStandard = "OFFICIAL"
        )
        val dieselSevere = dieselOfficial.copy(id = "dsl-2", maintenanceStandard = "SEVERE", isSevereCondition = true)
        val dieselShop = dieselOfficial.copy(id = "dsl-3", maintenanceStandard = "SHOP")

        val offRules = ApplicabilityResolver.resolveDefaultRules(dieselOfficial, DefaultCatalogData.items)
        val sevRules = ApplicabilityResolver.resolveDefaultRules(dieselSevere, DefaultCatalogData.items)
        val shopRules = ApplicabilityResolver.resolveDefaultRules(dieselShop, DefaultCatalogData.items)

        // 디젤 엔진오일
        assertEquals(15000, offRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)
        assertEquals(7500, sevRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)
        assertEquals(10000, shopRules.first { it.itemCode == "ENGINE_OIL" }.intervalKm)

        // 디젤 연료필터
        assertEquals(40000, offRules.first { it.itemCode == "FUEL_FILTER" }.intervalKm)
        assertEquals(24, offRules.first { it.itemCode == "FUEL_FILTER" }.intervalMonths)
        assertEquals(30000, sevRules.first { it.itemCode == "FUEL_FILTER" }.intervalKm)
        assertEquals(18, sevRules.first { it.itemCode == "FUEL_FILTER" }.intervalMonths)
        assertEquals(30000, shopRules.first { it.itemCode == "FUEL_FILTER" }.intervalKm)
        assertEquals(12, shopRules.first { it.itemCode == "FUEL_FILTER" }.intervalMonths)
    }
}
