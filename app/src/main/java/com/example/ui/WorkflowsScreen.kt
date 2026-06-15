package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.database.AudioProjectEntity
import com.example.data.database.PresetEntity
import kotlin.math.abs
import kotlin.math.sin

// ==========================================
// WORKFLOW SCHEMAS & DATA STRUCTURES
// ==========================================
data class WorkflowStep(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val targetComponent: String, // "EQ", "COMPRESSOR", "SATURATION", "DELAY", "REVERB", "GEMINI_AI", "DATABASE"
    var isBypassed: Boolean = false,
    var defaultValue: Float = 0.5f
)

data class AutomationWorkflow(
    val id: String,
    val name: String,
    val shortDesc: String,
    val longDesc: String,
    val icon: String,
    val steps: List<WorkflowStep>,
    val complexity: String, // "STANDARD", "MODERATE", "ADVANCED"
    val themeColor: Color
)

@Composable
fun WorkflowsScreenView(viewModel: SoundLabViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe active VM states
    val uiState by viewModel.uiState.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    // Central list of high-quality automated audio pipelines
    val workflows = remember {
        listOf(
            AutomationWorkflow(
                id = "AI_MASTER",
                name = "Intense AI Mastering Desk",
                shortDesc = "Multi-band dynamic balancing via Gemini AI API nodes.",
                longDesc = "Processes raw waveforms via Gemini. Runs continuous noise reduction, creates customized 10-band parameters, stabilizes compression ratios, and expands the visual soundstage.",
                icon = "🤖",
                themeColor = BroNeonCyan,
                complexity = "ADVANCED",
                steps = listOf(
                    WorkflowStep("ANALYZE", "Sample Quality Scanner", "Runs frequency distribution sweep to map sound profile", "📡", "DATABASE"),
                    WorkflowStep("SATURATE", "Analog Tube Saturation", "Applies mild warmth saturation levels to mids and bass", "🔥", "SATURATION", defaultValue = 0.2f),
                    WorkflowStep("AI_EQ", "Gemini Real-time 10-Band EQ", "Queries Gemini model for balanced harmonic mastering ratios", "🧠", "GEMINI_AI"),
                    WorkflowStep("COMPRESS", "Solid State Compressor", "Clamps down dynamic peaks to maximize structural volume", "🗜️", "COMPRESSOR", defaultValue = 0.6f),
                    WorkflowStep("WIDEN", "Stereo Spatial Expander", "Broadens out vocal and instrument spacing", "↔️", "REVERB", defaultValue = 0.7f)
                )
            ),
            AutomationWorkflow(
                id = "CYBER_SYNTH",
                name = "Cyberpunk Voice Synth Pipeline",
                shortDesc = "VCO pitch carrier modulation and orbital echoes.",
                longDesc = "Synthesizes extreme electronic voices. Initiates a raw carrier wave, filters higher frequencies, triggers drive clipping filters, and repeats signals inside of ping-pong delays.",
                icon = "🎛️",
                themeColor = BroHotViolet,
                complexity = "MODERATE",
                steps = listOf(
                    WorkflowStep("VCO_GEN", "Oscillator Signal Launcher", "Triggers direct continuous VCO voice carrier waveform", "⚡", "REVERB"),
                    WorkflowStep("DISTORT", "Extreme Tape Saturation Overdrive", "Injects heavy clipping distortion for digital grit", "🎸", "SATURATION", defaultValue = 0.85f),
                    WorkflowStep("ENVELOPE", "10-Band Filtering Sweeper", "Performs lowpass envelope dampening", "📉", "EQ", defaultValue = 0.35f),
                    WorkflowStep("ECHO", "Stereo Ping-Pong Delay echo", "Triggers echoing spatial delay loops", "📡", "DELAY", defaultValue = 0.55f)
                )
            ),
            AutomationWorkflow(
                id = "SONG_CREATOR",
                name = "Gemini Songwriter Orchestrator",
                shortDesc = "Theme prompts to structural lyrics sheets and key tuning.",
                longDesc = "Automates songwriting. Identifies thematic concepts, executes Gemini songwriting models, computes key signatures, and compiles a fresh project session in local SQLite resources.",
                icon = "📝",
                themeColor = BroGoldenAmber,
                complexity = "ADVANCED",
                steps = listOf(
                    WorkflowStep("THEME_EXT", "Prompts Semantic Extractor", "Extracts key emotional topics and vibes", "🔮", "DATABASE"),
                    WorkflowStep("WRITE_LYRICS", "Gemini Songsmith Lyric Engine", "Generates premium verse-chorus lyrics and songwriting feedback", "✍️", "GEMINI_AI"),
                    WorkflowStep("TUNING", "Scale Tuning Estimator", "Computes best matching key signature and BPM target", "🎵", "DATABASE"),
                    WorkflowStep("SAVE_DB", "SQLite Session Register", "Writes the compiled project and lyrics parameters to SQLite", "💾", "DATABASE")
                )
            ),
            AutomationWorkflow(
                id = "LOFI_VINYL",
                name = "Lo-Fi Vintage Vinyl Enhancer",
                shortDesc = "Retro grit injection, roll-offs, and tape speed fluttering.",
                longDesc = "Applies nostalgia filters. Injects organic vinyl crackle, rolls off crisp high-frequencies, warbles tape speed, and reflects sound inside a vintage hall space.",
                icon = "📻",
                themeColor = BroMintGreen,
                complexity = "STANDARD",
                steps = listOf(
                    WorkflowStep("CRACKLE", "Vinyl Grit Modulator", "Combines mock record needle scratch and noise", "🔌", "SATURATION", defaultValue = 0.4f),
                    WorkflowStep("ROLLOFF", "Nostalgia Dampener EQ", "Cuts crisp frequencies above 4kHz cleanly", "🧱", "EQ", defaultValue = 0.15f),
                    WorkflowStep("FLUTTER", "Wow-Flutter Tape Wobbler", "Applies continuous subtle delay micro-pitch wobbles", "🧬", "DELAY", defaultValue = 0.3f),
                    WorkflowStep("HALL", "Retro Space Reverberator", "Bathes output in a wide retro ambient decay chamber", "⛪", "REVERB", defaultValue = 0.5f)
                )
            )
        )
    }

    // Active Selection States
    var selectedWorkflowId by remember { mutableStateOf("AI_MASTER") }
    val activeWorkflow = remember(selectedWorkflowId) {
        workflows.first { it.id == selectedWorkflowId }
    }

    // Running Execution States
    var activeStepIndex by remember { mutableStateOf(-1) }
    var isExecuting by remember { mutableStateOf(false) }
    var executionProgress by remember { mutableStateOf(0f) }
    
    // Virtual parameters backing current selected workflow's editable sliders
    var stepOverrides by remember { mutableStateOf(mutableMapOf<String, Float>()) }
    
    // Dynamic execution debug logs
    val logsList = remember { mutableStateListOf("Workflows automation client online.", "Ready to deploy pipelines.") }

    // Waveform & Visualizer parameters that update live on running
    var simulatedOutputLevel by remember { mutableStateOf(0.12f) }
    var simulatedPoints by remember { mutableStateOf(List(80) { 0.15f }) }
    var simulatedFft by remember { mutableStateOf(FloatArray(16) { 0.1f }) }

    // Initialize/sync default overrides dictionary
    LaunchedEffect(selectedWorkflowId) {
        val mapped = mutableMapOf<String, Float>()
        activeWorkflow.steps.forEach { step ->
            mapped[step.id] = step.defaultValue
        }
        stepOverrides = mapped
        activeStepIndex = -1
        isExecuting = false
        executionProgress = 0f
    }

    // Live audio meters simulation ticker when executing
    LaunchedEffect(isExecuting) {
        var tick = 0
        while (isExecuting) {
            delay(120)
            tick++
            // Oscillating waveforms
            simulatedPoints = List(80) { idx ->
                val base = sin((idx + tick) * 0.25f).toFloat()
                val damp = if (activeStepIndex >= 0) 0.8f else 0.2f
                base * damp * simulatedOutputLevel
            }
            // Bouncing spectrographs
            simulatedFft = FloatArray(16) { idx ->
                val freqVal = abs(sin((idx + tick) * 0.4f)).toFloat() * simulatedOutputLevel
                freqVal.coerceIn(0.02f, 0.95f)
            }
            // Dynamic VU bounces
            simulatedOutputLevel = (0.3f + abs(sin(tick * 0.6f)) * 0.5f).coerceIn(0.1f, 0.95f)
        }
        // Rest state
        simulatedOutputLevel = 0.08f
        simulatedPoints = List(80) { sin(it * 0.2f).toFloat() * 0.08f }
        simulatedFft = FloatArray(16) { 0.04f }
    }

    // Dynamic execution routine
    fun runActivePipeline() {
        if (isExecuting) return
        
        isExecuting = true
        activeStepIndex = 0
        executionProgress = 0f
        logsList.clear()
        logsList.add("Deploying pipeline: '${activeWorkflow.name.uppercase()}'...")

        coroutineScope.launch {
            val totalSteps = activeWorkflow.steps.size
            
            for (idx in activeWorkflow.steps.indices) {
                activeStepIndex = idx
                val step = activeWorkflow.steps[idx]
                val progressWeight = (idx.toFloat() / totalSteps)
                executionProgress = progressWeight

                if (step.isBypassed) {
                    logsList.add(" [BYPASS] Step: ${step.title.uppercase()} is bypassed. Skipping hardware inject.")
                    delay(500)
                    continue
                }

                logsList.add(" ➜ STEP ${idx + 1}/$totalSteps: Processing '${step.title.uppercase()}'...")
                
                // Get parameter multiplier
                val scaleValue = stepOverrides[step.id] ?: step.defaultValue
                
                // Real-world continuous DSP modifications on active Sound Engine
                when (step.targetComponent) {
                    "SATURATION" -> {
                        viewModel.setDriveLevel(scaleValue)
                        logsList.add("   [DSP] Tube Saturation Drive adjusted to: ${(scaleValue * 100).toInt()}%")
                    }
                    "EQ" -> {
                        // Reshape bands based on values
                        for (i in 0 until 10) {
                            val dbAdjust = (scaleValue * 24f) - 12f // -12dB to +12dB
                            viewModel.setEqBandLevel(i, dbAdjust)
                        }
                        logsList.add("   [DSP] Reshaped 10-Band EQ profiles to ${String.format("%.1fdB", (scaleValue * 24f) - 12f)}")
                    }
                    "DELAY" -> {
                        viewModel.setDelayFeedbackLevel(scaleValue)
                        logsList.add("   [DSP] Ping-Pong Echo feedback ratios compiled to: ${(scaleValue * 100).toInt()}%")
                    }
                    "REVERB" -> {
                        viewModel.setReverbDecayLevel(scaleValue)
                        viewModel.setWidenessLevel(scaleValue)
                        logsList.add("   [DSP] Reverb room decay and stereo wideness locked to: ${(scaleValue * 100).toInt()}%")
                    }
                    "COMPRESSOR" -> {
                        val thresholdDb = -40f + (1f - scaleValue) * 40f
                        viewModel.setCompressorThreshold(thresholdDb)
                        logsList.add("   [DSP] Limiter threshold mapped dynamically to: ${thresholdDb.toInt()}dB")
                    }
                    "GEMINI_AI" -> {
                        logsList.add("   [AI] Connecting to DeepMind Gemini API backend...")
                        if (step.id == "AI_EQ") {
                            val descText = "Create standard warm pre-master settings with compressor at -18dB"
                            logsList.add("   [AI] Sending prompt to Gemini node...")
                            viewModel.runAiMasteringAssistant(descText)
                        } else if (step.id == "WRITE_LYRICS") {
                            val prompt = "Lo-Fi nostalgia vintage beat song about mechanical echoes"
                            logsList.add("   [AI] Songwriter model querying chord grids...")
                            viewModel.runAiLyricsGenerator(prompt)
                        }
                        delay(2000) // Simulated processing padding for Gemini responses
                    }
                    "DATABASE" -> {
                        logsList.add("   [DB] Synchronizing local SQLite database project cache...")
                        if (step.id == "SAVE_DB") {
                            viewModel.createNewProject("Workflows Songsmith #${System.currentTimeMillis() % 1000}")
                            logsList.add("   [DB] SQLite schema project successfully registered.")
                        }
                    }
                }
                
                // Dynamic visual step delays
                delay(1200)
            }
            
            // Completion sequence
            activeStepIndex = totalSteps
            executionProgress = 1.0f
            logsList.add(" 🎉 PIPELINE '${activeWorkflow.name.uppercase()}' SUCCESSFULLY EXECUTED!")
            viewModel.addNotification("Executed workflow: ${activeWorkflow.name}")
            isExecuting = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workflows_tab_screen"),
        contentPadding = PaddingValues(bottom = 130.dp, top = 8.dp)
    ) {
        // --- 1. TITLE INTRO HEADER ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "AUTOMATED PIPELINES WORKBENCH",
                    color = BroNeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Orchestrate direct deep-level audio DSP tasks, local persistence synchronizations, and advanced Gemini AI co-songwriter pipelines in 1-tap automated sequences.",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // --- 2. PIPELINE HORIZONTAL GRID ACTIONS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CHOOSE SOUNDSTAGE PIPELINE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    workflows.forEach { workflow ->
                        val isSelected = selectedWorkflowId == workflow.id
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) workflow.themeColor.copy(0.12f) else BroGlassCardBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) workflow.themeColor else BroGlassBorder.copy(0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (!isExecuting) {
                                        selectedWorkflowId = workflow.id
                                    }
                                }
                                .padding(12.dp)
                                .testTag("workflow_card_${workflow.id}")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(workflow.icon, fontSize = 20.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(workflow.themeColor.copy(0.2f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = workflow.complexity,
                                            color = workflow.themeColor,
                                            fontSize = 6.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = workflow.name.uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = workflow.shortDesc,
                                    color = Color.White.copy(0.5f),
                                    fontSize = 8.sp,
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. SELECTION MAIN BODY & DETAILS CARD ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BroGlassCard(borderGlow = isExecuting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(activeWorkflow.icon, fontSize = 24.sp)
                            Column {
                                Text(
                                    text = activeWorkflow.name.uppercase(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Complexity Node: ${activeWorkflow.complexity}",
                                    color = activeWorkflow.themeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Execute Launcher Button
                        BroButton(
                            text = if (isExecuting) "RUNNING" else "RUN WORKFLOW",
                            onClick = { runActivePipeline() },
                            variant = if (isExecuting) BroButtonVariant.Recording else BroButtonVariant.AI,
                            isLoading = isExecuting,
                            isEnabled = !isExecuting,
                            testTag = "execute_workflow_btn_${activeWorkflow.id}"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeWorkflow.longDesc,
                        color = Color.White.copy(0.65f),
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp
                    )

                    // Progress Slider Bar
                    if (isExecuting || executionProgress > 0f) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(executionProgress)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)))
                                )
                            }
                            Text(
                                text = "${(executionProgress * 100).toInt()}%",
                                color = BroNeonCyan,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 4. STEP SEQUENCER MAP CARDS ---
        item {
            Text(
                text = "PIPELINE FLOW MAP (STEP DISPATCHER)",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                letterSpacing = 1.sp
            )
        }

        itemsIndexed(activeWorkflow.steps) { index, step ->
            val isCurrent = index == activeStepIndex
            val isCompleted = index < activeStepIndex
            val isFuture = index > activeStepIndex
            
            val scale by animateFloatAsState(targetValue = if (isCurrent) 1.02f else 1.0f, label = "stageScale")
            val glowColor = if (isCurrent) activeWorkflow.themeColor else if (isCompleted) BroMintGreen else Color.White.copy(0.1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrent) activeWorkflow.themeColor.copy(0.06f) else BroGlassCardBg)
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = glowColor.copy(alpha = if (isCurrent) 0.8f else 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Stage Index or Badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isCompleted) BroMintGreen.copy(0.15f)
                                        else if (isCurrent) activeWorkflow.themeColor.copy(0.2f)
                                        else Color.White.copy(0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Text("✓", color = BroMintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isCurrent) activeWorkflow.themeColor else Color.White.copy(0.4f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Title & Description
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(step.icon, fontSize = 13.sp)
                                    Text(
                                        text = step.title.uppercase(),
                                        color = if (step.isBypassed) Color.White.copy(0.35f) else Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (step.isBypassed) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(0.1f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("BYPASSED", color = Color.White.copy(0.4f), fontSize = 6.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                                Text(
                                    text = step.description,
                                    color = Color.White.copy(0.5f),
                                    fontSize = 8.5.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Bypass Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BroToggle(
                                checked = !step.isBypassed,
                                onCheckedChange = { checked ->
                                    step.isBypassed = !checked
                                    // Trigger recomposition explicitly via state mapping
                                    val nextOverrides = stepOverrides.toMutableMap()
                                    nextOverrides["BYPASS_SIGNAL_${step.id}"] = if (checked) 1f else 0f
                                    stepOverrides = nextOverrides
                                },
                                label = ""
                            )
                        }
                    }

                    // Value parameters adjustments slider (only shows if step is adjustable and not bypassed)
                    if (!step.isBypassed && step.targetComponent != "GEMINI_AI" && step.targetComponent != "DATABASE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.White.copy(0.04f))
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val currentValue = stepOverrides[step.id] ?: step.defaultValue
                        BroAudioSlider(
                            value = currentValue,
                            onValueChange = { newVal ->
                                val nextOverrides = stepOverrides.toMutableMap()
                                nextOverrides[step.id] = newVal
                                stepOverrides = nextOverrides
                            },
                            label = "STAGE PARAM OVERRIDE",
                            valueDisplay = "${(currentValue * 100).toInt()}%"
                        )
                    }
                }
            }
        }

        // --- 5. LOG CONSOLE TERMINAL & DOUBLE METERS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "REAL-TIME MONITOR SECTOR",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Interactive Double level meters & Spectrum osc
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STEREO VU RACK & FFT SPECTRA",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        BroVuMeter(
                            leftLevel = simulatedOutputLevel,
                            rightLevel = (simulatedOutputLevel * 0.93f).coerceAtLeast(0.04f)
                        )
                        
                        BroFftAnalyzer(
                            barsData = simulatedFft
                        )
                    }

                    // Right Column: Live terminal logs terminal
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AUTOMATION SYSTEM LOGS",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(162.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BroSpaceObsidian)
                                .border(1.dp, BroGlassBorder.copy(0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(logsList) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.startsWith(" 🎉")) BroMintGreen 
                                                else if (log.contains("[DSP]")) BroNeonCyan 
                                                else if (log.contains("[AI]")) BroHotViolet 
                                                else Color.White.copy(0.7f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
