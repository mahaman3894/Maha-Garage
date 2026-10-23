package maha.garage

import maha.garage.domain.engine.InsuranceNotificationPolicy as Policy
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class InsuranceNotificationPolicyTest {
    private val today = LocalDate.of(2026, 10, 20)
    @Test fun deletedDateCancelsPreviousAlert() {
        assertNotNull(Policy.evaluate(today.minusDays(1).toString(), today))
        assertNull(Policy.evaluate(null, today))
        assertNull(Policy.evaluate("", today))
    }
    @Test fun invalidDateCancelsPreviousAlert() {
        assertNull(Policy.evaluate("2026-99-99", today))
    }
    @Test fun renewalBeyondThirtyDaysCancelsPreviousAlert() {
        assertNull(Policy.evaluate(today.plusDays(31).toString(), today))
    }
    @Test fun thirtyDayBoundary() {
        assertEquals("D_30", Policy.evaluate(today.plusDays(30).toString(), today)!!.stage)
        assertEquals("D_30", Policy.evaluate(today.plusDays(8).toString(), today)!!.stage)
    }
    @Test fun sevenDayAndExpiryBoundaries() {
        assertEquals("D_7", Policy.evaluate(today.plusDays(7).toString(), today)!!.stage)
        assertEquals("D_7", Policy.evaluate(today.plusDays(1).toString(), today)!!.stage)
        assertEquals("D_DAY", Policy.evaluate(today.toString(), today)!!.stage)
        assertEquals("OVERDUE", Policy.evaluate(today.minusDays(1).toString(), today)!!.stage)
    }
}
