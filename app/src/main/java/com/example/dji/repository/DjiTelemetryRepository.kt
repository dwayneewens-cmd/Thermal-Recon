package com.example.dji.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiConnectionMode
import com.example.dji.model.DjiConnectionState
import com.example.dji.model.FlightPattern
import com.example.dji.sdk.DjiKey
import com.example.dji.sdk.DjiKeyListener
import com.example.dji.service.DjiTelemetryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Repository layer abstracting the DJI Mobile SDK Service from ViewModels.
 * Provides type-safe Coroutine flows and reactive key observations.
 */
class DjiTelemetryRepository(private val context: Context) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var djiService: DjiTelemetryService? = null
    private var isBound = false

    private val _telemetry = MutableStateFlow(DjiComprehensiveTelemetry())
    val telemetry: StateFlow<DjiComprehensiveTelemetry> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow(DjiConnectionState.DISCONNECTED)
    val connectionState: StateFlow<DjiConnectionState> = _connectionState.asStateFlow()

    private val _connectionMode = MutableStateFlow(DjiConnectionMode.SIMULATOR_BRIDGE)
    val connectionMode: StateFlow<DjiConnectionMode> = _connectionMode.asStateFlow()

    private val _flightPattern = MutableStateFlow(FlightPattern.ORBIT_INSPECTION)
    val flightPattern: StateFlow<FlightPattern> = _flightPattern.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? DjiTelemetryService.LocalBinder
            val boundService = binder?.getService()
            djiService = boundService
            isBound = true
            Log.i(TAG, "DjiTelemetryService connected to Repository")

            boundService?.let { svc ->
                repositoryScope.launch {
                    svc.telemetry.collect { _telemetry.value = it }
                }
                repositoryScope.launch {
                    svc.connectionState.collect { _connectionState.value = it }
                }
                repositoryScope.launch {
                    svc.connectionMode.collect { _connectionMode.value = it }
                }
                repositoryScope.launch {
                    svc.flightPattern.collect { _flightPattern.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "DjiTelemetryService disconnected unexpectedly")
            djiService = null
            isBound = false
            _connectionState.value = DjiConnectionState.DISCONNECTED
        }
    }

    init {
        bindService()
    }

    fun bindService() {
        if (!isBound) {
            val intent = Intent(context, DjiTelemetryService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            djiService = null
        }
    }

    fun initializeSdk(appKey: String, onComplete: (Boolean, String) -> Unit) {
        djiService?.initializeSdk(appKey, onComplete) ?: run {
            onComplete(false, "Service not yet bound")
        }
    }

    fun setConnectionMode(mode: DjiConnectionMode) {
        djiService?.setConnectionMode(mode)
    }

    fun setFlightPattern(pattern: FlightPattern) {
        djiService?.setFlightPattern(pattern)
    }

    fun setHomeLocation(lat: Double, lng: Double) {
        djiService?.setHomeLocation(lat, lng)
    }

    fun setAircraftModel(model: String) {
        djiService?.setAircraftModel(model)
        _telemetry.value = _telemetry.value.copy(modelName = model)
    }

    fun triggerReturnToHome() {
        djiService?.triggerReturnToHome()
    }

    /**
     * Dedicated Flow for Altitude updates (MSDK KeyManager style)
     */
    fun observeAltitudeAgl(): Flow<Float> = callbackFlow {
        val listener = DjiKeyListener<Float> { _, newValue ->
            trySend(newValue)
        }
        djiService?.addKeyListener(DjiKey.AltitudeAgl, listener)
        awaitClose {
            djiService?.removeKeyListener(DjiKey.AltitudeAgl)
        }
    }

    /**
     * Dedicated Flow for Compass Heading updates (0° to 359.9°)
     */
    fun observeCompassHeading(): Flow<Float> = callbackFlow {
        val listener = DjiKeyListener<Float> { _, newValue ->
            trySend(newValue)
        }
        djiService?.addKeyListener(DjiKey.CompassHeading, listener)
        awaitClose {
            djiService?.removeKeyListener(DjiKey.CompassHeading)
        }
    }

    /**
     * Dedicated Flow for GPS Coordinate updates
     */
    fun observeAircraftCoordinates(): Flow<Pair<Double, Double>> = callbackFlow {
        val listener = DjiKeyListener<Pair<Double, Double>> { _, newValue ->
            trySend(newValue)
        }
        djiService?.addKeyListener(DjiKey.AircraftLocation, listener)
        awaitClose {
            djiService?.removeKeyListener(DjiKey.AircraftLocation)
        }
    }

    companion object {
        private const val TAG = "DjiTelemetryRepository"
    }
}
