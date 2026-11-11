package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.LabScenario

/**
 * Realistic aerial drone video feed simulation backdrop.
 * Renders the optical spectrum camera view (terrain, infrastructure, solar arrays, forestry)
 * underneath the color-coded thermal intensity heatmap overlay.
 */
@Composable
fun DroneVideoBackdropView(
    scenario: LabScenario,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base Aerial Terrain Background Gradient
            val baseColors = when (scenario) {
                LabScenario.SEARCH_AND_RESCUE -> listOf(
                    Color(0xFF142416), // Dark pine forest canopy
                    Color(0xFF1D3320),
                    Color(0xFF0F1A11)
                )
                LabScenario.SOLAR_FARM_HOTSPOT -> listOf(
                    Color(0xFF1C2228), // Industrial gravel ground
                    Color(0xFF263238),
                    Color(0xFF151B20)
                )
                LabScenario.SUBSTATION_TRANSFORMER -> listOf(
                    Color(0xFF212529), // Concrete gravel substation pad
                    Color(0xFF2B303A),
                    Color(0xFF191C21)
                )
                LabScenario.WILDFIRE_PERIMETER -> listOf(
                    Color(0xFF261D18), // Scorched forest earth
                    Color(0xFF33271E),
                    Color(0xFF1E1713)
                )
                LabScenario.COLD_HVAC_LOSS -> listOf(
                    Color(0xFF22262B), // Commercial rooftop membrane
                    Color(0xFF2A3036),
                    Color(0xFF1A1D21)
                )
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = baseColors,
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.8f
                ),
                size = Size(w, h)
            )

            // 2. Optical Structural Elements based on Scenario
            when (scenario) {
                LabScenario.SOLAR_FARM_HOTSPOT -> {
                    // Draw solar array photovoltaic rack rows
                    val rowCount = 7
                    val panelH = h / (rowCount * 2f)
                    for (i in 0 until rowCount) {
                        val rowY = (i * 2 + 1) * panelH
                        drawRect(
                            color = Color(0xFF162A45),
                            topLeft = Offset(w * 0.1f, rowY),
                            size = Size(w * 0.8f, panelH * 1.3f)
                        )
                        // Aluminum framing lines
                        for (c in 1..8) {
                            val colX = w * 0.1f + (c * (w * 0.8f / 9))
                            drawLine(
                                color = Color(0xFF4A6572).copy(alpha = 0.5f),
                                start = Offset(colX, rowY),
                                end = Offset(colX, rowY + panelH * 1.3f),
                                strokeWidth = 1.2f
                            )
                        }
                    }
                }
                LabScenario.SEARCH_AND_RESCUE -> {
                    // Draw winding hiking trail through canopy
                    val pathColor = Color(0xFF3E3A32).copy(alpha = 0.7f)
                    drawLine(pathColor, Offset(w * 0.15f, 0f), Offset(w * 0.35f, h * 0.4f), 18f)
                    drawLine(pathColor, Offset(w * 0.35f, h * 0.4f), Offset(w * 0.75f, h * 0.75f), 16f)
                    drawLine(pathColor, Offset(w * 0.75f, h * 0.75f), Offset(w * 0.6f, h), 16f)

                    // Scattered trees foliage texture
                    for (i in 0..16) {
                        val treeX = (i * 67 % w.toInt()).toFloat()
                        val treeY = (i * 97 % h.toInt()).toFloat()
                        drawCircle(
                            color = Color(0xFF112214).copy(alpha = 0.6f),
                            radius = 24f,
                            center = Offset(treeX, treeY)
                        )
                    }
                }
                LabScenario.SUBSTATION_TRANSFORMER -> {
                    // Concrete transformer pad and bus bars
                    drawRect(
                        color = Color(0xFF343A40),
                        topLeft = Offset(w * 0.25f, h * 0.2f),
                        size = Size(w * 0.5f, h * 0.6f)
                    )
                    drawRect(
                        color = Color(0xFF495057),
                        topLeft = Offset(w * 0.35f, h * 0.3f),
                        size = Size(w * 0.3f, h * 0.4f)
                    )
                    // High-voltage insulators
                    for (k in 0..2) {
                        val insX = w * 0.38f + (k * (w * 0.12f))
                        drawCircle(Color(0xFFADB5BD), 10f, Offset(insX, h * 0.38f))
                        drawCircle(Color(0xFFCED4DA), 5f, Offset(insX, h * 0.38f))
                    }
                }
                else -> {
                    // Structural subtle grid
                    val gridStep = 40f
                    for (gx in 0..(w / gridStep).toInt()) {
                        drawLine(
                            Color.White.copy(alpha = 0.03f),
                            Offset(gx * gridStep, 0f),
                            Offset(gx * gridStep, h),
                            1f
                        )
                    }
                    for (gy in 0..(h / gridStep).toInt()) {
                        drawLine(
                            Color.White.copy(alpha = 0.03f),
                            Offset(0f, gy * gridStep),
                            Offset(w, gy * gridStep),
                            1f
                        )
                    }
                }
            }

            // 3. Drone Optical Camera Lens Vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.65f
                ),
                size = Size(w, h)
            )
        }
    }
}
