package maha.garage

import maha.garage.data.catalog.VehicleSearchIndex
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.seed.DefaultCatalogData
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.domain.engine.TireSwapCalculator
import maha.garage.ui.components.calculateSketchLayout
import org.junit.Assert.*
import org.junit.Test

class RequestedFixesTest {
    @Test fun mercedesClassAliasesAndInitials() {
        for (letter in listOf("A", "B", "C", "E", "S", "G")) {
            for (model in listOf("$letter-Class", "$letter-클래스(W213)", "AMG $letter-Class")) {
                for (query in listOf(letter, letter.lowercase(), "${letter}클래스", "$letter-class", "벤츠 $letter")) {
                    assertTrue("$query => $model", VehicleSearchIndex.matches("벤츠", model, "", "", query))
                }
            }
        }
        assertTrue(VehicleSearchIndex.matches("벤츠", "G-Class", "", "", "지바겐"))
        assertTrue(VehicleSearchIndex.matches("벤츠", "G-Class", "", "", "G바겐"))
        assertFalse(VehicleSearchIndex.matches("벤츠", "C-Class", "C 200", "AMG Line", "e"))
        assertFalse(VehicleSearchIndex.matches("벤츠", "GLC", "", "", "G클래스"))
        assertTrue(VehicleSearchIndex.matches("현대", "쏘나타", "가솔린", "", "쏘나타 가솔린"))
    }

    @Test fun mercedesBenzPrioritizedFirstOnAlphabetQueryCaseInsensitive() {
        fun makeTestItem(id: String, brand: String, modelName: String, lineup: String = "", trim: String = "", modelYear: Int = 2026) =
            maha.garage.data.catalog.VehicleCatalogItem(
                id = id, brand = brand, modelName = modelName, lineup = lineup, trim = trim,
                modelYear = modelYear, powertrain = "GASOLINE", vehicleClass = "SEDAN",
                timingType = "CHAIN", timingDesc = "", displacement = "", power = "",
                torque = "", transmission = "", driveSystem = "", fuelEconomy = "", dimensions = ""
            )

        val testCatalog = listOf(
            makeTestItem("1", "Infiniti", "EX(J50)", modelYear = 2015),
            makeTestItem("2", "Lotus", "Eletre", modelYear = 2024),
            makeTestItem("3", "벤츠", "E-Class", lineup = "E 300 4MATIC", modelYear = 2026),
            makeTestItem("4", "벤츠", "E-클래스(W213)", lineup = "E 220 d", modelYear = 2023),
            makeTestItem("5", "렉서스", "ES(XV60)", lineup = "300h", modelYear = 2022),
            makeTestItem("6", "GMC", "Canyon", modelYear = 2023),
            makeTestItem("7", "벤츠", "C-Class", lineup = "C 200", modelYear = 2026),
            makeTestItem("8", "KG모빌리티", "코란도 C(C200)", modelYear = 2017),
            makeTestItem("9", "BYD", "SEAL", modelYear = 2024),
            makeTestItem("10", "벤츠", "S-Class", lineup = "S 500", modelYear = 2026),
            makeTestItem("11", "Ineos", "Grenadier", modelYear = 2024),
            makeTestItem("12", "벤츠", "G-Class", lineup = "G 63", modelYear = 2026),
            makeTestItem("13", "벤츠", "A-Class Hatchback", lineup = "A 220", modelYear = 2026),
            makeTestItem("14", "벤츠", "B-클래스(W246)", lineup = "B 200", modelYear = 2022)
        )

        val letters = listOf("A", "B", "C", "E", "S", "G")
        for (letter in letters) {
            for (query in listOf(letter, letter.lowercase())) {
                val results = maha.garage.data.catalog.VehicleCatalogHelper.search(testCatalog, query)
                assertTrue("결과가 존재해야 함: $query", results.isNotEmpty())
                assertEquals("최우선 항목은 벤츠 차량이어야 함: $query", "벤츠", results.first().brand)
            }
        }

        for (query in listOf("e300", "E300", "e 300", "E 300")) {
            val results = maha.garage.data.catalog.VehicleCatalogHelper.search(testCatalog, query)
            assertTrue("결과 존재: $query", results.isNotEmpty())
            assertEquals("최우선 항목은 벤츠 E클래스: $query", "벤츠", results.first().brand)
            assertTrue(results.first().modelName.contains("E-"))
        }

        for (query in listOf("c200", "C200")) {
            val results = maha.garage.data.catalog.VehicleCatalogHelper.search(testCatalog, query)
            assertTrue("결과 존재: $query", results.isNotEmpty())
            assertEquals("최우선 항목은 벤츠 C클래스: $query", "벤츠", results.first().brand)
            assertEquals("C-Class", results.first().modelName)
        }

        for (query in listOf("s500", "S500")) {
            val results = maha.garage.data.catalog.VehicleCatalogHelper.search(testCatalog, query)
            assertTrue("결과 존재: $query", results.isNotEmpty())
            assertEquals("최우선 항목은 벤츠 S클래스: $query", "벤츠", results.first().brand)
            assertEquals("S-Class", results.first().modelName)
        }
    }

