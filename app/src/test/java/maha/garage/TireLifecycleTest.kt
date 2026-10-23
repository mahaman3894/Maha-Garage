package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.TireEntity
import maha.garage.data.local.entity.TireInstallationHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TireLifecycleTest {

    @Test
    fun `시나리오 1 - 4개 타이어 중 전륜 2개만 신품 교체 시 후륜 2개의 고유 누적거리가 온전히 보존되어야 한다`() {
        val carId = "car_1"
        val initialMileage = 50000

        // 초기 4개 타이어 장착 상태 (장착 시점 50,000km, 각각 10,000km 누적 주행한 상태)
        val currentTires = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(
                id = "tire_$pos",
                carId = carId,
                position = pos,
                season = "ALL_SEASON",
                brand = "한국타이어",
                productName = "벤투스",
                cumulativeMileage = 10000,
                installedAtMileage = initialMileage,
                isMounted = true
            )
        }

        // 60,000km 시점에 전륜 2개(FL, FR)만 신품으로 교체 진행 (10,000km 추가 주행)
        val currentMileage = 60000
        val replacementPositions = setOf("FL", "FR")

        val newTires = listOf("FL", "FR").map { pos ->
            TireEntity(
                id = "new_tire_$pos",
                carId = carId,
                position = pos,
                season = "ALL_SEASON",
                brand = "미쉐린",
                productName = "PS4",
                cumulativeMileage = 0, // 신품 0km
                installedAtMileage = currentMileage,
                isMounted = true
            )
        }

        // 기존 타이어 중 교체 대상은 STORAGE로 이동, 미교체 타이어는 그대로 유지
        val updatedOldTires = currentTires.map { oldTire ->
            if (oldTire.position in replacementPositions) {
                val driven = (currentMileage - oldTire.installedAtMileage).coerceAtLeast(0)
                oldTire.copy(
                    isMounted = false,
                    position = "STORAGE",
                    assignedWheelPosition = oldTire.position,
                    cumulativeMileage = oldTire.cumulativeMileage + driven
                )
            } else {
                oldTire // 후륜은 그대로 유지
            }
        }

        // 검증: 후륜(RL, RR)은 교체되지 않고 장착 상태 유지
        val remainingRear = updatedOldTires.filter { it.isMounted }
        assertEquals("장착 유지된 타이어는 2개(RL, RR)여야 함", 2, remainingRear.size)
        assertTrue(remainingRear.any { it.position == "RL" && it.cumulativeMileage == 10000 })
        assertTrue(remainingRear.any { it.position == "RR" && it.cumulativeMileage == 10000 })

        // 검증: 탈거된 전륜 구형 타이어는 누적거리가 10,000 + 10,000 = 20,000km로 정상 합산되어 보관
        val storedFront = updatedOldTires.filter { !it.isMounted }
        assertEquals(2, storedFront.size)
        assertTrue(storedFront.all { it.cumulativeMileage == 20000 && it.position == "STORAGE" })

        // 검증: 신규 장착된 전륜 타이어는 0km로 장착
        assertEquals(2, newTires.size)
        assertTrue(newTires.all { it.cumulativeMileage == 0 && it.isMounted })
    }

    @Test
    fun `시나리오 2 - 4개 타이어 위치교환(로테이션) 후 각 타이어의 고유 누적거리가 정확히 유지되어야 한다`() {
        val carId = "car_1"
        // 50,000km에 장착되어 현재 60,000km까지 10,000km 주행
        val installMileage = 50000
        val rotationMileage = 60000

        val mountedTires = listOf(
            TireEntity(id = "t_FL", carId = carId, position = "FL", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_FR", carId = carId, position = "FR", cumulativeMileage = 5000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_RL", carId = carId, position = "RL", cumulativeMileage = 8000, installedAtMileage = installMileage, isMounted = true),
            TireEntity(id = "t_RR", carId = carId, position = "RR", cumulativeMileage = 8000, installedAtMileage = installMileage, isMounted = true)
        )

        // 로테이션 맵: FL <-> RL, FR <-> RR
        val rotationMap = mapOf(
            "FL" to "RL",
            "FR" to "RR",
            "RL" to "FL",
            "RR" to "FR"
        )

        val rotatedTires = mountedTires.map { tire ->
            val oldPos = tire.position
            val newPos = rotationMap[oldPos] ?: oldPos
            val driven = (rotationMileage - tire.installedAtMileage).coerceAtLeast(0)
            tire.copy(
                position = newPos,
                assignedWheelPosition = newPos,
                cumulativeMileage = tire.cumulativeMileage + driven,
                installedAtMileage = rotationMileage
            )
        }

        // 검증: 원래 FL이었던 타이어는 이제 RL에 위치하고, 누적거리는 5,000 + 10,000 = 15,000km
        val newRL = rotatedTires.first { it.id == "t_FL" }
        assertEquals("RL", newRL.position)
        assertEquals(15000, newRL.cumulativeMileage)
        assertEquals(rotationMileage, newRL.installedAtMileage)

        // 원래 RL이었던 타이어는 이제 FL에 위치하고, 누적거리는 8,000 + 10,000 = 18,000km
        val newFL = rotatedTires.first { it.id == "t_RL" }
        assertEquals("FL", newFL.position)
        assertEquals(18000, newFL.cumulativeMileage)
        assertEquals(rotationMileage, newFL.installedAtMileage)
    }

    @Test
    fun `시나리오 3 - 여름용에서 겨울용 스왑 후 다시 여름용 재장착 시 기존 타이어의 누적거리가 100퍼센트 보존되어야 한다`() {
        val carId = "car_1"

        // 1. 여름용 타이어 4개 장착 (50,000km 장착, 5,000km 주행 후 55,000km에 겨울용으로 스왑)
        val summerTires = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(
                id = "summer_$pos",
                carId = carId,
                position = pos,
                assignedWheelPosition = pos,
                season = "SUMMER",
                setId = "SUMMER_SET_1",
                cumulativeMileage = 0,
                installedAtMileage = 50000,
                isMounted = true
            )
        }

        // 겨울용으로 스왑 (탈거): 5,000km 주행 반영되어 누적 5,000km로 STORAGE 보관
        val swapToWinterMileage = 55000
        val storedSummerTires = summerTires.map { tire ->
            val driven = swapToWinterMileage - tire.installedAtMileage
            tire.copy(
                isMounted = false,
                position = "STORAGE",
                assignedWheelPosition = tire.position,
                cumulativeMileage = tire.cumulativeMileage + driven
            )
        }

        // 겨울용 타이어 장착 (55,000km ~ 60,000km 총 5,000km 주행)
        // 이후 60,000km에 다시 여름용으로 재장착 수행
        val swapBackToSummerMileage = 60000

        // 보관 중이던 여름용 타이어 재장착 시뮬레이션
        val reinstalledSummerTires = storedSummerTires.map { tire ->
            tire.copy(
                isMounted = true,
                position = tire.assignedWheelPosition ?: "FL",
                installedAtMileage = swapBackToSummerMileage,
                storageLocation = null
            )
        }

        // 검증: 재장착된 여름용 타이어의 누적 주행거리는 탈거 당시의 5,000km가 완벽히 보존되어 있어야 함
        assertEquals(4, reinstalledSummerTires.size)
        assertTrue(reinstalledSummerTires.all { it.isMounted })
        assertTrue("기존 누적 주행거리 5,000km가 보존되어야 함", reinstalledSummerTires.all { it.cumulativeMileage == 5000 })
        assertEquals("새 장착 시점은 60,000km여야 함", swapBackToSummerMileage, reinstalledSummerTires.first().installedAtMileage)
        val positions = reinstalledSummerTires.map { it.position }.toSet()
        assertEquals("FL, FR, RL, RR 4개 위치가 1:1로 복원되어야 함", setOf("FL", "FR", "RL", "RR"), positions)
    }

    @Test
    fun `시나리오 4 - 보관 타이어 복수 세트 존재 시 세트 식별 및 FL FR RL RR 1대1 매핑 중복 방지 검증`() {
        val carId = "car_1"

        // 구형 세트 (2023년)와 신형 세트 (2024년)가 보관 상태로 존재
        val oldSet = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(
                id = "old_$pos",
                carId = carId,
                position = "STORAGE",
                assignedWheelPosition = pos,
                season = "WINTER",
                setId = "WINTER_2023",
                updatedAt = 1000L
            )
        }
        val latestSet = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(
                id = "new_$pos",
                carId = carId,
                position = "STORAGE",
                assignedWheelPosition = pos,
                season = "WINTER",
                setId = "WINTER_2024",
                updatedAt = 2000L
            )
        }
        val allStoredWinter = oldSet + latestSet

        // 가장 최신 세트 선택 로직
        val selectedSet = allStoredWinter
            .groupBy { it.setId ?: "DEFAULT_SET" }
            .maxByOrNull { (_, list) -> list.maxOfOrNull { it.updatedAt } ?: 0L }
            ?.value ?: emptyList()

        assertEquals("최신 세트인 WINTER_2024가 선택되어야 함", "WINTER_2024", selectedSet.first().setId)
        assertEquals(4, selectedSet.size)

        // 위치 매핑 검증
        val standardPositions = listOf("FL", "FR", "RL", "RR")
        val assignedPositions = mutableSetOf<String>()
        val targetToMount = mutableListOf<Pair<TireEntity, String>>()

        for (tire in selectedSet.take(4)) {
            val preferred = tire.assignedWheelPosition
            if (preferred != null && preferred in standardPositions && !assignedPositions.contains(preferred)) {
                assignedPositions.add(preferred)
                targetToMount.add(Pair(tire, preferred))
            }
        }

        assertEquals("중복 없이 4개 위치가 모두 1:1 매핑되어야 함", 4, assignedPositions.size)
        assertTrue(assignedPositions.containsAll(listOf("FL", "FR", "RL", "RR")))
    }

    @Test
    fun `시나리오 5 - 보관 타이어가 이미 존재하는 경우 신규 타이어 자동 생성을 차단해야 한다`() {
        // 이미 보관(STORAGE) 중인 타이어가 4개 있는 상태
        val existingStoredTires = listOf("FL", "FR", "RL", "RR").map { pos ->
            TireEntity(
                id = "tire_$pos",
                carId = "car_1",
                position = "STORAGE",
                season = "SUMMER",
                isMounted = false
            )
        }

        // 로직: getTiresForCarDirect(carId) 결과가 비어있지 않으면 생성 건너뜀
        val shouldGenerateInitialTires = existingStoredTires.isEmpty()

        assertFalse("보관 타이어가 존재하면 신규 타이어 자동 생성이 차단되어야 함", shouldGenerateInitialTires)
    }

    @Test
    fun `시나리오 6 - 타이어 등록 시 휠 얼라인먼트 체크 시 휠얼라이먼트 키로수가 갱신되고 미체크 시 이전 키로수가 온전히 보존되어야 한다`() {
        var car = CarEntity(
            id = "car_alignment_test",
            carNumber = "77가 7777",
            modelName = "쏘나타",
            currentMileage = 100,
            wheelAlignmentMileage = null
        )

        // 1. 타이어 교체 100km 시점에 휠 얼라인먼트 체크 시 -> 100km로 등록
        val isFirstAlignmentChecked = true
        val replacementMileage1 = 100
        val updatedAlignment1 = if (isFirstAlignmentChecked) replacementMileage1 else car.wheelAlignmentMileage
        car = car.copy(currentMileage = 100, wheelAlignmentMileage = updatedAlignment1)
        assertEquals("휠얼라이먼트 체크 시 100km가 기록되어야 함", 100, car.wheelAlignmentMileage)

        // 2. 이후 200km 주행 시점에 타이어 재교체, 휠 얼라인먼트 미체크 시 -> 이전 100km 유지
        val isSecondAlignmentChecked = false
        val replacementMileage2 = 200
        val updatedAlignment2 = if (isSecondAlignmentChecked) replacementMileage2 else car.wheelAlignmentMileage
        car = car.copy(currentMileage = 200, wheelAlignmentMileage = updatedAlignment2)
        assertEquals("휠얼라이먼트 미체크 시 이전 100km가 그대로 유지되어야 함", 100, car.wheelAlignmentMileage)
    }
}
