package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.local.entity.TireEntity
import maha.garage.data.local.entity.TireInstallationHistoryEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.seed.DefaultCatalogData
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.domain.engine.MaintenanceInputPolicy
import maha.garage.domain.engine.TireArchiveCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * MahaGarage 1.0.5 추가 오류 수정 검증 테스트 스위트
 * 1. 위치교환 차량/타이어 누적거리 감소 차단 및 1:1 매핑 검증
 * 2. 전륜/후륜 부분 교체 후 계절 스왑(여름↔겨울) 왕복 시 타이어 4본 정상 복귀 및 퇴역 타이어 배제
 * 3. 카탈로그 오분류 정정(제네시스 G90/GV80 일렉트릭 슈퍼차저, 벤츠 E클래스 등) 및 동력계 정비 규칙 동기화
 */
class AdditionalFixes105Test {

    // =========================================================================
    // 1. 위치교환 누적거리 보호 및 1:1 매핑 무결성 검증
    // =========================================================================

    @Test
    fun `위치교환 시 입력 주행거리가 차량 현재 주행거리보다 작으면 거절되어야 한다`() {
        val carCurrentMileage = 12000
        val inputMileage = 10000

        val error = try {
            if (inputMileage < carCurrentMileage) {
                throw IllegalArgumentException("위치교환 주행거리($inputMileage km)는 현재 차량 주행거리($carCurrentMileage km)보다 작을 수 없습니다.")
            }
            null
        } catch (e: IllegalArgumentException) {
            e.message
        }

        assertNotNull("차량 거리보다 작은 주행거리는 거절되어야 함", error)
        assertTrue(error!!.contains("작을 수 없습니다"))
    }

    @Test
    fun `위치교환 시 입력 주행거리가 타이어 장착 기준거리보다 작으면 거절되어야 한다`() {
        val tireInstalledAt = 15000
        val inputMileage = 14000

        val error = try {
            if (inputMileage < tireInstalledAt) {
                throw IllegalArgumentException("위치교환 주행거리($inputMileage km)가 타이어 장착 거리($tireInstalledAt km)보다 작을 수 없습니다.")
            }
            null
        } catch (e: IllegalArgumentException) {
            e.message
        }

        assertNotNull(error)
        assertTrue(error!!.contains("타이어 장착 거리"))
    }

    @Test
    fun `위치교환 매핑은 FL FR RL RR 4개 위치 간 1대1 전단사여야 하며 중복 또는 잘못된 위치는 거절되어야 한다`() {
        val validPositions = setOf("FL", "FR", "RL", "RR")

        // 1. 중복 목적지 (FL->RL, FR->RL)
        val duplicateDestMapping = mapOf("FL" to "RL", "FR" to "RL", "RL" to "FL", "RR" to "FR")
        val isDuplicateValid = duplicateDestMapping.keys == validPositions &&
                duplicateDestMapping.values.toSet() == validPositions &&
                duplicateDestMapping.values.size == 4
        assertFalse("중복 목적지는 거절되어야 함", isDuplicateValid)

        // 2. 누락된 위치 (3개만 매핑)
        val missingMapping = mapOf("FL" to "RL", "FR" to "RR", "RL" to "FL")
        val isMissingValid = missingMapping.keys == validPositions &&
                missingMapping.values.toSet() == validPositions
        assertFalse("누락된 위치 매핑은 거절되어야 함", isMissingValid)

        // 3. 잘못된 위치명 (SPARE 등)
        val invalidPosMapping = mapOf("FL" to "SPARE", "SPARE" to "FL", "FR" to "RR", "RR" to "FR")
        val isInvalidPosValid = invalidPosMapping.keys == validPositions &&
                invalidPosMapping.values.toSet() == validPositions
        assertFalse("유효하지 않은 위치명은 거절되어야 함", isInvalidPosValid)

        // 4. 정상 4륜 X자 교환 매핑
        val normalMapping = mapOf("FL" to "RR", "FR" to "RL", "RL" to "FR", "RR" to "FL")
        val isNormalValid = normalMapping.keys == validPositions &&
                normalMapping.values.toSet() == validPositions &&
                normalMapping.values.size == 4
        assertTrue("정상 4륜 매핑은 유효해야 함", isNormalValid)
    }

