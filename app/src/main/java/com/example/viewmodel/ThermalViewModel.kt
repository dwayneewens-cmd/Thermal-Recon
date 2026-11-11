package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ThermalDatabase
import com.example.data.ThermalReportEntity
import com.example.model.AlertSeverity
import com.example.model.CameraFeedSource
import com.example.model.DroneTelemetry
import com.example.model.HeatSignatureType
import com.example.model.InspectionSpot
import com.example.model.LabScenario
import com.example.model.RoiBox
import com.example.model.TemperatureUnit
import com.example.model.ThermalAlert
import com.example.model.ThermalFrameData
import com.example.model.ThermalPalette
import com.example.sensors.AlertAudioManager
import com.example.sensors.DeviceAttitude
import com.example.sensors.DeviceLocation
import com.example.sensors.TelemetrySensorManager
import com.example.thermal.ThermalEngine
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiConnectionMode
import com.example.dji.model.DjiConnectionState
import com.example.dji.model.FlightPattern
import com.example.dji.repository.DjiTelemetryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ThermalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ThermalDatabase.getInstance(application)
    private val reportDao = db.thermalReportDao()
    private val audioManager = AlertAudioManager(application)
    private val sensorManager = TelemetrySensorManager(application)
    private val thermalEngine = ThermalEngine()
    private val djiRepository = DjiTelemetryRepository(application)

    // DJI Mobile SDK Telemetry Flows
    val djiTelemetry: StateFlow<DjiComprehensiveTelemetry> = djiRepository.telemetry
    val djiConnectionState: StateFlow<DjiConnectionState> = djiRepository.connectionState
    val djiConnectionMode: StateFlow<DjiConnectionMode> = djiRepository.connectionMode
    val djiFlightPattern: StateFlow<FlightPattern> = djiRepository.flightPattern

    val reportsList: StateFlow<List<ThermalReportEntity>> = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera Feed Source
    private val _feedSource = MutableStateFlow(CameraFeedSource.LAB_SIMULATION)
    val feedSource: StateFlow<CameraFeedSource> = _feedSource.asStateFlow()

    private val _labScenario = MutableStateFlow(LabScenario.SEARCH_AND_RESCUE)
    val labScenario: StateFlow<LabScenario> = _labScenario.asStateFlow()

    // Palette & Units
    private val _palette = MutableStateFlow(ThermalPalette.IRONBOW)
    val palette: StateFlow<ThermalPalette> = _palette.asStateFlow()

    private val _tempUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val tempUnit: StateFlow<TemperatureUnit> = _tempUnit.asStateFlow()

    // Thresholds & Isotherms
    private val _alertThreshold = MutableStateFlow(55.0f)
    val alertThreshold: StateFlow<Float> = _alertThreshold.asStateFlow()

    private val _isothermEnabled = MutableStateFlow(false)
    val isothermEnabled: StateFlow<Boolean> = _isothermEnabled.asStateFlow()

    private val _isothermMin = MutableStateFlow(34.0f)
    val isothermMin: StateFlow<Float> = _isothermMin.asStateFlow()

    private val _isothermMax = MutableStateFlow(42.0f)
    val isothermMax: StateFlow<Float> = _isothermMax.asStateFlow()

    // Heatmap Overlay & Fusion Visualization
    private val _heatmapOpacity = MutableStateFlow(0.75f)
    val heatmapOpacity: StateFlow<Float> = _heatmapOpacity.asStateFlow()

    private val _fusionMode = MutableStateFlow(com.example.model.ThermalFusionMode.HEATMAP_OVERLAY)
    val fusionMode: StateFlow<com.example.model.ThermalFusionMode> = _fusionMode.asStateFlow()

    private val _showContourLines = MutableStateFlow(true)
    val showContourLines: StateFlow<Boolean> = _showContourLines.asStateFlow()

    private val _showHeatGlow = MutableStateFlow(true)
    val showHeatGlow: StateFlow<Boolean> = _showHeatGlow.asStateFlow()

    // Audio & Haptic Alarm Toggles
    private val _soundAlarmEnabled = MutableStateFlow(true)
    val soundAlarmEnabled: StateFlow<Boolean> = _soundAlarmEnabled.asStateFlow()

    private val _hapticAlarmEnabled = MutableStateFlow(true)
    val hapticAlarmEnabled: StateFlow<Boolean> = _hapticAlarmEnabled.asStateFlow()

    // Freeze & ROI
    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen.asStateFlow()

    private val _roiBox = MutableStateFlow(RoiBox(0.2f, 0.2f, 0.8f, 0.8f, enabled = false))
    val roiBox: StateFlow<RoiBox> = _roiBox.asStateFlow()

    // Interactive Inspection Pins
    private val _inspectionSpots = MutableStateFlow<List<InspectionSpot>>(emptyList())
    val inspectionSpots: StateFlow<List<InspectionSpot>> = _inspectionSpots.asStateFlow()

    // Drone Telemetry
    private val _droneTelemetry = MutableStateFlow(DroneTelemetry())
    val droneTelemetry: StateFlow<DroneTelemetry> = _droneTelemetry.asStateFlow()

    // Frame Data
    private val _currentFrame = MutableStateFlow(
        thermalEngine.generateScenarioFrame(LabScenario.SEARCH_AND_RESCUE, _roiBox.value)
    )
    val currentFrame: StateFlow<ThermalFrameData> = _currentFrame.asStateFlow()

    // Active Alerts History & HUD Flashing Banner
    private val _alertsLog = MutableStateFlow<List<ThermalAlert>>(emptyList())
    val alertsLog: StateFlow<List<ThermalAlert>> = _alertsLog.asStateFlow()

    private val _activeAlertBanner = MutableStateFlow<ThermalAlert?>(null)
    val activeAlertBanner: StateFlow<ThermalAlert?> = _activeAlertBanner.asStateFlow()

    // Hardware Sensors
    val deviceAttitude: StateFlow<DeviceAttitude> = sensorManager.attitude
    val deviceLocation: StateFlow<DeviceLocation> = sensorManager.location

    // Emissivity & Ambient
    val currentEmissivity: Float get() = thermalEngine.emissivity
    val currentAmbient: Float get() = thermalEngine.ambientTempCelsius

    private var simulationLoopJob: Job? = null
    private var lastAlarmTriggerTime: Long = 0

    init {
        sensorManager.startListening()
        startSimulationLoop()
        addInitialAlerts()
        observeDjiTelemetry()
    }

    private fun observeDjiTelemetry() {
        viewModelScope.launch {
            djiRepository.telemetry.collect { dji ->
                _droneTelemetry.value = _droneTelemetry.value.copy(
                    connected = dji.isFlying,
                    modelName = dji.modelName,
                    batteryPercent = dji.battery.chargeRemainingPercent,
                    altitudeMeters = dji.gps.altitudeAglMeters,
                    groundSpeedKmh = dji.velocity.groundSpeedKmh,
                    pitchDegrees = dji.attitude.pitchDegrees,
                    rollDegrees = dji.attitude.rollDegrees,
                    yawDegrees = dji.attitude.compassHeadingDegrees,
                    gpsSatellites = dji.gps.satelliteCount,
                    signalDbm = dji.airLink.signalDbm,
                    latitude = dji.gps.latitude,
                    longitude = dji.gps.longitude
                )
            }
        }
    }

    private fun addInitialAlerts() {
        _alertsLog.value = listOf(
            ThermalAlert(
                title = "SAR Heat Signature Locked",
                description = "Living core biological thermal signature (37.4°C) acquired at grid Sector 4",
                peakTemp = 37.4f,
                thresholdTemp = 35.0f,
                signatureType = HeatSignatureType.HUMAN_LIVING,
                severity = AlertSeverity.ADVISORY,
                location = "47.6062° N, 122.3321° W (Alt: 48m)"
            )
        )
    }

    private fun startSimulationLoop() {
        simulationLoopJob?.cancel()
        simulationLoopJob = viewModelScope.launch {
            while (isActive) {
                if (!_isFrozen.value && _feedSource.value != CameraFeedSource.DEVICE_CAMERA) {
                    val newFrame = thermalEngine.generateScenarioFrame(_labScenario.value, _roiBox.value)
                    _currentFrame.value = newFrame
                    checkThresholdBreaches(newFrame)

                    // Update live inspection spots temperatures based on new frame
                    updateSpotTemperatures(newFrame)

                    // Update simulated drone telemetry oscillations
                    if (_feedSource.value == CameraFeedSource.DJI_DRONE_STREAM || _feedSource.value == CameraFeedSource.LAB_SIMULATION) {
                        updateDroneTelemetry()
                    }
                }
                delay(66L) // ~15-20 FPS for silky responsive thermal synthesis
            }
        }
    }

    private fun updateDroneTelemetry() {
        val current = _droneTelemetry.value
        val att = sensorManager.attitude.value
        val loc = sensorManager.location.value
        _droneTelemetry.value = current.copy(
            pitchDegrees = if (att.pitch != 0f) att.pitch else -11.5f,
            rollDegrees = if (att.roll != 0f) att.roll else 1.8f,
            latitude = if (loc.hasLocation) loc.latitude else 47.6062,
            longitude = if (loc.hasLocation) loc.longitude else -122.3321,
            altitudeMeters = if (loc.hasLocation) loc.altitudeMeters else 48.5f
        )
    }

    fun onCameraBitmapAvailable(bitmap: Bitmap) {
        if (_isFrozen.value || _feedSource.value != CameraFeedSource.DEVICE_CAMERA) return
        val newFrame = thermalEngine.processCameraBitmap(bitmap, _roiBox.value)
        _currentFrame.value = newFrame
        checkThresholdBreaches(newFrame)
        updateSpotTemperatures(newFrame)
    }

    private fun updateSpotTemperatures(frame: ThermalFrameData) {
        if (_inspectionSpots.value.isEmpty()) return
        val updated = _inspectionSpots.value.map { spot ->
            val xIdx = (spot.xNorm * (frame.matrixWidth - 1)).toInt().coerceIn(0, frame.matrixWidth - 1)
            val yIdx = (spot.yNorm * (frame.matrixHeight - 1)).toInt().coerceIn(0, frame.matrixHeight - 1)
            val temp = frame.temperatures[yIdx * frame.matrixWidth + xIdx]
            spot.copy(temperature = temp)
        }
        _inspectionSpots.value = updated
    }

    private fun checkThresholdBreaches(frame: ThermalFrameData) {
        val peak = if (_roiBox.value.enabled) frame.roiMaxTemp else frame.maxTemp
        val threshold = _alertThreshold.value
        val now = System.currentTimeMillis()

        if (peak >= threshold) {
            val primarySig = frame.detectedSignatures.firstOrNull() ?: HeatSignatureType.INDUSTRIAL_OVERHEAT
            val alert = ThermalAlert(
                title = "High Heat Anomaly: ${_tempUnit.value.format(peak)}",
                description = "Critical threshold (${_tempUnit.value.format(threshold)}) breached. Type: ${primarySig.title}",
                peakTemp = peak,
                thresholdTemp = threshold,
                signatureType = primarySig,
                severity = AlertSeverity.CRITICAL,
                location = "${String.format("%.4f", _droneTelemetry.value.latitude)}° N, ${String.format("%.4f", _droneTelemetry.value.longitude)}° W"
            )
            _activeAlertBanner.value = alert

            // Rate-limit audio/haptic alert trigger every 3 seconds
            if (now - lastAlarmTriggerTime > 3000L) {
                lastAlarmTriggerTime = now
                if (_soundAlarmEnabled.value) {
                    audioManager.playCriticalAlarm()
                }
                // Add to log if not already recent
                val currentLog = _alertsLog.value.toMutableList()
                currentLog.add(0, alert)
                if (currentLog.size > 50) currentLog.removeAt(currentLog.lastIndex)
                _alertsLog.value = currentLog
            }
        } else {
            // Check if banner should auto-clear
            if (_activeAlertBanner.value?.severity == AlertSeverity.CRITICAL && now - lastAlarmTriggerTime > 4000L) {
                _activeAlertBanner.value = null
            }
        }
    }

    fun dismissAlertBanner() {
        _activeAlertBanner.value = null
    }

    fun setFeedSource(source: CameraFeedSource) {
        _feedSource.value = source
        audioManager.playClick()
    }

    fun setLabScenario(scenario: LabScenario) {
        _labScenario.value = scenario
        audioManager.playClick()
    }

    fun setPalette(palette: ThermalPalette) {
        _palette.value = palette
        audioManager.playClick()
    }

    fun cycleNextPalette(): ThermalPalette {
        val all = ThermalPalette.values()
        val nextIdx = (all.indexOf(_palette.value) + 1) % all.size
        val next = all[nextIdx]
        _palette.value = next
        audioManager.playClick()
        return next
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        _tempUnit.value = unit
        audioManager.playClick()
    }

    fun setAlertThreshold(threshold: Float) {
        _alertThreshold.value = threshold
    }

    fun toggleIsotherm(enabled: Boolean) {
        _isothermEnabled.value = enabled
        audioManager.playClick()
    }

    fun setIsothermRange(min: Float, max: Float) {
        _isothermMin.value = min
        _isothermMax.value = max
    }

    fun setHeatmapOpacity(opacity: Float) {
        _heatmapOpacity.value = opacity.coerceIn(0.1f, 1.0f)
    }

    fun setFusionMode(mode: com.example.model.ThermalFusionMode) {
        _fusionMode.value = mode
        audioManager.playClick()
    }

    fun toggleContourLines() {
        _showContourLines.value = !_showContourLines.value
        audioManager.playClick()
    }

    fun toggleHeatGlow() {
        _showHeatGlow.value = !_showHeatGlow.value
        audioManager.playClick()
    }

    fun toggleFreeze() {
        _isFrozen.value = !_isFrozen.value
        audioManager.playClick()
    }

    fun toggleSoundAlarm() {
        _soundAlarmEnabled.value = !_soundAlarmEnabled.value
        audioManager.playClick()
    }

    fun toggleHapticAlarm() {
        _hapticAlarmEnabled.value = !_hapticAlarmEnabled.value
        audioManager.playClick()
    }

    fun toggleRoi(enabled: Boolean) {
        _roiBox.value = _roiBox.value.copy(enabled = enabled)
        audioManager.playClick()
    }

    fun updateRoiBounds(left: Float, top: Float, right: Float, bottom: Float) {
        _roiBox.value = _roiBox.value.copy(
            left = left.coerceIn(0.05f, 0.95f),
            top = top.coerceIn(0.05f, 0.95f),
            right = right.coerceIn(0.05f, 0.95f),
            bottom = bottom.coerceIn(0.05f, 0.95f)
        )
    }

    fun addInspectionSpot(xNorm: Float, yNorm: Float) {
        val frame = _currentFrame.value
        val xIdx = (xNorm * (frame.matrixWidth - 1)).toInt().coerceIn(0, frame.matrixWidth - 1)
        val yIdx = (yNorm * (frame.matrixHeight - 1)).toInt().coerceIn(0, frame.matrixHeight - 1)
        val temp = frame.temperatures[yIdx * frame.matrixWidth + xIdx]
        val spotNum = _inspectionSpots.value.size + 1
        val newSpot = InspectionSpot(
            id = UUID.randomUUID().toString(),
            xNorm = xNorm,
            yNorm = yNorm,
            label = "P$spotNum",
            temperature = temp
        )
        _inspectionSpots.value = _inspectionSpots.value + newSpot
        audioManager.playClick()
    }

    fun removeInspectionSpot(spotId: String) {
        _inspectionSpots.value = _inspectionSpots.value.filter { it.id != spotId }
        audioManager.playClick()
    }

    fun clearInspectionSpots() {
        _inspectionSpots.value = emptyList()
        audioManager.playClick()
    }

    fun setEmissivity(value: Float) {
        thermalEngine.emissivity = value.coerceIn(0.1f, 1.0f)
    }

    fun setAmbientTemp(value: Float) {
        thermalEngine.ambientTempCelsius = value
    }

    fun setCalibrationOffset(offset: Float) {
        thermalEngine.calibrationOffset = offset
    }

    fun updateDroneRtspUrl(url: String) {
        _droneTelemetry.value = _droneTelemetry.value.copy(rtspUrl = url)
    }

    fun testAlarm() {
        audioManager.playCriticalAlarm()
    }

    fun saveSnapshotReport(title: String = "Thermal Inspection Snapshot", notes: String = "") {
        viewModelScope.launch {
            val frame = _currentFrame.value
            val tele = _droneTelemetry.value
            val primarySig = frame.detectedSignatures.firstOrNull()?.title ?: "Ambient"
            val entity = ThermalReportEntity(
                title = title,
                timestamp = System.currentTimeMillis(),
                maxTemp = frame.maxTemp,
                minTemp = frame.minTemp,
                avgTemp = frame.avgTemp,
                centerTemp = frame.centerSpotTemp,
                paletteName = _palette.value.title,
                signatureDetected = primarySig,
                droneModel = tele.modelName,
                altitudeMeters = tele.altitudeMeters,
                gpsCoordinates = "${String.format("%.4f", tele.latitude)}° N, ${String.format("%.4f", tele.longitude)}° W",
                notes = notes,
                alertTriggered = frame.maxTemp >= _alertThreshold.value
            )
            reportDao.insertReport(entity)
            audioManager.playWarningBeep()
        }
    }

    fun deleteReport(report: ThermalReportEntity) {
        viewModelScope.launch {
            reportDao.deleteReport(report)
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            reportDao.clearAllReports()
        }
    }

    // DJI Mobile SDK Telemetry Controls
    fun initializeDjiSdk(appKey: String, onResult: (Boolean, String) -> Unit) {
        djiRepository.initializeSdk(appKey, onResult)
    }

    fun setDjiConnectionMode(mode: DjiConnectionMode) {
        djiRepository.setConnectionMode(mode)
    }

    fun setDjiFlightPattern(pattern: FlightPattern) {
        djiRepository.setFlightPattern(pattern)
    }

    fun setDjiHomeLocation(lat: Double, lng: Double) {
        djiRepository.setHomeLocation(lat, lng)
    }

    fun triggerDjiReturnToHome() {
        djiRepository.triggerReturnToHome()
    }

    private val _selectedAircraftModel = MutableStateFlow(com.example.dji.model.DjiAircraftModel.MATRICE_4TD)
    val selectedAircraftModel: StateFlow<com.example.dji.model.DjiAircraftModel> = _selectedAircraftModel.asStateFlow()

    fun setAircraftModel(model: com.example.dji.model.DjiAircraftModel) {
        _selectedAircraftModel.value = model
        djiRepository.setAircraftModel(model.modelName)
        _droneTelemetry.value = _droneTelemetry.value.copy(modelName = "${model.modelName} // Radiometric IR")
        audioManager.playClick()
    }

    override fun onCleared() {
        super.onCleared()
        simulationLoopJob?.cancel()
        sensorManager.stopListening()
        audioManager.release()
        djiRepository.unbindService()
    }
}
