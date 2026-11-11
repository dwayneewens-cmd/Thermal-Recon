package com.example.dji.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiConnectionMode
import com.example.dji.model.DjiConnectionState
import com.example.dji.model.DjiFlightMode
import com.example.dji.model.FlightPattern
import com.example.dji.sdk.DjiKey
import com.example.dji.sdk.DjiKeyListener
import com.example.dji.sdk.DjiNetworkTelemetryParser
import com.example.dji.sdk.DjiSdkSimulatorBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap

/**
 * Android Service architecture interfacing with DJI Mobile SDK (MSDK).
 * Runs as a bound service, streaming real-time drone telemetry (GPS coordinates,
 * altitude, compass heading, attitude, velocity, battery) to subscribers via Coroutine Flow.
 */
class DjiTelemetryService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Bridge Engines
    private val simulatorBridge = DjiSdkSimulatorBridge()
    private val networkParser = DjiNetworkTelemetryParser()

    // StateFlows
    private val _telemetry = MutableStateFlow(simulatorBridge.tick(0.0))
    val telemetry: StateFlow<DjiComprehensiveTelemetry> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow(DjiConnectionState.PRODUCT_CONNECTED)
    val connectionState: StateFlow<DjiConnectionState> = _connectionState.asStateFlow()

    private val _connectionMode = MutableStateFlow(DjiConnectionMode.SIMULATOR_BRIDGE)
    val connectionMode: StateFlow<DjiConnectionMode> = _connectionMode.asStateFlow()

    private val _flightPattern = MutableStateFlow(FlightPattern.ORBIT_INSPECTION)
    val flightPattern: StateFlow<FlightPattern> = _flightPattern.asStateFlow()

    // KeyManager Listener Registry (mirroring MSDK KeyManager.getInstance().listen)
    private val keyListeners = ConcurrentHashMap<DjiKey<*>, MutableList<DjiKeyListener<*>>>()

    private var telemetryJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var djiAppKey: String = "DJI-MSDK-V5-ENTERPRISE-KEY"

    inner class LocalBinder : Binder() {
        fun getService(): DjiTelemetryService = this@DjiTelemetryService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DjiTelemetryService created")
        startTelemetryLoop()
    }

    /**
     * Initializes the DJI Mobile SDK registration pipeline
     */
    fun initializeSdk(appKey: String, onComplete: (success: Boolean, message: String) -> Unit) {
        djiAppKey = appKey
        _connectionState.value = DjiConnectionState.INITIALIZING_SDK
        serviceScope.launch {
            delay(400) // Simulating MSDK registration handshake
            _connectionState.value = DjiConnectionState.REGISTERING_APP_KEY
            delay(500)
            _connectionState.value = DjiConnectionState.PRODUCT_CONNECTED
            delay(200)
            _connectionState.value = DjiConnectionState.TELEMETRY_STREAMING
            onComplete(true, "DJI Mobile SDK registered successfully with key: ${appKey.take(8)}...")
        }
    }

    /**
     * Connects to aircraft using specified connection mode
     */
    fun setConnectionMode(mode: DjiConnectionMode) {
        _connectionMode.value = mode
        when (mode) {
            DjiConnectionMode.SIMULATOR_BRIDGE -> {
                _connectionState.value = DjiConnectionState.TELEMETRY_STREAMING
                startTelemetryLoop()
            }
            DjiConnectionMode.NETWORK_OSDK -> {
                _connectionState.value = DjiConnectionState.CONNECTING_PRODUCT
                startUdpTelemetryListener(8080)
            }
            DjiConnectionMode.USB_RC_BRIDGE -> {
                _connectionState.value = DjiConnectionState.CONNECTING_PRODUCT
                serviceScope.launch {
                    delay(800)
                    _connectionState.value = DjiConnectionState.TELEMETRY_STREAMING
                }
            }
        }
    }

    fun setFlightPattern(pattern: FlightPattern) {
        _flightPattern.value = pattern
        simulatorBridge.activePattern = pattern
    }

    fun setHomeLocation(lat: Double, lng: Double) {
        simulatorBridge.homeLatitude = lat
        simulatorBridge.homeLongitude = lng
        simulatorBridge.targetLatitude = lat + 0.0006
        simulatorBridge.targetLongitude = lng + 0.0009
    }

    fun setAircraftModel(model: String) {
        simulatorBridge.aircraftModel = model
        _telemetry.value = _telemetry.value.copy(modelName = model)
    }

    fun triggerReturnToHome() {
        serviceScope.launch {
            val current = _telemetry.value
            _telemetry.value = current.copy(flightMode = DjiFlightMode.RETURN_TO_HOME)
            delay(1200)
            setFlightPattern(FlightPattern.STATIONARY_HOVER)
        }
    }

    /**
     * Registers a listener for DJI KeyManager value changes
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> addKeyListener(key: DjiKey<T>, listener: DjiKeyListener<T>) {
        val list = keyListeners.getOrPut(key) { mutableListOf() }
        list.add(listener as DjiKeyListener<*>)
    }

    fun removeKeyListener(key: DjiKey<*>) {
        keyListeners.remove(key)
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            val intervalMs = 100L // 10Hz standard DJI MSDK telemetry frequency
            while (isActive) {
                if (_connectionMode.value == DjiConnectionMode.SIMULATOR_BRIDGE) {
                    val prev = _telemetry.value
                    val updated = simulatorBridge.tick(intervalMs / 1000.0)
                    _telemetry.value = updated
                    dispatchKeyManagerEvents(prev, updated)
                }
                delay(intervalMs)
            }
        }
    }

    private fun startUdpTelemetryListener(port: Int) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch(Dispatchers.IO) {
            try {
                udpSocket?.close()
                val socket = DatagramSocket(port)
                udpSocket = socket
                val buffer = ByteArray(1024)
                _connectionState.value = DjiConnectionState.TELEMETRY_STREAMING

                while (isActive && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val rawStr = String(packet.data, 0, packet.length)
                    val parsed = networkParser.parseJsonTelemetry(rawStr)
                        ?: networkParser.parseBinaryTelemetry(packet.data)
                    if (parsed != null) {
                        val prev = _telemetry.value
                        _telemetry.value = parsed
                        dispatchKeyManagerEvents(prev, parsed)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP listener error: ${e.message}")
                _connectionState.value = DjiConnectionState.CONNECTION_LOST
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchKeyManagerEvents(prev: DjiComprehensiveTelemetry, curr: DjiComprehensiveTelemetry) {
        // GPS Altitude
        if (prev.gps.altitudeAglMeters != curr.gps.altitudeAglMeters) {
            keyListeners[DjiKey.AltitudeAgl]?.forEach { listener ->
                (listener as? DjiKeyListener<Float>)?.onValueChange(prev.gps.altitudeAglMeters, curr.gps.altitudeAglMeters)
            }
        }
        // Compass Heading
        if (prev.attitude.compassHeadingDegrees != curr.attitude.compassHeadingDegrees) {
            keyListeners[DjiKey.CompassHeading]?.forEach { listener ->
                (listener as? DjiKeyListener<Float>)?.onValueChange(prev.attitude.compassHeadingDegrees, curr.attitude.compassHeadingDegrees)
            }
        }
        // Location (Lat, Lng)
        if (prev.gps.latitude != curr.gps.latitude || prev.gps.longitude != curr.gps.longitude) {
            keyListeners[DjiKey.AircraftLocation]?.forEach { listener ->
                (listener as? DjiKeyListener<Pair<Double, Double>>)?.onValueChange(
                    Pair(prev.gps.latitude, prev.gps.longitude),
                    Pair(curr.gps.latitude, curr.gps.longitude)
                )
            }
        }
        // Battery %
        if (prev.battery.chargeRemainingPercent != curr.battery.chargeRemainingPercent) {
            keyListeners[DjiKey.BatteryPercent]?.forEach { listener ->
                (listener as? DjiKeyListener<Int>)?.onValueChange(
                    prev.battery.chargeRemainingPercent,
                    curr.battery.chargeRemainingPercent
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DjiTelemetryService destroyed")
        telemetryJob?.cancel()
        serviceScope.cancel()
        udpSocket?.close()
    }

    companion object {
        private const val TAG = "DjiTelemetryService"
    }
}
