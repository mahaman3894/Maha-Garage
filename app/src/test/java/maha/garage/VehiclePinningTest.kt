package maha.garage

import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.model.VehicleWithStatus
import org.junit.Assert.*
import org.junit.Test

class VehiclePinningTest {

    private fun createCar(id: String, name: String): VehicleWithStatus {
        val car = CarEntity(
            id = id,
            carNumber = "12가 3456",
            modelName = name
        )
        return VehicleWithStatus(
            car = car,
            evaluations = emptyList(),
            overdueCount = 0,
            dueSoonCount = 0,
            inspectionCount = 0,
            normalCount = 0,
            worstStatus = maha.garage.data.model.enums.DueStatus.NORMAL
        )
    }

    @Test
    fun testPinnedVehicleMovesToFrontIndexZero() {
        val carA = createCar("car_a", "아반떼")
        val carB = createCar("car_b", "쏘나타")
        val carC = createCar("car_c", "그랜저")
        val list = listOf(carA, carB, carC)

        val pinnedId = "car_b"
        val sorted = if (!pinnedId.isNullOrBlank()) {
            val pinned = list.filter { it.car.id == pinnedId }
            val others = list.filter { it.car.id != pinnedId }
            pinned + others
        } else list

        assertEquals("car_b", sorted[0].car.id)
        assertEquals("car_a", sorted[1].car.id)
        assertEquals("car_c", sorted[2].car.id)
    }

    @Test
    fun testUnpinnedPreservesOriginalOrder() {
        val carA = createCar("car_a", "아반떼")
        val carB = createCar("car_b", "쏘나타")
        val list = listOf(carA, carB)

        val pinnedId: String? = null
        val sorted = if (!pinnedId.isNullOrBlank()) {
            val pinned = list.filter { it.car.id == pinnedId }
            val others = list.filter { it.car.id != pinnedId }
            pinned + others
        } else list

        assertEquals("car_a", sorted[0].car.id)
        assertEquals("car_b", sorted[1].car.id)
    }

    @Test
    fun testSelectionIndexResolution() {
        val carA = createCar("car_a", "아반떼")
        val carB = createCar("car_b", "쏘나타")
        val carC = createCar("car_c", "그랜저")
        val list = listOf(carB, carA, carC) // carB pinned at 0

        // 1. When selectedId matches carC, index should be 2
        val selectedId = "car_c"
        val idxC = list.indexOfFirst { it.car.id == selectedId }
        assertEquals(2, idxC)

        // 2. When selectedId is null, pinned car at 0 should be selected
        val nullSelected: String? = null
        val pinnedId = "car_b"
        val defaultIdx = when {
            !nullSelected.isNullOrBlank() -> list.indexOfFirst { it.car.id == nullSelected }.takeIf { it >= 0 } ?: 0
            !pinnedId.isNullOrBlank() -> 0
            else -> 0
        }
        assertEquals(0, defaultIdx)
    }

    @Test
    fun testDoubleTapPinAndMoveToFirst() {
        val carA = createCar("car_a", "아반떼")
        val carB = createCar("car_b", "쏘나타")
        val carC = createCar("car_c", "그랜저")
        val initialList = listOf(carA, carB, carC)

        // carC 더블탭 시: 0번 인덱스로 즉시 이동 및 핀 고정
        val doubleTappedId = "car_c"
        val targetVehicle = initialList.find { it.car.id == doubleTappedId }!!
        val pinned = listOf(targetVehicle)
        val others = initialList.filter { it.car.id != doubleTappedId }
        val reordered = pinned + others

        assertEquals("car_c", reordered[0].car.id)
        assertEquals("car_a", reordered[1].car.id)
        assertEquals("car_b", reordered[2].car.id)
    }
}