    @Test fun everyHybridRetainsEngineCalloutsAndRules() {
        val required = setOf("ENGINE_OIL", "OIL_FILTER", "ENGINE_AIR_FILTER", "COOLANT", "TRANSMISSION_OIL")
        for (pt in listOf(PowertrainType.HEV_GASOLINE, PowertrainType.HEV_DIESEL,
            PowertrainType.PHEV_GASOLINE, PowertrainType.PHEV_DIESEL)) {
            val car = CarEntity(carNumber = "test", modelName = "Hybrid", powertrain = pt)
            val codes = ApplicabilityResolver.resolveDefaultRules(car, DefaultCatalogData.items).map { it.itemCode }.toSet()
            assertTrue("$pt missing ${required - codes}", codes.containsAll(required))
            val callouts = calculateSketchLayout(400f, 450f, 1f, pt == PowertrainType.BEV, false).callouts.map { it.code }
            assertTrue(callouts.containsAll(required - "OIL_FILTER"))
        }
        val ev = CarEntity(carNumber = "ev", modelName = "EV", powertrain = PowertrainType.BEV)
        assertFalse(ApplicabilityResolver.resolveDefaultRules(ev, DefaultCatalogData.items).any { it.itemCode == "ENGINE_OIL" })
    }

    @Test fun roundTripSwapPreservesMileageAndInput() {
        val original = CarEntity(carNumber = "test", modelName = "Test", currentMileage = 12000,
            lastSwapMileage = 10000, summerTireMileage = 5000, winterTireMileage = 3000)
        val first = TireSwapCalculator.performTireSwap(original, "WINTER", 12000,
            storageLocation = "창고", swapCost = 40000, notes = "메모", frontTreadDepthMm = 6.5)
        assertEquals("WINTER", first.updatedCar.currentTireMode)
        assertEquals(7000, first.updatedCar.summerTireMileage)
        assertEquals(40000L, first.swapLog.swapCost)
        assertEquals("창고", first.swapLog.storageLocation)
        assertEquals("메모", first.swapLog.notes)
        val second = TireSwapCalculator.performTireSwap(first.updatedCar, "SUMMER", 15000)
        assertEquals(6000, second.updatedCar.winterTireMileage)
        assertEquals(7000, second.updatedCar.summerTireMileage)
    }

    @Test fun ignitionCoilHasStandardIntervalAndCountsFromZeroForNewCar() {
        val car = CarEntity(
            carNumber = "12가 3456",
            modelName = "아반떼",
            powertrain = PowertrainType.GASOLINE,
            currentMileage = 0,
            firstRegistrationDate = "2026-10-24"
        )
        val catalog = DefaultCatalogData.items
        val rules = ApplicabilityResolver.resolveDefaultRules(car, catalog)
        val coilRule = rules.firstOrNull { it.itemCode == "IGNITION_COIL" }
        assertNotNull("가솔린 차량에 점화코일 규칙이 존재해야 함", coilRule)
        assertEquals("기본 주행거리는 100,000km여야 함", 100000, coilRule?.intervalKm)
        assertEquals("기본 개월수는 60개월이어야 함", 60, coilRule?.intervalMonths)

        // 신차 출고 baselineMileage = 0 적용 시 검증
        val newCarCoilRule = coilRule!!.copy(baselineMileage = 0, baselineDate = "2026-10-24")
        val coilCatalog = catalog.first { it.itemCode == "IGNITION_COIL" }
        val eval = maha.garage.domain.engine.MaintenanceScheduleEngine.evaluateItem(
            car,
            newCarCoilRule,
            coilCatalog,
            java.time.LocalDate.parse("2026-10-24")
        )

        assertEquals("신차 출고 시 상태는 NORMAL이어야 함", maha.garage.data.model.enums.DueStatus.NORMAL, eval.status)
        assertEquals("남은 주행거리는 100,000km여야 함", 100000, eval.remainingKm)
        assertTrue("isBaseline은 true여야 함", eval.isBaseline)
        assertEquals(0, eval.baselineMileage)
    }
}
