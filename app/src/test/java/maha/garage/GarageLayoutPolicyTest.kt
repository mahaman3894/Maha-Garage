package maha.garage

import maha.garage.ui.vehicle.garageColumnCount
import maha.garage.ui.vehicle.garageRestColumnCount
import org.junit.Assert.*
import org.junit.Test

class GarageLayoutPolicyTest {
    @Test fun singleVehicleIsWideAtEveryScreenWidth() {
        for (width in listOf(320f, 599f, 600f, 999f, 1000f, 1920f)) {
            assertEquals(1, garageColumnCount(width, 1))
        }
    }

    @Test fun phoneTabletAndExpandedLayoutsUseTwoToFourColumns() {
        assertEquals(2, garageColumnCount(599f, 8))
        assertEquals(3, garageColumnCount(600f, 8))
        assertEquals(3, garageColumnCount(999f, 8))
        assertEquals(4, garageColumnCount(1000f, 8))
        assertEquals(4, garageColumnCount(2400f, 8))
    }

    @Test fun restAndActiveSectionsUseTheirOwnCounts() {
        assertEquals(1, garageColumnCount(1200f, 1))
        assertEquals(2, garageColumnCount(1200f, 2))
        assertEquals(3, garageColumnCount(1200f, 3))
        assertEquals(1, garageColumnCount(360f, 0))
    }

    @Test fun oneRestCarNeverExpandsWhenTwoOrMoreActiveCarsExist() {
        for (width in listOf(360f, 600f, 1200f)) {
            for (active in 2..6) {
                assertEquals(garageColumnCount(width, active), garageRestColumnCount(width, active, 1))
                assertEquals(garageColumnCount(width, active), garageRestColumnCount(width, active, 8))
            }
        }
    }

    @Test fun singleActiveCarStaysWideWhileRestCarsDivideIndependently() {
        for (width in listOf(360f, 600f, 1200f)) {
            assertEquals(1, garageColumnCount(width, 1))
            for (rest in 1..8) {
                assertEquals(garageColumnCount(width, rest), garageRestColumnCount(width, 1, rest))
                assertEquals(garageColumnCount(width, rest), garageRestColumnCount(width, 0, rest))
            }
        }
    }
}
