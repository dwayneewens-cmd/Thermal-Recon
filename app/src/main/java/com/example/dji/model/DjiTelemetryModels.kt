package com.example.dji.model

enum class DjiConnectionState(val label: String, val isConnected: Boolean) {
    DISCONNECTED("Disconnected", false),
    INITIALIZING_SDK("Initializing MSDK", false),
    REGISTERING_APP_KEY("Registering DJI App Key", false),
    CONNECTING_PRODUCT("Connecting to Aircraft", false),
    PRODUCT_CONNECTED("Aircraft Linked", true),
    TELEMETRY_STREAMING("Telemetry Streaming (10Hz)", true),
    CONNECTION_LOST("Connection Lost / Signal Weak", false),
    ERROR("SDK Error", false)
}

enum class DjiConnectionMode(val title: String, val description: String) {
    USB_RC_BRIDGE("DJI RC USB Bridge", "Direct USB connection to DJI RC / Smart Controller"),
    NETWORK_OSDK("Network OSDK Stream", "UDP/RTSP telemetry stream from DJI Onboard SDK"),
    SIMULATOR_BRIDGE("MSDK Flight Simulator", "Virtual aircraft flight kinematics engine for field tests")
}

enum class GpsFixType(val label: String) {
    NO_FIX("No Fix"),
    FIX_2D("2D Fix"),
    FIX_3D("3D Fix"),
    RTK_FLOAT("RTK Float"),
    RTK_FIXED("RTK Fixed (Centimeter)")
}

enum class DjiFlightMode(val label: String) {
    MANUAL("Manual (P-Mode)"),
    ATTITUDE("Attitude (ATTI)"),
    GPS_ATTI("GPS Positioning (P-GPS)"),
    WAYPOINT_NAV("Autonomous Waypoint Mission"),
    AUTO_TAKEOFF("Auto Takeoff"),
    AUTO_LANDING("Auto Landing"),
    RETURN_TO_HOME("Return-to-Home (RTH)"),
    HOVERING("Precise Position Hover"),
    CONFIRM_LANDING("Landing Confirmation Required")
}

enum class FlightPattern(val title: String, val description: String) {
    ORBIT_INSPECTION("Orbit Point of Interest", "360° circular orbit around thermal inspection target"),
    GRID_MAPPING_SURVEY("Grid Mapping Scan", "Lawnmower serpentine pattern for wide-area thermal survey"),
    PERIMETER_PATROL("Perimeter Corridor", "Linear border patrol along industrial boundary"),
    STATIONARY_HOVER("Stationary Hover", "Fixed GPS waypoint lock with heading rotation")
}

data class DjiGpsTelemetry(
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val altitudeMslMeters: Double = 52.4, // Mean Sea Level altitude
    val altitudeAglMeters: Float = 48.5f, // Above Ground Level altitude
    val satelliteCount: Int = 24,
    val fixType: GpsFixType = GpsFixType.FIX_3D,
    val horizontalAccuracyMeters: Float = 0.6f,
    val verticalAccuracyMeters: Float = 0.8f,
    val rtkEnabled: Boolean = false
) {
    fun formatCoordinates(): String {
        val latDir = if (latitude >= 0) "N" else "S"
        val lngDir = if (longitude >= 0) "E" else "W"
        return String.format("%.6f° %s, %.6f° %s", kotlin.math.abs(latitude), latDir, kotlin.math.abs(longitude), lngDir)
    }
}

data class DjiAttitudeTelemetry(
    val pitchDegrees: Float = -12.4f,
    val rollDegrees: Float = 1.8f,
    val yawDegrees: Float = 214.5f,
    val compassHeadingDegrees: Float = 214.5f // 0 - 359.9°
) {
    fun formatHeadingCardinal(): String {
        val normalized = ((compassHeadingDegrees % 360) + 360) % 360
        return when {
            normalized >= 337.5 || normalized < 22.5 -> "N"
            normalized < 67.5 -> "NE"
            normalized < 112.5 -> "E"
            normalized < 157.5 -> "SE"
            normalized < 202.5 -> "S"
            normalized < 247.5 -> "SW"
            normalized < 292.5 -> "W"
            else -> "NW"
        }
    }
}

data class DjiVelocityTelemetry(
    val velocityXMps: Float = 3.2f, // North velocity in m/s
    val velocityYMps: Float = 1.4f, // East velocity in m/s
    val velocityZMps: Float = -0.2f, // Down velocity in m/s (climb/descent)
    val groundSpeedKmh: Float = 12.6f
)