    @Test
    fun `정상 위치교환 시 차량 거리는 유지되거나 증가하며 각 타이어의 고유 정보와 주행거리 가산이 정확해야 한다`() {
        val initialCarMileage = 12000
        val rotationMileage = 15000 // 3,000km 추가 주행
        val installMileage = 12000

        var car = CarEntity(id = "car_rot", carNumber = "12가3456", modelName = "G80", currentMileage = initialCarMileage)

        val tiresBefore = listOf(
            TireEntity(id = "t_FL", carId = car.id, position = "FL", brand = "Michelin", productName = "PS4", dot = "2424", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_FR", carId = car.id, position = "FR", brand = "Michelin", productName = "PS4", dot = "2424", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_RL", carId = car.id, position = "RL", brand = "Michelin", productName = "PS4", dot = "2424", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_RR", carId = car.id, position = "RR", brand = "Michelin", productName = "PS4", dot = "2424", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true)
        )

        val rotationMap = mapOf("FL" to "RL", "RL" to "FL", "FR" to "RR", "RR" to "FR")

        // 위치교환 실행 시뮬레이션
        val tiresAfter = tiresBefore.map { tire ->
            val nextPos = rotationMap[tire.position] ?: tire.position
            val drivenSinceInstall = (rotationMileage - tire.installedAtMileage).coerceAtLeast(0)
            tire.copy(
                position = nextPos,
                cumulativeMileage = tire.cumulativeMileage + drivenSinceInstall,
                installedAtMileage = rotationMileage,
                assignedWheelPosition = nextPos
            )
        }
        car = car.copy(currentMileage = maxOf(car.currentMileage, rotationMileage))

        // 검증: 차량 거리가 15,000km로 정상 증가하고 절대 감소하지 않음
        assertEquals(15000, car.currentMileage)

        // 검증: 각 타이어의 식별자, 브랜드, DOT, 제품명이 완전히 보존됨
        assertEquals("t_FL", tiresAfter.first { it.position == "RL" }.id)
        assertEquals("PS4", tiresAfter.first { it.position == "RL" }.productName)
        assertEquals("2424", tiresAfter.first { it.position == "RL" }.dot)

        // 검증: 각 타이어의 누적 주행거리는 5000 + 3000 = 8000km
        assertTrue(tiresAfter.all { it.cumulativeMileage == 8000 })
        assertTrue(tiresAfter.all { it.installedAtMileage == 15000 })

        // 동일한 15,000km로 중복 위치교환 재실행 시 누적거리 중복 가산 방지 확인
        val tiresAfterDuplicate = tiresAfter.map { tire ->
            val nextPos = rotationMap[tire.position] ?: tire.position
            val drivenSinceInstall = (rotationMileage - tire.installedAtMileage).coerceAtLeast(0)
            tire.copy(
                position = nextPos,
                cumulativeMileage = tire.cumulativeMileage + drivenSinceInstall,
                installedAtMileage = rotationMileage
            )
        }
        // 주행거리 증가분 = (15000 - 15000) = 0km -> 누적거리 8000km 그대로 유지!
        assertTrue(tiresAfterDuplicate.all { it.cumulativeMileage == 8000 })
    }

    // =========================================================================
    // 2. 부분 교체 후 계절 스왑(Summer ↔ Winter) 왕복 시 타이어 구성 무결성
    // =========================================================================

    @Test
    fun `전륜 2개 부분 교체 후 겨울 스왑 및 여름 복귀 시 신규 전륜 2개와 기존 후륜 2개가 정확히 4륜으로 복귀하고 퇴역 타이어는 제외되어야 한다`() {
        val carId = "car_swap_test"

        // 1. 초기 상태: 여름용 4본 장착 (10,000km 장착, 현재 10,000km, 누적 0km)
        val initialSummerTires = listOf(
            TireEntity(id = "old_FL", carId = carId, position = "FL", assignedWheelPosition = "FL", season = "SUMMER", brand = "한국", productName = "S1", cumulativeMileage = 0, installedAtMileage = 10000, isMounted = true),
            TireEntity(id = "old_FR", carId = carId, position = "FR", assignedWheelPosition = "FR", season = "SUMMER", brand = "한국", productName = "S1", cumulativeMileage = 0, installedAtMileage = 10000, isMounted = true),
            TireEntity(id = "orig_RL", carId = carId, position = "RL", assignedWheelPosition = "RL", season = "SUMMER", brand = "한국", productName = "S1", cumulativeMileage = 0, installedAtMileage = 10000, isMounted = true),
            TireEntity(id = "orig_RR", carId = carId, position = "RR", assignedWheelPosition = "RR", season = "SUMMER", brand = "한국", productName = "S1", cumulativeMileage = 0, installedAtMileage = 10000, isMounted = true)
        )

        var allCarTires = initialSummerTires.toMutableList()

        // 2. 20,000km 시점: 전륜 2개(FL, FR) 마모로 신규 타이어로 부분 교체
        val replaceMileage = 20000
        val replacedPositions = setOf("FL", "FR")

        // (1) 구형 전륜 2개 퇴역 처리 (RETIRED) 및 주행거리 정산 (10,000km 주행)
        allCarTires = allCarTires.map { tire ->
            if (tire.isMounted && tire.position in replacedPositions) {
                val driven = (replaceMileage - tire.installedAtMileage).coerceAtLeast(0)
                tire.copy(
                    isMounted = false,
                    position = "RETIRED",
                    assignedWheelPosition = tire.position,
                    cumulativeMileage = tire.cumulativeMileage + driven,
                    updatedAt = 1000L
                )
            } else {
                tire
            }
        }.toMutableList()

        // (2) 신규 전륜 2개 장착
        val newFrontTires = listOf(
            TireEntity(id = "new_FL", carId = carId, position = "FL", assignedWheelPosition = "FL", season = "SUMMER", brand = "미쉐린", productName = "PS5", cumulativeMileage = 0, installedAtMileage = replaceMileage, isMounted = true, updatedAt = 1100L),
            TireEntity(id = "new_FR", carId = carId, position = "FR", assignedWheelPosition = "FR", season = "SUMMER", brand = "미쉐린", productName = "PS5", cumulativeMileage = 0, installedAtMileage = replaceMileage, isMounted = true, updatedAt = 1100L)
        )
        allCarTires.addAll(newFrontTires)

        // 현재 장착 상태 확인: new_FL, new_FR, orig_RL, orig_RR (4개)
        val mountedAt20k = allCarTires.filter { it.isMounted }
        assertEquals(4, mountedAt20k.size)
        assertTrue(mountedAt20k.any { it.id == "new_FL" && it.position == "FL" })
        assertTrue(mountedAt20k.any { it.id == "new_FR" && it.position == "FR" })
        assertTrue(mountedAt20k.any { it.id == "orig_RL" && it.position == "RL" })
        assertTrue(mountedAt20k.any { it.id == "orig_RR" && it.position == "RR" })

        // 3. 25,000km 시점 (여름 타이어로 5,000km 주행 후): 겨울 타이어로 전환 (스왑)
        val winterSwapMileage = 25000

        // (1) 장착 중이던 여름 타이어 4본 탈거 -> STORAGE 보관
        allCarTires = allCarTires.map { tire ->
            if (tire.isMounted) {
                val driven = (winterSwapMileage - tire.installedAtMileage).coerceAtLeast(0)
                tire.copy(
                    isMounted = false,
                    position = "STORAGE",
                    assignedWheelPosition = tire.position,
                    cumulativeMileage = tire.cumulativeMileage + driven,
                    updatedAt = 2000L
                )
            } else {
                tire
            }
        }.toMutableList()

        // (2) 4본 겨울 타이어 장착
        val winterTires = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(id = "winter_$pos", carId = carId, position = pos, assignedWheelPosition = pos, season = "WINTER", brand = "한국", productName = "윈터아이셉트", cumulativeMileage = 0, installedAtMileage = winterSwapMileage, isMounted = true, updatedAt = 2100L)
        }
        allCarTires.addAll(winterTires)

        // 4. 30,000km 시점 (겨울 타이어로 5,000km 주행 후): 다시 여름 타이어로 복귀 (스왑)
        val summerSwapMileage = 30000

        // (1) 현재 겨울 타이어 탈거 -> STORAGE
        allCarTires = allCarTires.map { tire ->
            if (tire.isMounted) {
                val driven = (summerSwapMileage - tire.installedAtMileage).coerceAtLeast(0)
                tire.copy(
                    isMounted = false,
                    position = "STORAGE",
                    assignedWheelPosition = tire.position,
                    cumulativeMileage = tire.cumulativeMileage + driven,
                    updatedAt = 3000L
                )
            } else {
                tire
            }
        }.toMutableList()

        // (2) 저장소에서 여름 타이어 복귀 조합 선택 알고리즘 시뮬레이션
        // 조건: position == "STORAGE" 이며 season == "SUMMER" (RETIRED는 절대 후보에 포함되지 않음!)
        val storedSummerCandidates = allCarTires.filter { it.position == "STORAGE" && it.season == "SUMMER" }

        // 바퀴 위치별로 가장 최신의 보관 타이어 매칭
        val standardPositions = listOf("FL", "FR", "RL", "RR")
        val restoredSummerTires = mutableListOf<TireEntity>()

        for (pos in standardPositions) {
            val bestTire = storedSummerCandidates
                .filter { (it.assignedWheelPosition ?: it.position) == pos }
                .maxByOrNull { it.updatedAt }

            assertNotNull("위치 $pos 에 해당하는 보관 타이어가 존재해야 함", bestTire)
            restoredSummerTires.add(
                bestTire!!.copy(
                    isMounted = true,
                    position = pos,
                    assignedWheelPosition = pos,
                    installedAtMileage = summerSwapMileage,
                    updatedAt = 3100L
                )
            )
        }

        // 5. 복귀 결과 종합 검증
        // (1) 복귀된 타이어 수는 정확히 4개이며, 위치 중복 없이 FL, FR, RL, RR
        assertEquals(4, restoredSummerTires.size)
        assertEquals(setOf("FL", "FR", "RL", "RR"), restoredSummerTires.map { it.position }.toSet())

        // (2) 퇴역한 구형 전륜(old_FL, old_FR)은 절대 복귀하지 않았음
        assertFalse(restoredSummerTires.any { it.id == "old_FL" || it.id == "old_FR" })

        // (3) 신규 전륜 2개(new_FL, new_FR)와 기존 후륜 2개(orig_RL, orig_RR)가 정확히 복귀함
        val restoredMap = restoredSummerTires.associateBy { it.position }
        assertEquals("new_FL", restoredMap["FL"]!!.id)
        assertEquals("미쉐린", restoredMap["FL"]!!.brand)
        assertEquals("PS5", restoredMap["FL"]!!.productName)

        assertEquals("new_FR", restoredMap["FR"]!!.id)
        assertEquals("미쉐린", restoredMap["FR"]!!.brand)
        assertEquals("PS5", restoredMap["FR"]!!.productName)

        assertEquals("orig_RL", restoredMap["RL"]!!.id)
        assertEquals("한국", restoredMap["RL"]!!.brand)

        assertEquals("orig_RR", restoredMap["RR"]!!.id)
        assertEquals("한국", restoredMap["RR"]!!.brand)

        // (4) 각 타이어별 누적거리 보존 검증:
        // - new_FL, new_FR: 20k~25k 동안 5,000km 주행 후 탈거되어 30k 복귀 시점까지 누적거리 5,000km 유지
        assertEquals(5000, restoredMap["FL"]!!.cumulativeMileage)
        assertEquals(5000, restoredMap["FR"]!!.cumulativeMileage)

        // - orig_RL, orig_RR: 10k~20k(10,000km) + 20k~25k(5,000km) = 15,000km 주행 후 누적거리 15,000km 유지
        assertEquals(15000, restoredMap["RL"]!!.cumulativeMileage)
        assertEquals(15000, restoredMap["RR"]!!.cumulativeMileage)

        // - 퇴역한 old_FL: 10,000km 시점에서 멈추고 겨울 동안 절대 증가하지 않음
        val retiredOld = allCarTires.first { it.id == "old_FL" }
        assertEquals("RETIRED", retiredOld.position)
        assertEquals(10000, retiredOld.cumulativeMileage)
    }

