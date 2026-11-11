package com.example.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class DeviceAttitude(
    val pitch: Float = 0f,
    val roll: Float = 0f
)

data class DeviceLocation(
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val altitudeMeters: Float = 34.0f,
    val hasLocation: Boolean = false
)

class TelemetrySensorManager(private val context: Context) : SensorEventListener, LocationListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _attitude = MutableStateFlow(DeviceAttitude())
    val attitude: StateFlow<DeviceAttitude> = _attitude.asStateFlow()

    private val _location = MutableStateFlow(DeviceLocation())
    val location: StateFlow<DeviceLocation> = _location.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var isRegistered = false

    fun startListening() {
        if (isRegistered) return
        isRegistered = true
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        tryStartLocation()
    }

    fun stopListening() {
        if (!isRegistered) return
        isRegistered = false
        sensorManager?.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun tryStartLocation() {
        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            val provider = when {
                hasGps -> LocationManager.GPS_PROVIDER
                hasNetwork -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider != null) {
                locationManager?.requestLocationUpdates(provider, 3000L, 5f, this)
                val lastLoc = locationManager?.getLastKnownLocation(provider)
                if (lastLoc != null) {
                    onLocationChanged(lastLoc)
                }
            }
        } catch (e: Exception) {
            Log.d("TelemetrySensorManager", "Location permission not yet granted or unavailable: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val alpha = 0.8f
            gravityValues[0] = alpha * gravityValues[0] + (1 - alpha) * event.values[0]
            gravityValues[1] = alpha * gravityValues[1] + (1 - alpha) * event.values[1]
            gravityValues[2] = alpha * gravityValues[2] + (1 - alpha) * event.values[2]

            val ax = gravityValues[0]
            val ay = gravityValues[1]
            val az = gravityValues[2]

            // Calculate Pitch & Roll
            val pitch = Math.toDegrees(atan2(-ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))).toFloat()
            val roll = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()

            _attitude.value = DeviceAttitude(pitch = pitch, roll = roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(loc: Location) {
        _location.value = DeviceLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitudeMeters = if (loc.hasAltitude()) loc.altitude.toFloat() else 45f,
            hasLocation = true
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