data class DjiBatteryTelemetry(
    val chargeRemainingPercent: Int = 84,
    val voltageMv: Int = 22800, // 22.8V 6S LiPo
    val temperatureCelsius: Float = 28.5f,
    val currentMa: Int = -6400,
    val lifetimeDischargeCycles: Int = 18,
    val batteryHealthPercent: Int = 98,
    val cellVoltagesMv: List<Int> = listOf(3800, 3802, 3799, 3801, 3800, 3798)
)

data class DjiGimbalTelemetry(
    val pitchDegrees: Float = -45.0f, // -90° straight down to 0° forward
    val rollDegrees: Float = 0.0f,
    val yawDegrees: Float = 0.0f,
    val isCalibrated: Boolean = true
)

data class DjiAirLinkTelemetry(
    val uplinkSignalQualityPercent: Int = 94,
    val downlinkSignalQualityPercent: Int = 91,
    val signalDbm: Int = -62,
    val frequencyGhz: Float = 5.8f
)

enum class DjiAircraftModel(
    val modelName: String,
    val shortName: String,
    val series: String,
    val thermalSpecs: String,
    val opticalSpecs: String,
    val transmission: String,
    val isDockCompatible: Boolean
) {
    MATRICE_4TD(
        modelName = "DJI Matrice 4TD",
        shortName = "M4TD",
        series = "Matrice 4D / Dock 2 Enterprise Autonomous Aircraft",
        thermalSpecs = "640×512 Radiometric IR, 28mm equiv., High/Low Gain, Spot/Area Radiometry",
        opticalSpecs = "4/3 CMOS Wide 20MP, 1/2-inch Tele 12MP (56× Hybrid Zoom), NIR Aux Light",
        transmission = "DJI O4 Enterprise (15km Range, 4-Antenna Low Latency)",
        isDockCompatible = true
    ),
    MATRICE_350_RTK(
        modelName = "DJI Matrice 350 RTK",
        shortName = "M350 RTK",
        series = "Flagship Enterprise Heavy-Lift Platform",
        thermalSpecs = "Zenmuse H20T / H30T Payload (640×512 Radiometric IR, 1280×1024 Infrared)",
        opticalSpecs = "Zenmuse H20T 20MP Zoom (23× Hybrid), Laser Rangefinder (1200m)",
        transmission = "DJI O3 Enterprise (20km 3-Channel 1080p)",
        isDockCompatible = false
    ),
    MAVIC_3_THERMAL(
        modelName = "DJI Mavic 3 Thermal (M3T)",
        shortName = "Mavic 3T",
        series = "Compact Portable Enterprise Scouting",
        thermalSpecs = "640×512 Radiometric Thermal, 40mm equiv., 28× Digital Zoom",
        opticalSpecs = "48MP Wide, 12MP Tele (56× Hybrid Zoom)",
        transmission = "DJI O3 Enterprise (15km 1080p)",
        isDockCompatible = false
    ),
    MATRICE_30T(
        modelName = "DJI Matrice 30T",
        shortName = "M30T",
        series = "All-Weather Rugged IP55 Enterprise",
        thermalSpecs = "640×512 Radiometric Thermal, 40mm equiv.",
        opticalSpecs = "48MP Zoom (16× Optical, 200× Max), 12MP Wide, Laser Rangefinder",
        transmission = "DJI O3 Enterprise (15km 4-Antenna)",
        isDockCompatible = true
    )
}

data class DjiComprehensiveTelemetry(
    val timestampMs: Long = System.currentTimeMillis(),
    val modelName: String = "DJI Matrice 4TD",
    val serialNumber: String = "1581F5GBA23049281",
    val firmwareVersion: String = "v07.01.01.00",
    val flightMode: DjiFlightMode = DjiFlightMode.GPS_ATTI,
    val flightTimeSeconds: Int = 412,
    val isMotorsOn: Boolean = true,
    val isFlying: Boolean = true,
    val gps: DjiGpsTelemetry = DjiGpsTelemetry(),
    val attitude: DjiAttitudeTelemetry = DjiAttitudeTelemetry(),
    val velocity: DjiVelocityTelemetry = DjiVelocityTelemetry(),
    val battery: DjiBatteryTelemetry = DjiBatteryTelemetry(),
    val gimbal: DjiGimbalTelemetry = DjiGimbalTelemetry(),
    val airLink: DjiAirLinkTelemetry = DjiAirLinkTelemetry(),
    val homeLocationLat: Double = 47.6058,
    val homeLocationLng: Double = -122.3315,
    val distanceToHomeMeters: Float = 58.2f
)
