package maha.garage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import maha.garage.ui.tutorial.*
import org.junit.Assert.*
import org.junit.Test

class TutorialPersistenceTest {
    @Test fun completionAndSkipSurviveNewPreferenceInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "tutorial-test-${System.nanoTime()}"
        try {
            assertFalse(TutorialPreferences(context, name).completed)
            TutorialPreferences(context, name).finish()
            assertTrue(TutorialPreferences(context, name).completed)
            // A replay session does not reset the persistent completion flag.
            TutorialSession.create(context).close()
            assertTrue(TutorialPreferences(context, name).completed)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun realRepositoriesInTutorialNeverWriteToTheRealGarage() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MahaGarageApp>()
        suspend fun snapshot() = withContext(Dispatchers.IO) {
            app.database.openHelper.readableDatabase.query(
                "SELECT id || ':' || currentMileage FROM cars ORDER BY id"
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } } +
            listOf("fuel_records", "service_visits", "service_lines", "tires").map { table ->
                app.database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
                    it.moveToFirst(); "$table:${it.getInt(0)}"
                }
            }
        }
        val before = snapshot()
        val session = TutorialSession.create(app)
        try {
            session.vehicles.saveVehicle(TutorialFixtures.car.copy(currentMileage = 50600))
            session.fuel.addFuelRecord(TutorialFixtures.CAR_ID, 50600, 50.0, 1700, 85000, true, false, "예시 주유소")
            val records = session.database.fuelRecordDao().getRecordsForVehicleAsc(TutorialFixtures.CAR_ID)
            assertEquals(2, records.size)
            assertEquals(12.0, records.last().calculatedEconomy!!, 0.0001)
            assertEquals(before, snapshot())
        } finally { session.close() }
        val fresh = TutorialSession.create(app)
        try { assertEquals(1, fresh.database.fuelRecordDao().getRecordsForVehicleAsc(TutorialFixtures.CAR_ID).size) }
        finally { fresh.close() }
        assertEquals(before, snapshot())
    }

    @Test fun rewindAndRotationRestoreDeterministicFuelExamples() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saveIndex = TutorialCues.indexOfFirst { it.target == "fuel.save" }
        val summaryIndex = TutorialCues.indexOfFirst { it.target == "fuel.summary" }
        val registrationIndex = TutorialCues.indexOfFirst { it.target == "vehicle.save" }
        val mileageSaveIndex = TutorialCues.indexOfFirst { it.target == "mileage.save" }
        for ((index, count) in listOf(saveIndex to 1, summaryIndex to 2, saveIndex to 1, registrationIndex to 1)) {
            val session = TutorialSession.create(context, index)
            try {
                val records = session.database.fuelRecordDao().getRecordsForVehicleAsc(TutorialFixtures.CAR_ID)
                assertEquals(count, records.size)
                if (count == 2) assertEquals(12.0, records.last().calculatedEconomy!!, 0.0001)
                assertEquals(if (index > mileageSaveIndex) 50600 else 50000,
                    session.database.carDao().getCarByIdDirect(TutorialFixtures.CAR_ID)!!.currentMileage)
            } finally { session.close() }
        }
    }
}
