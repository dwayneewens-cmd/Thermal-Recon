package com.example

import com.example.model.LabScenario
import com.example.model.RoiBox
import com.example.model.TemperatureUnit
import com.example.model.ThermalPalette
import com.example.thermal.ThermalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test temperature unit conversion`() {
        val celsius = 100.0f
        val fahrenheit = TemperatureUnit.FAHRENHEIT.fromCelsius(celsius)
        assertEquals(212.0f, fahrenheit, 0.01f)
        assertEquals("100.0°C", TemperatureUnit.CELSIUS.format(celsius))
    }

    @Test
    fun `test thermal engine scenario generation and spot tracking`() {
        val engine = ThermalEngine()
        val frame = engine.generateScenarioFrame(LabScenario.SEARCH_AND_RESCUE, RoiBox())
        assertNotNull(frame)
        assertTrue(frame.temperatures.isNotEmpty())
        assertTrue(frame.maxTemp > frame.minTemp)
        assertTrue(frame.hotSpotX in 0f..1f)
        assertTrue(frame.hotSpotY in 0f..1f)
    }

    @Test
    fun `test thermal palette color mapping range`() {
        for (palette in ThermalPalette.values()) {
            val colorMin = palette.mapToColor(0.0f)
            val colorMax = palette.mapToColor(1.0f)
            assertNotNull(colorMin)
            assertNotNull(colorMax)
        }
    }

    @Test
    fun `test White Hot Black Hot and Ironbow radiometric signatures`() {
        val whiteHotMin = ThermalPalette.WHITE_HOT.mapToColor(0.0f)
        val whiteHotMax = ThermalPalette.WHITE_HOT.mapToColor(1.0f)
        assertEquals(0.0f, whiteHotMin.red, 0.001f)
        assertEquals(1.0f, whiteHotMax.red, 0.001f)

        val blackHotMin = ThermalPalette.BLACK_HOT.mapToColor(0.0f)
        val blackHotMax = ThermalPalette.BLACK_HOT.mapToColor(1.0f)
        assertEquals(1.0f, blackHotMin.red, 0.001f)
        assertEquals(0.0f, blackHotMax.red, 0.001f)

        val ironbowMax = ThermalPalette.IRONBOW.mapToColor(1.0f)
        assertEquals(1.0f, ironbowMax.red, 0.001f)
        assertEquals(1.0f, ironbowMax.green, 0.001f)
        assertEquals(1.0f, ironbowMax.blue, 0.001f)
    }
}
