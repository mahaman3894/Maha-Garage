package maha.garage.domain.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object InsuranceNotificationPolicy {
    data class Alert(val daysRemaining: Long, val stage: String)

    /** Null means any previously displayed insurance warning must be cancelled. */
    fun evaluate(endDate: String?, today: LocalDate): Alert? {
        val date = RenewalDatePolicy.parse(endDate?.trim()) ?: return null
        val days = ChronoUnit.DAYS.between(today, date)
        if (days > 30) return null
        return Alert(days, when {
            days < 0 -> "OVERDUE"
            days == 0L -> "D_DAY"
            days <= 7 -> "D_7"
            else -> "D_30"
        })
    }
}
