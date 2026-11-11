package com.example

import com.example.model.AlertSeverity
import com.example.model.GpsCoordinateFormat
import com.example.model.GpsMathUtils
import com.example.model.HeatSignatureType
import com.example.model.IdentifiedHeatAnomaly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsAnomalyTrackingTest {

    @Test
    fun testGroundDistanceCalculation() {
        // Drone at (47.6062, -122.3321) and anomaly at (47.6068, -122.3312)
        val lat1 = 47.6062
        val lon1 = -122.3321
        val lat2 = 47.6068
        val lon2 = -122.3312

        val distance = GpsMathUtils.calculateGroundDistance(lat1, lon1, lat2, lon2)
        // Delta Lat = 0.0006 deg * ~111,111m = ~66.6m North
        // Delta Lon = 0.0009 deg * cos(47.6)*111,111m = ~67.4m East
        // Sqrt(66.6^2 + 67.4^2) = ~94.8m
        assertTrue("Distance should be around 95 meters, was $distance", distance in 80f..115f)
    }

    @Test
    fun testBearingAndClockDirection() {
        // Due North
        val bearingN = GpsMathUtils.calculateBearing(47.6000, -122.3300, 47.6010, -122.3300)
        assertTrue("Due North bearing should be near 0° or 360°, was $bearingN", bearingN in 359f..360f || bearingN in 0f..1f)

        // Due East
        val bearingE = GpsMathUtils.calculateBearing(47.6000, -122.3300, 47.6000, -122.3290)
        assertEquals(90f, bearingE, 1.5f)

        // Relative Azimuth: aircraft heading 45°, target bearing 90° -> relative +45° (Right)
        val relAzimuth = GpsMathUtils.calculateRelativeAzimuth(90f, 45f)
        assertEquals(45f, relAzimuth, 0.1f)

        // Clock Direction for +45° is "1 O'CLOCK" or "2 O'CLOCK"
        val clock = GpsMathUtils.calculateClockDirection(45f)
        assertTrue("Clock position should be 1 or 2 o'clock, was $clock", clock in listOf("1 O'CLOCK", "2 O'CLOCK"))
    }

    @Test
    fun testCoordinateFormatters() {
        val lat = 47.606820
        val lon = -122.331210

        // Decimal Degrees
        val dd = GpsMathUtils.formatCoordinateDecimal(lat, lon)
        assertTrue("DD format should contain N and W", dd.contains("N") && dd.contains("W"))
        assertTrue("DD format should contain 47.606820", dd.contains("47.606820"))

        // Degrees Minutes Seconds
        val dms = GpsMathUtils.formatCoordinateDms(lat, lon)
        assertTrue("DMS format should contain degree symbols", dms.contains("°") && dms.contains("'"))

        // MGRS / UTM
        val mgrs = GpsMathUtils.formatCoordinateMgrs(lat, lon)
        assertTrue("MGRS for Seattle should start with zone 10", mgrs.startsWith("10"))
    }

    @Test
    fun testIdentifiedHeatAnomalyModel() {
        val anomaly = IdentifiedHeatAnomaly(
            id = "ANOM-TX01",
            name = "Phase B Bushing Overheat",
            signatureType = HeatSignatureType.INDUSTRIAL_OVERHEAT,
            peakTemp = 98.2f,
            latitude = 47.606820,
            longitude = -122.331210,
            severity = AlertSeverity.CRITICAL,
            isPrimaryTarget = true
        )

        assertEquals("ANOM-TX01", anomaly.id)
        assertEquals(98.2f, anomaly.peakTemp, 0.01f)
        assertTrue(anomaly.formatCoordinates(GpsCoordinateFormat.DECIMAL_DEGREES).contains("47.606820"))
    }

    @Test
    fun testRelativeAnomalyTelemetryCalculation() {
        val anomaly = IdentifiedHeatAnomaly(
            id = "ANOM-TX01",
            name = "Substation Transformer Overheat",
            signatureType = HeatSignatureType.INDUSTRIAL_OVERHEAT,
            peakTemp = 98.2f,
            latitude = 47.606820,
            longitude = -122.331210
        )

        val rel = GpsMathUtils.calculateRelativeTelemetry(
            droneLat = 47.606240,
            droneLng = -122.332110,
            droneAltAgl = 48.5f,
            droneAltMsl = 52.4,
            droneHeading = 42.0f,
            droneVx = 3.2f,
            droneVy = 1.4f,
            gimbalPitch = -45.0f,
            anomaly = anomaly
        )

        assertNotNull(rel)
        assertTrue("Ground distance should be > 0", rel.groundDistanceMeters > 0f)
        assertTrue("Slant range should be > ground distance", rel.slantRangeMeters >= rel.groundDistanceMeters)
        assertNotNull(rel.clockPosition)
    }
}
