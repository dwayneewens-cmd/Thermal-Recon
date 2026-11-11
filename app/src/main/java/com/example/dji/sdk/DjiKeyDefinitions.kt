package com.example.dji.sdk

/**
 * Clean architectural abstraction of DJI Mobile SDK KeyManager keys (MSDK V4 & V5).
 * Mirrors DJI's Key-Value payload subscription architecture.
 */
sealed class DjiKey<T>(val component: String, val identifier: String) {

    // Flight Controller Telemetry Keys
    object AircraftLocation : DjiKey<Pair<Double, Double>>("FlightController", "AircraftLocation")
    object AltitudeMsl : DjiKey<Double>("FlightController", "AltitudeMsl")
    object AltitudeAgl : DjiKey<Float>("FlightController", "AltitudeAgl")
    object CompassHeading : DjiKey<Float>("FlightController", "CompassHeading")
    object AttitudePitch : DjiKey<Float>("FlightController", "AttitudePitch")
    object AttitudeRoll : DjiKey<Float>("FlightController", "AttitudeRoll")
    object AttitudeYaw : DjiKey<Float>("FlightController", "AttitudeYaw")
    object VelocityX : DjiKey<Float>("FlightController", "VelocityX")
    object VelocityY : DjiKey<Float>("FlightController", "VelocityY")
    object VelocityZ : DjiKey<Float>("FlightController", "VelocityZ")
    object GpsSatelliteCount : DjiKey<Int>("FlightController", "GpsSatelliteCount")
    object FlightModeString : DjiKey<String>("FlightController", "FlightMode")
    object AreMotorsOn : DjiKey<Boolean>("FlightController", "AreMotorsOn")
    object IsFlying : DjiKey<Boolean>("FlightController", "IsFlying")
    object HomeLocation : DjiKey<Pair<Double, Double>>("FlightController", "HomeLocation")

    // Battery Telemetry Keys
    object BatteryPercent : DjiKey<Int>("Battery", "ChargeRemainingPercent")
    object BatteryVoltage : DjiKey<Int>("Battery", "VoltageMv")
    object BatteryTemperature : DjiKey<Float>("Battery", "TemperatureCelsius")

    // Gimbal Telemetry Keys
    object GimbalPitch : DjiKey<Float>("Gimbal", "PitchDegrees")
    object GimbalYaw : DjiKey<Float>("Gimbal", "YawDegrees")

    // AirLink Telemetry Keys
    object DownlinkQuality : DjiKey<Int>("AirLink", "DownlinkSignalQuality")
    object UplinkQuality : DjiKey<Int>("AirLink", "UplinkSignalQuality")
}

/**
 * Callback listener interface mirroring DJI KeyManager's addListener() callback.
 */
fun interface DjiKeyListener<T> {
    fun onValueChange(oldValue: T?, newValue: T)
}
