package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun fromCelsius(celsius: Float): Float {
        return when (this) {
            CELSIUS -> celsius
            FAHRENHEIT -> (celsius * 9f / 5f) + 32f
        }
    }

    fun format(celsius: Float): String {
        val converted = fromCelsius(celsius)
        return String.format("%.1f%s", converted, symbol)
    }
}

enum class ThermalPalette(val title: String, val description: String) {
    IRONBOW("Ironbow", "Standard infrared FLIR/DJI palette for high-detail gradient detection"),
    RAINBOW("Rainbow", "Maximum dynamic range with multi-spectral color transitions"),
    WHITE_HOT("White Hot", "Tactical & military search standard: hotter targets are bright white"),
    BLACK_HOT("Black Hot", "High contrast target silhouette: hotter targets are dark black"),
    INFERNO("Inferno", "Deep purple to incandescent gold for subtle heat leaks"),
    ARCTIC("Arctic", "Cool blue highlights with high contrast heat spike markers"),
    LAVA("Lava", "Industrial molten palette for high temperature anomaly thresholding");

    fun mapToColor(t: Float): Color {
        val clamped = t.coerceIn(0f, 1f)
        return when (this) {
            IRONBOW -> {
                // Black -> Dark Purple -> Red/Orange -> Yellow -> White
                when {
                    clamped < 0.25f -> {
                        val factor = clamped / 0.25f
                        Color(0.1f * factor, 0f, 0.35f * factor, 1f)
                    }
                    clamped < 0.5f -> {
                        val factor = (clamped - 0.25f) / 0.25f
                        Color(0.1f + 0.7f * factor, 0f, 0.35f * (1f - factor), 1f)
                    }
                    clamped < 0.75f -> {
                        val factor = (clamped - 0.5f) / 0.25f
                        Color(0.8f + 0.2f * factor, 0.6f * factor, 0f, 1f)
                    }
                    else -> {
                        val factor = (clamped - 0.75f) / 0.25f
                        Color(1f, 0.6f + 0.4f * factor, factor, 1f)
                    }
                }
            }
            RAINBOW -> {
                // Blue -> Cyan -> Green -> Yellow -> Red
                when {
                    clamped < 0.25f -> {
                        val factor = clamped / 0.25f
                        Color(0f, factor, 1f, 1f)
                    }
                    clamped < 0.5f -> {
                        val factor = (clamped - 0.25f) / 0.25f
                        Color(0f, 1f, 1f - factor, 1f)
                    }
                    clamped < 0.75f -> {
                        val factor = (clamped - 0.5f) / 0.25f
                        Color(factor, 1f, 0f, 1f)
                    }
                    else -> {
                        val factor = (clamped - 0.75f) / 0.25f
                        Color(1f, 1f - factor, 0f, 1f)
                    }
                }
            }
            WHITE_HOT -> {
                Color(clamped, clamped, clamped, 1f)
            }
            BLACK_HOT -> {
                val inv = 1f - clamped
                Color(inv, inv, inv, 1f)
            }
            INFERNO -> {
                // Black -> Dark Indigo -> Magenta -> Orange -> Pale Yellow
                when {
                    clamped < 0.33f -> {
                        val f = clamped / 0.33f
                        Color(0.3f * f, 0f, 0.4f * f, 1f)
                    }
                    clamped < 0.66f -> {
                        val f = (clamped - 0.33f) / 0.33f
                        Color(0.3f + 0.6f * f, 0.1f * f, 0.4f + 0.2f * f, 1f)
                    }
                    else -> {
                        val f = (clamped - 0.66f) / 0.34f
                        Color(0.9f + 0.1f * f, 0.1f + 0.8f * f, 0.6f * (1f - f), 1f)
                    }
                }
            }
            ARCTIC -> {
                // Dark Cyan -> Soft Blue -> White -> Flame Orange
                when {
                    clamped < 0.5f -> {
                        val f = clamped / 0.5f
                        Color(0.05f * f, 0.2f + 0.5f * f, 0.4f + 0.6f * f, 1f)
                    }
                    clamped < 0.85f -> {
                        val f = (clamped - 0.5f) / 0.35f
                        Color(0.05f + 0.95f * f, 0.7f + 0.3f * f, 1f, 1f)
                    }
                    else -> {
                        val f = (clamped - 0.85f) / 0.15f
                        Color(1f, 0.3f * (1f - f), 0f, 1f)
                    }
                }
            }
            LAVA -> {
                // Carbon Black -> Rust Red -> Vivid Orange -> Golden Yellow
                when {
                    clamped < 0.4f -> {
                        val f = clamped / 0.4f
                        Color(0.1f + 0.5f * f, 0.05f * f, 0.05f * f, 1f)
                    }
                    clamped < 0.75f -> {
                        val f = (clamped - 0.4f) / 0.35f
                        Color(0.6f + 0.4f * f, 0.05f + 0.45f * f, 0f, 1f)
                    }
                    else -> {
                        val f = (clamped - 0.75f) / 0.25f
                        Color(1f, 0.5f + 0.5f * f, 0.2f * f, 1f)
                    }
                }
            }
        }
    }
}

