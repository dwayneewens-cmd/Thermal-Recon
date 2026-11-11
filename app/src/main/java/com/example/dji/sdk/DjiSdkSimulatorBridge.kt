package com.example.dji.sdk

import com.example.dji.model.DjiAirLinkTelemetry
import com.example.dji.model.DjiAttitudeTelemetry
import com.example.dji.model.DjiBatteryTelemetry
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiFlightMode
import com.example.dji.model.DjiGimbalTelemetry
import com.example.dji.model.DjiGpsTelemetry
import com.example.dji.model.DjiVelocityTelemetry
import com.example.dji.model.FlightPattern
import com.example.dji.model.GpsFixType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-fidelity DJI Flight Simulation Engine.
 * Generates realistic 6-DOF aircraft kinematics, GPS coordinates, heading,
 * and battery discharge curves for offline development and safety testing.
 */
class DjiSdkSimulatorBridge {

    var aircraftModel: String = "DJI Matrice 4TD"
    var activePattern: FlightPattern = FlightPattern.ORBIT_INSPECTION

    // Home / Base Coordinates (Seattle Industrial District by default)
    var homeLatitude: Double = 47.6062
    var homeLongitude: Double = -122.3321
    var homeAltitudeMsl: Double = 12.0

    // Target Inspection Center (e.g. Substation Transformer / Solar Array Hotspot)
    var targetLatitude: Double = 47.6068
    var targetLongitude: Double = -122.3312

    private var timeSeconds: Double = 0.0
    private var baseBatteryPercent: Float = 92.0f
    private var batteryTemp: Float = 26.5f

    fun reset() {
        timeSeconds = 0.0
        baseBatteryPercent = 92.0f
        batteryTemp = 26.5f
    }

