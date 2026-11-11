package com.example.ai.model

data class AiCopilotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: AiMessageSender,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val telemetrySnapshot: String? = null
)

enum class AiMessageSender {
    PILOT,
    GEMINI_COPILOT
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val partialText: String) : AiUiState
    data class Success(val responseText: String) : AiUiState
    data class StructuredSuccess(val report: AiMissionReport) : AiUiState
    data class Error(val errorMessage: String) : AiUiState
}

data class AiMissionReport(
    val anomalyClassification: String = "Thermal Hotspot",
    val severityLevel: String = "ELEVATED",
    val estimatedTargetTempC: Double = 68.5,
    val deltaTempFromAmbientC: Double = 42.1,
    val flightRecommendation: String = "Hold station at 35m AGL with -90° Nadir gimbal pitch for radiometric capture.",
    val geographicContext: String = "GPS 47.606200° N, 122.332100° W. Industrial utility infrastructure corridor.",
    val emergencyActionSummary: String = "Isolate circuit breaker and dispatch ground thermal maintenance crew.",
    val confidenceScore: Double = 0.94
)
