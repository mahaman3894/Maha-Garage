package maha.garage

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import maha.garage.ui.tutorial.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class TutorialWalkthroughTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allFiveChaptersReachActualControlsAndFinish() {
        val exited = AtomicBoolean(false)
        val app = ApplicationProvider.getApplicationContext<MahaGarageApp>()
        compose.setContent { TutorialExperience(app) { exited.set(true) } }
        TutorialCues.forEachIndexed { index, cue ->
            compose.waitUntil(20000) {
                compose.onAllNodesWithTag("tutorial-cue-${cue.target}").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithTag("tutorial-next").fetchSemanticsNodes().any {
                        !it.config.contains(SemanticsProperties.Disabled)
                    }
            }
            compose.onNodeWithTag("tutorial-cue-${cue.target}").assertIsDisplayed()
            if (index % 2 == 0) compose.onNodeWithTag("tutorial-cue-${cue.target}").performTouchInput { click(center) }
            else compose.onNodeWithTag("tutorial-next").assertIsDisplayed().performClick()
        }
        compose.waitUntil(10000) { exited.get() }
        assertTrue(exited.get())
    }
}
