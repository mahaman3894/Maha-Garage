package maha.garage.domain.engine

/** Independent of Play/Android so failure and restoration paths can be tested. */
object UpdateGatePolicy {
    enum class Availability { NONE, AVAILABLE, IN_PROGRESS, UNKNOWN }
    enum class Action { ALLOW, START, REQUIRE_STORE, RETRY }
    data class Decision(val action: Action, val target: Long)

    fun restoredTarget(installed: Long, saved: Long): Long = saved.takeIf { it > installed } ?: 0L

    fun decide(
        installed: Long,
        saved: Long,
        available: Long,
        availability: Availability,
        immediateAllowed: Boolean
    ): Decision {
        val retained = restoredTarget(installed, saved)
        return when (availability) {
            // A successful explicit no-update response also permits a withdrawn release.
            Availability.NONE -> Decision(Action.ALLOW, 0)
            Availability.UNKNOWN -> failed(installed, retained)
            Availability.AVAILABLE, Availability.IN_PROGRESS -> {
                val target = maxOf(retained, available)
                if (target <= installed) failed(installed, retained)
                else if (availability == Availability.IN_PROGRESS || immediateAllowed) Decision(Action.START, target)
                else Decision(Action.REQUIRE_STORE, target)
            }
        }
    }

    fun failed(installed: Long, saved: Long): Decision {
        val target = restoredTarget(installed, saved)
        return Decision(if (target > 0) Action.RETRY else Action.ALLOW, target)
    }
}