enum class HeatSignatureType(
    val title: String,
    val subtitle: String,
    val minTypicalTemp: Float,
    val maxTypicalTemp: Float,
    val badgeColor: Long
) {
    HUMAN_LIVING(
        title = "Human / Living Signature",
        subtitle = "Biological thermoregulated core heat (34°C - 39°C)",
        minTypicalTemp = 34.0f,
        maxTypicalTemp = 39.0f,
        badgeColor = 0xFFFF6A00
    ),
    INDUSTRIAL_OVERHEAT(
        title = "Industrial Overheat Fault",
        subtitle = "Mechanical friction / electrical resistance hotspot (>65°C)",
        minTypicalTemp = 65.0f,
        maxTypicalTemp = 120.0f,
        badgeColor = 0xFFFF2A1B
    ),
    FIRE_ANOMALY(
        title = "Combustion / Fire Anomaly",
        subtitle = "Active flame or underground root fire embers (>90°C)",
        minTypicalTemp = 90.0f,
        maxTypicalTemp = 400.0f,
        badgeColor = 0xFFFF1744
    ),
    COLD_LEAK(
        title = "Thermal Depression / Cold Leak",
        subtitle = "Refrigerant rupture, HVAC thermal bridge, gas expansion (<8°C)",
        minTypicalTemp = -20.0f,
        maxTypicalTemp = 8.0f,
        badgeColor = 0xFF00E5FF
    ),
    NORMAL_AMBIENT(
        title = "Ambient Thermal Equilibrium",
        subtitle = "Stable background environmental temperature",
        minTypicalTemp = 15.0f,
        maxTypicalTemp = 30.0f,
        badgeColor = 0xFF00E676
    )
}

enum class AlertSeverity(val label: String, val colorHex: Long) {
    CRITICAL("CRITICAL ALARM", 0xFFFF2A1B),
    WARNING("THERMAL WARNING", 0xFFFFAB00),
    ADVISORY("SIGNATURE DETECTED", 0xFF00E5FF)
}

data class ThermalAlert(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val peakTemp: Float,
    val thresholdTemp: Float,
    val signatureType: HeatSignatureType,
    val severity: AlertSeverity,
    val location: String = "47.6062° N, 122.3321° W (Elev: 42m)"
)

data class InspectionSpot(
    val id: String,
    val xNorm: Float, // 0.0f to 1.0f
    val yNorm: Float, // 0.0f to 1.0f
    val label: String,
    val temperature: Float
)

data class RoiBox(
    val left: Float = 0.25f,
    val top: Float = 0.25f,
    val right: Float = 0.75f,
    val bottom: Float = 0.75f,
    val enabled: Boolean = false
)

enum class CameraFeedSource(val title: String, val subtitle: String) {
    DEVICE_CAMERA("Device Infrared Lens", "Integrated camera with radiometric synthesis"),
    DJI_DRONE_STREAM("DJI Drone Thermal Link", "RTSP/O4 stream link from Matrice 4TD / 350 RTK / M3T"),
    LAB_SIMULATION("Lab Bench Flight Simulator", "High-precision radiometric scenario testing")
}

enum class ThermalFusionMode(val title: String, val description: String) {
    HEATMAP_OVERLAY("Heatmap Overlay", "Alpha-blended radiometric thermal heatmap overlaid on live optical drone video"),
    MSX_FUSION("MSX Dynamic Fusion", "High-contrast thermal heatmap with structural edge contours"),
    ISOTHERM_HIGHLIGHT("Hotspot Isolation", "Only target thermal anomalies are overlaid; background is clear optical video"),
    THERMAL_SOLO("Full Thermal IR", "100% radiometric thermal infrared spectrum")
}

enum class LabScenario(val title: String, val missionType: String, val targetSignature: HeatSignatureType) {
    SEARCH_AND_RESCUE("Wilderness SAR - Lost Hiker", "Human Rescue", HeatSignatureType.HUMAN_LIVING),
    SOLAR_FARM_HOTSPOT("Utility Solar Array - Diode Fault", "Industrial Solar", HeatSignatureType.INDUSTRIAL_OVERHEAT),
    SUBSTATION_TRANSFORMER("Electrical Grid - Bushing Overheat", "Power Infrastructure", HeatSignatureType.INDUSTRIAL_OVERHEAT),
    WILDFIRE_PERIMETER("Brushfire - Hot Spot Boundary", "Disaster Recon", HeatSignatureType.FIRE_ANOMALY),
    COLD_HVAC_LOSS("Commercial Building Envelope Loss", "Energy Audit", HeatSignatureType.COLD_LEAK)
}

data class DroneTelemetry(
    val connected: Boolean = true,
    val modelName: String = "DJI Matrice 4TD // Radiometric IR",
    val batteryPercent: Int = 84,
    val altitudeMeters: Float = 48.5f,
    val groundSpeedKmh: Float = 14.2f,
    val pitchDegrees: Float = -12.4f,
    val rollDegrees: Float = 2.1f,
    val yawDegrees: Float = 214.0f,
    val signalDbm: Int = -62,
    val gpsSatellites: Int = 22,
    val streamFps: Int = 30,
    val streamBitrateMbps: Float = 12.8f,
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val rtspUrl: String = "rtsp://192.168.1.1:8554/live"
)

data class ThermalFrameData(
    val matrixWidth: Int = 32,
    val matrixHeight: Int = 24,
    val temperatures: FloatArray, // 1D array of row-major temperature values
    val minTemp: Float,
    val maxTemp: Float,
    val avgTemp: Float,
    val centerSpotTemp: Float,
    val hotSpotX: Float, // Normalized 0..1
    val hotSpotY: Float,
    val coldSpotX: Float,
    val coldSpotY: Float,
    val detectedSignatures: List<HeatSignatureType> = emptyList(),
    val roiAvgTemp: Float = 0f,
    val roiMaxTemp: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ThermalFrameData
        return matrixWidth == other.matrixWidth &&
                matrixHeight == other.matrixHeight &&
                temperatures.contentEquals(other.temperatures) &&
                minTemp == other.minTemp &&
                maxTemp == other.maxTemp
    }

    override fun hashCode(): Int {
        var result = matrixWidth
        result = 31 * result + matrixHeight
        result = 31 * result + temperatures.contentHashCode()
        result = 31 * result + minTemp.hashCode()
        result = 31 * result + maxTemp.hashCode()
        return result
    }
}
