package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.camera.CameraThermalHelper
import com.example.model.CameraFeedSource
import com.example.model.LabScenario
import com.example.model.ThermalFusionMode
import com.example.model.ThermalPalette
import com.example.ui.components.AvionicsHudOverlay
import com.example.ui.components.DroneTelemetryOverlay
import com.example.ui.components.DroneVideoBackdropView
import com.example.ui.components.ThermalHeatmapOverlayView
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
import com.example.ui.theme.ThermalCoreYellow
import com.example.ui.theme.ThermalHotRed
import com.example.viewmodel.ThermalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalHudScreen(
    viewModel: ThermalViewModel,
    onNavigateToAiCopilot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val feedSource by viewModel.feedSource.collectAsState()
    val labScenario by viewModel.labScenario.collectAsState()
    val palette by viewModel.palette.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val droneTelemetry by viewModel.droneTelemetry.collectAsState()
    val djiTelemetry by viewModel.djiTelemetry.collectAsState()
    val deviceAttitude by viewModel.deviceAttitude.collectAsState()
    val activeAlertBanner by viewModel.activeAlertBanner.collectAsState()
    val isFrozen by viewModel.isFrozen.collectAsState()
    val roiBox by viewModel.roiBox.collectAsState()
    val inspectionSpots by viewModel.inspectionSpots.collectAsState()
    val isothermEnabled by viewModel.isothermEnabled.collectAsState()
    val isothermMin by viewModel.isothermMin.collectAsState()
    val isothermMax by viewModel.isothermMax.collectAsState()
    val soundAlarmEnabled by viewModel.soundAlarmEnabled.collectAsState()
    val heatmapOpacity by viewModel.heatmapOpacity.collectAsState()
    val fusionMode by viewModel.fusionMode.collectAsState()
    val showContourLines by viewModel.showContourLines.collectAsState()
    val showHeatGlow by viewModel.showHeatGlow.collectAsState()

    var showPaletteSheet by remember { mutableStateOf(false) }
    var showHeatmapControlsSheet by remember { mutableStateOf(false) }
    var showIsothermDialog by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var showTelemetryOverlay by remember { mutableStateOf(true) }
    var snapshotTitle by remember { mutableStateOf("Thermal Inspection Snapshot") }
    var snapshotNotes by remember { mutableStateOf("") }

    // Camera Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.setFeedSource(CameraFeedSource.DEVICE_CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DeepObsidian)) {

        // 1. Video / Thermal Viewfinder
        if (feedSource == CameraFeedSource.DEVICE_CAMERA && hasCameraPermission) {
            // Live Device Camera Feed through CameraX + Radiometric Heatmap Overlay
            var cameraHelper by remember { mutableStateOf<CameraThermalHelper?>(null) }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            cameraHelper = CameraThermalHelper(ctx, lifecycleOwner) { bitmap ->
                                viewModel.onCameraBitmapAvailable(bitmap)
                            }
                            cameraHelper?.bindCamera(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        cameraHelper?.unbind()
                    }
                }

                // Radiometric Thermal Intensity Heatmap Overlay on Live Optical Camera Feed
                ThermalHeatmapOverlayView(
                    frameData = currentFrame,
                    palette = palette,
                    tempUnit = tempUnit,
                    heatmapOpacity = heatmapOpacity,
                    fusionMode = fusionMode,
                    isothermEnabled = isothermEnabled,
                    isothermMin = isothermMin,
                    isothermMax = isothermMax,
                    showContourLines = showContourLines,
                    showHeatGlow = showHeatGlow,
                    roiBox = roiBox,
                    inspectionSpots = inspectionSpots,
                    onTap = { x, y -> viewModel.addInspectionSpot(x, y) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // DJI Drone Stream or Lab Simulation Mode: Optical Aerial Backdrop + Color-Coded Heatmap Overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Live Aerial Drone Optical Video Backdrop
                DroneVideoBackdropView(
                    scenario = labScenario,
                    modifier = Modifier.fillMaxSize()
                )

                // Radiometric Thermal Intensity Heatmap Layer
                ThermalHeatmapOverlayView(
                    frameData = currentFrame,
                    palette = palette,
                    tempUnit = tempUnit,
                    heatmapOpacity = heatmapOpacity,
                    fusionMode = fusionMode,
                    isothermEnabled = isothermEnabled,
                    isothermMin = isothermMin,
                    isothermMax = isothermMax,
                    showContourLines = showContourLines,
                    showHeatGlow = showHeatGlow,
                    roiBox = roiBox,
                    inspectionSpots = inspectionSpots,
                    onTap = { x, y -> viewModel.addInspectionSpot(x, y) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Avionics HUD Overlay (OSD Bar, Attitude Horizon, Radiometric Color Scale, Alerts)
        AvionicsHudOverlay(
            feedSource = feedSource,
            droneTelemetry = droneTelemetry,
            djiTelemetry = djiTelemetry,
            deviceAttitude = deviceAttitude,
            frameData = currentFrame,
            palette = palette,
            tempUnit = tempUnit,
            emissivity = viewModel.currentEmissivity,
            activeAlert = activeAlertBanner,
            onDismissAlert = { viewModel.dismissAlertBanner() },
            onTogglePalette = { viewModel.cycleNextPalette() }
        )

        // 2.5 Translucent Drone Telemetry Overlay (Altitude, Battery, GPS, Gimbal Orientation)
        AnimatedVisibility(
            visible = showTelemetryOverlay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 8.dp, end = 8.dp)
        ) {
            DroneTelemetryOverlay(
                telemetry = djiTelemetry,
                initiallyExpanded = true,
                onClose = { showTelemetryOverlay = false }
            )
        }

        // Floating quick-open badge when overlay is closed
        if (!showTelemetryOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 38.dp, end = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD9090D12))
                    .border(1.dp, ThermalAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { showTelemetryOverlay = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("toggle_telemetry_overlay_floating_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Open Telemetry HUD",
                        tint = ThermalAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE TELEMETRY",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 3. Bottom Mission Control Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Live Status Strip & Spot Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudOverlayBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ThermalCoreYellow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "CTR: ${tempUnit.format(currentFrame.centerSpotTemp)}",
                        color = ThermalCoreYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "HOT: ${tempUnit.format(currentFrame.maxTemp)}",
                        color = ThermalHotRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AVG: ${tempUnit.format(currentFrame.avgTemp)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (inspectionSpots.isNotEmpty()) {
                    Text(
                        text = "${inspectionSpots.size} PINS (TAP CLEAR)",
                        color = ThermalAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { viewModel.clearInspectionSpots() }
                            .testTag("clear_pins_button")
                    )
                } else {
                    Text(
                        text = "TAP SCREEN TO PIN",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Real-Time Infrared Color Mapping Toggle Row (White Hot, Black Hot, Ironbow, etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudOverlayBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("ir_color_mapping_toggle_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.cycleNextPalette() }
                        .padding(start = 2.dp, end = 6.dp)
                        .testTag("ir_map_label_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "IR Color Map",
                        tint = ThermalAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IR MAP:",
                        color = ThermalAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Ordered with White Hot, Black Hot, Ironbow first for instant tactical recognition
                val primaryPalettes = listOf(
                    ThermalPalette.WHITE_HOT,
                    ThermalPalette.BLACK_HOT,
                    ThermalPalette.IRONBOW,
                    ThermalPalette.RAINBOW,
                    ThermalPalette.INFERNO,
                    ThermalPalette.ARCTIC,
                    ThermalPalette.LAVA
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    primaryPalettes.forEach { p ->
                        val isSelected = palette == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPalette(p) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 9.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    p.mapToColor(0.0f),
                                                    p.mapToColor(0.5f),
                                                    p.mapToColor(1.0f)
                                                )
                                            )
                                        )
                                )
                            },
                            label = {
                                Text(
                                    text = p.title.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThermalAmber,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceElevated,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("color_mapping_toggle_${p.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // One-tap quick cycle button
                IconButton(
                    onClick = { viewModel.cycleNextPalette() },
                    modifier = Modifier.size(28.dp).testTag("quick_cycle_palette_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Cycle Palette",
                        tint = ThermalAmber,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Feed Source & Scenario Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Camera Chip
                FilterChip(
                    selected = feedSource == CameraFeedSource.DEVICE_CAMERA,
                    onClick = {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            viewModel.setFeedSource(CameraFeedSource.DEVICE_CAMERA)
                        }
                    },
                    label = { Text("Mobile Camera", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThermalAmber,
                        selectedLabelColor = DeepObsidian
                    ),
                    modifier = Modifier.testTag("feed_device_camera")
                )

                // DJI Drone Stream Chip
                FilterChip(
                    selected = feedSource == CameraFeedSource.DJI_DRONE_STREAM,
                    onClick = { viewModel.setFeedSource(CameraFeedSource.DJI_DRONE_STREAM) },
                    label = { Text("DJI Drone Link", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterDrama,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TelemetryGreen,
                        selectedLabelColor = DeepObsidian
                    ),
                    modifier = Modifier.testTag("feed_dji_drone")
                )

                // Lab Simulation Chip
                FilterChip(
                    selected = feedSource == CameraFeedSource.LAB_SIMULATION,
                    onClick = { viewModel.setFeedSource(CameraFeedSource.LAB_SIMULATION) },
                    label = { Text("Lab Flight Sim", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThermalColdCyan,
                        selectedLabelColor = DeepObsidian
                    ),
                    modifier = Modifier.testTag("feed_lab_sim")
                )

                // Scenario Chips when in Lab Sim
                if (feedSource == CameraFeedSource.LAB_SIMULATION) {
                    LabScenario.values().forEach { scenario ->
                        FilterChip(
                            selected = labScenario == scenario,
                            onClick = { viewModel.setLabScenario(scenario) },
                            label = { Text(scenario.missionType, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurfaceElevated,
                                selectedLabelColor = ThermalAmber
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Control Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudOverlayBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 0. Heatmap Layer & Fusion Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showHeatmapControlsSheet = true }
                        .testTag("heatmap_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Heatmap Layer",
                        tint = ThermalAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "HEATMAP",
                        color = ThermalAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 0.5 Translucent Drone Telemetry Overlay Toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showTelemetryOverlay = !showTelemetryOverlay }
                        .testTag("toggle_telemetry_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Drone Telemetry",
                        tint = if (showTelemetryOverlay) TelemetryGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (showTelemetryOverlay) "TLM ON" else "TLM OFF",
                        color = if (showTelemetryOverlay) TelemetryGreen else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 0.6 Gemini Thermal AI Copilot Action
                if (onNavigateToAiCopilot != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToAiCopilot() }
                            .testTag("hud_ai_copilot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = ThermalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI COPILOT",
                            color = ThermalAmber,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Palette Switcher
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showPaletteSheet = true }
                        .testTag("palette_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Palette",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = palette.title,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 2. Isotherm Threshold
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showIsothermDialog = true }
                        .testTag("isotherm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Isotherm",
                        tint = if (isothermEnabled) TelemetryGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isothermEnabled) "ISO ON" else "ISO OFF",
                        color = if (isothermEnabled) TelemetryGreen else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 3. ROI Box Toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.toggleRoi(!roiBox.enabled) }
                        .testTag("roi_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "ROI",
                        tint = if (roiBox.enabled) ThermalColdCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (roiBox.enabled) "ROI ON" else "ROI OFF",
                        color = if (roiBox.enabled) ThermalColdCyan else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 4. Freeze Frame Toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.toggleFreeze() }
                        .testTag("freeze_button")
                ) {
                    Icon(
                        imageVector = if (isFrozen) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Freeze",
                        tint = if (isFrozen) ThermalColdCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFrozen) "FROZEN" else "LIVE",
                        color = if (isFrozen) ThermalColdCyan else TelemetryGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 5. Sound Alarm Toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.toggleSoundAlarm() }
                        .testTag("mute_alarm_button")
                ) {
                    Icon(
                        imageVector = if (soundAlarmEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Alarm Sound",
                        tint = if (soundAlarmEnabled) ThermalHotRed else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (soundAlarmEnabled) "ALARM" else "MUTED",
                        color = if (soundAlarmEnabled) ThermalHotRed else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 6. Capture Inspection Snapshot
                Button(
                    onClick = { showSnapshotDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("capture_snapshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Capture",
                        tint = DeepObsidian,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "REC",
                        color = DeepObsidian,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    // Modal: Heatmap Layer & Fusion Controls BottomSheet
    if (showHeatmapControlsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHeatmapControlsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THERMAL HEATMAP OVERLAY",
                        color = ThermalAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(heatmapOpacity * 100).toInt()}% OPACITY",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heatmap Opacity Slider
                Text(
                    text = "Heatmap Layer Opacity",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = heatmapOpacity,
                    onValueChange = { viewModel.setHeatmapOpacity(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = ThermalAmber,
                        activeTrackColor = ThermalAmber
                    ),
                    modifier = Modifier.testTag("heatmap_opacity_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fusion Modes Selector
                Text(
                    text = "Thermal Video Fusion Mode",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThermalFusionMode.values().forEach { mode ->
                    val isSelected = fusionMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceElevated else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ThermalAmber else SurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.setFusionMode(mode)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                color = if (isSelected) ThermalAmber else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mode.description,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ThermalAmber, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 1: Dynamic Isothermal Contours
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Isothermal Intensity Contours",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Displays concentric radiometric gradient rings",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = showContourLines,
                        onCheckedChange = { viewModel.toggleContourLines() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThermalAmber,
                            checkedTrackColor = ThermalAmber.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("contour_lines_switch")
                    )
                }

                // Toggle 2: Radiant Heat Halo Glow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hotspot Radiance Heat Halo",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Radial thermal plume glow at maximum temperature point",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = showHeatGlow,
                        onCheckedChange = { viewModel.toggleHeatGlow() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThermalHotRed,
                            checkedTrackColor = ThermalHotRed.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("heat_glow_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal: Palette Selector BottomSheet
    if (showPaletteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SELECT RADIOMETRIC PALETTE",
                    color = ThermalAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                ThermalPalette.values().forEach { p ->
                    val isSelected = palette == p
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceElevated else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ThermalAmber else SurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.setPalette(p)
                                showPaletteSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.title,
                                color = if (isSelected) ThermalAmber else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = p.description,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        // Palette preview gradient swatch
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(
                                            p.mapToColor(0f),
                                            p.mapToColor(0.5f),
                                            p.mapToColor(1f)
                                        )
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Isotherm Thresholding
    if (showIsothermDialog) {
        var tempMinSlider by remember { mutableStateOf(isothermMin) }
        var tempMaxSlider by remember { mutableStateOf(isothermMax) }

        AlertDialog(
            onDismissRequest = { showIsothermDialog = false },
            title = {
                Text(
                    text = "ISOTHERM COLOR BANDING",
                    color = ThermalAmber,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Isotherms isolate critical target temperatures (e.g. human core 34-39°C or solar hotspot >65°C) while desaturating non-target background pixels.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Isotherm", color = TextPrimary, fontSize = 12.sp)
                        Button(
                            onClick = {
                                viewModel.toggleIsotherm(!isothermEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isothermEnabled) TelemetryGreen else SurfaceElevated
                            )
                        ) {
                            Text(
                                text = if (isothermEnabled) "ACTIVE" else "DISABLED",
                                color = if (isothermEnabled) DeepObsidian else TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Threshold Range: ${tempUnit.format(tempMinSlider)} - ${tempUnit.format(tempMaxSlider)}",
                        color = ThermalAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Min Threshold (${tempUnit.format(tempMinSlider)})", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = tempMinSlider,
                        onValueChange = { tempMinSlider = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = ThermalColdCyan, activeTrackColor = ThermalColdCyan)
                    )

                    Text("Max Threshold (${tempUnit.format(tempMaxSlider)})", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = tempMaxSlider,
                        onValueChange = { tempMaxSlider = it },
                        valueRange = 10f..120f,
                        colors = SliderDefaults.colors(thumbColor = ThermalHotRed, activeTrackColor = ThermalHotRed)
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempMinSlider = 34.0f
                                tempMaxSlider = 39.5f
                                viewModel.toggleIsotherm(true)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Human SAR", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                tempMinSlider = 60.0f
                                tempMaxSlider = 110.0f
                                viewModel.toggleIsotherm(true)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Overheat", fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setIsothermRange(tempMinSlider, tempMaxSlider)
                        showIsothermDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber)
                ) {
                    Text("Apply Range", color = DeepObsidian)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIsothermDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Capture Inspection Snapshot
    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = {
                Text(
                    text = "SAVE THERMAL INSPECTION REPORT",
                    color = ThermalAmber,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current frame metrics: Peak ${tempUnit.format(currentFrame.maxTemp)} | Avg ${tempUnit.format(currentFrame.avgTemp)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        label = { Text("Report Title", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("snapshot_title_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = snapshotNotes,
                        onValueChange = { snapshotNotes = it },
                        label = { Text("Field Notes / Findings", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("snapshot_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSnapshotReport(snapshotTitle, snapshotNotes)
                        showSnapshotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber),
                    modifier = Modifier.testTag("save_snapshot_confirm")
                ) {
                    Text("Save to DB", color = DeepObsidian)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
