package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ThermalAiService
import com.example.ai.model.AiCopilotMessage
import com.example.ai.model.AiMessageSender
import com.example.ai.model.AiMissionReport
import com.example.ai.model.AiUiState
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.model.ThermalFrameData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ThermalAiViewModel"

class ThermalAiViewModel(
    private val aiService: ThermalAiService = ThermalAiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiCopilotMessage>>(emptyList())
    val messages: StateFlow<List<AiCopilotMessage>> = _messages.asStateFlow()

    private val _latestReport = MutableStateFlow<AiMissionReport?>(null)
    val latestReport: StateFlow<AiMissionReport?> = _latestReport.asStateFlow()

    private var activeJob: Job? = null

    init {
        // Initial welcome copilot briefing
        _messages.value = listOf(
            AiCopilotMessage(
                sender = AiMessageSender.GEMINI_COPILOT,
                text = "Thermal AI Copilot linked via Firebase AI Logic (Gemini 3.8 Flash). Standing by for real-time Matrice 4TD radiometric inspection analysis, anomaly diagnostics, and location intelligence."
            )
        )
    }

    /**
     * Sends a query to the Gemini model and streams the diagnostic response.
     */
    fun sendCopilotQuery(
        prompt: String,
        telemetry: DjiComprehensiveTelemetry,
        frameData: ThermalFrameData
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val userMessage = AiCopilotMessage(
            sender = AiMessageSender.PILOT,
            text = trimmed,
            telemetrySnapshot = "ALT ${String.format("%.1f", telemetry.gps.altitudeAglMeters)}m | PEAK ${String.format("%.1f°C", frameData.maxTemp)} | ${telemetry.gps.formatCoordinates()}"
        )
        _messages.value = _messages.value + userMessage

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val accumulated = StringBuilder()

            try {
                aiService.analyzeThermalFrameStream(trimmed, telemetry, frameData)
                    .collect { chunk ->
                        accumulated.append(chunk)
                        _uiState.value = AiUiState.Streaming(accumulated.toString())
                    }

                val finalOutput = accumulated.toString()
                if (finalOutput.isNotBlank()) {
                    _uiState.value = AiUiState.Success(finalOutput)
                    _messages.value = _messages.value + AiCopilotMessage(
                        sender = AiMessageSender.GEMINI_COPILOT,
                        text = finalOutput
                    )
                } else {
                    _uiState.value = AiUiState.Error("Model returned empty diagnostic output.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                val errMsg = e.localizedMessage ?: "AI analysis failed. Please verify network and App Check."
                _uiState.value = AiUiState.Error(errMsg)
                _messages.value = _messages.value + AiCopilotMessage(
                    sender = AiMessageSender.GEMINI_COPILOT,
                    text = "⚠️ Diagnostic Alert: $errMsg"
                )
            }
        }
    }

    /**
     * Generates a formal structured JSON mission report for the current scene.
     */
    fun generateStructuredMissionReport(
        telemetry: DjiComprehensiveTelemetry,
        frameData: ThermalFrameData,
        fieldNotes: String = ""
    ) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val report = aiService.generateStructuredReport(telemetry, frameData, fieldNotes)
                _latestReport.value = report
                _uiState.value = AiUiState.StructuredSuccess(report)

                _messages.value = _messages.value + AiCopilotMessage(
                    sender = AiMessageSender.GEMINI_COPILOT,
                    text = "📋 Structured Mission Report Generated:\n" +
                            "• Anomaly: ${report.anomalyClassification} [${report.severityLevel}]\n" +
                            "• Target Temp: ${String.format("%.1f°C", report.estimatedTargetTempC)} (ΔT: +${String.format("%.1f°C", report.deltaTempFromAmbientC)})\n" +
                            "• Flight Action: ${report.flightRecommendation}\n" +
                            "• Geo Context: ${report.geographicContext}\n" +
                            "• SOP Action: ${report.emergencyActionSummary}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Report generation failed", e)
                val errMsg = e.localizedMessage ?: "Failed to generate structured report."
                _uiState.value = AiUiState.Error(errMsg)
            }
        }
    }

    fun clearHistory() {
        _messages.value = listOf(
            AiCopilotMessage(
                sender = AiMessageSender.GEMINI_COPILOT,
                text = "History cleared. Thermal AI Copilot standing by."
            )
        )
        _uiState.value = AiUiState.Idle
    }
}
