package maha.garage

import maha.garage.util.OdometerParser
import org.junit.Assert.*
import org.junit.Test

class OdometerParserTest {

    @Test
    fun testStandardOdoPattern() {
        val raw = """
            ECO
            ODO 123456 km
            80 km/h
        """.trimIndent()
        assertEquals(123456, OdometerParser.parse(raw))
    }

    @Test
    fun testOdoWithColonAndComma() {
        val raw = """
            D 12:35
            ODO: 85,210 km
            Trip A 250.4 km
        """.trimIndent()
        assertEquals(85210, OdometerParser.parse(raw))
    }

    @Test
    fun testTotalKeywordPattern() {
        val raw = """
            READY
            TOTAL 65432 km
        """.trimIndent()
        assertEquals(65432, OdometerParser.parse(raw))
    }

    @Test
    fun testMultiLineOdo() {
        val raw = """
            ODO
            154,200 km
        """.trimIndent()
        assertEquals(154200, OdometerParser.parse(raw))
    }

    @Test
    fun testTripFiltering() {
        // Trip meter with decimal point must be ignored, ODO must be picked
        val raw = """
            TRIP A 324.5 km
            105,420 km
        """.trimIndent()
        assertEquals(105420, OdometerParser.parse(raw))
    }

    @Test
    fun testStandalone5DigitKm() {
        val raw = """
            OUTSIDE 24°C
            78912 km
        """.trimIndent()
        assertEquals(78912, OdometerParser.parse(raw))
    }

    @Test
    fun testSpeedExclusion() {
        val raw = """
            100 km/h
            ODO 45120
        """.trimIndent()
        assertEquals(45120, OdometerParser.parse(raw))
    }
}
