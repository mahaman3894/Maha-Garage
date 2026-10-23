package maha.garage

import maha.garage.domain.engine.*
import maha.garage.data.local.entity.CarEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RenewalDatePolicyTest {
    @Test fun beforeWarningWindowIsNormal() { assertEquals(RenewalDateState.NORMAL,RenewalDatePolicy.evaluate("2026-11-18",LocalDate.of(2026,10,17)).state) }
    @Test fun exactlyOneCalendarMonthBeforeIsWarning() { assertEquals(RenewalDateState.WARNING,RenewalDatePolicy.evaluate("2026-11-18",LocalDate.of(2026,10,18)).state) }
    @Test fun oneDayBeforeIsWarning() { assertEquals(RenewalDateState.WARNING,RenewalDatePolicy.evaluate("2026-10-19",LocalDate.of(2026,10,18)).state) }
    @Test fun dueDateIsWarningAndTodayBadge() { val e=RenewalDatePolicy.evaluate("2026-10-18",LocalDate.of(2026,10,18));assertEquals(RenewalDateState.WARNING,e.state);assertEquals("오늘 만료",e.badge) }
    @Test fun dayAfterExpiryIsRed() { val e=RenewalDatePolicy.evaluate("2026-10-17",LocalDate.of(2026,10,18));assertEquals(RenewalDateState.EXPIRED,e.state);assertEquals("1일 지남",e.badge) }
    @Test fun endOfMonthUsesCalendarMonth() { assertEquals(RenewalDateState.NORMAL,RenewalDatePolicy.evaluate("2026-03-31",LocalDate.of(2026,2,27)).state);assertEquals(RenewalDateState.WARNING,RenewalDatePolicy.evaluate("2026-03-31",LocalDate.of(2026,2,28)).state) }
    @Test fun leapMonthBoundary() { assertEquals(RenewalDateState.WARNING,RenewalDatePolicy.evaluate("2024-03-31",LocalDate.of(2024,2,29)).state) }
    @Test fun invalidOrMissingIsNeutral() { for (v in listOf(null,"","2026-02-30")) {assertEquals(RenewalDateState.MISSING,RenewalDatePolicy.evaluate(v).state);assertEquals("미등록",RenewalDatePolicy.display(v))} }
    @Test fun companyAloneDoesNotInventDates() {
        val c=CarEntity(carNumber="A",modelName="test",insuranceCompany="보험사")
        val e=VehicleComplianceCalculator.evaluateInsurance(c,LocalDate.of(2026,10,18))
        assertEquals("-",e.startDateStr);assertEquals("-",e.endDateStr);assertFalse(e.isExpired)
        assertEquals(RenewalDateState.MISSING,RenewalDatePolicy.insurance(c).state)
    }
    @Test fun retestDeadlineOverridesRegularInspectionExpiry() {
        val c=CarEntity(carNumber="A",modelName="test",inspectionEndDate="2027-10-18",retestDeadline="2026-10-17")
        assertEquals(RenewalDateState.EXPIRED,RenewalDatePolicy.inspection(c,LocalDate.of(2026,10,18)).state)
    }

    @Test fun calculateInitialInspection_privatePassengerCar_adds4YearsAnd31DaysWindow() {
        // 2026-05-10 신차 등록 -> 2030-05-10 만료, 2030-04-09 ~ 2030-06-10
        val period = RenewalDatePolicy.calculateInitialInspection(
            registrationDateStr = "2026-05-10",
            isCommercial = false,
            vehicleClass = maha.garage.data.model.enums.VehicleClass.SEDAN,
            modelName = "아반떼"
        )
        assertNotNull(period)
        assertEquals(LocalDate.of(2030, 5, 10), period!!.baseExpiryDate)
        assertEquals(LocalDate.of(2030, 4, 9), period.validStartDate)
        assertEquals(LocalDate.of(2030, 6, 10), period.validEndDate)
        assertTrue(period.cycleDescription.contains("4년"))
    }

    @Test fun calculateInitialInspection_truckOrVan_adds1Year() {
        val period = RenewalDatePolicy.calculateInitialInspection(
            registrationDateStr = "2026-05-10",
            isCommercial = false,
            vehicleClass = maha.garage.data.model.enums.VehicleClass.LIGHT_TRUCK,
            modelName = "포터2"
        )
        assertNotNull(period)
        assertEquals(LocalDate.of(2027, 5, 10), period!!.baseExpiryDate)
        assertTrue(period.cycleDescription.contains("1년"))
    }

    @Test fun calculateInitialInspection_commercialPassengerCar_adds2Years() {
        val period = RenewalDatePolicy.calculateInitialInspection(
            registrationDateStr = "2026-05-10",
            isCommercial = true,
            vehicleClass = maha.garage.data.model.enums.VehicleClass.SEDAN,
            modelName = "그랜저 택시"
        )
        assertNotNull(period)
        assertEquals(LocalDate.of(2028, 5, 10), period!!.baseExpiryDate)
        assertTrue(period.cycleDescription.contains("2년"))
    }
}
