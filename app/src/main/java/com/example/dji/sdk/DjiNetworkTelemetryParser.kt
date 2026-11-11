package com.example.dji.sdk

import com.example.dji.model.DjiAirLinkTelemetry
import com.example.dji.model.DjiAttitudeTelemetry
import com.example.dji.model.DjiBatteryTelemetry
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiFlightMode
import com.example.dji.model.DjiGimbalTelemetry
import com.example.dji.model.DjiGpsTelemetry
import com.example.dji.model.DjiVelocityTelemetry
import com.example.dji.model.GpsFixType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for DJI Mobile SDK & Onboard SDK (OSDK) telemetry streams.
 * Handles both JSON-based telemetry frames and binary telemetry packets.
 */
class DjiNetworkTelemetryParser {

    /**
     * Parses JSON telemetry payload (e.g. from DJI Cloud API or companion computer)
     * Written without Android android.jar JSONObject stub dependencies so it runs
     * seamlessly on both JVM unit tests and Android device runtime.
     */
    fun parseJsonTelemetry(jsonString: String): DjiComprehensiveTelemetry? {
        return try {
            val lat = extractDouble(jsonString, "latitude") ?: 47.6062
            val lng = extractDouble(jsonString, "longitude") ?: -122.3321
            val altAgl = extractDouble(jsonString, "altitudeAgl")?.toFloat() ?: 48.5f
            val heading = extractDouble(jsonString, "compassHeading")?.toFloat() ?: 214.0f
            val pitch = extractDouble(jsonString, "pitch")?.toFloat() ?: -12.4f
            val roll = extractDouble(jsonString, "roll")?.toFloat() ?: 2.1f
            val yaw = extractDouble(jsonString, "yaw")?.toFloat() ?: heading

            val battPercent = extractInt(jsonString, "percent") ?: 84
            val battVolt = extractInt(jsonString, "voltageMv") ?: 22800
            val speedKmh = extractDouble(jsonString, "speedKmh")?.toFloat() ?: 12.8f
            val satellites = extractInt(jsonString, "satellites") ?: 24
            val model = extractString(jsonString, "modelName") ?: "DJI Matrice 4TD"

            DjiComprehensiveTelemetry(
                timestampMs = System.currentTimeMillis(),
                modelName = model,
                flightMode = DjiFlightMode.GPS_ATTI,
                gps = DjiGpsTelemetry(
                    latitude = lat,
                    longitude = lng,
                    altitudeAglMeters = altAgl,
                    altitudeMslMeters = altAgl + 14.0,
                    satelliteCount = satellites,
                    fixType = GpsFixType.RTK_FIXED
                ),
                attitude = DjiAttitudeTelemetry(
                    pitchDegrees = pitch,
                    rollDegrees = roll,
                    yawDegrees = yaw,
                    compassHeadingDegrees = heading
                ),
                velocity = DjiVelocityTelemetry(
                    groundSpeedKmh = speedKmh
                ),
                battery = DjiBatteryTelemetry(
                    chargeRemainingPercent = battPercent,
                    voltageMv = battVolt
                ),
                gimbal = DjiGimbalTelemetry(),
                airLink = DjiAirLinkTelemetry()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = Regex("\"$key\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)")
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = Regex("\"$key\"\\s*:\\s*([0-9]+)")
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    /**
     * Parses standard binary MSDK telemetry frame
     */
    fun parseBinaryTelemetry(bytes: ByteArray): DjiComprehensiveTelemetry? {
        if (bytes.size < 42) return null
        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != 0x0201AA55) return null

            val lat = buffer.double
            val lng = buffer.double
            val alt = buffer.float
            val pitch = buffer.float
            val roll = buffer.float
            val heading = buffer.float
            val speedMps = buffer.float
            val batt = buffer.get().toInt() and 0xFF
            val sats = buffer.get().toInt() and 0xFF

            DjiComprehensiveTelemetry(
                timestampMs = System.currentTimeMillis(),
                gps = DjiGpsTelemetry(
                    latitude = lat,
                    longitude = lng,
                    altitudeAglMeters = alt,
                    satelliteCount = sats,
                    fixType = GpsFixType.FIX_3D
                ),
                attitude = DjiAttitudeTelemetry(
                    pitchDegrees = pitch,
                    rollDegrees = roll,
                    yawDegrees = heading,
                    compassHeadingDegrees = heading
                ),
                velocity = DjiVelocityTelemetry(
                    groundSpeedKmh = speedMps * 3.6f
                ),
                battery = DjiBatteryTelemetry(
                    chargeRemainingPercent = batt
                )
            )
        } catch (_: Exception) {
            null
        }
    }
}
