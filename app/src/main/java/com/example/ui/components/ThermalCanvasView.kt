package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.model.ThermalPalette
import com.example.ui.theme.ThermalColdCyan
import com.example.ui.theme.ThermalCoreYellow
import com.example.ui.theme.ThermalHotRed

@Composable
fun ThermalCanvasView(
    frameData: ThermalFrameData,
    palette: ThermalPalette,
    tempUnit: TemperatureUnit,
    isothermEnabled: Boolean,
    isothermMin: Float,
    isothermMax: Float,
    roiBox: RoiBox,
    inspectionSpots: List<InspectionSpot>,
    onTap: (xNorm: Float, yNorm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("thermal_canvas_container")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val xNorm = (offset.x / size.width).coerceIn(0f, 1f)
                    val yNorm = (offset.y / size.height).coerceIn(0f, 1f)
                    onTap(xNorm, yNorm)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("thermal_canvas")) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridW = frameData.matrixWidth
            val gridH = frameData.matrixHeight
            val cellW = canvasWidth / gridW
            val cellH = canvasHeight / gridH

            val tMin = frameData.minTemp
            val tMax = frameData.maxTemp
            val tRange = if (tMax - tMin > 0.1f) tMax - tMin else 1.0f

            // 1. Draw Thermal Microbolometer Matrix
            for (y in 0 until gridH) {
                for (x in 0 until gridW) {
                    val temp = frameData.temperatures[y * gridW + x]
                    val normalized = (temp - tMin) / tRange

                    val color = if (isothermEnabled) {
                        if (temp in isothermMin..isothermMax) {
                            // Highlight in vivid isotherm color
                            palette.mapToColor(normalized)
                        } else {
                            // Monochromatic grayscale outside isotherm range
                            val gray = (normalized * 0.4f).coerceIn(0.08f, 0.45f)
                            Color(gray, gray, gray, 1f)
                        }
                    } else {
                        palette.mapToColor(normalized)
                    }

                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellW, y * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }

            // 2. Draw Region of Interest (ROI) Box if enabled
            if (roiBox.enabled) {
                val roiL = roiBox.left * canvasWidth
                val roiT = roiBox.top * canvasHeight
                val roiR = roiBox.right * canvasWidth
                val roiB = roiBox.bottom * canvasHeight
                val roiW = roiR - roiL
                val roiH = roiB - roiT

                drawRect(
                    color = ThermalColdCyan,
                    topLeft = Offset(roiL, roiT),
                    size = Size(roiW, roiH),
                    style = Stroke(width = 2.5f)
                )

                // Corner bracket accents
                val bracketLen = 18f
                drawLine(ThermalColdCyan, Offset(roiL, roiT), Offset(roiL + bracketLen, roiT), 4f)
                drawLine(ThermalColdCyan, Offset(roiL, roiT), Offset(roiL, roiT + bracketLen), 4f)
                drawLine(ThermalColdCyan, Offset(roiR, roiT), Offset(roiR - bracketLen, roiT), 4f)
                drawLine(ThermalColdCyan, Offset(roiR, roiT), Offset(roiR, roiT + bracketLen), 4f)
                drawLine(ThermalColdCyan, Offset(roiL, roiB), Offset(roiL + bracketLen, roiB), 4f)
                drawLine(ThermalColdCyan, Offset(roiL, roiB), Offset(roiL, roiB - bracketLen), 4f)
                drawLine(ThermalColdCyan, Offset(roiR, roiB), Offset(roiR - bracketLen, roiB), 4f)
                drawLine(ThermalColdCyan, Offset(roiR, roiB), Offset(roiR, roiB - bracketLen), 4f)

                // ROI Label Text
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        isFakeBoldText = true
                        setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                    }
                    drawText(
                        "ROI: MAX ${tempUnit.format(frameData.roiMaxTemp)} | AVG ${tempUnit.format(frameData.roiAvgTemp)}",
                        roiL + 8f,
                        roiT - 10f,
                        paint
                    )
                }
            }

            // 3. Draw Global MAX Hot Spot Marker (Red Target Crosshair)
            val hotX = frameData.hotSpotX * canvasWidth
            val hotY = frameData.hotSpotY * canvasHeight
            drawReticleMarker(
                center = Offset(hotX, hotY),
                color = ThermalHotRed,
                label = "MAX ${tempUnit.format(frameData.maxTemp)}",
                isHot = true
            )

            // 4. Draw Global MIN Cold Spot Marker (Blue Target Crosshair)
            val coldX = frameData.coldSpotX * canvasWidth
            val coldY = frameData.coldSpotY * canvasHeight
            drawReticleMarker(
                center = Offset(coldX, coldY),
                color = ThermalColdCyan,
                label = "MIN ${tempUnit.format(frameData.minTemp)}",
                isHot = false
            )

            // 5. Draw Center Spot Crosshairs
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            drawCenterCrosshair(
                center = Offset(centerX, centerY),
                label = tempUnit.format(frameData.centerSpotTemp)
            )

            // 6. Draw Custom User Inspection Pins
            for (spot in inspectionSpots) {
                val spotX = spot.xNorm * canvasWidth
                val spotY = spot.yNorm * canvasHeight
                drawInspectionPin(
                    center = Offset(spotX, spotY),
                    label = "${spot.label}: ${tempUnit.format(spot.temperature)}"
                )
            }
        }
    }
}

private fun DrawScope.drawReticleMarker(
    center: Offset,
    color: Color,
    label: String,
    isHot: Boolean
) {
    val radius = 16f
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = color,
        radius = 3.5f,
        center = center
    )
    // Crosshair ticks
    val tick = 12f
    drawLine(color, Offset(center.x - radius - tick, center.y), Offset(center.x - radius + 2f, center.y), 2f)
    drawLine(color, Offset(center.x + radius - 2f, center.y), Offset(center.x + radius + tick, center.y), 2f)
    drawLine(color, Offset(center.x, center.y - radius - tick), Offset(center.x, center.y - radius + 2f), 2f)
    drawLine(color, Offset(center.x, center.y + radius - 2f), Offset(center.x, center.y + radius + tick), 2f)

    // Text Badge
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = if (isHot) android.graphics.Color.RED else android.graphics.Color.CYAN
            textSize = 28f
            this.isFakeBoldText = true
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
        }
        val yOffset = if (center.y > 60f) -24f else 40f
        drawText(label, center.x - 45f, center.y + yOffset, paint)
    }
}

private fun DrawScope.drawCenterCrosshair(
    center: Offset,
    label: String
) {
    val crossLen = 14f
    val gap = 6f
    val color = Color.White
    // Horizontal cross
    drawLine(color, Offset(center.x - crossLen - gap, center.y), Offset(center.x - gap, center.y), 2f)
    drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + crossLen + gap, center.y), 2f)
    // Vertical cross
    drawLine(color, Offset(center.x, center.y - crossLen - gap), Offset(center.x, center.y - gap), 2f)
    drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + crossLen + gap), 2f)

    // Center text readout
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.YELLOW
            textSize = 30f
            this.isFakeBoldText = true
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
        }
        drawText(label, center.x + 16f, center.y - 12f, paint)
    }
}

private fun DrawScope.drawInspectionPin(
    center: Offset,
    label: String
) {
    drawCircle(
        color = ThermalCoreYellow,
        radius = 8f,
        center = center
    )
    drawCircle(
        color = Color.Black,
        radius = 3f,
        center = center
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.YELLOW
            textSize = 24f
            this.isFakeBoldText = true
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
        drawText(label, center.x + 12f, center.y + 8f, paint)
    }
}
