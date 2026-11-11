package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.InspectionSpot
import com.example.model.RoiBox
import com.example.model.TemperatureUnit
import com.example.model.ThermalFrameData
import com.example.model.ThermalFusionMode
import com.example.model.ThermalPalette
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalColdCyan
import com.example.ui.theme.ThermalCoreYellow
import com.example.ui.theme.ThermalHotRed

/**
 * Visualization layer that overlays radiometric thermal intensity data as a
 * color-coded heatmap over the live drone video feed.
 * Supports alpha blending, MSX edge contour fusion, isotherm isolation, and hot spot radiance halos.
 */
@Composable
fun ThermalHeatmapOverlayView(
    frameData: ThermalFrameData,
    palette: ThermalPalette,
    tempUnit: TemperatureUnit,
    heatmapOpacity: Float = 0.75f,
    fusionMode: ThermalFusionMode = ThermalFusionMode.HEATMAP_OVERLAY,
    isothermEnabled: Boolean = false,
    isothermMin: Float = 34f,
    isothermMax: Float = 42f,
    showContourLines: Boolean = true,
    showHeatGlow: Boolean = true,
    roiBox: RoiBox = RoiBox(),
    inspectionSpots: List<InspectionSpot> = emptyList(),
    onTap: (xNorm: Float, yNorm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("thermal_heatmap_overlay_container")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val xNorm = (offset.x / size.width).coerceIn(0f, 1f)
                    val yNorm = (offset.y / size.height).coerceIn(0f, 1f)
                    onTap(xNorm, yNorm)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("thermal_heatmap_canvas")) {
            val canvasW = size.width
            val canvasH = size.height

            val gridW = frameData.matrixWidth
            val gridH = frameData.matrixHeight
            val cellW = canvasW / gridW
            val cellH = canvasH / gridH

            val tMin = frameData.minTemp
            val tMax = frameData.maxTemp
            val tRange = if (tMax - tMin > 0.1f) tMax - tMin else 1.0f

            val baseAlpha = when (fusionMode) {
                ThermalFusionMode.THERMAL_SOLO -> 1.0f
                ThermalFusionMode.HEATMAP_OVERLAY -> heatmapOpacity.coerceIn(0.1f, 1.0f)
                ThermalFusionMode.MSX_FUSION -> (heatmapOpacity * 0.85f).coerceIn(0.2f, 0.95f)
                ThermalFusionMode.ISOTHERM_HIGHLIGHT -> heatmapOpacity.coerceIn(0.2f, 1.0f)
            }

            // 1. Draw Color-Coded Thermal Intensity Heatmap Layer
            for (y in 0 until gridH) {
                for (x in 0 until gridW) {
                    val temp = frameData.temperatures[y * gridW + x]
                    val normalized = ((temp - tMin) / tRange).coerceIn(0f, 1f)

                    val color = when (fusionMode) {
                        ThermalFusionMode.ISOTHERM_HIGHLIGHT -> {
                            val inBand = if (isothermEnabled) {
                                temp in isothermMin..isothermMax
                            } else {
                                normalized >= 0.55f // Only highlight higher intensity areas
                            }
                            if (inBand) {
                                palette.mapToColor(normalized).copy(alpha = baseAlpha)
                            } else {
                                Color.Transparent // Fully transparent to show live optical video feed!
                            }
                        }
                        ThermalFusionMode.MSX_FUSION -> {
                            val c = palette.mapToColor(normalized)
                            // Boost saturation and contrast for MSX fusion
                            c.copy(alpha = baseAlpha)
                        }
                        else -> {
                            if (isothermEnabled) {
                                if (temp in isothermMin..isothermMax) {
                                    palette.mapToColor(normalized).copy(alpha = baseAlpha)
                                } else {
                                    val gray = (normalized * 0.35f).coerceIn(0.05f, 0.4f)
                                    Color(gray, gray, gray, baseAlpha * 0.5f)
                                }
                            } else {
                                palette.mapToColor(normalized).copy(alpha = baseAlpha)
                            }
                        }
                    }

                    if (color != Color.Transparent) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x * cellW, y * cellH),
                            size = Size(cellW + 0.5f, cellH + 0.5f)
                        )
                    }
                }
            }

            // 2. MSX Edge Structure Lines (Edge Contrast Overlay)
            if (fusionMode == ThermalFusionMode.MSX_FUSION) {
                for (y in 0 until gridH - 1) {
                    for (x in 0 until gridW - 1) {
                        val tCenter = frameData.temperatures[y * gridW + x]
                        val tRight = frameData.temperatures[y * gridW + (x + 1)]
                        val tBottom = frameData.temperatures[(y + 1) * gridW + x]

                        val gradX = kotlin.math.abs(tRight - tCenter)
                        val gradY = kotlin.math.abs(tBottom - tCenter)

                        if (gradX > 3.5f || gradY > 3.5f) {
                            val edgeColor = Color.White.copy(alpha = 0.45f)
                            drawLine(
                                color = edgeColor,
                                start = Offset(x * cellW, y * cellH),
                                end = Offset((x + 1) * cellW, (y + 1) * cellH),
                                strokeWidth = 1.2f
                            )
                        }
                    }
                }
            }

            // 3. Dynamic Isothermal Intensity Contours
            if (showContourLines) {
                val hotCenter = Offset(frameData.hotSpotX * canvasW, frameData.hotSpotY * canvasH)
                val contourSteps = listOf(0.4f, 0.65f, 0.85f)

                for ((idx, step) in contourSteps.withIndex()) {
                    val radius = (18f + idx * 24f) * (1f + (step * 0.5f))
                    val contourColor = when (idx) {
                        0 -> ThermalColdCyan.copy(alpha = 0.5f)
                        1 -> ThermalAmber.copy(alpha = 0.6f)
                        else -> ThermalHotRed.copy(alpha = 0.75f)
                    }

                    drawCircle(
                        color = contourColor,
                        radius = radius,
                        center = hotCenter,
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // 4. Hot Spot Radiant Heat Plume Glow Halo
            if (showHeatGlow) {
                val hotCenter = Offset(frameData.hotSpotX * canvasW, frameData.hotSpotY * canvasH)
                val glowRadius = 45f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ThermalHotRed.copy(alpha = 0.45f),
                            ThermalAmber.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = hotCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = hotCenter
                )
            }

            // 5. Region of Interest (ROI) Box
            if (roiBox.enabled) {
                val roiL = roiBox.left * canvasW
                val roiT = roiBox.top * canvasH
                val roiR = roiBox.right * canvasW
                val roiB = roiBox.bottom * canvasH
                val roiW = roiR - roiL
                val roiH = roiB - roiT

                drawRect(
                    color = ThermalColdCyan,
                    topLeft = Offset(roiL, roiT),
                    size = Size(roiW, roiH),
                    style = Stroke(width = 2f)
                )

                // Corner brackets
                val bLen = 16f
                drawLine(ThermalColdCyan, Offset(roiL, roiT), Offset(roiL + bLen, roiT), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiL, roiT), Offset(roiL, roiT + bLen), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiR, roiT), Offset(roiR - bLen, roiT), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiR, roiT), Offset(roiR, roiT + bLen), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiL, roiB), Offset(roiL + bLen, roiB), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiL, roiB), Offset(roiL, roiB - bLen), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiR, roiB), Offset(roiR - bLen, roiB), 3.5f)
                drawLine(ThermalColdCyan, Offset(roiR, roiB), Offset(roiR, roiB - bLen), 3.5f)

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 26f
                        isFakeBoldText = true
                        setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                    }
                    drawText(
                        "ROI MAX: ${tempUnit.format(frameData.roiMaxTemp)} | AVG: ${tempUnit.format(frameData.roiAvgTemp)}",
                        roiL + 8f,
                        roiT - 8f,
                        paint
                    )
                }
            }

            // 6. Global MAX Hot Spot Reticle & Callout
            val hotX = frameData.hotSpotX * canvasW
            val hotY = frameData.hotSpotY * canvasH
            drawReticle(
                center = Offset(hotX, hotY),
                color = ThermalHotRed,
                label = "HOT ${tempUnit.format(frameData.maxTemp)}",
                isHot = true
            )

            // 7. Global MIN Cold Spot Reticle & Callout
            val coldX = frameData.coldSpotX * canvasW
            val coldY = frameData.coldSpotY * canvasH
            drawReticle(
                center = Offset(coldX, coldY),
                color = ThermalColdCyan,
                label = "COLD ${tempUnit.format(frameData.minTemp)}",
                isHot = false
            )

            // 8. Center Spot Crosshairs
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f
            drawCenterCrosshair(
                center = Offset(centerX, centerY),
                label = tempUnit.format(frameData.centerSpotTemp)
            )

            // 9. Interactive Inspection Spot Pins
            for (spot in inspectionSpots) {
                val spotX = spot.xNorm * canvasW
                val spotY = spot.yNorm * canvasH
                drawInspectionPin(
                    center = Offset(spotX, spotY),
                    label = "${spot.label}: ${tempUnit.format(spot.temperature)}"
                )
            }
        }
    }
}

