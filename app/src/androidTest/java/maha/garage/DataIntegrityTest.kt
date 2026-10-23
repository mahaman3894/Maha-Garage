package maha.garage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.runBlocking
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.*
import maha.garage.data.repository.*
import maha.garage.data.model.enums.ActionType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataIntegrityTest {
    @Test fun migrateV2ToV9PreservesVehicleAndValidatesRoomSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-regression.db"
        context.deleteDatabase(name)
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets.open("v2-schema.sql").bufferedReader().use { it.readText() }
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            fixture.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEach { old.execSQL(it) }
            old.execSQL("INSERT INTO cars (id,carNumber,modelName,make,modelYear,powertrain,vehicleClass,currentMileage,currentTireMode,summerTireMileage,winterTireMileage,lastSwapMileage,isSevereCondition,isArchived,updatedAt) VALUES ('a','A','A','',2020,'GASOLINE','SEDAN',12345,'SUMMER',0,0,0,0,0,0)")
        }
        val upgraded = Room.databaseBuilder(context, MahaGarageDatabase::class.java, name)
            .addMigrations(MahaGarageDatabase.MIGRATION_1_2, MahaGarageDatabase.MIGRATION_2_3,
                MahaGarageDatabase.MIGRATION_3_4, MahaGarageDatabase.MIGRATION_4_5, MahaGarageDatabase.MIGRATION_5_6,
                MahaGarageDatabase.MIGRATION_6_7, MahaGarageDatabase.MIGRATION_7_8, MahaGarageDatabase.MIGRATION_8_9).build()
        try {
            upgraded.openHelper.writableDatabase.query("SELECT currentMileage, firstRegistrationDate FROM cars WHERE id='a'").use {
                assertTrue(it.moveToFirst())
                assertEquals(12345, it.getInt(0))
                assertTrue(it.isNull(1))
            }
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
    @Test fun editingCarPreservesChildrenAndInspectionPreservesReplacement() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MahaGarageDatabase::class.java).build()
        try {
            val vehicles = VehicleRepositoryImpl(db)
            val maintenance = MaintenanceRepositoryImpl(db)
            val car = CarEntity(id = "a", carNumber = "A", modelName = "A")
            vehicles.saveVehicle(car)
            val v = ServiceVisitEntity(carId = "a", serviceDate = "2026-01-01", odometerKm = 10000)
            maintenance.recordServiceVisit(v, listOf(ServiceLineEntity(visitId = v.id, itemCode = "ENGINE_OIL")))
            vehicles.saveVehicle(car.copy(currentMileage = 10000, nickname = "edited", isSevereCondition = true))
            assertEquals(1, db.serviceVisitDao().getVisitsForCarDirect("a").size)
            assertEquals(10000, db.maintenanceRuleDao().getRuleByItem("a", "ENGINE_OIL")!!.lastServiceMileage)
            val inspection = ServiceVisitEntity(carId = "a", serviceDate = "2026-02-01", odometerKm = 12000)
            maintenance.recordServiceVisit(inspection, listOf(ServiceLineEntity(visitId = inspection.id, itemCode = "ENGINE_OIL", actionType = ActionType.INSPECT)))
            assertEquals(10000, db.maintenanceRuleDao().getRuleByItem("a", "ENGINE_OIL")!!.lastServiceMileage)
            assertEquals(12000, db.carDao().getCarByIdDirect("a")!!.currentMileage)
        } finally { db.close() }
    }
    @Test fun invalidLineDoesNotPartiallySaveVisit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MahaGarageDatabase::class.java).build()
        try {
            val car = CarEntity(id = "a", carNumber = "A", modelName = "A")
            VehicleRepositoryImpl(db).saveVehicle(car)
            val v = ServiceVisitEntity(carId = "a", serviceDate = "2026-01-01", odometerKm = 10000)
            try {
                MaintenanceRepositoryImpl(db).recordServiceVisit(v, listOf(ServiceLineEntity(visitId = "wrong-car-visit", itemCode = "ENGINE_OIL")))
                fail("invalid visit must fail")
            } catch (_: IllegalArgumentException) { }
            assertTrue(db.serviceVisitDao().getVisitsForCarDirect("a").isEmpty())
            assertEquals(0, db.carDao().getCarByIdDirect("a")!!.currentMileage)
        } finally { db.close() }
    }
}
