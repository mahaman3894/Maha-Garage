package maha.garage

import maha.garage.domain.engine.UpdateGatePolicy as Policy
import org.junit.Assert.*
import org.junit.Test

class UpdateGatePolicyTest {
    @Test fun failedQueryAfterProcessRecreationKeepsStoredRequirement() {
        val restored = Policy.restoredTarget(4, 5)
        val result = Policy.failed(4, restored)
        assertEquals(Policy.Action.RETRY, result.action)
        assertEquals(5L, result.target)
    }
    @Test fun offlineWithoutAnyConfirmedRequirementAllowsLocalData() {
        assertEquals(Policy.Action.ALLOW, Policy.failed(4, 0).action)
    }
    @Test fun updatedInstallClearsOldRequirement() {
        assertEquals(0L, Policy.restoredTarget(5, 5))
        assertEquals(Policy.Action.ALLOW, Policy.failed(6, 5).action)
    }
    @Test fun availableButImmediateDisallowedRequiresStore() {
        val result = Policy.decide(4, 0, 5, Policy.Availability.AVAILABLE, false)
        assertEquals(Policy.Action.REQUIRE_STORE, result.action)
        assertEquals(5L, result.target)
    }
    @Test fun immediateAllowedStartsFlow() {
        assertEquals(Policy.Action.START, Policy.decide(4, 0, 5, Policy.Availability.AVAILABLE, true).action)
    }
    @Test fun interruptedFlowCanBeResumed() {
        assertEquals(Policy.Action.START, Policy.decide(4, 5, 5, Policy.Availability.IN_PROGRESS, false).action)
    }
    @Test fun unknownDoesNotClearConfirmedRequirement() {
        assertEquals(Policy.Action.RETRY, Policy.decide(4, 5, 0, Policy.Availability.UNKNOWN, false).action)
    }
    @Test fun explicitNoUpdateReleasesWithdrawnVersion() {
        val result = Policy.decide(4, 5, 0, Policy.Availability.NONE, false)
        assertEquals(Policy.Action.ALLOW, result.action)
        assertEquals(0L, result.target)
    }
    @Test fun invalidAvailableVersionDoesNotDiscardRequirement() {
        assertEquals(5L, Policy.decide(4, 5, 0, Policy.Availability.AVAILABLE, false).target)
    }
}
