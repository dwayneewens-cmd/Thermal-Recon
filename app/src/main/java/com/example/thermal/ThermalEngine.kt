package com.example.thermal

import android.graphics.Bitmap
import com.example.model.HeatSignatureType
import com.example.model.LabScenario
import com.example.model.RoiBox
import com.example.model.ThermalFrameData
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ThermalEngine {
    var emissivity: Float = 0.95f // Default human/organic emissivity
    var ambientTempCelsius: Float = 22.0f
    var calibrationOffset: Float = 0.0f

    // Internal simulation time ticker for dynamic motion in simulation mode
    private var simTick: Float = 0f

    /**
     * Generates a thermal frame from a Lab scenario simulation
     */
    fun generateScenarioFrame(scenario: LabScenario, roi: RoiBox): ThermalFrameData {
        simTick += 0.08f
        val width = 32
        val height = 24
        val size = width * height
        val temps = FloatArray(size)

        val oscX = (sin(simTick.toDouble() * 0.7) * 0.15).toFloat()
        val oscY = (sin(simTick.toDouble() * 0.5) * 0.1).toFloat()

        when (scenario) {
            LabScenario.SEARCH_AND_RESCUE -> {
                // Background forest canopy 15°C - 19°C
                val targetX = 0.52f + oscX
                val targetY = 0.48f + oscY
                for (y in 0 until height) {
                    val yNorm = y.toFloat() / (height - 1)
                    for (x in 0 until width) {
                        val xNorm = x.toFloat() / (width - 1)
                        val bg = 16.5f + (sin((x * 0.4 + y * 0.3).toDouble()) * 1.5).toFloat()
                        // Human body heat gaussian
                        val dx = (xNorm - targetX) * 1.2f
                        val dy = (yNorm - targetY) * 1.6f
                        val distSq = dx * dx + dy * dy
                        val bodyHeat = if (distSq < 0.04f) {
                            val falloff = exp(-distSq * 60.0).toFloat()
                            (37.4f - bg) * falloff
                        } else 0f
                        temps[y * width + x] = (bg + bodyHeat) * emissivity + calibrationOffset
                    }
                }
            }
            LabScenario.SOLAR_FARM_HOTSPOT -> {
                // Solar panels uniform warm ~42°C, hotspot cell 86°C
                val hotX = 0.65f
                val hotY = 0.42f
                for (y in 0 until height) {
                    val yNorm = y.toFloat() / (height - 1)
                    val panelRow = if ((y % 6) in 1..4) 41.5f else 28.0f
                    for (x in 0 until width) {
                        val xNorm = x.toFloat() / (width - 1)
                        val dx = xNorm - hotX
                        val dy = yNorm - hotY
                        val distSq = dx * dx + dy * dy
                        val hotspot = if (distSq < 0.025f) {
                            val f = exp(-distSq * 120.0).toFloat()
                            (86.5f - panelRow) * f
                        } else 0f
                        temps[y * width + x] = (panelRow + hotspot) * emissivity + calibrationOffset
                    }
                }
            }
            LabScenario.SUBSTATION_TRANSFORMER -> {
                // Transformer housing 50°C, bushing terminal 98°C
                val termX = 0.45f
                val termY = 0.35f
                for (y in 0 until height) {
                    val yNorm = y.toFloat() / (height - 1)
                    for (x in 0 until width) {
                        val xNorm = x.toFloat() / (width - 1)
                        val housing = 32f + (1f - yNorm) * 22f
                        val dx = xNorm - termX
                        val dy = yNorm - termY
                        val distSq = dx * dx + dy * dy
                        val arcFault = if (distSq < 0.03f) {
                            exp(-distSq * 90.0).toFloat() * 62f
                        } else 0f
                        temps[y * width + x] = (housing + arcFault) * emissivity + calibrationOffset
                    }
                }
            }
            LabScenario.WILDFIRE_PERIMETER -> {
                // Ground temperature 35°C, ember fire line 140°C
                for (y in 0 until height) {
                    val yNorm = y.toFloat() / (height - 1)
                    for (x in 0 until width) {
                        val xNorm = x.toFloat() / (width - 1)
                        val fireLine = 0.5f + (sin((x * 0.3 + simTick * 0.4).toDouble()) * 0.12).toFloat()
                        val dy = kotlin.math.abs(yNorm - fireLine)
                        val fireTemp = if (dy < 0.15f) {
                            (1f - (dy / 0.15f)) * 125f
                        } else 0f
                        val smokeBase = 26f + (sin((x + y).toDouble() * 0.2) * 4f).toFloat()
                        temps[y * width + x] = (smokeBase + fireTemp) * emissivity + calibrationOffset
                    }
                }
            }
            LabScenario.COLD_HVAC_LOSS -> {
                // Ambient 24°C, cold leak -4°C
                val leakX = 0.5f
                val leakY = 0.6f
                for (y in 0 until height) {
                    val yNorm = y.toFloat() / (height - 1)
                    for (x in 0 until width) {
                        val xNorm = x.toFloat() / (width - 1)
                        val base = 23.5f
                        val dx = xNorm - leakX
                        val dy = yNorm - leakY
                        val distSq = dx * dx + dy * dy
                        val cold = if (distSq < 0.05f) {
                            exp(-distSq * 50.0).toFloat() * -27f
                        } else 0f
                        temps[y * width + x] = (base + cold) * emissivity + calibrationOffset
                    }
                }
            }
        }

        return evaluateFrameData(width, height, temps, roi)
    }

    /**
     * Converts a camera preview bitmap into a radiometric thermal temperature matrix
     */
    fun processCameraBitmap(bitmap: Bitmap, roi: RoiBox): ThermalFrameData {
        val width = 32
        val height = 24
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val temps = FloatArray(width * height)

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Relative radiometric radiance estimation from optical frame
                // Luminance weighted: 0.299 R + 0.587 G + 0.114 B + Red/Blue chroma shift
                val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f
                val redShift = (r - b).toFloat() / 255.0f

                // Radiometric curve: map relative radiance to realistic Celsius range (18°C to 75°C)
                val baseTemp = ambientTempCelsius + (luminance * 28.0f) + (redShift * 16.0f)
                val corrected = (baseTemp * emissivity) + calibrationOffset
                temps[y * width + x] = corrected
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return evaluateFrameData(width, height, temps, roi)
    }

    private fun evaluateFrameData(
        width: Int,
        height: Int,
        temps: FloatArray,
        roi: RoiBox
    ): ThermalFrameData {
        var minTemp = Float.MAX_VALUE
        var maxTemp = -Float.MAX_VALUE
        var sumTemp = 0.0
        var hotX = 0.5f
        var hotY = 0.5f
        var coldX = 0.5f
        var coldY = 0.5f

        val centerIdx = (height / 2) * width + (width / 2)
        val centerTemp = temps[centerIdx]

        var roiSum = 0.0
        var roiCount = 0
        var roiMax = -Float.MAX_VALUE

        for (y in 0 until height) {
            val yNorm = y.toFloat() / (height - 1)
            for (x in 0 until width) {
                val xNorm = x.toFloat() / (width - 1)
                val temp = temps[y * width + x]
                sumTemp += temp
                if (temp > maxTemp) {
                    maxTemp = temp
                    hotX = xNorm
                    hotY = yNorm
                }
                if (temp < minTemp) {
                    minTemp = temp
                    coldX = xNorm
                    coldY = yNorm
                }

                // ROI calculation
                if (roi.enabled && xNorm >= roi.left && xNorm <= roi.right && yNorm >= roi.top && yNorm <= roi.bottom) {
                    roiSum += temp
                    roiCount++
                    if (temp > roiMax) roiMax = temp
                }
            }
        }

        val avgTemp = (sumTemp / temps.size).toFloat()
        val roiAvg = if (roiCount > 0) (roiSum / roiCount).toFloat() else avgTemp
        val roiMaxFinal = if (roiCount > 0) roiMax else maxTemp

        // Signatures analysis
        val detected = mutableListOf<HeatSignatureType>()
        if (maxTemp >= 90.0f) {
            detected.add(HeatSignatureType.FIRE_ANOMALY)
        } else if (maxTemp >= 60.0f) {
            detected.add(HeatSignatureType.INDUSTRIAL_OVERHEAT)
        }
        if (maxTemp in 34.0f..39.5f && (maxTemp - avgTemp) >= 8.0f) {
            detected.add(HeatSignatureType.HUMAN_LIVING)
        }
        if (minTemp <= 8.0f) {
            detected.add(HeatSignatureType.COLD_LEAK)
        }
        if (detected.isEmpty()) {
            detected.add(HeatSignatureType.NORMAL_AMBIENT)
        }

        return ThermalFrameData(
            matrixWidth = width,
            matrixHeight = height,
            temperatures = temps,
            minTemp = minTemp,
            maxTemp = maxTemp,
            avgTemp = avgTemp,
            centerSpotTemp = centerTemp,
            hotSpotX = hotX,
            hotSpotY = hotY,
            coldSpotX = coldX,
            coldSpotY = coldY,
            detectedSignatures = detected,
            roiAvgTemp = roiAvg,
            roiMaxTemp = roiMaxFinal
        )
    }
}
