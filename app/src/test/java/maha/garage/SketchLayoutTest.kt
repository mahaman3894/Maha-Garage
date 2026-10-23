package maha.garage

import maha.garage.ui.components.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max

class SketchLayoutTest {
    @Test fun calloutsStayInsideAndLeadersAvoidOtherBoxesAcrossWidthsAndFonts() {
        for (w in listOf(240f, 264f, 304f, 360f, 460f, 680f, 1000f)) {
            for (font in listOf(1f, 1.3f, 1.5f, 2f)) for (diff in listOf(false, true)) {
                val h = sketchDiagramHeight(w, font, 390f)
                val layout = calculateSketchLayout(w, h, font, false, diff)
                layout.callouts.forEach { m ->
                    val b = m.box
                    assertTrue("${m.code} is clipped at $w/$font", b.x >= 0 && b.y >= 0 && b.x+b.width <= w+.01f && b.y+b.height <= h+.01f)
                    assertTrue(m.anchor.x in layout.car.x..(layout.car.x+layout.car.width))
                    assertTrue(m.anchor.y in layout.car.y..(layout.car.y+layout.car.height))
                    assertTrue("leader starts on box boundary", m.start.x == b.x || m.start.x == b.x+b.width || m.start.y == b.y || m.start.y == b.y+b.height)
                    layout.callouts.filter { it != m }.forEach { other ->
                        val o = other.box
                        assertFalse("boxes overlap", b.x < o.x+o.width && b.x+b.width > o.x && b.y < o.y+o.height && b.y+b.height > o.y)
                        // Sample the visible segment, excluding its own box boundary.
                        (listOf(m.start) + m.bends + m.anchor).zipWithNext().forEach { (start, end) ->
                          for (i in 1..100) {
                            val x = start.x + (end.x-start.x)*i/100
                            val y = start.y + (end.y-start.y)*i/100
                            assertFalse("${m.code} crosses ${other.code} at $w/$font", x > o.x && x < o.x+o.width && y > o.y && y < o.y+o.height)
                          }
                        }
                    }
                }
            }
        }
    }
    @Test fun approvedOrderAndElectricApplicabilityArePreserved() {
        val layout = calculateSketchLayout(800f, 550f, 1f, false, false)
        assertEquals(listOf("ENGINE_OIL", "ENGINE_AIR_FILTER", "WIPER_BLADE", "CABIN_FILTER"), layout.callouts.take(4).map { it.code })
        assertEquals(listOf("BATTERY_12V", "BRAKE_PAD_FRONT", "TRANSMISSION_OIL", "BRAKE_PAD_REAR"), layout.callouts.takeLast(4).map { it.code })
        val ev = calculateSketchLayout(300f, 450f, 1f, true, false).callouts.map { it.code }
        assertFalse(ev.contains("ENGINE_OIL"))
        assertTrue(ev.contains("EV_COOLANT"))
        assertTrue(ev.contains("REDUCTION_GEAR_OIL"))
    }
}