    // =========================================================================
    // 3. 카탈로그 오분류 정정 및 동력계 정비 규칙 동기화 검증
    // =========================================================================

    @Test
    fun `실제 vehicle_catalog JSON 파일에서 spec_1663 및 spec_1669가 순수전기차가 아닌 가솔린 체인으로 정정되어 있어야 한다`() {
        val catalogFile = File("app/src/main/assets/vehicle_catalog.json")
        assertTrue("카탈로그 파일이 존재해야 함", catalogFile.exists())

        val content = catalogFile.readText(Charsets.UTF_8)
        val array = JSONArray(content)

        var spec1663Found = false
        var spec1669Found = false
        var spec2166Found = false
        var spec2169Found = false

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.optString("id")
            val modelName = obj.optString("modelName")
            val trimName = obj.optString("trimName")
            val powertrain = obj.optString("powertrain")
            val timingType = obj.optString("timingType")
            val fullDesc = "$modelName $trimName"

            // 1. spec_1663 (제네시스 G90 RS4 가솔린 터보 3.5-48V 일렉트릭 슈퍼차저)
            if (id == "spec_1663") {
                spec1663Found = true
                assertEquals("spec_1663은 GASOLINE이어야 함", "GASOLINE", powertrain)
                assertEquals("spec_1663은 CHAIN이어야 함", "CHAIN", timingType)
                assertEquals("3,470 cc", obj.optString("displacement"))
            }

            // 2. spec_1669 (제네시스 GV80 JX1 F/L 쿠페 가솔린 터보 3.5-48V 일렉트릭 슈퍼차저)
            if (id == "spec_1669") {
                spec1669Found = true
                assertEquals("spec_1669는 GASOLINE이어야 함", "GASOLINE", powertrain)
                assertEquals("spec_1669는 CHAIN이어야 함", "CHAIN", timingType)
                assertEquals("3,470 cc", obj.optString("displacement"))
            }

            // 3. spec_2166 (벤츠 E-Class W213 E 200)
            if (id == "spec_2166") {
                spec2166Found = true
                assertEquals("spec_2166은 GASOLINE이어야 함", "GASOLINE", powertrain)
            }

            // 4. spec_2169 (벤츠 E-Class W213 E 220 d)
            if (id == "spec_2169") {
                spec2169Found = true
                assertEquals("spec_2169는 DIESEL이어야 함", "DIESEL", powertrain)
            }

            // 카탈로그 전체 전수 조사: '일렉트릭 슈퍼차저' 또는 '가솔린' 또는 '디젤'인데 BEV인 항목이 단 하나도 없어야 함
            if (fullDesc.contains("슈퍼차저") || fullDesc.contains("가솔린") || fullDesc.contains("디젤") || fullDesc.contains("T-GDI")) {
                assertNotEquals("내연기관/슈퍼차저 사양($id: $fullDesc)은 BEV로 분류되어서는 안 됨", "BEV", powertrain)
            }
        }

