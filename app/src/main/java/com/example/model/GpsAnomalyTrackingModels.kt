package com.example.model

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class GpsCoordinateFormat(val label: String, val shortLabel: String) {
    DECIMAL_DEGREES("Decimal Degrees (DD)", "DD"),
    DEGREES_MINUTES_SECONDS("Degrees Minutes Seconds (DMS)", "DMS"),
    MGRS_UTM("Military Grid / UTM (MGRS)", "MGRS")
}

data class IdentifiedHeatAnomaly(
    val id: String,
    val name: String,
    val signatureType: HeatSignatureType,
    val peakTemp: Float,
    val latitude: Double,
    val longitude: Double,
    val elevationMslMeters: Float = 14.5f,
    val isPrimaryTarget: Boolean = false,
    val severity: AlertSeverity = AlertSeverity.CRITICAL,
    val confidencePercent: Int = 98,
    val detectedTimeMs: Long = System.currentTimeMillis()
) {
    fun formatCoordinates(format: GpsCoordinateFormat = GpsCoordinateFormat.DECIMAL_DEGREES): String {
        return when (format) {
            GpsCoordinateFormat.DECIMAL_DEGREES -> GpsMathUtils.formatCoordinateDecimal(latitude, longitude)
            GpsCoordinateFormat.DEGREES_MINUTES_SECONDS -> GpsMathUtils.formatCoordinateDms(latitude, longitude)
            GpsCoordinateFormat.MGRS_UTM -> GpsMathUtils.formatCoordinateMgrs(latitude, longitude)
        }
    }
}

data class RelativeAnomalyTelemetry(
    val targetAnomaly: IdentifiedHeatAnomaly,
    val droneLatitude: Double,
    val droneLongitude: Double,
    val droneAltitudeAgl: Float,
    val droneAltitudeMsl: Double,
    val droneHeading: Float,
    val groundDistanceMeters: Float,
    val slantRangeMeters: Float,
    val trueBearingDegrees: Float,
    val relativeAzimuthDegrees: Float, // -180° (left) to +180° (right), 0° is directly ahead
    val clockPosition: String, // e.g. "12 O'CLOCK", "1 O'CLOCK"
    val deltaNorthMeters: Float,
    val deltaEastMeters: Float,
    val deltaAltitudeMeters: Float, // Drone alt - target alt
    val closureRateMps: Float, // Positive = closing in, negative = opening distance
    val isTargetInFov: Boolean,
    val fovScreenX: Float, // Normalized -1.0f (left edge) to +1.0f (right edge), 0.0f is center
    val fovScreenY: Float  // Normalized -1.0f (top edge) to +1.0f (bottom edge), 0.0f is center
)

object GpsMathUtils {

    const val METERS_PER_DEG_LAT = 111111.0

    fun metersPerDegLng(lat: Double): Double {
        return 111111.0 * cos(Math.toRadians(lat))
    }