private fun DrawScope.drawReticle(
    center: Offset,
    color: Color,
    label: String,
    isHot: Boolean
) {
    val radius = 15f
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2.2f))
    drawCircle(color = color, radius = 3.5f, center = center)

    val tick = 10f
    drawLine(color, Offset(center.x - radius - tick, center.y), Offset(center.x - radius + 2f, center.y), 2f)
    drawLine(color, Offset(center.x + radius - 2f, center.y), Offset(center.x + radius + tick, center.y), 2f)
    drawLine(color, Offset(center.x, center.y - radius - tick), Offset(center.x, center.y - radius + 2f), 2f)
    drawLine(color, Offset(center.x, center.y + radius - 2f), Offset(center.x, center.y + radius + tick), 2f)

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = if (isHot) android.graphics.Color.RED else android.graphics.Color.CYAN
            textSize = 26f
            this.isFakeBoldText = true
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
        }
        val yOffset = if (center.y > 60f) -22f else 38f
        drawText(label, center.x - 40f, center.y + yOffset, paint)
    }
}

private fun DrawScope.drawCenterCrosshair(
    center: Offset,
    label: String
) {
    val crossLen = 14f
    val gap = 6f
    val color = Color.White
    drawLine(color, Offset(center.x - crossLen - gap, center.y), Offset(center.x - gap, center.y), 2f)
    drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + crossLen + gap, center.y), 2f)
    drawLine(color, Offset(center.x, center.y - crossLen - gap), Offset(center.x, center.y - gap), 2f)
    drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + crossLen + gap), 2f)

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.YELLOW
            textSize = 28f
            this.isFakeBoldText = true
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
        }
        drawText(label, center.x + 16f, center.y - 10f, paint)
    }
}

private fun DrawScope.drawInspectionPin(
    center: Offset,
    label: String
) {
    drawCircle(color = ThermalCoreYellow, radius = 7f, center = center)
    drawCircle(color = Color.Black, radius = 2.5f, center = center)

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.YELLOW
            textSize = 22f
            this.isFakeBoldText = true
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
        drawText(label, center.x + 10f, center.y + 7f, paint)
    }
}
