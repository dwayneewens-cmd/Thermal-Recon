package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.model.AiMessageSender
import com.example.ai.model.AiMissionReport
import com.example.ai.model.AiUiState
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
import com.example.viewmodel.ThermalAiViewModel
import com.example.viewmodel.ThermalViewModel

@Composable
fun AiCopilotScreen(
    aiViewModel: ThermalAiViewModel,
    thermalViewModel: ThermalViewModel,
    onNavigateToHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by aiViewModel.uiState.collectAsState()
    val messages by aiViewModel.messages.collectAsState()
    val djiTelemetry by thermalViewModel.djiTelemetry.collectAsState()
    val currentFrame by thermalViewModel.currentFrame.collectAsState()
    val tempUnit by thermalViewModel.tempUnit.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    var inputPrompt by remember { mutableStateOf("") }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, uiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .padding(top = 8.dp)
            .testTag("ai_copilot_screen")
    ) {

        // =========================================================================
        // 1. TOP HEADER: Title, Live Status, Model Badge & Clear Button
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ThermalAmber, ThermalCoreYellow)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Copilot",
                        tint = DeepObsidian,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "THERMAL AI COPILOT",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gemini 3.8 Flash // Firebase AI Logic",
                        color = ThermalAmber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = { aiViewModel.clearHistory() },
                modifier = Modifier.testTag("clear_ai_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear History",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // =========================================================================
        // 2. LIVE TELEMETRY CONTEXT STRIP (Matrice 4TD, GPS, Peak Temp, Gimbal)
        // =========================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable { onNavigateToHud() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(TelemetryGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${djiTelemetry.modelName} (OK)",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "GPS: ${djiTelemetry.gps.formatCoordinates()}",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PEAK: ${tempUnit.format(currentFrame.maxTemp)}",
                        color = ThermalHotRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ALT ${String.format("%.1fm", djiTelemetry.gps.altitudeAglMeters)} | GIMBAL ${String.format("%.0f°", djiTelemetry.gimbal.pitchDegrees)}",
                        color = ThermalAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // =========================================================================
        // 3. TACTICAL PRESET QUERY CHIPS
        // =========================================================================
        val presetQueries = listOf(
            "⚡ Diagnose hotspot severity & hazard" to "Diagnose the current thermal hotspot, estimate core temperature, and assess equipment failure risk.",
            "🗺️ GPS surroundings & map context" to "Analyze the drone's current GPS location and provide situational intelligence regarding nearby terrain, utility grid lines, and emergency access.",
            "🎨 Recommend optimal IR color map" to "Recommend the best thermal palette (White Hot, Black Hot, or Ironbow) to distinguish this heat signature from background terrain.",
            "📋 Generate mission inspection report" to "__GENERATE_REPORT__",
            "🔍 SAR survivor body heat analysis" to "Verify if the detected thermal signature at current GPS coordinates matches a human survivor profile."
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetQueries.forEach { (label, prompt) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        if (prompt == "__GENERATE_REPORT__") {
                            aiViewModel.generateStructuredMissionReport(
                                telemetry = djiTelemetry,
                                frameData = currentFrame
                            )
                        } else {
                            aiViewModel.sendCopilotQuery(
                                prompt = prompt,
                                telemetry = djiTelemetry,
                                frameData = currentFrame
                            )
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = ThermalAmber.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("preset_query_chip")
                )
            }
        }

        // =========================================================================
        // 4. CONVERSATION & REPORT LIST
        // =========================================================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isPilot = msg.sender == AiMessageSender.PILOT

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isPilot) Alignment.End else Alignment.Start
                ) {
                    // Sender Label & Telemetry tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = if (isPilot) "PILOT" else "GEMINI COPILOT",
                            color = if (isPilot) ThermalAmber else ThermalColdCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (msg.telemetrySnapshot != null) {
                            Text(
                                text = " [${msg.telemetrySnapshot}]",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Message Bubble
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPilot) SurfaceElevated else SurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPilot) ThermalAmber.copy(alpha = 0.5f) else SurfaceBorder
                        ),
                        shape = RoundedCornerShape(
                            topStart = 10.dp,
                            topEnd = 10.dp,
                            bottomStart = if (isPilot) 10.dp else 2.dp,
                            bottomEnd = if (isPilot) 2.dp else 10.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth(if (isPilot) 0.85f else 1.0f)
                            .testTag("chat_bubble_${msg.sender.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = if (!isPilot) FontFamily.Monospace else FontFamily.Default
                            )

                            // Quick Copy Action for responses
                            if (!isPilot) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(msg.text))
                                            }
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("COPY", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Streaming or Loading feedback item
            if (uiState is AiUiState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ThermalAmber,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini 3.8 Flash analyzing radiometric stream & location telemetry...",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Error notice
            if (uiState is AiUiState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF2A1B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ThermalHotRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_error_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = ThermalHotRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (uiState as AiUiState.Error).errorMessage,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. BOTTOM INPUT BAR
        // =========================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            "Ask Gemini thermal copilot...",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThermalAmber,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ThermalAmber
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val prompt = inputPrompt.trim()
                        if (prompt.isNotEmpty()) {
                            aiViewModel.sendCopilotQuery(
                                prompt = prompt,
                                telemetry = djiTelemetry,
                                frameData = currentFrame
                            )
                            inputPrompt = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThermalAmber),
                    shape = RoundedCornerShape(10.dp),
                    enabled = inputPrompt.isNotBlank() && uiState !is AiUiState.Loading,
                    modifier = Modifier.testTag("ai_send_query_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send to Gemini",
                        tint = DeepObsidian,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
