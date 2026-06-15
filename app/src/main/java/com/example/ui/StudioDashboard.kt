package com.example.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.data.database.AudioProjectEntity
import com.example.data.database.PresetEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- THEME COLOR SPECIFICATION ---
val SpaceObsidian = Color(0xFF04060B)
val GlassCardBg = Color(0x1F1A2235)
val GlassBorder = Color(0x336C63FF)
val NeonCyan = Color(0xFF00E5FF)
val ElectricIndigo = Color(0xFF7B2CBF)
val HotViolet = Color(0xFFE040FB)
val MintGreen = Color(0xFF00E676)
val GoldenAmber = Color(0xFFFFB300)

@Composable
fun StudioDashboard(viewModel: SoundLabViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SpaceObsidian, Color(0xFF0C0E17), Color(0xFF131521))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Workspace Stats & Controls Header
            StudioHeader(viewModel)

            // 2. Real-Time Wave & FFT Analyzer Desk
            LiveVisualizerRack(viewModel)

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Main Workspace Division (Tablet 3-Pane vs Compact 1-Column)
            if (isTablet) {
                WidescreenDesktopWorkspace(viewModel)
            } else {
                CompactWorkspaceDeck(viewModel)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Anchor Interactive Piano Keys Synthesizer
            SynthesizerKeyboardController(viewModel)
        }

        // Floating toast message indicator for AI operations
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            when (uiState) {
                is SoundLabUiState.Loading -> {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("ai_loading_spinner")
                    )
                }
                is SoundLabUiState.Success -> {
                    val alertMessage = (uiState as SoundLabUiState.Success).message
                    FloatingAlertBubble(message = alertMessage, color = MintGreen)
                }
                is SoundLabUiState.Error -> {
                    val errMsg = (uiState as SoundLabUiState.Error).throwable.message ?: "Unknown Error"
                    FloatingAlertBubble(message = errMsg, color = Color.Red)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun StudioHeader(viewModel: SoundLabViewModel) {
    val isSynthPlaying by viewModel.isSynthPlaying.collectAsState()
    val activeProject by viewModel.selectedProject.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(GlassCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "BRO SOUND LAB AI",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = activeProject?.title ?: "DASHBOARD SESSION ENGINE",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Live stats pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSynthPlaying) MintGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSynthPlaying) "LIVE OUTPUT" else "STANDBY",
                        color = if (isSynthPlaying) MintGreen else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "21.0 kHz Stereo • 32b FP",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Power button with dynamic glow
            IconButton(
                onClick = { viewModel.toggleSynthesizer() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSynthPlaying) HotViolet.copy(alpha = 0.25f) else Color.White.copy(0.08f))
                    .border(1.dp, if (isSynthPlaying) HotViolet else Color.Transparent, RoundedCornerShape(50))
                    .testTag("power_button")
            ) {
                Text(
                    text = if (isSynthPlaying) "STOP" else "PLAY",
                    color = if (isSynthPlaying) HotViolet else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LiveVisualizerRack(viewModel: SoundLabViewModel) {
    val spectrum by viewModel.audioEngine.spectrumFlow.collectAsState()
    val vuLeft by viewModel.audioEngine.vuLeftFlow.collectAsState()
    val vuRight by viewModel.audioEngine.vuRightFlow.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(96.dp)
            .background(Color(0xFF0F111D), RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. FFT Dynamic Spectrum Bars
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("fft_spectrum_canvas")
        ) {
            val width = size.width
            val height = size.height
            val spacing = 6f
            val totalBars = 10
            val barWidth = (width - (spacing * (totalBars - 1))) / totalBars

            for (i in 0 until totalBars) {
                val magnitude = spectrum.getOrElse(i) { 0.1f }.coerceIn(0.05f, 1.0f)
                val barHeight = height * magnitude
                val x = i * (barWidth + spacing)
                val y = height - barHeight

                val gradient = Brush.verticalGradient(
                    colors = listOf(NeonCyan, ElectricIndigo),
                    startY = y,
                    endY = height
                )

                // Render customized top peak segment + bar trunk
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        // B. Dual Channel stereo VU level meter meters
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Channel Indicator Label
            VuGaugesSegment("L", vuLeft)
            // Right Channel Indicator Label
            VuGaugesSegment("R", vuRight)
        }
    }
}

@Composable
fun VuGaugesSegment(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(10.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        ) {
            val width = size.width
            val height = size.height
            val fillWidth = (width * value).coerceIn(0f, width)

            // Background gauge track
            drawRoundRect(
                color = Color.White.copy(0.07f),
                size = size,
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Active glow segment
            if (fillWidth > 0) {
                val color = when {
                    value > 0.85f -> Color.Red
                    value > 0.6f -> GoldenAmber
                    else -> MintGreen
                }
                drawRoundRect(
                    color = color,
                    size = Size(fillWidth, height),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
fun CompactWorkspaceDeck(viewModel: SoundLabViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("MASTERING", "GRAPHIC EQ", "AI CHAT", "PROJECTS")

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = NeonCyan,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> DspMasterRack(viewModel)
                1 -> TactileEqRack(viewModel)
                2 -> AiStudioAssistantPanel(viewModel)
                3 -> SessionArchivePanel(viewModel)
            }
        }
    }
}

@Composable
fun WidescreenDesktopWorkspace(viewModel: SoundLabViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(290.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            TactileEqRack(viewModel)
        }
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            DspMasterRack(viewModel)
        }
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1.4f)) {
                    AiStudioAssistantPanel(viewModel)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SessionArchivePanel(viewModel)
                }
            }
        }
    }
}

