package maha.garage

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.vehicle.TireSwapDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TireSwapDialogRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun movingCardsStillConfirmsOppositeSeasonAndWaitsForSave() {
        var target: String? = null
        var dismissed = false
        compose.setContent {
            TireSwapDialog(CarEntity(carNumber = "test", modelName = "Test"),
                onDismiss = { dismissed = true },
                onConfirmSwap = { mode, _, _, _, _, _, _ -> target = mode })
        }
        compose.onNodeWithText("터치하여 타이어 표시 위치 변경 ⇄").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("스왑 확정 및 기록").performClick()
        compose.runOnIdle {
            assertEquals("WINTER", target)
            assertFalse(dismissed)
        }
    }

    @Test fun savingPreventsDuplicateConfirmation() {
        compose.setContent {
            TireSwapDialog(CarEntity(carNumber = "test", modelName = "Test"),
                onDismiss = {}, isSaving = true,
                onConfirmSwap = { _, _, _, _, _, _, _ -> fail("must be disabled") })
        }
        compose.onNodeWithText("저장 중…").assertIsNotEnabled()
        compose.onNodeWithText("취소").assertIsNotEnabled()
    }
}
