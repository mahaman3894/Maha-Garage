package maha.garage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import maha.garage.ui.tutorial.tutorialLocalRect
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialCoordinatesTest {
    @Test fun nativeDialogInsetsDoNotDisplacePointerTip() {
        val target = Rect(160f, 380f, 460f, 460f)
        val local = tutorialLocalRect(target, Offset(20f, 64f))
        assertEquals(Offset(290f, 356f), local.center)
        assertEquals(300f, local.width)
        assertEquals(80f, local.height)
    }

    @Test fun orientationAndDensityUsePhysicalCoordinatesWithoutDoubleScaling() {
        for (scale in listOf(1f, 1.5f, 2.75f, 3.5f)) {
            val screen = Rect(90f * scale, 170f * scale, 230f * scale, 218f * scale)
            val origin = Offset(12f * scale, 24f * scale)
            val local = tutorialLocalRect(screen, origin)
            assertEquals(screen.center, local.center + origin)
            assertEquals(screen.size, local.size)
        }
    }
}
