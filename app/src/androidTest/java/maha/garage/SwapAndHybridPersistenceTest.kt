package maha.garage

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import maha.garage.data.local.DatabaseSeeder
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.repository.MaintenanceRepositoryImpl
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwapAndHybridPersistenceTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), MahaGarageDatabase::class.java).build()

    @Test fun swapCreatesFirstSeasonAndRejectsStaleDoubleSave() = runBlocking {
        val db = database()
        try {
            val car = CarEntity(id = "swap", carNumber = "test", modelName = "Test", currentMileage = 10000)
            db.carDao().insertCar(car)
            db.tireDao().insertTires(listOf("FL", "FR", "RL", "RR").map {
                TireEntity(carId = car.id, position = it, season = "SUMMER", installedAtMileage = 8000)
            })
            val repo = MaintenanceRepositoryImpl(db)
            repo.swapSeasonTires(car, "WINTER", 10000, "2026-10-20", "창고", 6.0, 5.5, 40000, "메모")
            assertEquals(4, db.tireDao().getMountedTiresForCarDirect(car.id).size)
            assertTrue(db.tireDao().getMountedTiresForCarDirect(car.id).all { it.season == "WINTER" })
            assertTrue(db.tireDao().getTiresForCarDirect(car.id).filter { it.season == "SUMMER" }.all {
                !it.isMounted && it.cumulativeMileage == 2000 && it.storageLocation == "창고"
            })
            try {
                repo.swapSeasonTires(car, "WINTER", 10000, "2026-10-20", null, null, null, 0, null)
                fail("stale duplicate must be rejected")
            } catch (_: IllegalArgumentException) { }
            assertEquals(8, db.tireDao().getTiresForCarDirect(car.id).size)
            repo.swapSeasonTires(car, "SUMMER", 12000, "2026-11-20", null, null, null, 0, null)
            assertEquals(4, db.tireDao().getMountedTiresForCarDirect(car.id).size)
            assertTrue(db.tireDao().getMountedTiresForCarDirect(car.id).all { it.season == "SUMMER" })
        } finally { db.close() }
    }

    @Test fun startupRepairsHybridWithoutReplacingHistoryOrDisabledSettings() = runBlocking {
        val db = database()
        try {
            val car = CarEntity(id = "hybrid", carNumber = "test", modelName = "Hybrid", powertrain = PowertrainType.PHEV_GASOLINE)
            db.carDao().insertCar(car)
            val oil = MaintenanceRuleEntity(carId = car.id, itemCode = "ENGINE_OIL", intervalKm = 7777,
                lastServiceMileage = 5000, lastServiceDate = "2026-01-01", isEnabled = false)
            db.maintenanceRuleDao().insertRules(listOf(oil))
            val seeder = DatabaseSeeder(db)
            seeder.seedIfNeeded()
            seeder.seedIfNeeded()
            assertEquals(oil, db.maintenanceRuleDao().getRuleByItem(car.id, "ENGINE_OIL"))
            val rules = db.maintenanceRuleDao().getAllRulesForCarDirect(car.id)
            assertTrue(rules.any { it.itemCode == "ENGINE_AIR_FILTER" })
            assertTrue(rules.any { it.itemCode == "TRANSMISSION_OIL" })
            assertEquals(rules.size, rules.map { it.itemCode }.distinct().size)
        } finally { db.close() }
    }
}