@Composable
fun TactileEqRack(viewModel: SoundLabViewModel) {
    val bands by viewModel.eqBandsState.collectAsState()
    val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-BAND TACTILE EQUALIZER",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "RESET ALL",
                color = HotViolet,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        for (i in 0 until 10) {
                            viewModel.setEqBandLevel(i, 0.0f)
                        }
                    }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bands.forEachIndexed { index, gain ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    Text(
                        text = String.format("%s%.1f", if (gain > 0) "+" else "", gain),
                        color = if (gain != 0f) NeonCyan else Color.White.copy(0.4f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("eq_value_$index")
                    )

                    Slider(
                        value = gain,
                        onValueChange = { viewModel.setEqBandLevel(index, it) },
                        valueRange = -12.0f..12.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(0.08f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_slider_$index")
                    )

                    Text(
                        text = bandLabels[index],
                        color = Color.White.copy(0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DspMasterRack(viewModel: SoundLabViewModel) {
    val drive by viewModel.driveState.collectAsState()
    val delayFeedback by viewModel.delayFeedbackState.collectAsState()
    val reverbDecay by viewModel.reverbDecayState.collectAsState()
    val wideness by viewModel.widenessState.collectAsState()
    val compressThreshold by viewModel.compressThresholdState.collectAsState()
    val vocalsGain by viewModel.vocalsGainState.collectAsState()
    val beatsGain by viewModel.beatsGainState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI ANALOG SIGNAL RACK",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rack sliders
        RackKnobController("SATURATION OVERDRIVE", drive, 0f..1.5f, String.format("%.2fx", drive + 1.0f), "drive_slider") {
            viewModel.setDriveLevel(it)
        }
        RackKnobController("ATMOSPHERIC REVERB DECAY", reverbDecay, 0.0f..0.85f, String.format("%d%%", (reverbDecay * 100).toInt()), "reverb_slider") {
            viewModel.setReverbDecayLevel(it)
        }
        RackKnobController("STEREO ECHO DELAY FEEDBACK", delayFeedback, 0.0f..0.7f, String.format("%d%%", (delayFeedback * 100).toInt()), "delay_slider") {
            viewModel.setDelayFeedbackLevel(it)
        }
        RackKnobController("DYNAMIC LIMIT ARCHITECTURE", compressThreshold, -40f..0f, String.format("%.1fdB", compressThreshold), "compressor_slider") {
            viewModel.setCompressorThreshold(it)
        }
        RackKnobController("STEREO EXPANSION WIDENESS", wideness, 0.1f..1.0f, String.format("%.0f%%", wideness * 100), "wideness_slider") {
            viewModel.setWidenessLevel(it)
        }

        // Mixer faders
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = Color.White.copy(0.08f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("VOCAL GAIN", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1fx", vocalsGain), color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = vocalsGain,
                    onValueChange = { viewModel.setVocalsVolume(it) },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("BEATS GAIN", color = HotViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1fx", beatsGain), color = HotViolet, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = beatsGain,
                    onValueChange = { viewModel.setBeatsVolume(it) },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = HotViolet, activeTrackColor = HotViolet)
                )
            }
        }
    }
}

@Composable
fun RackKnobController(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueString: String,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueString,
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.White.copy(0.08f)
            ),
            modifier = Modifier
                .height(28.dp)
                .testTag(tag)
        )
    }
}