    /**
     * Calculates flat-earth ground distance in meters (highly accurate for inspection range < 10km)
     */
    fun calculateGroundDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = (lat2 - lat1) * METERS_PER_DEG_LAT
        val dLng = (lon2 - lon1) * metersPerDegLng((lat1 + lat2) / 2.0)
        return sqrt(dLat * dLat + dLng * dLng).toFloat()
    }

    /**
     * Calculates true compass bearing from Point 1 to Point 2 in degrees (0° - 359.9°)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = (lat2 - lat1) * METERS_PER_DEG_LAT
        val dLng = (lon2 - lon1) * metersPerDegLng((lat1 + lat2) / 2.0)
        val rad = atan2(dLng, dLat)
        val deg = Math.toDegrees(rad)
        return (((deg % 360) + 360) % 360).toFloat()
    }

    /**
     * Relative azimuth between aircraft heading and target bearing (-180° to +180°)
     */
    fun calculateRelativeAzimuth(bearing: Float, heading: Float): Float {
        var diff = (bearing - heading) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    /**
     * Translates relative azimuth into tactical clock directions
     */
    fun calculateClockDirection(relativeAzimuth: Float): String {
        val norm = if (relativeAzimuth < 0) relativeAzimuth + 360f else relativeAzimuth
        return when {
            norm >= 345f || norm < 15f -> "12 O'CLOCK"
            norm < 45f -> "1 O'CLOCK"
            norm < 75f -> "2 O'CLOCK"
            norm < 105f -> "3 O'CLOCK"
            norm < 135f -> "4 O'CLOCK"
            norm < 165f -> "5 O'CLOCK"
            norm < 195f -> "6 O'CLOCK"
            norm < 225f -> "7 O'CLOCK"
            norm < 255f -> "8 O'CLOCK"
            norm < 285f -> "9 O'CLOCK"
            norm < 315f -> "10 O'CLOCK"
            else -> "11 O'CLOCK"
        }
    }

    fun formatCoordinateDecimal(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s, %.6f° %s", abs(lat), latDir, abs(lon), lonDir)
    }

    fun formatCoordinateDms(lat: Double, lon: Double): String {
        val latD = abs(lat).toInt()
        val latM = ((abs(lat) - latD) * 60).toInt()
        val latS = (((abs(lat) - latD) * 60) - latM) * 60
        val latDir = if (lat >= 0) "N" else "S"

        val lonD = abs(lon).toInt()
        val lonM = ((abs(lon) - lonD) * 60).toInt()
        val lonS = (((abs(lon) - lonD) * 60) - lonM) * 60
        val lonDir = if (lon >= 0) "E" else "W"

        return String.format(
            Locale.US,
            "%02d°%02d'%04.1f\"%s, %03d°%02d'%04.1f\"%s",
            latD, latM, latS, latDir,
            lonD, lonM, lonS, lonDir
        )
    }

    /**
     * WGS84 Transverse Mercator projection into standard MGRS / UTM string
     */
    fun formatCoordinateMgrs(lat: Double, lon: Double): String {
        val zone = (((lon + 180.0) / 6.0).toInt() + 1).coerceIn(1, 60)
        val latBands = "CDEFGHJKLMNPQRSTUVWX"
        val bandIdx = (((lat + 80.0) / 8.0).toInt()).coerceIn(0, latBands.length - 1)
        val bandLetter = latBands[bandIdx]

        // Local UTM projection approximation for display
        val centralMeridian = (zone - 1) * 6 - 180 + 3
        val deltaLonRad = Math.toRadians(lon - centralMeridian)
        val latRad = Math.toRadians(lat)

        val falseEasting = 500000.0
        val a = 6378137.0 // WGS84 semi-major axis
        val k0 = 0.9996

        val easting = falseEasting + k0 * a * deltaLonRad * cos(latRad)
        val northing = (if (lat >= 0) 0.0 else 10000000.0) + k0 * a * latRad

        val eastingInt = (easting % 100000).toInt().coerceIn(0, 99999)
        val northingInt = (northing % 100000).toInt().coerceIn(0, 99999)

        // 100km grid square identifier for zone 10T
        val squareCol = ((easting / 100000).toInt() % 8)
        val squareRow = ((northing / 100000).toInt() % 20)
        val colLetter = ('A' + squareCol).toString()
        val rowLetter = ('A' + squareRow).toString()

        return String.format(Locale.US, "%02d%c %s%s %05d %05d", zone, bandLetter, colLetter, rowLetter, eastingInt, northingInt)
    }

    /**
     * Computes comprehensive relative vector kinematics from drone to identified heat anomaly
     */
    fun calculateRelativeTelemetry(
        droneLat: Double,
        droneLng: Double,
        droneAltAgl: Float,
        droneAltMsl: Double,
        droneHeading: Float,
        droneVx: Float, // North velocity m/s
        droneVy: Float, // East velocity m/s
        gimbalPitch: Float, // e.g. -45°
        anomaly: IdentifiedHeatAnomaly
    ): RelativeAnomalyTelemetry {
        val dLat = (anomaly.latitude - droneLat) * METERS_PER_DEG_LAT
        val dLng = (anomaly.longitude - droneLng) * metersPerDegLng((droneLat + anomaly.latitude) / 2.0)
        val deltaNorth = dLat.toFloat()
        val deltaEast = dLng.toFloat()
        val deltaAltitude = (droneAltAgl - 0.0f).coerceAtLeast(0.1f) // Relative height above ground anomaly

        val groundDistance = sqrt(deltaNorth * deltaNorth + deltaEast * deltaEast)
        val slantRange = sqrt(groundDistance * groundDistance + deltaAltitude * deltaAltitude)

        val trueBearing = calculateBearing(droneLat, droneLng, anomaly.latitude, anomaly.longitude)
        val relativeAzimuth = calculateRelativeAzimuth(trueBearing, droneHeading)
        val clockPos = calculateClockDirection(relativeAzimuth)

        // Compute closure rate along line-of-sight vector
        val losUnitN = if (groundDistance > 0.1f) deltaNorth / groundDistance else 0f
        val losUnitE = if (groundDistance > 0.1f) deltaEast / groundDistance else 0f
        val closureRate = (droneVx * losUnitN + droneVy * losUnitE)

        // Project into thermal camera view-cone:
        // DJI Matrice 4TD Radiometric IR has ~61° Horizontal FOV and ~48° Vertical FOV
        val hFovHalf = 30.5f
        val vFovHalf = 24.0f

        // Camera line-of-sight pitch is drone heading + gimbal pitch
        val elevationAngle = Math.toDegrees(atan2(-deltaAltitude.toDouble(), groundDistance.toDouble())).toFloat()
        val relPitch = elevationAngle - gimbalPitch

        val inHFov = abs(relativeAzimuth) <= hFovHalf
        val inVFov = abs(relPitch) <= vFovHalf
        val isTargetInFov = inHFov && inVFov

        val screenX = (relativeAzimuth / hFovHalf).coerceIn(-1.0f, 1.0f)
        val screenY = (-relPitch / vFovHalf).coerceIn(-1.0f, 1.0f)

        return RelativeAnomalyTelemetry(
            targetAnomaly = anomaly,
            droneLatitude = droneLat,
            droneLongitude = droneLng,
            droneAltitudeAgl = droneAltAgl,
            droneAltitudeMsl = droneAltMsl,
            droneHeading = droneHeading,
            groundDistanceMeters = groundDistance,
            slantRangeMeters = slantRange,
            trueBearingDegrees = trueBearing,
            relativeAzimuthDegrees = relativeAzimuth,
            clockPosition = clockPos,
            deltaNorthMeters = deltaNorth,
            deltaEastMeters = deltaEast,
            deltaAltitudeMeters = deltaAltitude,
            closureRateMps = closureRate,
            isTargetInFov = isTargetInFov,
            fovScreenX = screenX,
            fovScreenY = screenY
        )
    }
}
