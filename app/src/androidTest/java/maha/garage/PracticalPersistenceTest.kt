package maha.garage

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.*
import maha.garage.data.repository.MaintenanceRepositoryImpl
import maha.garage.data.model.enums.ActionType
import maha.garage.util.BackupRestoreManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PracticalPersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun db() = Room.inMemoryDatabaseBuilder(context, MahaGarageDatabase::class.java).build()
    @Test fun bothAxlesSettingsAreAtomicOnValidationFailure() = runBlocking {
        val db=db()
        try {
            val car=CarEntity(id="c",carNumber="A",modelName="test",currentMileage=10000)
            db.carDao().insertCar(car)
            val front=MaintenanceRuleEntity(carId="c",itemCode="BRAKE_PAD_FRONT",intervalKm=40000)
            val rear=MaintenanceRuleEntity(carId="c",itemCode="BRAKE_PAD_REAR",intervalKm=70000)
            db.maintenanceRuleDao().insertRules(listOf(front,rear))
            db.catalogDao().insertAll(listOf(MaintenanceCatalogEntity(itemCode=front.itemCode,displayName="front",category="brake"),MaintenanceCatalogEntity(itemCode=rear.itemCode,displayName="rear",category="brake")))
            try {
                MaintenanceRepositoryImpl(db).updateCustomRules(listOf(front.copy(intervalKm=20000),rear.copy(baselineMileage=10001)))
                fail("invalid rear must reject the entire change")
            } catch (_:IllegalArgumentException) { }
            assertEquals(40000,db.maintenanceRuleDao().getRuleByItem("c",front.itemCode)!!.intervalKm)
            assertEquals(70000,db.maintenanceRuleDao().getRuleByItem("c",rear.itemCode)!!.intervalKm)
        } finally { db.close() }
    }
    @Test fun rearReplacementDoesNotResetFrontHistory() = runBlocking {
        val db=db()
        try {
            val car=CarEntity(id="c",carNumber="A",modelName="test",currentMileage=10000)
            db.carDao().insertCar(car)
            val front=MaintenanceRuleEntity(carId="c",itemCode="BRAKE_PAD_FRONT",intervalKm=40000,lastServiceMileage=5000,lastServiceDate="2026-01-01")
            val rear=MaintenanceRuleEntity(carId="c",itemCode="BRAKE_PAD_REAR",intervalKm=70000)
            db.maintenanceRuleDao().insertRules(listOf(front,rear))
            db.catalogDao().insertAll(listOf(MaintenanceCatalogEntity(itemCode=front.itemCode,displayName="front",category="brake"),MaintenanceCatalogEntity(itemCode=rear.itemCode,displayName="rear",category="brake")))
            val visit=ServiceVisitEntity(carId="c",serviceDate=LocalDate.now().toString(),odometerKm=10000)
            MaintenanceRepositoryImpl(db).recordServiceVisit(visit,listOf(ServiceLineEntity(visitId=visit.id,itemCode=rear.itemCode,actionType=ActionType.REPLACE)))
            assertEquals(5000,db.maintenanceRuleDao().getRuleByItem("c",front.itemCode)!!.lastServiceMileage)
            assertEquals(10000,db.maintenanceRuleDao().getRuleByItem("c",rear.itemCode)!!.lastServiceMileage)
        } finally { db.close() }
    }
    @Test fun actualBackupRestorePreservesTiresAndPhotoBytes() = runBlocking {
        val source=db();val target=db()
        val photo=File(context.cacheDir,"test-photo-${System.nanoTime()}.jpg").apply { writeBytes(byteArrayOf(1,2,3,4,5)) }
        var restoredPhoto:File?=null
        try {
            source.carDao().insertCar(CarEntity(id="c",carNumber="A",modelName="test",photoUri=photo.absolutePath))
            val tire=TireEntity(id="t",carId="c",position="STORAGE",isMounted=false,setId="winter2",assignedWheelPosition="RR",updatedAt=42)
            source.tireDao().insertTires(listOf(tire))
            val json=BackupRestoreManager.generateBackupJson(source,context)
            assertEquals(1,json.getJSONArray("attachments").length())
            BackupRestoreManager.importBackup(context,target,json.toString()).getOrThrow()
            val path=target.carDao().getCarByIdDirect("c")!!.photoUri!!
            restoredPhoto=File(path)
            assertNotEquals(photo.absolutePath,path)
            assertArrayEquals(photo.readBytes(),restoredPhoto!!.readBytes())
            assertEquals(tire,target.tireDao().getAllTiresDirect().single())
        } finally { source.close();target.close();photo.delete();restoredPhoto?.delete() }
    }
}
