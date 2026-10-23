package maha.garage

import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.*
import maha.garage.domain.engine.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.Base64
import java.util.Random

class PracticalMaintenanceFixTest {
    private val today = LocalDate.of(2026, 10, 18)
    private val car = CarEntity(id="c",carNumber="A",modelName="Test",currentMileage=110000,engineHours=200.0)
    private val rule = MaintenanceRuleEntity(carId="c",itemCode="ENGINE_OIL", intervalKm=10000,intervalMonths=12,intervalHours=100.0,
        baselineMileage=100000,baselineDate="2026-01-01",baselineHours=100.0,lastServiceDate="2026-08-01",lastServiceMileage=105000)
    @Test fun rearMarkerTargetsOnlyRear() {
        assertEquals(listOf("BRAKE_PAD_REAR"), MaintenanceInputPolicy.padCodes(MaintenanceInputPolicy.initialPadPosition("BRAKE_PAD_REAR")))
        assertEquals(listOf("BRAKE_PAD_FRONT"), MaintenanceInputPolicy.padCodes(MaintenanceInputPolicy.initialPadPosition("BRAKE_PAD_FRONT")))
        assertEquals(2,MaintenanceInputPolicy.padCodes("BOTH").size)
    }
    @Test fun settingsPreserveHistoryAndUneditedHours() {
        val result=MaintenanceInputPolicy.applySettings(rule,rule.copy(intervalKm=15000,intervalMonths=6),car,today)
        assertEquals(105000,result.lastServiceMileage);assertEquals("2026-08-01",result.lastServiceDate)
        assertEquals(100.0,result.intervalHours);assertEquals(100.0,result.baselineHours);assertEquals(rule.id,result.id)
    }
    @Test fun distanceCriterionCanBeDisabled() {
        val result=MaintenanceInputPolicy.applySettings(rule,rule.copy(intervalKm=null),car,today)
        assertNull(result.intervalKm);assertEquals(12,result.intervalMonths)
    }
    @Test fun dateOnlyAndDistanceOnlyAreAllowed() {
        assertNull(MaintenanceInputPolicy.applySettings(rule,rule.copy(intervalKm=null,intervalHours=null),car,today).intervalKm)
        assertNull(MaintenanceInputPolicy.applySettings(rule,rule.copy(intervalMonths=null,intervalHours=null),car,today).intervalMonths)
    }
    @Test fun clearingBaselineDoesNotClearHistoryOrHoursInterval() {
        val result=MaintenanceInputPolicy.applySettings(rule,rule.copy(baselineDate=null,baselineMileage=null,baselineHours=null),car,today)
        assertNull(result.baselineHours);assertEquals(100.0,result.intervalHours);assertEquals(105000,result.lastServiceMileage)
    }
    @Test(expected=IllegalArgumentException::class) fun noCriteriaRejected() { MaintenanceInputPolicy.applySettings(rule,rule.copy(intervalKm=null,intervalMonths=null,intervalHours=null),car,today) }
    @Test(expected=IllegalArgumentException::class) fun futureBaselineRejected() { MaintenanceInputPolicy.applySettings(rule,rule.copy(baselineDate="2026-10-19"),car,today) }
    @Test(expected=IllegalArgumentException::class) fun mileageBeyondCarRejected() { MaintenanceInputPolicy.applySettings(rule,rule.copy(baselineMileage=110001),car,today) }
    @Test(expected=IllegalArgumentException::class) fun invalidServiceDateRejected() { MaintenanceInputPolicy.validateService("2026-99-99","110000",listOf(""),today) }
    @Test(expected=IllegalArgumentException::class) fun futureServiceRejected() { MaintenanceInputPolicy.validateService("2026-10-19","110000",listOf(""),today) }
    @Test(expected=IllegalArgumentException::class) fun overflowingMileageIsNotSilentlyReplaced() { MaintenanceInputPolicy.validateService("2026-10-18","999999999999",listOf(""),today) }
    @Test(expected=IllegalArgumentException::class) fun invalidCostIsNotSilentlyZero() { MaintenanceInputPolicy.cost("999999999999999999999") }
    @Test fun blankCostAndZeroHaveNoInventedCharge() { assertEquals(0L,MaintenanceInputPolicy.cost(""));assertEquals(0L,MaintenanceInputPolicy.cost("0")) }
    @Test fun measuredWarningDoesNotDisappearWhenBaselineAdded() {
        val item=MaintenanceCatalogEntity(itemCode="BRAKE_PAD_FRONT",displayName="Pad",category="brake",isConditionBased=true)
        val r=MaintenanceRuleEntity(carId="c",itemCode=item.itemCode,intervalKm=40000,lastConditionValue="4mm")
        val without=MaintenanceScheduleEngine.evaluateItem(car,r,item,today)
        val with=MaintenanceScheduleEngine.evaluateItem(car,r.copy(baselineMileage=109000),item,today)
        assertEquals(DueStatus.DUE_SOON,without.status);assertEquals(without.status,with.status)
    }
    @Test fun tireArchiveRoundTripPreservesEveryField() {
        val tire=TireEntity(id="t",carId="c",position="STORAGE",season="WINTER",setId="set2",assignedWheelPosition="RR",
            brand="brand",productName="name",spec="245",dot="2424",cumulativeMileage=12345,installedAtMileage=90000,
            installDate="2025-11-01",treadDepthMm=7.5,storageLocation="shop",isMounted=false,purchaseCost=100000,notes="note",updatedAt=123L)
        assertEquals(tire,TireArchiveCodec.decode(JSONObject(TireArchiveCodec.encode(tire).toString())))
    }
    @Test fun oldTireArchiveWithoutNewFieldsStillReadable() {
        val tire=TireEntity(id="t",carId="c",position="FL")
        val json=TireArchiveCodec.encode(tire);json.remove("setId");json.remove("assignedWheelPosition")
        assertNull(TireArchiveCodec.decode(json).setId);assertNull(TireArchiveCodec.decode(json).assignedWheelPosition)
    }
    @Test fun multiMegabyteTransferRoundTripUsesSmallDocuments() {
        val bytes=ByteArray(2*1024*1024);Random(42).nextBytes(bytes)
        val raw=JSONObject().put("cars",Base64.getEncoder().encodeToString(bytes)).put("memo","차량 정비 기록").toString()
        val encoded=TransferPayloadCodec.encode(raw)
        assertTrue(encoded.parts.size>1);assertTrue(encoded.parts.all { it.toByteArray().size<=160000 })
        assertEquals(raw,TransferPayloadCodec.decode(encoded.parts,encoded.checksum))
    }
    @Test(expected=IllegalArgumentException::class) fun transferChecksumMismatchRejected() {
        val e=TransferPayloadCodec.encode("hello");TransferPayloadCodec.decode(e.parts,"incorrect")
    }
    @Test fun optionalCriterionBlankIsExplicitlyNull() { assertNull(MaintenanceInputPolicy.optionalPositive("",240)) }
    @Test(expected=IllegalArgumentException::class) fun optionalCriterionOverflowRejected() { MaintenanceInputPolicy.optionalPositive("999999999999",240) }
}