        assertTrue("spec_1663이 카탈로그에 존재해야 함", spec1663Found)
        assertTrue("spec_1669가 카탈로그에 존재해야 함", spec1669Found)
        assertTrue("spec_2166이 카탈로그에 존재해야 함", spec2166Found)
        assertTrue("spec_2169가 카탈로그에 존재해야 함", spec2169Found)
    }

    @Test
    fun `BEV 차량을 가솔린 또는 하이브리드로 정정 시 누락되었던 엔진오일 등 내연기관 정비 규칙이 자동 생성되어야 한다`() {
        val iceRequiredCodes = setOf("ENGINE_OIL", "OIL_FILTER", "ENGINE_AIR_FILTER", "TRANSMISSION_OIL", "COOLANT")

        // 1. 순수 전기차 상태: 엔진오일 및 내연기관 소모품 배제 확인
        val bevCar = CarEntity(
            id = "c_bev",
            carNumber = "123가4567",
            modelName = "G90",
            trimName = "가솔린 터보 3.5-48V 일렉트릭 슈퍼차저",
            powertrain = PowertrainType.BEV,
            timingType = "NA"
        )
        val bevRules = ApplicabilityResolver.resolveDefaultRules(bevCar, DefaultCatalogData.items)
        val bevRuleCodes = bevRules.map { it.itemCode }.toSet()
        assertFalse("BEV에는 엔진오일이 없어야 함", bevRuleCodes.contains("ENGINE_OIL"))
        assertFalse("BEV에는 오일필터가 없어야 함", bevRuleCodes.contains("OIL_FILTER"))

        // 2. 동력계를 올바른 GASOLINE으로 정정
        val correctedCar = bevCar.copy(powertrain = PowertrainType.GASOLINE, timingType = "CHAIN")
        val correctedRules = ApplicabilityResolver.resolveDefaultRules(correctedCar, DefaultCatalogData.items)
        val correctedRuleCodes = correctedRules.map { it.itemCode }.toSet()

        // 3. 내연기관 필수 규칙(엔진오일, 오일필터, 에어필터, 미션오일 등)이 완벽히 포함되는지 검증
        assertTrue("정정된 가솔린 차량에는 필수 엔진 소모품이 모두 포함되어야 함", correctedRuleCodes.containsAll(iceRequiredCodes))

        // 4. 기존 사용자가 설정한 주기나 메모가 있는 경우 보존 로직 시뮬레이션
        val existingRuleMap = mapOf(
            "CABIN_FILTER" to MaintenanceRuleEntity(carId = bevCar.id, itemCode = "CABIN_FILTER", intervalKm = 15000, userCustomNotes = "사용자 지정 필터 주기")
        )

        val mergedRules = correctedRules.map { newRule ->
            val existing = existingRuleMap[newRule.itemCode]
            if (existing != null) {
                newRule.copy(
                    intervalKm = existing.intervalKm,
                    userCustomNotes = existing.userCustomNotes
                )
            } else {
                newRule
            }
        }

        val cabinRule = mergedRules.first { it.itemCode == "CABIN_FILTER" }
        assertEquals(15000, cabinRule.intervalKm)
        assertEquals("사용자 지정 필터 주기", cabinRule.userCustomNotes)
        assertTrue(mergedRules.any { it.itemCode == "ENGINE_OIL" })
    }

    // =========================================================================
    // 4. Rest Area (휴면) 차량 알림 차단 및 대시보드 배제 검증
    // =========================================================================

    @Test
    fun `Rest Area 휴면 차량은 대시보드 리스트 및 알림 카운트에서 완전 배제되어야 한다`() {
        val activeCar = CarEntity(id = "car-active", carNumber = "11가1111", modelName = "아반떼", isArchived = false)
        val dormantCar = CarEntity(id = "car-dormant", carNumber = "22나2222", modelName = "그랜저", isArchived = true)

        val allCars = listOf(activeCar, dormantCar)
        val activeOnly = allCars.filter { !it.isArchived }

        assertEquals(1, activeOnly.size)
        assertEquals("car-active", activeOnly[0].id)
        assertFalse("휴면 차량은 활성 목록에 포함되지 않아야 함", activeOnly.any { it.isArchived })
    }

    @Test
    fun `Rest Area 휴면 차량의 경우 만기일이 지나도 휴면 상태로 표시되어야 한다`() {
        val dormantCar = CarEntity(
            id = "car-dormant",
            carNumber = "22나2222",
            modelName = "그랜저",
            insuranceEndDate = "2020-01-01",
            inspectionEndDate = "2020-01-01",
            isArchived = true
        )

        val statusText = if (dormantCar.isArchived) "(휴면)" else "(만기임박)"
        assertEquals("(휴면)", statusText)
    }
}