@Composable
fun AiStudioAssistantPanel(viewModel: SoundLabViewModel) {
    var aiFeatureTab by remember { mutableStateOf(0) } // 0: Mastering prompt, 1: Lyric builder prompt
    var masteringPromptText by remember { mutableStateOf("") }
    var lyricsPromptText by remember { mutableStateOf("") }
    val activeProject by viewModel.selectedProject.collectAsState()
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    val promptSuggestions = listOf(
        "Crisp Hi-fidelity vocal podcaster",
        "Deep club synth bass enhancer",
        "Lofi atmospheric acoustic chamber",
        "Clear bright vocal with heavy limiter"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GEMINI AI COCREATOR",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { aiFeatureTab = if (aiFeatureTab == 0) 1 else 0 },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = if (aiFeatureTab == 0) "GO LYRICIST" else "GO MASTERING",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (aiFeatureTab == 0) {
            // A. AI MASTERING ASSISTANT
            Text(
                text = "Describe your audio characteristics. Gemini will instantly synthesize custom filter coefficients, thresholds and effects delay lines:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = masteringPromptText,
                onValueChange = { masteringPromptText = it },
                placeholder = { Text("e.g., Warm female podcaster vocal...", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (masteringPromptText.isNotBlank()) {
                        viewModel.runAiMasteringAssistant(masteringPromptText)
                        softwareKeyboardController?.hide()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("ai_mastering_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Suggestions rows
            Text("SUGGESTIONS:", color = Color.White.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                promptSuggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(0.06f), RoundedCornerShape(50))
                            .clickable { masteringPromptText = suggestion }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(suggestion, color = Color.White, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (masteringPromptText.isNotBlank()) {
                        viewModel.runAiMasteringAssistant(masteringPromptText)
                        softwareKeyboardController?.hide()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("ai_mastering_submit")
            ) {
                Text("RUN AUDIO MASTER ENGINE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

        } else {
            // B. AI LYRICS BUILDER
            Text(
                text = "Enter song theme or lyrics prompts. Gemini will build complete lyrics maps, chord structures, recommended BPM, and title coordinates:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = lyricsPromptText,
                onValueChange = { lyricsPromptText = it },
                placeholder = { Text("e.g., Cyberpunk flight through neon clouds...", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HotViolet,
                    unfocusedBorderColor = GlassBorder
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (lyricsPromptText.isNotBlank()) {
                        viewModel.runAiLyricsGenerator(lyricsPromptText)
                        softwareKeyboardController?.hide()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("ai_lyrics_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (lyricsPromptText.isNotBlank()) {
                        viewModel.runAiLyricsGenerator(lyricsPromptText)
                        softwareKeyboardController?.hide()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("ai_lyrics_submit")
            ) {
                Text("GENERATE SONG MATRIX", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // Display Active lyric maps if active
            activeProject?.lyrics?.let { l ->
                if (l.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE VOCALS SHEET (Key: ${activeProject?.keySignature}, BPM: ${activeProject?.bpm})",
                            color = GoldenAmber,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                Text(
                                    text = l,
                                    color = Color.White.copy(0.85f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionArchivePanel(viewModel: SoundLabViewModel) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProject by viewModel.selectedProject.collectAsState()
    var newProjectTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AUDIO SESSIONS ARCHIVE",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = newProjectTitle,
                    onValueChange = { newProjectTitle = it },
                    placeholder = { Text("Project title", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder.copy(alpha = 0.4f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 9.sp),
                    modifier = Modifier
                        .width(100.dp)
                        .height(30.dp)
                        .testTag("new_project_title_input")
                )
                Button(
                    onClick = {
                        if (newProjectTitle.isNotBlank()) {
                            viewModel.createNewProject(newProjectTitle)
                            newProjectTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("ADD", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded sessions. Create a project above or load a lyric script to get started.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(projects) { project ->
                    val isSelected = activeProject?.id == project.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) ElectricIndigo.copy(alpha = 0.3f) else Color.White.copy(0.04f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) ElectricIndigo else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectProject(project) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "BPM: ${project.bpm} | Key: ${project.keySignature}",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteProject(project.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("❌", fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SynthesizerKeyboardController(viewModel: SoundLabViewModel) {
    var keyActiveMap by remember { mutableStateOf(mapOf<String, Boolean>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F111C), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .border(1.dp, GlassBorder.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE INTERACTIVE PIANO SYNTH MODULE",
                color = GoldenAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "MIDI OSC: QUAD VCO SINE WAVE",
                color = Color.White.copy(0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("synth_keyboard_row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viewModel.keyFrequencies.forEach { (noteName, frequency) ->
                val isActive = keyActiveMap[noteName] == true
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(if (isActive) GoldenAmber else Color.White)
                        .border(1.dp, SpaceObsidian, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .pointerInput(noteName) {
                            detectTapGestures(
                                onPress = {
                                    keyActiveMap = keyActiveMap.toMutableMap().apply { put(noteName, true) }
                                    viewModel.playNoteOnPress(frequency)
                                    tryAwaitRelease()
                                    keyActiveMap = keyActiveMap.toMutableMap().apply { put(noteName, false) }
                                    viewModel.stopNoteOnRelease()
                                }
                            )
                        }
                        .testTag("key_$noteName"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = noteName,
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingAlertBubble(message: String, color: Color) {
    Box(
        modifier = Modifier
            .background(SpaceObsidian.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Text(
                text = message,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