    /**
     * Ticks the physics model by deltaTime (e.g. 0.1s for 10Hz MSDK rate)
     */
    fun tick(deltaSeconds: Double = 0.1): DjiComprehensiveTelemetry {
        timeSeconds += deltaSeconds

        // Subtle battery drain (approx 1% every 30 seconds of active flight)
        baseBatteryPercent = (baseBatteryPercent - (deltaSeconds * 0.033).toFloat()).coerceIn(5f, 100f)
        batteryTemp = (batteryTemp + (deltaSeconds * 0.005).toFloat()).coerceIn(25f, 55f)

        // Earth coordinate delta scale (~111,111 meters per degree latitude)
        val metersPerDegLat = 111111.0
        val metersPerDegLng = 111111.0 * cos(Math.toRadians(homeLatitude))

        val currentLat: Double
        val currentLng: Double
        val currentAltAgl: Float
        val currentPitch: Float
        val currentRoll: Float
        val currentYaw: Float
        val compassHeading: Float
        val currentVx: Float
        val currentVy: Float
        val currentVz: Float
        val flightMode: DjiFlightMode

        when (activePattern) {
            FlightPattern.ORBIT_INSPECTION -> {
                // Circular orbit around target point with radius of 38 meters
                val orbitRadiusMeters = 38.0
                val angularSpeedRadPerSec = 0.12 // ~52 seconds per full orbit
                val currentAngle = timeSeconds * angularSpeedRadPerSec

                val eastOffsetMeters = cos(currentAngle) * orbitRadiusMeters
                val northOffsetMeters = sin(currentAngle) * orbitRadiusMeters

                currentLat = targetLatitude + (northOffsetMeters / metersPerDegLat)
                currentLng = targetLongitude + (eastOffsetMeters / metersPerDegLng)

                // Hovering altitude around 45m with slight atmospheric wave
                currentAltAgl = (45.0 + sin(timeSeconds * 0.2) * 1.2).toFloat()

                // Tangential velocities
                currentVx = (-sin(currentAngle) * orbitRadiusMeters * angularSpeedRadPerSec).toFloat()
                currentVy = (cos(currentAngle) * orbitRadiusMeters * angularSpeedRadPerSec).toFloat()
                currentVz = (cos(timeSeconds * 0.2) * 0.24).toFloat()

                // Heading faces the target center
                val dNorthToCenter = (targetLatitude - currentLat) * metersPerDegLat
                val dEastToCenter = (targetLongitude - currentLng) * metersPerDegLng
                val headingRad = atan2(dEastToCenter, dNorthToCenter)
                compassHeading = (((Math.toDegrees(headingRad) % 360) + 360) % 360).toFloat()
                currentYaw = compassHeading

                // Dynamic roll banking inward towards circle center (~4° bank)
                currentRoll = 4.2f
                currentPitch = -2.1f
                flightMode = DjiFlightMode.WAYPOINT_NAV
            }

            FlightPattern.GRID_MAPPING_SURVEY -> {
                // Back-and-forth serpentine scan over 80m x 60m area
                val speedMps = 4.5
                val legDuration = 18.0
                val totalCycle = legDuration * 4.0
                val cycleTime = timeSeconds % totalCycle
                val legIndex = (cycleTime / legDuration).toInt()
                val legProgress = (cycleTime % legDuration) / legDuration

                val northPos = (legIndex * 15.0) - 30.0
                val eastPos = if (legIndex % 2 == 0) {
                    (legProgress * 70.0) - 35.0
                } else {
                    ((1.0 - legProgress) * 70.0) - 35.0
                }

                currentLat = homeLatitude + (northPos / metersPerDegLat)
                currentLng = homeLongitude + (eastPos / metersPerDegLng)
                currentAltAgl = 55.0f

                currentVx = 0.1f
                currentVy = if (legIndex % 2 == 0) speedMps.toFloat() else -speedMps.toFloat()
                currentVz = 0.0f

                compassHeading = if (legIndex % 2 == 0) 90.0f else 270.0f
                currentYaw = compassHeading
                currentPitch = -3.5f
                currentRoll = 0.5f
                flightMode = DjiFlightMode.WAYPOINT_NAV
            }

            FlightPattern.PERIMETER_PATROL -> {
                // Linear corridor patrol along a fence line
                val patrolLen = 120.0
                val speed = 5.0
                val halfCycle = patrolLen / speed
                val progress = (sin(timeSeconds / halfCycle * PI) + 1.0) / 2.0 // 0 to 1

                val offsetEast = (progress - 0.5) * patrolLen
                currentLat = homeLatitude
                currentLng = homeLongitude + (offsetEast / metersPerDegLng)
                currentAltAgl = 38.0f

                val isMovingEast = cos(timeSeconds / halfCycle * PI) >= 0
                compassHeading = if (isMovingEast) 90.0f else 270.0f
                currentYaw = compassHeading
                currentVx = 0.0f
                currentVy = if (isMovingEast) speed.toFloat() else -speed.toFloat()
                currentVz = 0.0f
                currentPitch = -4.0f
                currentRoll = 0.0f
                flightMode = DjiFlightMode.GPS_ATTI
            }

            FlightPattern.STATIONARY_HOVER -> {
                // Fixed GPS coordinate with subtle GPS noise and heading hold
                val noiseN = sin(timeSeconds * 0.4) * 0.4
                val noiseE = cos(timeSeconds * 0.35) * 0.4
                currentLat = homeLatitude + (noiseN / metersPerDegLat)
                currentLng = homeLongitude + (noiseE / metersPerDegLng)
                currentAltAgl = 32.0f + (sin(timeSeconds * 0.5) * 0.3).toFloat()

                currentVx = (cos(timeSeconds * 0.4) * 0.15).toFloat()
                currentVy = (-sin(timeSeconds * 0.35) * 0.15).toFloat()
                currentVz = 0.0f
                compassHeading = 180.0f
                currentYaw = 180.0f
                currentPitch = 0.2f
                currentRoll = -0.1f
                flightMode = DjiFlightMode.HOVERING
            }
        }

        // Distance to home
        val dNorthHome = (currentLat - homeLatitude) * metersPerDegLat
        val dEastHome = (currentLng - homeLongitude) * metersPerDegLng
        val distanceHome = sqrt(dNorthHome * dNorthHome + dEastHome * dEastHome).toFloat()

        val groundSpeed = sqrt(currentVx * currentVx + currentVy * currentVy) * 3.6f // in km/h

        val battPercentInt = baseBatteryPercent.toInt()
        val battVoltageMv = 19800 + (battPercentInt * 32) // ~23.0V at 100%, 20.4V at 20%

        return DjiComprehensiveTelemetry(
            timestampMs = System.currentTimeMillis(),
            modelName = aircraftModel,
            flightMode = flightMode,
            flightTimeSeconds = timeSeconds.toInt(),
            isMotorsOn = true,
            isFlying = true,
            gps = DjiGpsTelemetry(
                latitude = currentLat,
                longitude = currentLng,
                altitudeMslMeters = homeAltitudeMsl + currentAltAgl,
                altitudeAglMeters = currentAltAgl,
                satelliteCount = 26,
                fixType = GpsFixType.RTK_FIXED,
                horizontalAccuracyMeters = 0.08f,
                verticalAccuracyMeters = 0.12f,
                rtkEnabled = true
            ),
            attitude = DjiAttitudeTelemetry(
                pitchDegrees = currentPitch,
                rollDegrees = currentRoll,
                yawDegrees = currentYaw,
                compassHeadingDegrees = compassHeading
            ),
            velocity = DjiVelocityTelemetry(
                velocityXMps = currentVx,
                velocityYMps = currentVy,
                velocityZMps = currentVz,
                groundSpeedKmh = groundSpeed
            ),
            battery = DjiBatteryTelemetry(
                chargeRemainingPercent = battPercentInt,
                voltageMv = battVoltageMv,
                temperatureCelsius = batteryTemp,
                currentMa = -7200,
                batteryHealthPercent = 98,
                cellVoltagesMv = listOf(
                    battVoltageMv / 6,
                    battVoltageMv / 6 + 2,
                    battVoltageMv / 6 - 1,
                    battVoltageMv / 6 + 1,
                    battVoltageMv / 6,
                    battVoltageMv / 6 - 2
                )
            ),
            gimbal = DjiGimbalTelemetry(
                pitchDegrees = -45.0f,
                rollDegrees = 0.0f,
                yawDegrees = 0.0f,
                isCalibrated = true
            ),
            airLink = DjiAirLinkTelemetry(
                uplinkSignalQualityPercent = 95,
                downlinkSignalQualityPercent = 92,
                signalDbm = -58,
                frequencyGhz = 5.8f
            ),
            homeLocationLat = homeLatitude,
            homeLocationLng = homeLongitude,
            distanceToHomeMeters = distanceHome
        )
    }
}
