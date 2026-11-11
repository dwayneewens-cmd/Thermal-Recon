package com.example

import com.example.dji.model.DjiAttitudeTelemetry
import com.example.dji.model.DjiGpsTelemetry
import com.example.dji.model.FlightPattern
import com.example.dji.sdk.DjiNetworkTelemetryParser
import com.example.dji.sdk.DjiSdkSimulatorBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DjiTelemetryServiceTest {

    @Test
    fun `test DJI Simulator Bridge generates realistic kinematics`() {
        val bridge = DjiSdkSimulatorBridge()
        bridge.activePattern = FlightPattern.ORBIT_INSPECTION

        val t0 = bridge.tick(0.1)
        assertNotNull(t0)
        assertEquals("DJI Matrice 4TD", t0.modelName)
        assertTrue(t0.gps.latitude in -90.0..90.0)
        assertTrue(t0.gps.longitude in -180.0..180.0)
        assertTrue(t0.gps.altitudeAglMeters > 0f)
        assertTrue(t0.attitude.compassHeadingDegrees in 0f..360f)
        assertTrue(t0.battery.chargeRemainingPercent in 0..100)
    }

    @Test
    fun `test DJI Simulator Bridge flight pattern switching`() {
        val bridge = DjiSdkSimulatorBridge()
        bridge.activePattern = FlightPattern.STATIONARY_HOVER
        val hover = bridge.tick(0.1)
        assertEquals(180.0f, hover.attitude.compassHeadingDegrees, 1.0f)
        assertEquals("DJI Matrice 4TD", hover.modelName)

        bridge.activePattern = FlightPattern.PERIMETER_PATROL
        val patrol = bridge.tick(0.1)
        assertNotNull(patrol)
        assertTrue(patrol.velocity.groundSpeedKmh > 0f)
    }

    @Test
    fun `test DJI Network Telemetry JSON Parser with Matrice 4TD`() {
        val parser = DjiNetworkTelemetryParser()
        val json = """
            {
                "modelName": "DJI Matrice 4TD",
                "gps": {
                    "latitude": 47.6062,
                    "longitude": -122.3321,
                    "altitudeAgl": 55.4,
                    "satellites": 26
                },
                "attitude": {
                    "pitch": -8.5,
                    "roll": 1.2,
                    "compassHeading": 218.0
                },
                "battery": {
                    "percent": 88,
                    "voltageMv": 22900
                },
                "velocity": {
                    "speedKmh": 16.5
                }
            }
        """.trimIndent()

        val parsed = parser.parseJsonTelemetry(json)
        assertNotNull(parsed)
        assertEquals("DJI Matrice 4TD", parsed!!.modelName)
        assertEquals(47.6062, parsed.gps.latitude, 0.0001)
        assertEquals(-122.3321, parsed.gps.longitude, 0.0001)
        assertEquals(55.4f, parsed.gps.altitudeAglMeters, 0.01f)
        assertEquals(218.0f, parsed.attitude.compassHeadingDegrees, 0.01f)
        assertEquals(88, parsed.battery.chargeRemainingPercent)
    }

    @Test
    fun `test DJI Network Telemetry Binary Parser`() {
        val parser = DjiNetworkTelemetryParser()
        val buffer = ByteBuffer.allocate(42).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x0201AA55) // Magic
        buffer.putDouble(47.6062) // Lat
        buffer.putDouble(-122.3321) // Lng
        buffer.putFloat(42.5f) // Alt
        buffer.putFloat(-5.0f) // Pitch
        buffer.putFloat(2.0f) // Roll
        buffer.putFloat(180.0f) // Heading
        buffer.putFloat(5.0f) // Speed m/s
        buffer.put(85.toByte()) // Batt %
        buffer.put(22.toByte()) // Sats

        val parsed = parser.parseBinaryTelemetry(buffer.array())
        assertNotNull(parsed)
        assertEquals(47.6062, parsed!!.gps.latitude, 0.0001)
        assertEquals(42.5f, parsed.gps.altitudeAglMeters, 0.01f)
        assertEquals(180.0f, parsed.attitude.compassHeadingDegrees, 0.01f)
        assertEquals(85, parsed.battery.chargeRemainingPercent)
    }

    @Test
    fun `test GPS and Attitude cardinal formatting`() {
        val gps = DjiGpsTelemetry(latitude = 47.6062, longitude = -122.3321)
        assertTrue(gps.formatCoordinates().contains("47.606200° N"))
        assertTrue(gps.formatCoordinates().contains("122.332100° W"))

        val northAttitude = DjiAttitudeTelemetry(compassHeadingDegrees = 5.0f)
        assertEquals("N", northAttitude.formatHeadingCardinal())

        val southWestAttitude = DjiAttitudeTelemetry(compassHeadingDegrees = 225.0f)
        assertEquals("SW", southWestAttitude.formatHeadingCardinal())
    }

    @Test
    fun `test Translucent Overlay telemetry models for altitude battery gps and gimbal`() {
        val comprehensive = com.example.dji.model.DjiComprehensiveTelemetry(
            modelName = "DJI Matrice 4TD",
            gps = DjiGpsTelemetry(
                latitude = 37.7749,
                longitude = -122.4194,
                altitudeAglMeters = 65.5f,
                altitudeMslMeters = 72.0,
                satelliteCount = 28,
                fixType = com.example.dji.model.GpsFixType.RTK_FIXED
            ),
            battery = com.example.dji.model.DjiBatteryTelemetry(
                chargeRemainingPercent = 79,
                voltageMv = 22750,
                temperatureCelsius = 29.2f
            ),
            gimbal = com.example.dji.model.DjiGimbalTelemetry(
                pitchDegrees = -45.0f,
                rollDegrees = 0.0f,
                yawDegrees = 12.0f,
                isCalibrated = true
            )
        )

        // 1. Altitude assertions
        assertEquals(65.5f, comprehensive.gps.altitudeAglMeters, 0.01f)
        assertEquals(72.0, comprehensive.gps.altitudeMslMeters, 0.01)

        // 2. Battery assertions
        assertEquals(79, comprehensive.battery.chargeRemainingPercent)
        assertEquals(22.75f, comprehensive.battery.voltageMv / 1000f, 0.01f)

        // 3. GPS assertions
        val formattedCoords = comprehensive.gps.formatCoordinates()
        assertTrue(formattedCoords.contains("37.774900° N"))
        assertTrue(formattedCoords.contains("122.419400° W"))
        assertEquals(com.example.dji.model.GpsFixType.RTK_FIXED, comprehensive.gps.fixType)

        // 4. Gimbal assertions
        assertEquals(-45.0f, comprehensive.gimbal.pitchDegrees, 0.01f)
        assertTrue(comprehensive.gimbal.isCalibrated)
    }
}

