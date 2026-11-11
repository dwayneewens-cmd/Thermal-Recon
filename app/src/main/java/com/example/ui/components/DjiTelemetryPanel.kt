package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.dji.model.DjiConnectionMode
import com.example.dji.model.DjiConnectionState
import com.example.dji.model.FlightPattern
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

@Composable
fun DjiTelemetryPanel(
    telemetry: DjiComprehensiveTelemetry,
    connectionState: DjiConnectionState,
    connectionMode: DjiConnectionMode,
    activePattern: FlightPattern,
    onModeSelected: (DjiConnectionMode) -> Unit,
    onPatternSelected: (FlightPattern) -> Unit,
    onTriggerRth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dji_telemetry_panel")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Model & Connection State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        tint = ThermalAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DJI MOBILE SDK SERVICE",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = telemetry.modelName,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Connection State Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (connectionState.isConnected) TelemetryGreen.copy(alpha = 0.2f)
                            else ThermalHotRed.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (connectionState.isConnected) TelemetryGreen else ThermalHotRed,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = connectionState.label.uppercase(),
                        color = if (connectionState.isConnected) TelemetryGreen else ThermalHotRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Connection Mode Chips
            Text(
                text = "SDK INTERFACE LINK",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DjiConnectionMode.values().forEach { mode ->
                    val isSelected = connectionMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.title, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThermalAmber,
                            selectedLabelColor = DeepObsidian
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. GPS Coordinates & Altitude Section
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = TelemetryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AIRCRAFT GPS POSITION",
                                color = ThermalAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TelemetryGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = telemetry.gps.fixType.label,
                                color = TelemetryGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = telemetry.gps.formatCoordinates(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ALTITUDE (AGL)", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("%.1f m (%.0f ft)", telemetry.gps.altitudeAglMeters, telemetry.gps.altitudeAglMeters * 3.28084f),
                                color = ThermalColdCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("ALTITUDE (MSL)", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("%.1f m", telemetry.gps.altitudeMslMeters),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("SATELLITES", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${telemetry.gps.satelliteCount} LOCK",
                                color = TelemetryGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Compass Heading & Kinematics Section
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compass Rose Dial Canvas
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(DeepObsidian, CircleShape)
                            .border(1.dp, SurfaceBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val heading = telemetry.attitude.compassHeadingDegrees
                        Canvas(modifier = Modifier.size(60.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f

                            // Outer degree circle
                            drawCircle(color = TextMuted.copy(alpha = 0.3f), radius = radius, style = Stroke(width = 1.5f))

                            // North tick
                            rotate(degrees = -heading, pivot = center) {
                                drawLine(ThermalHotRed, Offset(center.x, 2f), Offset(center.x, 10f), 3f)
                                drawLine(TextMuted, Offset(center.x, size.height - 2f), Offset(center.x, size.height - 10f), 2f)
                                drawLine(TextMuted, Offset(2f, center.y), Offset(10f, center.y), 2f)
                                drawLine(TextMuted, Offset(size.width - 2f, center.y), Offset(size.width - 10f, center.y), 2f)
                            }

                            // Aircraft Arrow pointer
                            drawLine(ThermalAmber, Offset(center.x, center.y + 12f), Offset(center.x, center.y - 14f), 3.5f)
                            drawLine(ThermalAmber, Offset(center.x - 7f, center.y - 4f), Offset(center.x, center.y - 14f), 3.5f)
                            drawLine(ThermalAmber, Offset(center.x + 7f, center.y - 4f), Offset(center.x, center.y - 14f), 3.5f)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPASS HEADING", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${String.format("%.1f", telemetry.attitude.compassHeadingDegrees)}° ${telemetry.attitude.formatHeadingCardinal()}",
                                    color = ThermalAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("GROUND SPEED", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = String.format("%.1f km/h", telemetry.velocity.groundSpeedKmh),
                                    color = TelemetryGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pitch: ${String.format("%.1f", telemetry.attitude.pitchDegrees)}°",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Roll: ${String.format("%.1f", telemetry.attitude.rollDegrees)}°",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Yaw: ${String.format("%.1f", telemetry.attitude.yawDegrees)}°",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Battery & Distance to Home Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (telemetry.battery.chargeRemainingPercent > 25) TelemetryGreen else TelemetryWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BATTERY", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${telemetry.battery.chargeRemainingPercent}% (${String.format("%.1fV", telemetry.battery.voltageMv / 1000f)})",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Temp: ${String.format("%.1f°C", telemetry.battery.temperatureCelsius)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Distance to Home Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = ThermalColdCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DISTANCE HOME", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f m", telemetry.distanceToHomeMeters),
                            color = ThermalColdCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Mode: ${telemetry.flightMode.label.take(12)}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. Flight Simulator Controls (if in simulator mode)
            if (connectionMode == DjiConnectionMode.SIMULATOR_BRIDGE) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SIMULATOR FLIGHT PATTERN",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlightPattern.values().forEach { pattern ->
                        val isSelected = activePattern == pattern
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPatternSelected(pattern) },
                            label = { Text(pattern.title, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurfaceElevated,
                                selectedLabelColor = ThermalAmber
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RTH Trigger Button
            Button(
                onClick = onTriggerRth,
                colors = ButtonDefaults.buttonColors(containerColor = ThermalHotRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("rth_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = "RTH",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMMAND RETURN-TO-HOME (RTH)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
