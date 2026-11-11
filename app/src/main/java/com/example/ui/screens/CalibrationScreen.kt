package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Brush
import com.example.dji.model.DjiAircraftModel
import com.example.model.ThermalPalette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Key
import com.example.ui.components.DjiTelemetryPanel
import com.example.model.TemperatureUnit
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
import com.example.viewmodel.ThermalViewModel

data class EmissivityPreset(val name: String, val value: Float, val description: String)

@Composable
fun CalibrationScreen(
    viewModel: ThermalViewModel,
    modifier: Modifier = Modifier
) {
    val tempUnit by viewModel.tempUnit.collectAsState()
    val droneTelemetry by viewModel.droneTelemetry.collectAsState()
    val deviceAttitude by viewModel.deviceAttitude.collectAsState()
    val deviceLocation by viewModel.deviceLocation.collectAsState()

    val djiTelemetry by viewModel.djiTelemetry.collectAsState()
    val djiConnectionState by viewModel.djiConnectionState.collectAsState()
    val djiConnectionMode by viewModel.djiConnectionMode.collectAsState()
    val djiFlightPattern by viewModel.djiFlightPattern.collectAsState()
    val selectedAircraft by viewModel.selectedAircraftModel.collectAsState()
    val palette by viewModel.palette.collectAsState()

    var emissivitySlider by remember { mutableFloatStateOf(viewModel.currentEmissivity) }
    var ambientSlider by remember { mutableFloatStateOf(viewModel.currentAmbient) }
    var rtspInput by remember { mutableStateOf(droneTelemetry.rtspUrl) }
    var calibrationOffsetSlider by remember { mutableFloatStateOf(0f) }
    var djiAppKeyInput by remember { mutableStateOf("DJI-MSDK-V5-ENTERPRISE-KEY-9821") }
    var registrationStatus by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        EmissivityPreset("Skin / Organic", 0.98f, "Search & Rescue, biological entities"),
        EmissivityPreset("Solar Cells", 0.92f, "Photovoltaic inspection, bypass diodes"),
        EmissivityPreset("Concrete / Asphalt", 0.94f, "Pavement, structural building survey"),
        EmissivityPreset("Oxidized Metal", 0.79f, "Electrical substation bushings, pipes"),
        EmissivityPreset("Water / Ice", 0.96f, "Maritime search, hydro survey"),
        EmissivityPreset("Polished Metal", 0.15f, "Reflective turbine blades, bare copper")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Calibration",
                    tint = ThermalColdCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SENSOR CALIBRATION & DRONE LINK",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Radiometric emissivity, unit standards, and RTSP stream",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            // Temperature Unit Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("temp_units_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TEMPERATURE UNIT STANDARD",
                        color = ThermalAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setTemperatureUnit(TemperatureUnit.CELSIUS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tempUnit == TemperatureUnit.CELSIUS) ThermalAmber else SurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("unit_celsius_button")
                        ) {
                            Text(
                                "Celsius (°C)",
                                color = if (tempUnit == TemperatureUnit.CELSIUS) DeepObsidian else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tempUnit == TemperatureUnit.FAHRENHEIT) ThermalAmber else SurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("unit_fahrenheit_button")
                        ) {
                            Text(
                                "Fahrenheit (°F)",
                                color = if (tempUnit == TemperatureUnit.FAHRENHEIT) DeepObsidian else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            // Infrared Color Mapping (Palettes) Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("ir_color_mapping_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = ThermalAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INFRARED COLOR MAPPING (PALETTE)",
                                color = ThermalAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ThermalAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = palette.title.uppercase(),
                                color = ThermalAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time radiometric color mapping transforms raw infrared sensor microbolometer values into human-interpretable temperature gradients:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tactical Palette List
                    val paletteList = listOf(
                        ThermalPalette.WHITE_HOT,
                        ThermalPalette.BLACK_HOT,
                        ThermalPalette.IRONBOW,
                        ThermalPalette.RAINBOW,
                        ThermalPalette.INFERNO,
                        ThermalPalette.ARCTIC,
                        ThermalPalette.LAVA
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        paletteList.forEach { p ->
                            val isSelected = palette == p
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SurfaceElevated else DeepObsidian)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ThermalAmber else SurfaceBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setPalette(p) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("calibration_palette_${p.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.title,
                                        color = if (isSelected) ThermalAmber else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = p.description,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Live gradient preview swatch
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 16.dp)
                                        .clip(RoundedCornerShape(3.dp))
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
                            }
                        }
                    }
                }
            }
        }

        item {
            // Radiometric Emissivity Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("emissivity_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SURFACE EMISSIVITY (ε)",
                            color = ThermalAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format("ε = %.2f", emissivitySlider),
                            color = TelemetryGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = emissivitySlider,
                        onValueChange = {
                            emissivitySlider = it
                            viewModel.setEmissivity(it)
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = ThermalAmber,
                            activeTrackColor = ThermalAmber,
                            inactiveTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("emissivity_slider")
                    )

                    Text(
                        text = "Quick Material Presets:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { preset ->
                            val isSelected = kotlin.math.abs(emissivitySlider - preset.value) < 0.02f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) SurfaceElevated else DeepObsidian)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ThermalAmber else SurfaceBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        emissivitySlider = preset.value
                                        viewModel.setEmissivity(preset.value)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) ThermalAmber else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = preset.description,
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    text = String.format("%.2f", preset.value),
                                    color = TelemetryGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Ambient Temperature & Offset Calibration
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AMBIENT REFERENCE & GAIN OFFSET",
                        color = ThermalAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ambient Temp:", color = TextSecondary, fontSize = 11.sp)
                        Text(tempUnit.format(ambientSlider), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = ambientSlider,
                        onValueChange = {
                            ambientSlider = it
                            viewModel.setAmbientTemp(it)
                        },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = ThermalColdCyan,
                            activeTrackColor = ThermalColdCyan,
                            inactiveTrackColor = SurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calibration Offset (ΔT):", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%+.1f°", calibrationOffsetSlider), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = calibrationOffsetSlider,
                        onValueChange = {
                            calibrationOffsetSlider = it
                            viewModel.setCalibrationOffset(it)
                        },
                        valueRange = -10f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = ThermalCoreYellow,
                            activeTrackColor = ThermalCoreYellow,
                            inactiveTrackColor = SurfaceElevated
                        )
                    )
                }
            }
        }

        item {
            // Aircraft Hardware Profile Selector (DJI Matrice 4TD)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ThermalAmber.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("aircraft_hardware_profile_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AIRCRAFT HARDWARE PROFILE",
                                color = ThermalAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TelemetryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedAircraft.shortName,
                                color = TelemetryGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DjiAircraftModel.values().forEach { model ->
                            val isSelected = selectedAircraft == model
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAircraftModel(model) },
                                label = {
                                    Text(
                                        text = model.shortName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThermalAmber,
                                    selectedLabelColor = DeepObsidian,
                                    containerColor = SurfaceElevated,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("select_model_${model.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Aircraft Specs Panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAircraft.modelName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            if (selectedAircraft == DjiAircraftModel.MATRICE_4TD) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ThermalAmber)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRIMARY AIRCRAFT",
                                        color = DeepObsidian,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = selectedAircraft.series,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "THERMAL SENSOR PAYLOAD:",
                            color = ThermalAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedAircraft.thermalSpecs,
                            color = TextPrimary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "OPTICAL & TELEPHOTO PAYLOAD:",
                            color = ThermalColdCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = selectedAircraft.opticalSpecs,
                            color = TextPrimary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AIRLINK PROTOCOL:",
                                    color = TelemetryGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = selectedAircraft.transmission,
                                    color = TextPrimary,
                                    fontSize = 10.sp
                                )
                            }
                            if (selectedAircraft.isDockCompatible) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TelemetryGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "DOCK 2 READY",
                                        color = TelemetryGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // DJI Mobile SDK Live Telemetry Section (GPS, Altitude, Heading, Battery, Flight Mode)
            DjiTelemetryPanel(
                telemetry = djiTelemetry,
                connectionState = djiConnectionState,
                connectionMode = djiConnectionMode,
                activePattern = djiFlightPattern,
                onModeSelected = { viewModel.setDjiConnectionMode(it) },
                onPatternSelected = { viewModel.setDjiFlightPattern(it) },
                onTriggerRth = { viewModel.triggerDjiReturnToHome() }
            )
        }

        item {
            // DJI Mobile SDK Registration & Key Provisioning Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("dji_sdk_reg_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ThermalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DJI MSDK REGISTRATION & APP KEY",
                            color = ThermalAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = djiAppKeyInput,
                        onValueChange = { djiAppKeyInput = it },
                        label = { Text("DJI Developer App Key", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("dji_app_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.initializeDjiSdk(djiAppKeyInput) { success, msg ->
                                registrationStatus = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("init_dji_sdk_button")
                    ) {
                        Text(
                            text = "REGISTER DJI MOBILE SDK V5",
                            color = DeepObsidian,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    registrationStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            color = TelemetryGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            // DJI Drone Stream URL Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("drone_stream_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = null,
                            tint = TelemetryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DJI DRONE RTSP / O3 PAYLOAD LINK",
                            color = ThermalAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rtspInput,
                        onValueChange = {
                            rtspInput = it
                            viewModel.updateDroneRtspUrl(it)
                        },
                        label = { Text("Thermal RTSP/UDP Stream URL", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("rtsp_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Configured Model: ${droneTelemetry.modelName}",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Bitrate: ${droneTelemetry.streamBitrateMbps} Mbps | 30 FPS Low Latency",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            // Hardware Telemetry Diagnostics Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = ThermalColdCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HARDWARE TELEMETRY SENSORS",
                            color = ThermalAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gyro Pitch Attitude:", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%.2f°", deviceAttitude.pitch), color = TelemetryGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gyro Roll Attitude:", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%.2f°", deviceAttitude.roll), color = TelemetryGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Coordinates:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format("%.4f", deviceLocation.latitude)}° N, ${String.format("%.4f", deviceLocation.longitude)}° W",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Altitude (MSL):", color = TextSecondary, fontSize = 11.sp)
                        Text(String.format("%.1f m", deviceLocation.altitudeMeters), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            // Install Application on Android Phone & DJI Remote Controller Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TelemetryGreen.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("install_app_android_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = TelemetryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTALL APP ON ANDROID PHONE",
                                color = TelemetryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TelemetryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "APK READY",
                                color = TelemetryGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Follow these simple steps to install Thermal Recon on your Android smartphone, tablet, or DJI Smart Controller (RC Plus / RC Pro):",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val installSteps = listOf(
                        "1. Download APK from AI Studio" to "Click the settings / export menu in the top-right corner of the AI Studio window and select 'Download APK' or 'Export APK' to save the compiled APK file (app-debug.apk).",
                        "2. Install on Android Phone" to "Open your phone's 'Files' or 'Downloads' app, tap 'app-debug.apk', and select 'Install'. If prompted with 'Install unknown apps', toggle Allow from this source.",
                        "3. Install on DJI RC Controller" to "For DJI RC Plus, RC Pro Enterprise, or RC 2, transfer the APK via USB-C cable or MicroSD card. Open the RC's built-in Android Files app and tap the APK to install.",
                        "4. Bind to DJI Matrice 4TD" to "Connect your Android device to the DJI Remote Controller via USB-C cable or Wi-Fi OSDK. Launch Thermal Recon and telemetry will stream automatically at 10Hz."
                    )

                    installSteps.forEach { (stepTitle, stepDesc) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TelemetryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stepTitle,
                                    color = ThermalAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stepDesc,
                                color = TextPrimary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Package Diagnostics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Application ID:", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("com.aistudio.thermalrecon.kztvpx", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Android Version:", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Android 10+ (API 29 - 36)", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
