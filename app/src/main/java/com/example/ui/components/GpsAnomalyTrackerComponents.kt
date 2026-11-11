package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dji.model.DjiComprehensiveTelemetry
import com.example.model.AlertSeverity
import com.example.model.DroneTelemetry
import com.example.model.GpsCoordinateFormat
import com.example.model.GpsMathUtils
import com.example.model.IdentifiedHeatAnomaly
import com.example.model.RelativeAnomalyTelemetry
import com.example.model.TemperatureUnit
import com.example.ui.theme.AlertCriticalBg
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.HudOverlayBg
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
import com.example.ui.theme.ThermalHotRed

/**
 * Tactical Line-Of-Sight Reticle & Peripheral Target Director.
 * Overlays an avionics targeting reticle over the identified heat anomaly when in camera FOV,
 * or displays an edge director pointer with range and clock direction when outside FOV.
 */
@Composable
fun TacticalAnomalyReticleOverlay(
    telemetry: RelativeAnomalyTelemetry,
    tempUnit: TemperatureUnit,
    onTapTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = telemetry.targetAnomaly

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 64.dp)) {
        if (telemetry.isTargetInFov) {
            // Target is inside thermal camera field of view: draw tactical target box
            val normX = ((telemetry.fovScreenX + 1f) / 2f).coerceIn(0.12f, 0.88f)
            val normY = ((telemetry.fovScreenY + 1f) / 2f).coerceIn(0.16f, 0.84f)

            Box(
                modifier = Modifier
                    .align(
                        BiasAlignment(
                            (normX * 2f) - 1f,
                            (normY * 2f) - 1f
                        )
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(HudOverlayBg.copy(alpha = 0.85f))
                    .border(1.5.dp, ThermalHotRed, RoundedCornerShape(6.dp))
                    .clickable { onTapTarget() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("hud_tactical_target_reticle")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Target Lock",
                            tint = ThermalHotRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[${target.id}] ${target.name.take(14)}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tempUnit.format(target.peakTemp),
                            color = ThermalHotRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "R: ${String.format("%.1fm", telemetry.groundDistanceMeters)} • BRG: ${String.format("%03d°", telemetry.trueBearingDegrees.toInt())} (${telemetry.clockPosition})",
                        color = ThermalAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Target is outside camera FOV: draw edge indicator pointer
            val relAzimuth = telemetry.relativeAzimuthDegrees
            val isLeft = relAzimuth < 0
            val edgeAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

            Box(
                modifier = Modifier
                    .align(edgeAlignment)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HudOverlayBg.copy(alpha = 0.9f))
                    .border(1.dp, ThermalAmber, RoundedCornerShape(6.dp))
                    .clickable { onTapTarget() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("hud_edge_target_director")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLeft) {
                        Text(
                            text = "◀",
                            color = ThermalHotRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "TGT: ${target.id} [${telemetry.clockPosition}]",
                            color = ThermalAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DST: ${String.format("%.0fm", telemetry.groundDistanceMeters)} • ${String.format("%03d°", telemetry.trueBearingDegrees.toInt())}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (!isLeft) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▶",
                            color = ThermalHotRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tactical GPS Geolocation & Heat Anomaly Tracking Modal Dialog.
 * Allows pilots to inspect the drone's position in DD, DMS, and MGRS formats,
 * examine all identified heat anomalies with relative vectors (bearing, range, delta N/E/Alt),
 * and select the active tracking target.
 */
@Composable
fun GpsAnomalyTrackerDialog(
    djiTelemetry: DjiComprehensiveTelemetry,
    droneTelemetry: DroneTelemetry,
    isDeviceCam: Boolean,
    gpsCoordinateFormat: GpsCoordinateFormat,
    onSelectGpsFormat: (GpsCoordinateFormat) -> Unit,
    anomalies: List<IdentifiedHeatAnomaly>,
    relativeTelemetry: RelativeAnomalyTelemetry?,
    onSelectAnomaly: (String) -> Unit,
    onDismiss: () -> Unit,
    tempUnit: TemperatureUnit
) {
    val droneLat = if (isDeviceCam) droneTelemetry.latitude else djiTelemetry.gps.latitude
    val droneLng = if (isDeviceCam) droneTelemetry.longitude else djiTelemetry.gps.longitude
    val altitudeAgl = if (isDeviceCam) 1.5f else djiTelemetry.gps.altitudeAglMeters
    val altitudeMsl = if (isDeviceCam) 35.0 else djiTelemetry.gps.altitudeMslMeters
    val heading = if (isDeviceCam) 214.0f else djiTelemetry.attitude.compassHeadingDegrees
    val groundSpeed = if (isDeviceCam) 0.0f else djiTelemetry.velocity.groundSpeedKmh

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = ThermalColdCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TACTICAL GPS & ANOMALY TRACKER",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DJI Matrice 4TD // Radiometric Georeferencing",
                            color = ThermalAmber,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("close_gps_anomaly_dialog")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().testTag("gps_anomaly_tracker_dialog_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. DRONE PRECISE GPS TELEMETRY CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = TelemetryGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "AIRCRAFT CURRENT GEOLOCATION",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "RTK FIXED (${djiTelemetry.gps.satelliteCount} SATS)",
                                    color = TelemetryGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Three-format coordinates display
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepObsidian, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "DECIMAL:  ${GpsMathUtils.formatCoordinateDecimal(droneLat, droneLng)}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "DMS:      ${GpsMathUtils.formatCoordinateDms(droneLat, droneLng)}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "MGRS/UTM: ${GpsMathUtils.formatCoordinateMgrs(droneLat, droneLng)}",
                                    color = ThermalColdCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Secondary kinematics row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ALTITUDE (AGL / MSL)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text("${String.format("%.1f", altitudeAgl)}m / ${String.format("%.1f", altitudeMsl)}m", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("HEADING", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text("${String.format("%.1f°", heading)} (${djiTelemetry.attitude.formatHeadingCardinal()})", color = ThermalAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("GROUND SPEED", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.1f km/h", groundSpeed), color = TelemetryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("HORIZ ACCURACY", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text("±0.4m (RTK)", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // 2. COORDINATE FORMAT SWITCHER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HUD DISPLAY FORMAT:",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GpsCoordinateFormat.values().forEach { fmt ->
                                val isSelected = fmt == gpsCoordinateFormat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) ThermalAmber else SurfaceElevated)
                                        .border(1.dp, if (isSelected) ThermalAmber else SurfaceBorder, RoundedCornerShape(4.dp))
                                        .clickable { onSelectGpsFormat(fmt) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("gps_format_chip_${fmt.shortLabel.lowercase()}")
                                ) {
                                    Text(
                                        text = fmt.shortLabel,
                                        color = if (isSelected) DeepObsidian else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. IDENTIFIED HEAT ANOMALIES LIST HEADER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = ThermalHotRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "IDENTIFIED HEAT ANOMALIES (${anomalies.size})",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "REAL-TIME RELATIVE TRACKING",
                            color = ThermalColdCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 4. ANOMALY CARDS WITH REAL-TIME RELATIVE VECTORS
                items(anomalies) { anomaly ->
                    val isTracked = anomaly.id == relativeTelemetry?.targetAnomaly?.id
                    val dist = GpsMathUtils.calculateGroundDistance(droneLat, droneLng, anomaly.latitude, anomaly.longitude)
                    val bearing = GpsMathUtils.calculateBearing(droneLat, droneLng, anomaly.latitude, anomaly.longitude)
                    val relAzimuth = GpsMathUtils.calculateRelativeAzimuth(bearing, heading)
                    val clock = GpsMathUtils.calculateClockDirection(relAzimuth)
                    val dNorth = ((anomaly.latitude - droneLat) * GpsMathUtils.METERS_PER_DEG_LAT).toFloat()
                    val dEast = ((anomaly.longitude - droneLng) * GpsMathUtils.metersPerDegLng(droneLat)).toFloat()

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTracked) SurfaceElevated else SurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isTracked) ThermalHotRed else SurfaceBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAnomaly(anomaly.id) }
                            .testTag("anomaly_item_card_${anomaly.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Top Row: Tag, Name, Temp Badge, Track button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (anomaly.severity == AlertSeverity.CRITICAL) ThermalHotRed else ThermalAmber,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "[${anomaly.id}] ${anomaly.name}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${anomaly.signatureType.title} • Conf: ${anomaly.confidencePercent}%",
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tempUnit.format(anomaly.peakTemp),
                                        color = ThermalHotRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onSelectAnomaly(anomaly.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTracked) ThermalHotRed else SurfaceBorder
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp).testTag("select_anomaly_button_${anomaly.id}")
                                    ) {
                                        Text(
                                            text = if (isTracked) "TRACKING" else "TRACK",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Target GPS Coordinates in selected format
                            Text(
                                text = "ANOMALY GPS: ${anomaly.formatCoordinates(gpsCoordinateFormat)}",
                                color = ThermalColdCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Relative Kinematics Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepObsidian, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("GROUND RANGE", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.1f m", dist), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("TRUE BEARING", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text("${String.format("%03d°", bearing.toInt())} ($clock)", color = ThermalAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("REL AZIMUTH", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%+.1f°", relAzimuth), color = if (kotlin.math.abs(relAzimuth) <= 30.5f) TelemetryGreen else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("OFFSETS (N/E)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text("${String.format("%+.0fm", dNorth)}, ${String.format("%+.0fm", dEast)}", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("dismiss_gps_anomaly_modal_button")
            ) {
                Text("DONE", color = DeepObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark
    )
}
