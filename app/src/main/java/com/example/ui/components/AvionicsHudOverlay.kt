package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.model.CameraFeedSource
import com.example.model.DroneTelemetry
import com.example.model.TemperatureUnit
import com.example.model.ThermalAlert
import com.example.model.ThermalFrameData
import com.example.model.ThermalPalette
import com.example.sensors.DeviceAttitude
import com.example.ui.theme.AlertCriticalBg
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.HudOverlayBg
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TelemetryGreen
import com.example.ui.theme.TelemetryWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalAmber
import com.example.ui.theme.ThermalColdCyan
import com.example.ui.theme.ThermalCoreYellow
import com.example.ui.theme.ThermalHotRed

enum class HudClutterMode(val title: String) {
    FULL_PFD("FULL PFD"),
    TACTICAL("TACTICAL"),
    MINIMAL("MINIMAL")
}

@Composable
fun AvionicsHudOverlay(
    feedSource: CameraFeedSource,
    droneTelemetry: DroneTelemetry,
    djiTelemetry: DjiComprehensiveTelemetry = DjiComprehensiveTelemetry(),
    deviceAttitude: DeviceAttitude,
    frameData: ThermalFrameData,
    palette: ThermalPalette,
    tempUnit: TemperatureUnit,
    emissivity: Float,
    activeAlert: ThermalAlert?,
    onDismissAlert: () -> Unit,
    onTogglePalette: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var hudMode by remember { mutableStateOf(HudClutterMode.FULL_PFD) }

    // Dynamic pitch, roll, heading, altitude, and GPS resolution
    val isDeviceCam = feedSource == CameraFeedSource.DEVICE_CAMERA
    val pitch = if (isDeviceCam) deviceAttitude.pitch else djiTelemetry.attitude.pitchDegrees
    val roll = if (isDeviceCam) deviceAttitude.roll else djiTelemetry.attitude.rollDegrees
    val heading = if (isDeviceCam) ((((deviceAttitude.roll * 2f) % 360) + 360) % 360) else djiTelemetry.attitude.compassHeadingDegrees
    val altitudeAgl = if (isDeviceCam) 1.5f else djiTelemetry.gps.altitudeAglMeters
    val altitudeMsl = if (isDeviceCam) 35.0 else djiTelemetry.gps.altitudeMslMeters
    val groundSpeedKmh = if (isDeviceCam) 0.0f else djiTelemetry.velocity.groundSpeedKmh
    val coordinatesStr = if (isDeviceCam) {
        "${String.format("%.6f", droneTelemetry.latitude)}° N, ${String.format("%.6f", droneTelemetry.longitude)}° W"
    } else {
        djiTelemetry.gps.formatCoordinates()
    }

    Box(modifier = modifier.fillMaxSize().testTag("avionics_hud_overlay")) {

        // =========================================================================
        // 1. TOP AVIONICS SYSTEM BAR (Status, Battery, Satellites, Signal, Emissivity)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(HudOverlayBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("hud_top_bar"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source & MSDK Model Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (feedSource == CameraFeedSource.DJI_DRONE_STREAM) TelemetryGreen
                            else if (feedSource == CameraFeedSource.DEVICE_CAMERA) ThermalAmber
                            else ThermalColdCyan,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (feedSource) {
                        CameraFeedSource.DEVICE_CAMERA -> "MOBILE SENSOR IR"
                        CameraFeedSource.DJI_DRONE_STREAM -> "${djiTelemetry.modelName} [MSDK V5]"
                        CameraFeedSource.LAB_SIMULATION -> "DJI MSDK SIM [10Hz]"
                    },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Central Telemetry: Satellites, Signal dBm, Battery
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS Lock",
                    tint = TelemetryGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${djiTelemetry.gps.satelliteCount} SAT",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = "Signal",
                    tint = TelemetryGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${djiTelemetry.airLink.signalDbm} dBm",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = if (djiTelemetry.battery.chargeRemainingPercent > 25) TelemetryGreen else TelemetryWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${djiTelemetry.battery.chargeRemainingPercent}%",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Right side: HUD Mode Toggle & Emissivity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ε=${String.format("%.2f", emissivity)}",
                    color = ThermalAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Real-Time Palette Quick-Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, ThermalAmber.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .then(
                            if (onTogglePalette != null) {
                                Modifier.clickable { onTogglePalette() }
                            } else Modifier
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("hud_palette_quick_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            palette.mapToColor(0.0f),
                                            palette.mapToColor(0.5f),
                                            palette.mapToColor(1.0f)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = palette.title.uppercase(),
                            color = ThermalAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Clutter Mode Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            hudMode = when (hudMode) {
                                HudClutterMode.FULL_PFD -> HudClutterMode.TACTICAL
                                HudClutterMode.TACTICAL -> HudClutterMode.MINIMAL
                                HudClutterMode.MINIMAL -> HudClutterMode.FULL_PFD
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("hud_mode_toggle")
                ) {
                    Text(
                        text = hudMode.title,
                        color = ThermalColdCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // =========================================================================
        // 2. HORIZONTAL COMPASS HEADING TAPE (Top Center)
        // =========================================================================
        if (hudMode != HudClutterMode.MINIMAL) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Digital Heading Readout Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudOverlayBg)
                        .border(1.dp, ThermalAmber, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("heading_readout_box")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = ThermalAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", heading)}° ${djiTelemetry.attitude.formatHeadingCardinal()}",
                            color = ThermalAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Rolling Heading Tape Canvas
                Canvas(
                    modifier = Modifier
                        .width(220.dp)
                        .height(22.dp)
                        .background(HudOverlayBg.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                ) {
                    drawCompassHeadingTape(heading = heading)
                }
            }
        }

        // =========================================================================
        // 3. HIGH-PRECISION CRITICAL THERMAL ALERT BANNER
        // =========================================================================
        AnimatedVisibility(
            visible = activeAlert != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp, start = 12.dp, end = 12.dp)
        ) {
            activeAlert?.let { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AlertCriticalBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("thermal_alert_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alarm",
                            tint = ThermalHotRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = alert.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = alert.description,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissAlert,
                        modifier = Modifier.size(26.dp).testTag("dismiss_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. LEFT PFD: ARTIFICIAL HORIZON, PITCH LADDER, SPEED
        // =========================================================================
        if (hudMode == HudClutterMode.FULL_PFD) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                // Pitch & Roll Horizon Indicator
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 150.dp)
                        .background(HudOverlayBg, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                        .testTag("attitude_horizon_indicator")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArtificialHorizonLadder(pitch = pitch, roll = roll)
                    }

                    // Pitch & Roll text
                    Text(
                        text = "P: ${String.format("%+.1f", pitch)}°\nR: ${String.format("%+.1f", roll)}°",
                        color = TelemetryGreen,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ground Speed Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudOverlayBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TelemetryGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f km/h", groundSpeedKmh),
                            color = TelemetryGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. RIGHT PFD: ALTITUDE TAPE (AGL / MSL) & RADIOMETRIC COLOR SCALE BAR
        // =========================================================================
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Altitude Ladder Tape (AGL & MSL)
            if (hudMode != HudClutterMode.MINIMAL) {
                Column(
                    modifier = Modifier
                        .background(HudOverlayBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("altitude_tape_box"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ALT (AGL)",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.1f m", altitudeAgl),
                        color = ThermalColdCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(altitudeAgl * 3.28084f).toInt()} FT",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mini vertical rate-of-climb bar
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(60.dp)
                            .background(SurfaceElevated, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val vz = djiTelemetry.velocity.velocityZMps
                        val shiftY = (-vz * 6f).coerceIn(-28f, 28f)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (vz >= 0) TelemetryGreen else ThermalAmber, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "MSL",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.1f", altitudeMsl),
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
            }

            // Radiometric Thermal Palette Color Bar (Clickable Real-Time Toggle)
            Column(
                modifier = Modifier
                    .background(HudOverlayBg, RoundedCornerShape(6.dp))
                    .then(
                        if (onTogglePalette != null) {
                            Modifier.clickable { onTogglePalette() }
                        } else Modifier
                    )
                    .padding(horizontal = 5.dp, vertical = 6.dp)
                    .testTag("hud_palette_scale_toggle"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Max Temp Readout
                Text(
                    text = tempUnit.format(frameData.maxTemp),
                    color = ThermalHotRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                val gradientColors = listOf(
                    palette.mapToColor(1.0f),
                    palette.mapToColor(0.75f),
                    palette.mapToColor(0.5f),
                    palette.mapToColor(0.25f),
                    palette.mapToColor(0.0f)
                )

                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(colors = gradientColors),
                            RoundedCornerShape(3.dp)
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Min Temp Readout
                Text(
                    text = tempUnit.format(frameData.minTemp),
                    color = TelemetryGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Palette mode label badge (e.g. WH, BH, IRON)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = when (palette) {
                            ThermalPalette.WHITE_HOT -> "W-HOT"
                            ThermalPalette.BLACK_HOT -> "B-HOT"
                            ThermalPalette.IRONBOW -> "IRON"
                            ThermalPalette.RAINBOW -> "RAIN"
                            ThermalPalette.INFERNO -> "INF"
                            ThermalPalette.ARCTIC -> "ARC"
                            ThermalPalette.LAVA -> "LAVA"
                        },
                        color = ThermalAmber,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // =========================================================================
        // 6. BOTTOM DJI GPS & NAVIGATION STATUS BAR
        // =========================================================================
        if (hudMode != HudClutterMode.MINIMAL) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 78.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .background(HudOverlayBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("hud_gps_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Coordinates
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = TelemetryGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = coordinatesStr,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Flight Mode & Distance to Home
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = djiTelemetry.flightMode.label.take(14),
                        color = ThermalAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = ThermalColdCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "D:${String.format("%.1fm", djiTelemetry.distanceToHomeMeters)}",
                        color = ThermalColdCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Draws rolling compass heading tape with tick marks and cardinal directions
 */
private fun DrawScope.drawCompassHeadingTape(heading: Float) {
    val w = size.width
    val h = size.height
    val midX = w / 2f

    // 1 degree corresponds to approximately 2.2 pixels width
    val pxPerDegree = 2.2f
    val visibleHalfDegrees = (midX / pxPerDegree).toInt() + 5

    val startDeg = (heading - visibleHalfDegrees).toInt()
    val endDeg = (heading + visibleHalfDegrees).toInt()

    for (deg in startDeg..endDeg) {
        val normalizedDeg = ((deg % 360) + 360) % 360
        val x = midX + (deg - heading) * pxPerDegree

        if (x in 0f..w) {
            when {
                deg % 30 == 0 -> {
                    // Major cardinal or 30° tick
                    drawLine(Color.White, Offset(x, 0f), Offset(x, 10f), 2f)
                    val label = when (normalizedDeg) {
                        0 -> "N"
                        90 -> "E"
                        180 -> "S"
                        270 -> "W"
                        else -> "$normalizedDeg"
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (label in listOf("N", "E", "S", "W")) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                            textSize = 18f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(label, x, h - 2f, paint)
                    }
                }
                deg % 10 == 0 -> {
                    // 10° tick
                    drawLine(TextSecondary, Offset(x, 0f), Offset(x, 6f), 1.5f)
                }
                deg % 5 == 0 -> {
                    // 5° tick
                    drawLine(TextMuted, Offset(x, 0f), Offset(x, 4f), 1f)
                }
            }
        }
    }

    // Center Lubber Index pointer (Yellow triangle)
    drawLine(ThermalAmber, Offset(midX, 0f), Offset(midX - 4f, 6f), 2f)
    drawLine(ThermalAmber, Offset(midX, 0f), Offset(midX + 4f, 6f), 2f)
    drawLine(ThermalAmber, Offset(midX - 4f, 6f), Offset(midX + 4f, 6f), 2f)
}

/**
 * Draws the artificial horizon and pitch ladder
 */
private fun DrawScope.drawArtificialHorizonLadder(pitch: Float, roll: Float) {
    val w = size.width
    val h = size.height
    val midX = w / 2f
    val midY = h / 2f

    // Pitch shift (pixels per degree)
    val pxPerDeg = 2.0f

    rotate(degrees = -roll, pivot = Offset(midX, midY)) {
        val horizonShiftY = (pitch * pxPerDeg).coerceIn(-midY + 10f, midY - 10f)
        val horizonY = midY + horizonShiftY

        // Main 0° Horizon Line
        drawLine(
            color = TelemetryGreen,
            start = Offset(4f, horizonY),
            end = Offset(w - 4f, horizonY),
            strokeWidth = 2.5f
        )

        // Pitch Rungs (+20°, +10°, -10°, -20°)
        val rungs = listOf(20f, 10f, -10f, -20f)
        for (rungDeg in rungs) {
            val rungY = horizonY - (rungDeg * pxPerDeg)
            if (rungY in 10f..(h - 10f)) {
                val rungWidth = if (kotlin.math.abs(rungDeg) == 20f) 22f else 16f
                val isNegative = rungDeg < 0

                // Draw dashed or solid rungs
                drawLine(
                    color = TelemetryGreen.copy(alpha = 0.7f),
                    start = Offset(midX - rungWidth, rungY),
                    end = Offset(midX + rungWidth, rungY),
                    strokeWidth = 1.5f
                )

                // Rung end brackets
                val bracketH = if (isNegative) -3f else 3f
                drawLine(TelemetryGreen.copy(alpha = 0.7f), Offset(midX - rungWidth, rungY), Offset(midX - rungWidth, rungY + bracketH), 1.5f)
                drawLine(TelemetryGreen.copy(alpha = 0.7f), Offset(midX + rungWidth, rungY), Offset(midX + rungWidth, rungY + bracketH), 1.5f)
            }
        }
    }

    // Aircraft fixed central boresight symbol
    drawCircle(color = ThermalAmber, radius = 2.5f, center = Offset(midX, midY))
    drawLine(ThermalAmber, Offset(midX - 12f, midY), Offset(midX - 4f, midY), 2.5f)
    drawLine(ThermalAmber, Offset(midX + 4f, midY), Offset(midX + 12f, midY), 2.5f)
    drawLine(ThermalAmber, Offset(midX, midY - 6f), Offset(midX, midY - 2f), 2.5f)
}
