package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.GpsFixType
import com.example.model.DroneTelemetry
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Translucent HUD overlay UI component that renders real-time telemetry from the drone,
 * specifically presenting:
 *  1. Altitude (AGL, MSL, vertical climb rate)
 *  2. Battery Level (percentage, voltage, cell status, estimated remaining time)
 *  3. GPS Coordinates (lat/lon, satellite count, RTK fix precision, distance to home)
 *  4. Gimbal Orientation (pitch, roll, yaw, visual tilt arc from 0° horizon to -90° nadir)
 *
 * Utilizes existing telemetry data structures ([DjiComprehensiveTelemetry] & [DroneTelemetry]).
 */
@Composable
fun DroneTelemetryOverlay(
    telemetry: DjiComprehensiveTelemetry,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    onClose: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    // Pulsing heartbeat beacon for live 10Hz stream feedback
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Translucent glassmorphism styling
    val translucentBg = Color(0xD90A0E17) // 85% alpha dark obsidian
    val glassBorder = Color(0x38FFB300)   // subtle amber border glow

    Card(
        colors = CardDefaults.cardColors(containerColor = translucentBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("drone_telemetry_translucent_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {

            // =========================================================================
            // TOP HEADER: Model Badge, Flight Mode, Live Beacon & Expand/Close Buttons
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing green link beacon
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                TelemetryGreen.copy(alpha = pulseAlpha),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = telemetry.modelName.uppercase(),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("telemetry_model_badge")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Flight Mode Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = telemetry.flightMode.label.uppercase(),
                            color = ThermalColdCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Flight Timer
                    val minutes = telemetry.flightTimeSeconds / 60
                    val seconds = telemetry.flightTimeSeconds % 60
                    Text(
                        text = String.format("%02d:%02d FLT", minutes, seconds),
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Header Controls: Expand/Collapse & Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("telemetry_expand_toggle")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Telemetry" else "Expand Telemetry",
                            tint = ThermalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("telemetry_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Telemetry Overlay",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // COMPACT READOUT (Shown when collapsed)
            // =========================================================================
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x44111720))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Altitude
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ALT: ", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%.1fm", telemetry.gps.altitudeAglMeters),
                            color = TelemetryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Battery
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("BAT: ", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        val batColor = if (telemetry.battery.chargeRemainingPercent > 50) TelemetryGreen
                        else if (telemetry.battery.chargeRemainingPercent > 25) TelemetryWarning
                        else ThermalHotRed
                        Text(
                            text = "${telemetry.battery.chargeRemainingPercent}%",
                            color = batColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // GPS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GPS: ", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${telemetry.gps.satelliteCount} SAT",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Gimbal
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GIMBAL: ", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = String.format("%.0f°", telemetry.gimbal.pitchDegrees),
                            color = ThermalAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // =========================================================================
            // EXPANDED TELEMETRY GRID: Altitude, Battery, GPS, Gimbal
            // =========================================================================
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 1: Altitude (Left) & Battery (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. ALTITUDE TILE
                        TranslucentTelemetryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_altitude_stat")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FlightTakeoff,
                                            contentDescription = "Altitude Icon",
                                            tint = ThermalColdCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ALTITUDE",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Climb Rate / Vertical Velocity
                                    val vz = telemetry.velocity.velocityZMps
                                    val vzText = if (vz >= 0.1f) "▲ +${String.format("%.1f", vz)} m/s"
                                    else if (vz <= -0.1f) "▼ ${String.format("%.1f", vz)} m/s"
                                    else "— 0.0 m/s"
                                    val vzColor = if (vz > 0.1f) TelemetryGreen else if (vz < -0.1f) ThermalHotRed else TextMuted

                                    Text(
                                        text = vzText,
                                        color = vzColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Main AGL Altitude
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format("%.1f", telemetry.gps.altitudeAglMeters),
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "M AGL",
                                        color = ThermalAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                // Secondary MSL Altitude & Ground Speed
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "MSL ${String.format("%.1f", telemetry.gps.altitudeMslMeters)}m",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "SPD ${String.format("%.1f", telemetry.velocity.groundSpeedKmh)} km/h",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // 2. BATTERY TILE
                        TranslucentTelemetryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_battery_stat")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                val batPercent = telemetry.battery.chargeRemainingPercent
                                val batColor = if (batPercent > 50) TelemetryGreen
                                else if (batPercent > 25) TelemetryWarning
                                else ThermalHotRed

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.BatteryChargingFull,
                                            contentDescription = "Battery Icon",
                                            tint = batColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "BATTERY",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Estimated Remaining Time
                                    val estMins = (batPercent * 0.42f).toInt().coerceAtLeast(1)
                                    Text(
                                        text = "~${estMins}m REM",
                                        color = batColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Main Battery Percent & Voltage
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$batPercent%",
                                        color = batColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1fV (6S)", telemetry.battery.voltageMv / 1000f),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                // Segmented Battery Gauge Canvas
                                BatterySegmentedGauge(
                                    percent = batPercent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .padding(vertical = 1.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Temp & Cell health
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${String.format("%.1f", telemetry.battery.temperatureCelsius)}°C",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "HLTH ${telemetry.battery.batteryHealthPercent}%",
                                        color = TelemetryGreen,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: GPS Coordinates (Left) & Gimbal Orientation (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. GPS COORDINATES TILE
                        TranslucentTelemetryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_gps_stat")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GpsFixed,
                                            contentDescription = "GPS Icon",
                                            tint = TelemetryGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "GPS RTK",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // RTK Fix Type Chip
                                    val fixColor = when (telemetry.gps.fixType) {
                                        GpsFixType.RTK_FIXED -> TelemetryGreen
                                        GpsFixType.RTK_FLOAT, GpsFixType.FIX_3D -> ThermalAmber
                                        else -> ThermalHotRed
                                    }
                                    Text(
                                        text = telemetry.gps.fixType.label.uppercase(),
                                        color = fixColor,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Lat / Lon
                                Text(
                                    text = telemetry.gps.formatCoordinates(),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Satellites & Precision Accuracy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${telemetry.gps.satelliteCount} SATS",
                                        color = ThermalColdCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "±${String.format("%.2f", telemetry.gps.horizontalAccuracyMeters)}m",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Distance to Home
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "RTH HOME",
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${String.format("%.1f", telemetry.distanceToHomeMeters)}m",
                                        color = ThermalAmber,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 4. GIMBAL ORIENTATION TILE
                        TranslucentTelemetryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_gimbal_stat")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                val pitch = telemetry.gimbal.pitchDegrees

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = "Gimbal Icon",
                                            tint = ThermalAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "GIMBAL",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Mode label: NADIR vs OBLIQUE vs HORIZON
                                    val modeLabel = when {
                                        pitch <= -75f -> "NADIR (90°)"
                                        pitch in -74f..-25f -> "OBLIQUE"
                                        else -> "HORIZON"
                                    }
                                    Text(
                                        text = modeLabel,
                                        color = ThermalAmber,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Main Pitch Readout
                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = String.format("%.1f°", pitch),
                                                color = TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "R: ${String.format("%.0f°", telemetry.gimbal.rollDegrees)} | Y: ${String.format("%.0f°", telemetry.gimbal.yawDegrees)}",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Graphical Gimbal Tilt Visualizer (Mini Arc & Pointer)
                                    GimbalTiltArcVisualizer(
                                        pitchDegrees = pitch,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                // Calibration Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "STATUS",
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (telemetry.gimbal.isCalibrated) "CALIBRATED (OK)" else "UNBOUND",
                                        color = if (telemetry.gimbal.isCalibrated) TelemetryGreen else TelemetryWarning,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // =========================================================================
                    // FOOTER: Compass Heading & AirLink Transmission Quality
                    // =========================================================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33182230))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aircraft Heading
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Compass",
                                tint = ThermalAmber,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "HDG ${String.format("%.0f°", telemetry.attitude.compassHeadingDegrees)} ${telemetry.attitude.formatHeadingCardinal()}",
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // AirLink Signal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = "AirLink",
                                tint = TelemetryGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "O4 LINK: ${telemetry.airLink.signalDbm} dBm (${telemetry.airLink.downlinkSignalQualityPercent}%)",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenience overload accepting the generic [DroneTelemetry] data structure.
 */
@Composable
fun DroneTelemetryOverlay(
    telemetry: DroneTelemetry,
    modifier: Modifier = Modifier,
    gimbalPitch: Float = -45.0f,
    initiallyExpanded: Boolean = true,
    onClose: (() -> Unit)? = null
) {
    // Map generic DroneTelemetry into DjiComprehensiveTelemetry representation
    val comprehensive = remember(telemetry, gimbalPitch) {
        DjiComprehensiveTelemetry(
            modelName = telemetry.modelName,
            battery = com.example.dji.model.DjiBatteryTelemetry(
                chargeRemainingPercent = telemetry.batteryPercent,
                voltageMv = 22800
            ),
            gps = com.example.dji.model.DjiGpsTelemetry(
                latitude = telemetry.latitude,
                longitude = telemetry.longitude,
                altitudeAglMeters = telemetry.altitudeMeters,
                altitudeMslMeters = (telemetry.altitudeMeters + 4.0).toDouble(),
                satelliteCount = telemetry.gpsSatellites,
                fixType = if (telemetry.gpsSatellites >= 18) GpsFixType.RTK_FIXED else GpsFixType.FIX_3D
            ),
            gimbal = com.example.dji.model.DjiGimbalTelemetry(
                pitchDegrees = gimbalPitch
            ),
            velocity = com.example.dji.model.DjiVelocityTelemetry(
                groundSpeedKmh = telemetry.groundSpeedKmh
            ),
            attitude = com.example.dji.model.DjiAttitudeTelemetry(
                pitchDegrees = telemetry.pitchDegrees,
                rollDegrees = telemetry.rollDegrees,
                yawDegrees = telemetry.yawDegrees,
                compassHeadingDegrees = telemetry.yawDegrees
            ),
            airLink = com.example.dji.model.DjiAirLinkTelemetry(
                signalDbm = telemetry.signalDbm
            )
        )
    }

    DroneTelemetryOverlay(
        telemetry = comprehensive,
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        onClose = onClose
    )
}

/**
 * Translucent secondary tile container for grouped telemetry metrics.
 */
@Composable
private fun TranslucentTelemetryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x55111720))
            .border(1.dp, Color(0x3326354A), RoundedCornerShape(8.dp))
    ) {
        content()
    }
}

/**
 * Dynamic segmented battery bar with LiPo cell visualization.
 */
@Composable
private fun BatterySegmentedGauge(
    percent: Int,
    modifier: Modifier = Modifier
) {
    val barColor = if (percent > 50) TelemetryGreen else if (percent > 25) TelemetryWarning else ThermalHotRed
    val segments = 6 // 6S Battery Representation

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until segments) {
            val segmentThreshold = (i + 1) * (100 / segments)
            val isFilled = percent >= segmentThreshold - (100 / (segments * 2))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isFilled) barColor else Color(0x33FFFFFF))
            )
        }
    }
}

/**
 * Graphical Gimbal Tilt Visualizer:
 * Draws an arc representing the camera range from 0° (horizontal forward) to -90° (nadir straight down),
 * with a dynamic pointer needle showing the live camera orientation.
 */
@Composable
private fun GimbalTiltArcVisualizer(
    pitchDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.2f, h * 0.2f)
        val radius = w * 0.7f

        // Draw Reference Arc from 0° (right) down to 90° (down)
        drawArc(
            color = Color(0x44FFFFFF),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw 0° (Horizon) and 90° (Nadir) tick marks
        val tickLen = 4.dp.toPx()
        // 0° tick (right)
        drawLine(
            color = TextMuted,
            start = Offset(center.x + radius - tickLen, center.y),
            end = Offset(center.x + radius + tickLen, center.y),
            strokeWidth = 1.5.dp.toPx()
        )
        // -90° tick (bottom)
        drawLine(
            color = TextMuted,
            start = Offset(center.x, center.y + radius - tickLen),
            end = Offset(center.x, center.y + radius + tickLen),
            strokeWidth = 1.5.dp.toPx()
        )

        // Calculate pitch needle angle:
        // pitch is 0° (horizontal) to -90° (down)
        val clampedPitch = pitchDegrees.coerceIn(-90f, 0f)
        val needleAngleDeg = -clampedPitch // 0° to 90° clockwise
        val needleRad = needleAngleDeg * (PI / 180.0)

        val endX = (center.x + radius * cos(needleRad)).toFloat()
        val endY = (center.y + radius * sin(needleRad)).toFloat()

        // Needle line
        drawLine(
            color = ThermalAmber,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center pivot point
        drawCircle(
            color = ThermalCoreYellow,
            radius = 2.5.dp.toPx(),
            center = center
        )

        // Pointer tip circle
        drawCircle(
            color = ThermalAmber,
            radius = 3.dp.toPx(),
            center = Offset(endX, endY)
        )
    }
}
