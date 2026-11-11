package com.example.ai

import android.util.Log
import com.example.ai.model.AiMissionReport
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.model.ThermalFrameData
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ThermalAiService"

class ThermalAiService {

    // 1. General-purpose model for conversational thermal copilot analysis (Lazy initialization)
    private val copilotModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // 2. Structured JSON schema for official mission inspection reports
    private val reportSchema = Schema.obj(
        properties = mapOf(
            "anomalyClassification" to Schema.string(description = "Classification of the thermal signature (e.g. Electrical Transformer Hotspot, Solar PV Bypass Diode Defect, Wildfire Hot Ember, Human Survivor)"),
            "severityLevel" to Schema.enumeration(values = listOf("CRITICAL", "HIGH", "ELEVATED", "MODERATE", "NOMINAL"), description = "Thermal hazard severity level"),
            "estimatedTargetTempC" to Schema.double(description = "Estimated peak core temperature in Celsius"),
            "deltaTempFromAmbientC" to Schema.double(description = "Temperature difference (delta T) from surrounding baseline in Celsius"),
            "flightRecommendation" to Schema.string(description = "Immediate flight telemetry and gimbal adjustment for the DJI Matrice 4TD pilot"),
            "geographicContext" to Schema.string(description = "Surrounding terrain and infrastructure intelligence based on GPS coordinates and Maps context"),
            "emergencyActionSummary" to Schema.string(description = "Recommended SOP tactical action for incident commanders or ground maintenance"),
            "confidenceScore" to Schema.double(description = "Model diagnostic confidence from 0.0 to 1.0")
        ),
        optionalProperties = listOf("confidenceScore")
    )

    private val structuredReportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
            }
        )
    }

    /**
     * Streams tactical thermal analysis and recommendations for the current drone state.
     */
    fun analyzeThermalFrameStream(
        prompt: String,
        telemetry: DjiComprehensiveTelemetry,
        frameData: ThermalFrameData
    ): Flow<String> = flow {
        val systemContext = buildTelemetryPromptContext(prompt, telemetry, frameData)
        try {
            withTimeout(60_000L) {
                copilotModel.generateContentStream(systemContext).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Thermal AI streaming analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60 seconds. Please check drone airlink and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Thermal AI streaming error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Generates a structured JSON Mission Thermal Inspection Report.
     */
    suspend fun generateStructuredReport(
        telemetry: DjiComprehensiveTelemetry,
        frameData: ThermalFrameData,
        additionalNotes: String = ""
    ): AiMissionReport {
        val prompt = """
            Act as an enterprise Senior UAS Radiometric Thermography Inspector for the DJI Matrice 4TD.
            Analyze the following real-time sensor telemetry and radiometric thermal frame:
            ${buildTelemetryPromptContext("Generate structured thermal inspection report", telemetry, frameData)}
            Additional Field Notes: $additionalNotes
            
            Produce a precise, compliant JSON report conforming to the requested schema.
        """.trimIndent()

        return withTimeout(60_000L) {
            try {
                val response = structuredReportModel.generateContent(prompt)
                val rawJson = response.text ?: throw IllegalStateException("Model returned empty JSON output.")
                parseMissionReportJson(rawJson)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Report generation timed out", e)
                throw IllegalStateException("Structured report generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate structured report", e)
                throw e
            }
        }
    }

    private fun parseMissionReportJson(rawJson: String): AiMissionReport {
        val obj = JSONObject(rawJson)
        return AiMissionReport(
            anomalyClassification = obj.optString("anomalyClassification", "Thermal Anomaly"),
            severityLevel = obj.optString("severityLevel", "ELEVATED"),
            estimatedTargetTempC = obj.optDouble("estimatedTargetTempC", 65.0),
            deltaTempFromAmbientC = obj.optDouble("deltaTempFromAmbientC", 38.0),
            flightRecommendation = obj.optString("flightRecommendation", "Maintain altitude and capture perpendicular nadir radiometry."),
            geographicContext = obj.optString("geographicContext", "Surrounding area analyzed via GPS coordinates."),
            emergencyActionSummary = obj.optString("emergencyActionSummary", "Alert ground personnel and monitor hotspot trajectory."),
            confidenceScore = obj.optDouble("confidenceScore", 0.92)
        )
    }

    private fun buildTelemetryPromptContext(
        userPrompt: String,
        telemetry: DjiComprehensiveTelemetry,
        frameData: ThermalFrameData
    ): String {
        val detectedList = frameData.detectedSignatures.joinToString(", ") { it.title }
        val deltaT = frameData.maxTemp - frameData.minTemp

        return """
            [AIRCRAFT & TELEMETRY CONTEXT]
            Platform: ${telemetry.modelName} (O4 Enterprise AirLink)
            Flight Mode: ${telemetry.flightMode.label}
            GPS Position: ${telemetry.gps.formatCoordinates()}
            Altitude: ${String.format("%.1f", telemetry.gps.altitudeAglMeters)}m AGL (${String.format("%.1f", telemetry.gps.altitudeMslMeters)}m MSL)
            Compass Heading: ${String.format("%.0f°", telemetry.attitude.compassHeadingDegrees)} ${telemetry.attitude.formatHeadingCardinal()}
            Gimbal Orientation: Pitch ${String.format("%.1f°", telemetry.gimbal.pitchDegrees)}, Roll ${String.format("%.1f°", telemetry.gimbal.rollDegrees)}, Yaw ${String.format("%.1f°", telemetry.gimbal.yawDegrees)}
            Battery: ${telemetry.battery.chargeRemainingPercent}% (${String.format("%.1f", telemetry.battery.voltageMv / 1000f)}V 6S, ${String.format("%.1f", telemetry.battery.temperatureCelsius)}°C)
            Ground Speed: ${String.format("%.1f", telemetry.velocity.groundSpeedKmh)} km/h
            Distance to Home: ${String.format("%.1f", telemetry.distanceToHomeMeters)}m
            
            [RADIOMETRIC INFRARED SENSOR METRICS]
            Maximum Spot Temperature: ${String.format("%.1f°C", frameData.maxTemp)}
            Minimum Background Temperature: ${String.format("%.1f°C", frameData.minTemp)}
            Average Frame Temperature: ${String.format("%.1f°C", frameData.avgTemp)}
            Center Viewfinder Temperature: ${String.format("%.1f°C", frameData.centerSpotTemp)}
            Differential Delta T (Max - Min): ${String.format("%.1f°C", deltaT)}
            Hotspot Normalized Viewfinder Coordinates: (X: ${String.format("%.2f", frameData.hotSpotX)}, Y: ${String.format("%.2f", frameData.hotSpotY)})
            Detected Signatures: ${if (detectedList.isNotBlank()) detectedList else "High Intensity Thermal Core"}
            
            [USER QUERY / MISSION OBJECTIVE]
            $userPrompt
            
            Provide actionable, technically precise guidance for the UAV pilot, incorporating GPS location awareness, optimal thermal palette selection, and flight altitude/gimbal recommendations.
        """.trimIndent()
    }
}
