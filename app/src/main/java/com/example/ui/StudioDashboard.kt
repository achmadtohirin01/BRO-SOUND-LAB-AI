@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui

import androidx.lifecycle.viewModelScope
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import android.widget.Toast

// --- PREMIUM NEON PALETTE SPECIFICATION ---
val SpaceObsidian = Color(0xFF04060B)
val GlassCardBg = Color(0x1F1A2235)
val GlassBorder = Color(0x336C63FF)
val NeonCyan = Color(0xFF00E5FF)
val ElectricIndigo = Color(0xFF7B2CBF)
val HotViolet = Color(0xFFE040FB)
val MintGreen = Color(0xFF00E676)
val GoldenAmber = Color(0xFFFFB300)
val DarkGunmetal = Color(0xFF131521)

@Composable
fun StudioDashboard(viewModel: SoundLabViewModel) {
    val startupState by viewModel.startupStage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SpaceObsidian, Color(0xFF0C0E17), Color(0xFF131521))
                )
            )
    ) {
        when (startupState) {
            StartupStage.SPLASH -> SplashScreenView(viewModel)
            StartupStage.INITIALIZING -> SystemInitializationView(viewModel)
            StartupStage.PERMISSION_CHECK -> PermissionCheckView(viewModel)
            StartupStage.ONBOARDING -> OnboardingScreenView(viewModel)
            StartupStage.MAIN_DASHBOARD -> MainDAWInterface(viewModel)
        }
    }
}

// ==========================================
// 1. APP STARTUP FLOW SCREENS
// ==========================================

@Composable
fun SplashScreenView(viewModel: SoundLabViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Cyber audio grid background
                val brickW = 40.dp.toPx()
                val brickH = 40.dp.toPx()
                val gridColor = GlassBorder.copy(alpha = 0.05f)
                for (x in 0 until (size.width / brickW).toInt() + 1) {
                    drawLine(gridColor, Offset(x * brickW, 0f), Offset(x * brickW, size.height))
                }
                for (y in 0 until (size.height / brickH).toInt() + 1) {
                    drawLine(gridColor, Offset(0f, y * brickH), Offset(size.width, y * brickH))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                // Double neon logo ring
                drawCircle(
                    brush = Brush.linearGradient(listOf(NeonCyan, HotViolet)),
                    radius = 36.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx()),
                    alpha = alphaAnim
                )
                drawCircle(
                    brush = Brush.linearGradient(listOf(HotViolet, ElectricIndigo)),
                    radius = 28.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Core pulse indicator
                drawCircle(
                    color = NeonCyan,
                    radius = 8.dp.toPx() * alphaAnim
                )
            }

            Text(
                text = "BRO SOUND LAB AI",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("splash_logo_text")
            )

            Text(
                text = "SYNTHESIZER • MASTERING • COCREATOR",
                color = NeonCyan.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SystemInitializationView(viewModel: SoundLabViewModel) {
    var loadingPercent by remember { mutableStateOf(0) }
    val stages = listOf(
        "Allocating memory heap for 32-bit float audio engine...",
        "Hooking native OpenSL ES output channels...",
        "Spawning vector matrix core filter filters...",
        "Interfacing local Room SQLite sessions archive...",
        "Waking server-side Gemini 3.5 AI mastering neural advisor...",
        "Initialization successful. Soundstage clear."
    )
    val activeStageIndex = (loadingPercent / 20).coerceIn(0, stages.size - 1)

    LaunchedEffect(Unit) {
        while (loadingPercent < 100) {
            delay(120)
            loadingPercent += (2..8).random()
            if (loadingPercent > 100) loadingPercent = 100
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SYSTEM INITIALIZATION",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )

            // Neon horizontal load bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(loadingPercent / 100f)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(NeonCyan, HotViolet)))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STATUS: ${stages[activeStageIndex]}",
                    color = Color.White.copy(0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$loadingPercent%",
                    color = HotViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("init_percent")
                )
            }
        }
    }
}

@Composable
fun PermissionCheckView(viewModel: SoundLabViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .background(GlassCardBg, RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ElectricIndigo.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎙️", fontSize = 28.sp)
            }

            Text(
                text = "LATENCY PERMISSIONS",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = "BRO Sound Lab requires direct microphone permissions to run the real-time Fourier transform scopes, sample vocal overlays, and feed live audio cues into the Gemini AI analysis engine.",
                color = Color.White.copy(0.6f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.setStartupStage(StartupStage.ONBOARDING) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_permission_button")
            ) {
                Text("ALLOW AUDIO ACCESS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            TextButton(
                onClick = { viewModel.setStartupStage(StartupStage.ONBOARDING) }
            ) {
                Text("Skip for offline synth sandbox", color = Color.White.copy(0.4f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun OnboardingScreenView(viewModel: SoundLabViewModel) {
    var stepIndex by remember { mutableStateOf(0) }
    val guidelines = listOf(
        Triple("AI AUDIO ADVOCATION", "Unleash server-side Gemini 3.5 AI to analyze descriptions, auto-configure filters, and sculpt crisp master tracks.", "🤖"),
        Triple("CONCURRENT VCO KEYS", "Interact directly with a quad voltage-controlled oscillator oscillator, generating pure real-time feedback sine waves.", "🎹"),
        Triple("TACTILE CONTROLLERS", "Command continuous 10-band tactile equalizers and analog slider rigs to mold your complete high-fidelity soundstage.", "🎛️")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .background(GlassCardBg, RoundedCornerShape(28.dp))
                .border(2.dp, GlassBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONBOARDING PRELIGHT",
                    color = GoldenAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${stepIndex + 1}/3",
                    color = Color.White.copy(0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, GlassBorder.copy(0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(guidelines[stepIndex].third, fontSize = 36.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = guidelines[stepIndex].first,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = guidelines[stepIndex].second,
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(58.dp)
                )
            }

            // Slide indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == stepIndex) NeonCyan else Color.White.copy(0.15f))
                    )
                }
            }

            Button(
                onClick = {
                    if (stepIndex < 2) {
                        stepIndex++
                    } else {
                        viewModel.setStartupStage(StartupStage.MAIN_DASHBOARD)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (stepIndex < 2) "CONTINUE PROTOCOL" else "INITIALIZE WORKSPACE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==========================================
// 2. MAIN APPLICATION WORKSPACE LAYOUT
// ==========================================

@Composable
fun MainDAWInterface(viewModel: SoundLabViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val currentTab by viewModel.currentTab.collectAsState()
    val isCreateModalOpen by viewModel.isCreateModalOpen.collectAsState()
    val isSideDrawerOpen by viewModel.isSideDrawerOpen.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Side Navigation Rail (Tablet/Landscape Only)
            if (isTablet) {
                AdaptiveNavRail(viewModel)
            }

            // Main Content Area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Persistent Top Header (Menu, Search, Alerts)
                MainTopHeader(viewModel)

                // Navigation Area Switcher
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        "HOME" -> HomeScreenView(viewModel)
                        "PROJECTS" -> ProjectsScreenView(viewModel)
                        "WORKFLOWS" -> WorkflowsScreenView(viewModel)
                        "TOOLS" -> ToolsScreenView(viewModel)
                        "PROFILE" -> ProfileScreenView(viewModel)
                    }
                }

                // Persistent Web-like Mini Music Player Overlay (Always Visible)
                PersistentMiniPlayer(viewModel)

                // Phone-specific Bottom Navigation Bar (Hidden on expand or Tablet rail)
                if (!isTablet) {
                    PhoneNavigationBar(viewModel)
                }
            }
        }

        // Custom Glassmorphic Drawer Overlay (Animates Left to Right)
        AnimatedVisibility(
            visible = isSideDrawerOpen,
            enter = slideInHorizontally(animationSpec = tween(280)),
            exit = slideOutHorizontally(animationSpec = tween(240))
        ) {
            CustomSideDrawerOverlay(viewModel)
        }

        // Full Screen Create Menu Modal
        AnimatedVisibility(
            visible = isCreateModalOpen,
            enter = fadeIn(tween(250)) + scaleIn(tween(250)),
            exit = fadeOut(tween(200)) + scaleOut(tween(200))
        ) {
            FullScreenCreateModal(viewModel)
        }
    }
}

// ==========================================
// 3. MAIN TOP HEADER
// ==========================================

@Composable
fun MainTopHeader(viewModel: SoundLabViewModel) {
    var globalSearchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showPromoAlerts by remember { mutableStateOf(false) }
    val currentTab by viewModel.currentTab.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    
    // Live ticking studio clock
    var timeString by remember { mutableStateOf("02:06:39 UTC") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            timeString = sdf.format(Date())
            delay(1000)
        }
    }

    // Dynamic greeting based on clock
    val hour = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        hour in 0..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // Pulse animation for the AI and live audio logos
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceObsidian.copy(0.85f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .border(1.dp, Brush.verticalGradient(listOf(GlassBorder.copy(alpha = 0.25f), Color.Transparent)), RoundedCornerShape(0.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Section A: Animated Logo & Profile Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Hamburger Menu Button
                IconButton(
                    onClick = { viewModel.setSideDrawerOpen(true) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, GlassBorder.copy(0.4f), RoundedCornerShape(8.dp))
                        .testTag("hamburger_menu_button")
                ) {
                    Text("☰", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                // Logo with flashing Neon status bubble and greeting
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(NeonCyan.copy(alpha = alphaAnim))
                        )
                        Text(
                            text = "BRO SOUND LAB AI",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Text(
                        text = "$greetingText, Achmad!",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Section B: Dedicated Studio Status Flags (Sync, Battery, AI) - Hidden on extra small width
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                // Cloud Sync Status badge
                Box(
                    modifier = Modifier
                        .background(MintGreen.copy(0.12f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, MintGreen.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☁️", fontSize = 7.sp)
                        Text("ONLINE", color = MintGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Battery Optimization Badge
                Box(
                    modifier = Modifier
                        .background(GoldenAmber.copy(0.12f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, GoldenAmber.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔋", fontSize = 7.sp)
                        Text("OPTIMAL", color = GoldenAmber, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // AI Status Badge
                Box(
                    modifier = Modifier
                        .background(HotViolet.copy(0.12f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, HotViolet.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔮", fontSize = 7.sp)
                        Text("GEMINI-3.5", color = HotViolet, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Clock indicator
                Text(
                    text = timeString,
                    color = Color.White.copy(0.45f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Section C: Standard Dashboard Control Items (Search, Settings, Notifications, Profile)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive settings button
                IconButton(
                    onClick = {
                        viewModel.addNotification("System Settings diagnostics validated.")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(0.02f), RoundedCornerShape(50))
                ) {
                    Text("⚙️", fontSize = 14.sp)
                }

                // Notification Center with BadgedBox overlay
                IconButton(
                    onClick = { showPromoAlerts = !showPromoAlerts },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(0.02f), RoundedCornerShape(50))
                ) {
                    BadgedBox(
                        badge = {
                            if (notifications.isNotEmpty()) {
                                Badge(containerColor = HotViolet) {
                                    Text(notifications.size.toString(), color = Color.White, fontSize = 7.sp)
                                }
                            }
                        }
                    ) {
                        Text("🔔", fontSize = 14.sp)
                    }
                }

                // Global Interactive Search trigger
                IconButton(
                    onClick = { globalSearchOpen = true },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(50))
                        .testTag("global_search_icon_button")
                ) {
                    Text("🔍", color = Color.White, fontSize = 14.sp)
                }

                // Profile Avatar Indicator
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.radialGradient(listOf(HotViolet.copy(0.3f), Color.Transparent)))
                        .border(1.dp, GlassBorder, RoundedCornerShape(50))
                        .clickable { viewModel.setCurrentTab("PROFILE") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍🎤", fontSize = 15.sp)
                }
            }
        }

        // Dropdown Notifications log drawer
        DropdownMenu(
            expanded = showPromoAlerts,
            onDismissRequest = { showPromoAlerts = false },
            modifier = Modifier
                .background(Color(0xFF0F121F))
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .width(260.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SYSTEM LOGS", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("CLEAR", color = HotViolet, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.clearNotifications() })
                    }
                },
                onClick = {}
            )
            Divider(color = Color.White.copy(0.08f))
            if (notifications.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No logs reported. Core silent.", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp) },
                    onClick = {}
                )
            } else {
                notifications.take(5).forEach { message ->
                    DropdownMenuItem(
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(MintGreen).align(Alignment.CenterVertically))
                                Text(message, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        onClick = {}
                    )
                }
            }
        }

        // Global Search overlay Dialog
        if (globalSearchOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE604060B))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            viewModel.setSearchQuery(it)
                        },
                        placeholder = { Text("Search projects, presets, effects, plugins...", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("global_search_box_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Text("❌", fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = { globalSearchOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CLOSE", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PHONE BOTTOM NAVIGATION
// ==========================================

@Composable
fun PhoneNavigationBar(viewModel: SoundLabViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isProjGridView by viewModel.isProjectGridView.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070912))
            .navigationBarsPadding()
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorder.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(0.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val navItems = listOf(
            Triple("HOME", "🏠", "Home"),
            Triple("PROJECTS", "📂", "Projects"),
            Triple("CREATE", "➕", "Create"),
            Triple("WORKFLOWS", "🔄", "Workflows"),
            Triple("TOOLS", "🎛️", "Tools")
        )

        navItems.forEach { (tabId, unicode, label) ->
            val isSelected = currentTab == tabId

            if (tabId == "CREATE") {
                // Interactive middle modal launcher
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(NeonCyan, ElectricIndigo)))
                        .clickable { viewModel.setCreateModalOpen(true) }
                        .testTag("tab_button_CREATE"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = unicode, fontSize = 24.sp)
                }
            } else {
                IconButton(
                    onClick = {
                        viewModel.setCurrentTab(tabId)
                        viewModel.setSelectedToolId(null)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_button_$tabId")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = unicode,
                            fontSize = 16.sp,
                            color = if (isSelected) NeonCyan else Color.White.copy(0.4f)
                        )
                        Text(
                            text = label,
                            color = if (isSelected) NeonCyan else Color.White.copy(0.4f),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ADAPTIVE TABLET NAVIGATION RAIL
// ==========================================

@Composable
fun AdaptiveNavRail(viewModel: SoundLabViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    Column(
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(Color(0xFF070912))
            .border(1.dp, Brush.horizontalGradient(listOf(GlassBorder.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(0.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Upper pulsing badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassCardBg)
                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎚️", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val navItems = listOf(
            Triple("HOME", "🏠", "Home"),
            Triple("PROJECTS", "📂", "Projects"),
            Triple("CREATE", "➕", "Create"),
            Triple("WORKFLOWS", "🔄", "Workflows"),
            Triple("TOOLS", "🎛️", "Tools")
        )

        navItems.forEach { (tabId, unicode, label) ->
            val isSelected = currentTab == tabId

            if (tabId == "CREATE") {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonCyan, HotViolet)))
                        .clickable { viewModel.setCreateModalOpen(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = unicode, fontSize = 20.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setCurrentTab(tabId)
                            viewModel.setSelectedToolId(null)
                        }
                        .padding(vertical = 8.dp)
                        .testTag("tablet_nav_button_$tabId"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = unicode,
                            fontSize = 20.sp,
                            color = if (isSelected) NeonCyan else Color.White.copy(0.4f)
                        )
                        Text(
                            text = label,
                            color = if (isSelected) NeonCyan else Color.White.copy(0.4f),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. CUSTOM INTUITIVE SIDE DRAWER OVERLAY
// ==========================================

@Composable
fun CustomSideDrawerOverlay(viewModel: SoundLabViewModel) {
    val drawerItems = listOf(
        Pair("☁️ Cloud Presets Storage", "Online synchronization node"),
        Pair("🛒 Sound FX Marketplace", "Import premium audio packets"),
        Pair("🔌 Plugin Expansion Store", "Synthesizers and limiters"),
        Pair("🎓 Sound Lab Academy", "Dynamic filter tutorials"),
        Pair("📺 Interactive Tutorials", "2-tap flow guides"),
        Pair("🤝 Community Portals", "Direct Discord collaborations"),
        Pair("🗺️ 2026 Innovation Roadmap", "Picture-in-picture roadmap"),
        Pair("📋 Changelog v1.50 Build", "Latency benchmark indices"),
        Pair("🧪 Beta Innovation Labs", "Warp soundstage nodes")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .clickable { viewModel.setSideDrawerOpen(false) }
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color(0xFF080B15))
                .border(1.dp, GlassBorder, RoundedCornerShape(0.dp))
                .clickable(enabled = false) {}
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HIDDEN NAVIGATION",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = { viewModel.setSideDrawerOpen(false) }, modifier = Modifier.testTag("close_drawer_button")) {
                    Text("❌", color = Color.White, fontSize = 10.sp)
                }
            }

            Divider(color = Color.White.copy(0.08f), modifier = Modifier.padding(bottom = 12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(drawerItems) { (title, subtitle) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(0.02f), RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorder.copy(0.2f), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.addNotification("Opened '$title' portal node")
                                viewModel.setSideDrawerOpen(false)
                            }
                            .padding(12.dp)
                    ) {
                        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.White.copy(0.5f), fontSize = 8.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "BRO SOUND LAB AI v1.40\nAchmad Tohirin Custom System Frame",
                color = Color.White.copy(0.3f),
                fontSize = 8.sp,
                lineHeight = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// 7. FULL-SCREEN CREATE MENUS MODAL (11 Actions)
// ==========================================

@Composable
fun FullScreenCreateModal(viewModel: SoundLabViewModel) {
    val itemsList = listOf(
        Triple("🎙️ New Recording", "Allocates linear device buffer core channel", "RECORDER"),
        Triple("📁 Import Audio Track", "Synchronizes external sample wav arrays", "IMPORT"),
        Triple("🤖 New AI Project", "Query Gemini models directly for project tracks", "AI"),
        Triple("📻 New Podcast Mix", "Pre-adjust limiter ratios for speech streams", "PODCAST"),
        Triple("🎤 New Karaoke Split", "Execute vocal isolation algorithms", "KARAOKE"),
        Triple("🎛️ DSP Mastering Desk", "Direct to interactive sliders architecture", "MASTERING"),
        Triple("📊 Audio Fourier Scope", "Opens FFT and Oscilloscope desk and nodes", "ANALYSIS"),
        Triple("🎶 Synthesize Music AI", "Generates full instrument layers", "MUSIC"),
        Triple("📝 Build Song Lyrics AI", "Interact with Lyricist network assistant", "LYRICS"),
        Triple("🥁 Generate Beats AI", "Spawn random grid rhythm maps", "BEAT"),
        Triple("📄 Create Empty Session", "Starts fresh baseline stereo grid", "EMPTY")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF204060B))
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CREATE MENU MATRIX",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Choose a specialized audio session template below",
                        color = Color.White.copy(0.5f),
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.setCreateModalOpen(false) },
                    modifier = Modifier
                        .background(Color.White.copy(0.08f), RoundedCornerShape(50))
                        .testTag("close_create_modal_button")
                ) {
                    Text("❌", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itemsList) { (title, desc, code) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCardBg, RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.setCreateModalOpen(false)
                                when (code) {
                                    "RECORDER" -> viewModel.startRecording()
                                    "MASTERING" -> {
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("MIXER")
                                    }
                                    "ANALYSIS" -> {
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("ANALYZER")
                                    }
                                    "LYRICS" -> {
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("AI_COCREATOR")
                                    }
                                    "EMPTY" -> viewModel.createNewProject("Untitled Studio Session")
                                    else -> {
                                        viewModel.createNewProject("AI $code Project")
                                        viewModel.addNotification("Successfully spawned system workflow $code")
                                    }
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = Color.White.copy(0.5f), fontSize = 9.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = NeonCyan, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. TAB AREA 1: HOME DASHBOARD
// ==========================================

@Composable
fun HomeScreenView(viewModel: SoundLabViewModel) {
    val activeProject by viewModel.selectedProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    
    // UI Local Stateful overlays for advanced DAW Tools
    var visSettingsOpen by remember { mutableStateOf(false) }
    var visDecayPeriod by remember { mutableStateOf(0.7f) }
    var visScaleDb by remember { mutableStateOf(48f) }
    
    var fileImporterOpen by remember { mutableStateOf(false) }
    var audioCropperOpen by remember { mutableStateOf(false) }
    var karaokeSplitterOpen by remember { mutableStateOf(false) }
    var aiMusicComposerOpen by remember { mutableStateOf(false) }
    var audioRepairOpen by remember { mutableStateOf(false) }
    var advancedRecorderSettingsOpen by remember { mutableStateOf(false) }
    
    // AI Assistant inputs & simulated output console
    var aiAssistantInput by remember { mutableStateOf("") }
    var aiAssistantOutput by remember { mutableStateOf("Co-Creator Ready. Choose a fast command below or type details to orchestrate models.") }
    var aiIsThinking by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Infinite animation loops for visualization dynamics
    val transition = rememberInfiniteTransition(label = "daw_oscilloscope")
    val sweepFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "oscilloscopeSweep"
    )
    
    val bounceFloat by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscilloscopeBounce"
    )

    // Layout Root Container with Floating Record FAB overlaid
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceObsidian)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_scrollable_container"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // Section 1: Dynamic Greeting & Audio Tip Panel
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0x0E7B2CBF), Color(0x0300E5FF))))
                        .border(1.dp, GlassBorder.copy(0.18f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(ElectricIndigo, HotViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Achmad Tohirin • Producer Console",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MASTERING TIP: Saturating the 16kHz vocal high-end band with 12ms delay feedbacks generates gorgeous digital air. Try custom EQ profiles below.",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ==========================================
            // Section 2: Realtime Live Audio Visualization Deck
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LIVE AUDIO VISUALIZER (TAP ANALYZER // LONG-PRESS CFG)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF070912))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        // Tap opens the analyzer tab tool
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("ANALYZER")
                                        Toast.makeText(context, "Navigated to Analyzer diagnostic deck.", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongPress = {
                                        visSettingsOpen = true
                                    }
                                )
                            }
                    ) {
                        // Drawing grid lines, FFT spectrum columns, VU led lights, and Soundwave oscillographs
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle horizontal grid ticks (Classic DAW cathode scopes look)
                            val gridLines = 5
                            val gridStep = h / gridLines
                            for (i in 0 until gridLines) {
                                drawLine(
                                    color = Color.White.copy(0.04f),
                                    start = Offset(0f, i * gridStep),
                                    end = Offset(w, i * gridStep),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // 1. Draw Simulated 16-Band FFT Columns
                            val barCount = 18
                            val barGap = 6.dp.toPx()
                            val totalGapWidth = barGap * (barCount - 1)
                            val barWidth = (w * 0.7f - totalGapWidth) / barCount

                            for (i in 0 until barCount) {
                                val amplitudeMultiplier = sin(sweepFloat + (i * 0.45f)) * 0.35f + 0.55f
                                val barHeight = h * 0.75f * amplitudeMultiplier * bounceFloat
                                val startX = i * (barWidth + barGap)

                                // Dual-color glowing metallic gradient for columns
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(HotViolet, NeonCyan)
                                    ),
                                    topLeft = Offset(startX, h - barHeight),
                                    size = Size(barWidth, barHeight),
                                    alpha = 0.82f
                                )
                            }

                            // 2. Draw Simulated Stereo VU Level Meter (L / R Channel)
                            val vuStartX = w * 0.75f
                            val vuWidth = 12.dp.toPx()
                            val segmentCount = 10
                            val segmentHeight = (h * 0.85f) / segmentCount
                            val segmentGap = 2.dp.toPx()

                            for (ch in 0..1) { // Left = 0, Right = 1
                                val currentChStartX = vuStartX + ch * (vuWidth + 14.dp.toPx())
                                val chRandomAmp = if (ch == 0) bounceFloat else (bounceFloat * 0.9f + 0.05f)

                                for (s in 0 until segmentCount) {
                                    val isLit = (segmentCount - s).toFloat() / segmentCount <= chRandomAmp
                                    val segmentColor = when {
                                        s < 2 -> Color.Red // peaks
                                        s < 5 -> GoldenAmber // high warnings
                                        else -> MintGreen // normal safety levels
                                    }
                                    val segmentAlpha = if (isLit) 0.95f else 0.08f

                                    drawRoundRect(
                                        color = segmentColor,
                                        topLeft = Offset(currentChStartX, s * (segmentHeight + segmentGap)),
                                        size = Size(vuWidth, segmentHeight),
                                        cornerRadius = CornerRadius(1.dp.toPx()),
                                        alpha = segmentAlpha
                                    )
                                }
                            }

                            // 3. Draw overlay continuous Waveform Line
                            val points = 64
                            val path = androidx.compose.ui.graphics.Path()
                            val waveWidth = w * 0.7f
                            val waveYCenter = h * 0.4f

                            for (p in 0..points) {
                                val x = (p.toFloat() / points) * waveWidth
                                val y = waveYCenter + sin(sweepFloat + (p * 0.35f)) * 14.dp.toPx() * bounceFloat

                                if (p == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = NeonCyan,
                                style = Stroke(width = 2.dp.toPx()),
                                alpha = 0.9f
                            )
                        }

                        // Top overlays displaying real-time parameters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(Color.Black.copy(0.4f))
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DECAY: ${(visDecayPeriod * 100).toInt()}% • SCALE: ${visScaleDb.toInt()}dB SPL",
                                color = Color.White.copy(0.55f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MintGreen)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // Section 3: Quick Studio Tools (Haptic Grid of 10)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK STUDIO TOOLS (TOUCH RESPONSIVE CORES)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 10 buttons aligned inside a premium multi-item row grid
                    val toolsList = listOf(
                        Quadruple("RECORD", "🎙️", { viewModel.startRecording() }, NeonCyan),
                        Quadruple("IMPORT", "📥", { fileImporterOpen = true }, NeonCyan),
                        Quadruple("EQ", "🎛️", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("EQUALIZER")
                        }, ElectricIndigo),
                        Quadruple("AI MASTER", "🪄", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("AI_COCREATOR")
                        }, ElectricIndigo),
                        Quadruple("MIXER", "🎚️", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("MIXER")
                        }, HotViolet),
                        Quadruple("EDITOR", "✂️", { audioCropperOpen = true }, HotViolet),
                        Quadruple("KARAOKE", "🎤", { karaokeSplitterOpen = true }, MintGreen),
                        Quadruple("PODCAST", "🗣️", {
                            // Apply broadcast podcast preset and notify
                            viewModel.savePreset("Broadcast Vocal")
                            viewModel.addNotification("Podcast vocal preset applied instantly.")
                            Toast.makeText(context, "Applied broadcast profile preset to Equalizer.", Toast.LENGTH_SHORT).show()
                        }, MintGreen),
                        Quadruple("AI MUSIC", "🎵", { aiMusicComposerOpen = true }, GoldenAmber),
                        Quadruple("REPAIR", "🩹", { audioRepairOpen = true }, GoldenAmber)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 5,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        toolsList.forEach { tool ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.2f), RoundedCornerShape(10.dp))
                                    .clickable { tool.action() }
                                    .testTag("quick_studio_tool_${tool.label}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(tool.unicode, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tool.label,
                                        color = tool.color,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 4: Continue Project (Focus Hub)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONTINUE RECENT WORKSPACE SESSION",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Target last project dynamically or show placeholder
                    val focalProj = allProjects.firstOrNull() ?: AudioProjectEntity(
                        id = 9999,
                        title = "Atmospheric Void Intro Synth",
                        bpm = 114,
                        keySignature = "F# Minor",
                        notes = "Analog saturation model tracks with heavy multi-stage tape delays.",
                        lyrics = "Silent soundwaves breathing inside the electronic neon void..."
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.radialGradient(listOf(Color(0x2A131521), Color(0xFF04060B))))
                            .border(1.5.dp, HotViolet.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .background(HotViolet.copy(0.18f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LAST WORKSPACE", color = HotViolet, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = focalProj.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "⏱️ ${focalProj.bpm} BPM // ${focalProj.keySignature}",
                                color = GoldenAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Simulated Soundwave artwork inside focal session
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.02f))
                                .border(0.5.dp, GlassBorder.copy(0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                                val w = size.width
                                val h = size.height
                                val lines = 44
                                val lineGap = w / lines

                                for (i in 0 until lines) {
                                    val valSin = sin(i * 0.18f) * cos(i * 0.4f)
                                    val finalLineHeight = (h * 0.8f) * abs(valSin)

                                    drawLine(
                                        color = if (i < lines * 0.35f) HotViolet else NeonCyan.copy(0.5f),
                                        start = Offset(i * lineGap, (h - finalLineHeight) / 2f),
                                        end = Offset(i * lineGap, (h + finalLineHeight) / 2f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved: " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(focalProj.lastSaved)),
                                color = Color.White.copy(0.4f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Delete/Discard button
                                Button(
                                    onClick = {
                                        if (allProjects.isNotEmpty() && focalProj.id != 9999) {
                                            viewModel.deleteProject(focalProj.id)
                                            Toast.makeText(context, "Workspace discarded: ${focalProj.title}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Cannot discard system core templates.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.12f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).border(0.5.dp, Color.Red.copy(0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Text("DISCARD", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                // Duplicate/Clone button
                                Button(
                                    onClick = {
                                        viewModel.createNewProject(focalProj.title + " (Copy)")
                                        Toast.makeText(context, "Project duplicated in Database.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassCardBg),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).border(0.5.dp, GlassBorder.copy(0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Text("CLONE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                // Continue/Edit button
                                Button(
                                    onClick = {
                                        viewModel.selectProject(focalProj)
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("MIXER")
                                        Toast.makeText(context, "Loaded DAW: ${focalProj.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("EDIT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 5: AI sound assistant card
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI CO-CREATOR STUDIO COMMAND CENTER",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F0B18))
                            .border(1.5.dp, HotViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Blinking AI core avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.radialGradient(listOf(HotViolet.copy(0.5f), Color.Transparent)))
                                    .border(1.dp, HotViolet, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔮", fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                            }

                            Column {
                                Text("AI SOUND ASSISTANT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("CO-CREATION ENGINES ACTIVE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // AI Console output
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(0.35f))
                                .padding(8.dp)
                        ) {
                            if (aiIsThinking) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = NeonCyan)
                                    Text("Synthesizing neural audio parameters...", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                Text(
                                    text = aiAssistantOutput,
                                    color = Color.White.copy(0.85f),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Text input & Voice mic button row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = aiAssistantInput,
                                onValueChange = { aiAssistantInput = it },
                                placeholder = { Text("Ask Gemini to isolate sound, write chords, split bass...", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("ai_prompt_text_field"),
                                textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HotViolet,
                                    unfocusedBorderColor = GlassBorder.copy(0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Voice dictation microphone button
                            IconButton(
                                onClick = {
                                    aiAssistantInput = "Record broadcast profile mic vocal track"
                                    Toast.makeText(context, "Voice dictation audio recorded.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(HotViolet.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, HotViolet.copy(0.4f), RoundedCornerShape(10.dp))
                                    .testTag("voice_prompt_mic_button")
                            ) {
                                Text("🎙️", fontSize = 16.sp)
                            }

                            // Run button
                            Button(
                                onClick = {
                                    if (aiAssistantInput.isNotEmpty()) {
                                        keyboardController?.hide()
                                        aiIsThinking = true
                                        aiAssistantOutput = ""
                                        // Execute custom mock response based on input
                                        val prompt = aiAssistantInput
                                        aiAssistantInput = ""
                                        coroutineScope.launch {
                                            delay(1500)
                                            aiIsThinking = false
                                            aiAssistantOutput = when {
                                                prompt.contains("Vocal", ignoreCase = true) || prompt.contains("vocal", ignoreCase = true) ->
                                                    "AI Separation: Vocal track successfully isolated from 'Beat Stem 1'. Separation fidelity is computed at 98.4dB SNR."
                                                prompt.contains("Chords", ignoreCase = true) || prompt.contains("chords", ignoreCase = true) ->
                                                    "AI Chords Composer: Generated (i - VI - III - VII) progression in F# Minor. Playback mapped to analog synth module."
                                                prompt.contains("Master", ignoreCase = true) || prompt.contains("master", ignoreCase = true) ->
                                                    "AI Mastering Suite: Set graphic EQ decay parameters. Added +2.4dB tube compression saturation at 250Hz. Lows gated."
                                                else -> "AI Engine: Executed pipeline for the requested action successfully. Equalizer registers configured."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("RUN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Presets Task pills (Interactive pills)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pills = listOf(
                                "Remove Vocal" to "Isolating vocal stems...",
                                "Master This Song" to "Applying heavy solid-state tube limiter and analog EQ outputs...",
                                "Clean My Voice" to "Noise Gate threshold mapped to -45dB. Attenuating hiss values...",
                                "Generate Beat" to "Generated 118 BPM deep ambient house grid...",
                                "Fix Noise" to "Attenuating clicks, high-frequency hiss, and low-end AC rumble..."
                            )

                            pills.forEach { (label, actionResponse) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(0.04f))
                                        .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(50))
                                        .clickable {
                                            aiIsThinking = true
                                            coroutineScope.launch {
                                                delay(1200)
                                                aiIsThinking = false
                                                aiAssistantOutput = "AI Suite: $actionResponse"
                                            }
                                        }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = NeonCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 6: Recent Projects Carousel (Unbounded Horizontal Scrolling)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RECENT PROJECTS (UNLIMITED HORIZONTAL CAROUSEL)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    if (allProjects.isEmpty()) {
                        // Empty state indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassCardBg)
                                .border(1.dp, GlassBorder.copy(0.12f), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.createNewProject("New Atmospheric Track")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📂 No tracks inside DB workspace.", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("CLICK TO GENERATE FIRST DAW TEMPLATE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allProjects) { proj ->
                                Column(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassCardBg)
                                        .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectProject(proj)
                                            viewModel.setCurrentTab("TOOLS")
                                            viewModel.setSelectedToolId("MIXER")
                                            Toast.makeText(context, "Loaded: ${proj.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = proj.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("☁️", fontSize = 8.sp)
                                            Text("⭐️", fontSize = 8.sp, color = GoldenAmber)
                                        }
                                    }

                                    // Minimal wave shape inside card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(0.01f))
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val steps = 18

                                            for (k in 0 until steps) {
                                                val hFraction = sin((k + proj.id) * 0.45f) * cos((k + proj.id) * 0.9f)
                                                val barH = h * 0.7f * abs(hFraction)
                                                drawRect(
                                                    color = if (proj.id % 2 == 0) NeonCyan.copy(0.6f) else HotViolet.copy(0.6f),
                                                    topLeft = Offset(k * (w / steps), (h - barH) / 2f),
                                                    size = Size((w / steps) * 0.7f, barH)
                                                )
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${proj.bpm} BPM // ${proj.keySignature}", color = Color.White.copy(0.5f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                        Box(
                                            modifier = Modifier
                                                .background(HotViolet.copy(0.12f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("STUDIO", color = HotViolet, fontSize = 6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 7: AI Suggestions (Category Filtering Chips)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI CLASSIFICATION CHIPS",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val suggestionModels = listOf(
                            "Offline AI" to MintGreen,
                            "Cloud AI" to NeonCyan,
                            "Trending AI" to HotViolet,
                            "Recommended AI" to GoldenAmber,
                            "Recently Used" to Color.White
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(suggestionModels) { (lbl, clr) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(0.04f))
                                        .border(0.5.dp, clr.copy(0.35f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.addNotification("AI profile matched to: $lbl")
                                            Toast.makeText(context, "$lbl module selected.", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(50)).background(clr))
                                        Text(lbl, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 8: Favorite Presets Carousel (Instant EQ Modifiers)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FAVORITE HARDWARE PRESETS (TAP TO APPLY EQ MIX)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Standard presets target lists
                    val favoritePresets = listOf(
                        Triple("Bass Boost", "Solid low-end sub boost (+6dB @ 60Hz)", listOf(6f, 4f, 2f, 0f, 0f, 0f, 1f, 2f, 3f, 4f)),
                        Triple("Podcast", "Vocal clarity emphasis mid-range", listOf(-2f, -1f, 1f, 4f, 5f, 3f, 2f, 1f, 3f, 1f)),
                        Triple("Studio Vocal", "High air dynamic compression", listOf(1f, 2f, 1f, 2f, 4f, 5f, 5f, 6f, 8f, 7f)),
                        Triple("Live Streaming", "Broad-stage ambient gating", listOf(3f, 3f, 2f, 1f, 2f, 3f, 4f, 5f, 4f, 3f)),
                        Triple("Gaming", "Expanded surround localization", listOf(5f, 2f, -1f, -1f, 1f, 3f, 4f, 5f, 6f, 8f)),
                        Triple("Car Audio", "Punchy solid acoustics limiter", listOf(6f, 6f, 4f, 2f, 0f, 1f, 2f, 3f, 4f, 6f)),
                        Triple("Headphones", "Attenuated harsh-high parameters", listOf(1f, 1f, 2f, 2f, 0f, -1f, -2f, -3f, 0f, 2f))
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoritePresets) { (name, bDesc, eqLevels) ->
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, NeonCyan.copy(0.18f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        // Update viewmodel EQ bands dynamically
                                        for (i in 0 until 10) {
                                            viewModel.setEqBandLevel(i, eqLevels[i])
                                        }
                                        viewModel.addNotification("$name physical EQ settings loaded.")
                                        Toast.makeText(context, "Loaded EQ Target Mix: $name", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(name, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(bDesc, color = Color.White.copy(0.45f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, lineHeight = 9.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // High visual EQ curve sparkline inside preset
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .background(Color.Black.copy(0.2f), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cW = size.width
                                        val cH = size.height
                                        val eStep = cW / 10

                                        for (e in 0 until 10) {
                                            val eqFraction = (eqLevels[e] + 12f) / 24f // Map -12..12 DB to scale 0..1
                                            val eqLineH = cH * eqFraction
                                            drawRect(
                                                color = HotViolet,
                                                topLeft = Offset(e * eStep, cH - eqLineH),
                                                size = Size(eStep * 0.7f, eqLineH)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 9: Recent Audio Files
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "IMPORTED AUDIO LIBRARY",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val audioLibrary = listOf(
                        Triple("vocal_lead_raw.wav", "00:44", "PCM 24-bit 96kHz Stereo"),
                        Triple("synthesizer_poly_pad.wav", "02:18", "PCM 24-bit 96kHz Stereo"),
                        Triple("acoustic_drum_break_120.wav", "00:08", "PCM 24-bit 96kHz Mono"),
                        Triple("recorded_ambient_hiss.wav", "01:10", "MP3 320kbps Stereo")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        audioLibrary.forEach { (aName, aDur, aSpec) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.12f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎵", fontSize = 16.sp)
                                    Column {
                                        Text(aName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("$aDur • $aSpec", color = Color.White.copy(0.4f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Open file
                                    IconButton(
                                        onClick = {
                                            viewModel.addNotification("Loaded $aName as Active Mastering Input.")
                                            Toast.makeText(context, "$aName selected.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp).background(Color.White.copy(0.04f), RoundedCornerShape(50))
                                    ) {
                                        Text("▶", fontSize = 10.sp, color = NeonCyan)
                                    }

                                    // Share Node file
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "System share callback generated for: $aName", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp).background(Color.White.copy(0.04f), RoundedCornerShape(50))
                                    ) {
                                        Text("📤", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 10: Learning Center Cards
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LEARNING CENTER",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val tutorials = listOf(
                        Triple("PRO AUDIO MASTERING SECRETS", "Unlock the hidden solid-state saturator algorithms to double vocal loudness safely.", "LEARN PRO MASTER"),
                        Triple("OPTIMIZING 10-BAND EQ PARAMETERS", "Visual guide mapping raw frequency spectra to professional car and headphone speakers.", "VIEW EQ GUIDE"),
                        Triple("AI CREATIVE LYRICS GENERATORS", "How to trigger Gemini LLMs inside our lyrics generation matrix effectively.", "OPEN GUIDE")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tutorials) { (titleText, descText, actionLabel) ->
                            Column(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.24f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(titleText, color = GoldenAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(descText, color = Color.White.copy(0.6f), fontSize = 9.sp, lineHeight = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.addNotification("Tutorial loaded: $titleText")
                                        Toast.makeText(context, "Redirecting to $titleText", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(actionLabel, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Margin bottom to allow scroll clearance above PersistentMiniPlayer
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        // ==========================================
        // Section 11: Heartbeat-pulsing Red Record FAB
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
        ) {
            val recordActive by viewModel.isRecordingActive.collectAsState()
            
            // Pulse glow size animation
            val fabTransition = rememberInfiniteTransition(label = "fab_pulse")
            val pulseScale by fabTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fabScale"
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer(
                        scaleX = if (recordActive) 1.0f else pulseScale,
                        scaleY = if (recordActive) 1.0f else pulseScale
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Red,
                                if (recordActive) Color.Red.copy(0.4f) else Color.Red.copy(0.12f)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.startRecording()
                            },
                            onLongPress = {
                                // Save a fast Voice Memo directly
                                val name = "Voice_Memo_" + System.currentTimeMillis() / 1000
                                viewModel.startRecording()
                                coroutineScope.launch {
                                    delay(2000)
                                    viewModel.stopAndSaveRecording(name)
                                    Toast.makeText(context, "Voice Memo saved automatically: $name", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDoubleTap = {
                                advancedRecorderSettingsOpen = true
                            }
                        )
                    }
                    .testTag("floating_live_audio_record_fab"),
                contentAlignment = Alignment.Center
            ) {
                // Glow boundary circles
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF04060B))
                        .border(2.dp, Color.Red, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔴", fontSize = 18.sp)
                }
            }
        }

        // ==========================================
        // LOCAL STATE MODALS & TELEMETRY DIALOGS
        // ==========================================

        // 1. Interactive Visualization settings overlay panel
        if (visSettingsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.8f))
                    .clickable { visSettingsOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0A0C16), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("VISUALIZATION ENGINE VALUES", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { visSettingsOpen = false })
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Decay Coefficient: ${(visDecayPeriod * 100).toInt()}%", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = visDecayPeriod,
                            onValueChange = { visDecayPeriod = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("FFT Scale Power: ${visScaleDb.toInt()} dB SPL", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = visScaleDb,
                            onValueChange = { visScaleDb = it },
                            valueRange = 12f..96f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Button(
                        onClick = { visSettingsOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("APPLY PHYSICAL GRID EFFECTS", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Beautiful File Importer Dialog
        if (fileImporterOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.8f))
                    .clickable { fileImporterOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF090B12), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SELECT AUDIO SOURCE FILE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { fileImporterOpen = false })
                    }

                    val samples = listOf(
                        "guitar_strum_clean_E.wav" to "00:15 • 24-bit PCM",
                        "rhythm_trap_drums_808.wav" to "00:48 • 24-bit PCM",
                        "synth_wave_space_intro.wav" to "02:04 • 24-bit PCM",
                        "ambient_rain_sound_effect.mp3" to "01:30 • 320kbps MP3"
                    )

                    samples.forEach { (sName, sSpec) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.02f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, GlassBorder.copy(0.12f), RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.addNotification("Imported $sName successfully.")
                                    fileImporterOpen = false
                                    Toast.makeText(context, "$sName added to project tracks.", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 14.sp)
                                Column {
                                    Text(sName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(sSpec, color = Color.White.copy(0.5f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("SELECT", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 3. Audio Cropper/Editor Waveform Dialog
        if (audioCropperOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { audioCropperOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0F0B18), RoundedCornerShape(16.dp))
                        .border(1.5.dp, HotViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AUDIO CROPPING WAVEFORM TOOL", color = HotViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { audioCropperOpen = false })
                    }

                    Text("Drag gates to prune the active sample duration.", color = Color.White.copy(0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    // Simulated Cropper Area with left/right seek gates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, HotViolet, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            val w = size.width
                            val h = size.height
                            val lines = 36
                            val lineGap = w / lines

                            for (i in 0 until lines) {
                                val valSin = sin(i * 0.25f) * cos(i * 0.12f)
                                val finalLineHeight = (h * 0.64f) * abs(valSin)

                                drawLine(
                                    color = NeonCyan,
                                    start = Offset(i * lineGap, (h - finalLineHeight) / 2f),
                                    end = Offset(i * lineGap, (h + finalLineHeight) / 2f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        // Left select crop handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(HotViolet)
                                .align(Alignment.CenterStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", color = Color.White, fontSize = 6.sp)
                        }

                        // Right select crop handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(HotViolet)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, fontSize = 6.sp)
                        }
                    }

                    Button(
                        onClick = {
                            audioCropperOpen = false
                            viewModel.addNotification("Audio file cropped successfully.")
                            Toast.makeText(context, "Pruned sample successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE CROP SELECTION", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 4. Vocal Splitter / Karaoke processor dialogue
        if (karaokeSplitterOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { karaokeSplitterOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var separatorProgress by remember { mutableStateOf(0f) }
                var separatingState by remember { mutableStateOf("IDLE") }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF070912), RoundedCornerShape(16.dp))
                        .border(1.5.dp, MintGreen, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI MULTI-STEM SPLITTER", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { karaokeSplitterOpen = false })
                    }

                    Text("Process current track into independent Vocal, Beat, Bass, and Synth stems using DSP separations models.", color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    if (separatingState == "PROCESSING") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(progress = separatorProgress, color = MintGreen, modifier = Modifier.fillMaxWidth())
                            Text("Neural Stem Isolations: ${(separatorProgress * 100).toInt()}% completed", color = MintGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else if (separatingState == "DONE") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.04f)).padding(8.dp)) {
                                Text("Vocal Stem: ISOLATED (24-bit WAV)\nBeat Drum Stem: ISOLATED (24-bit WAV)\nMelodics Pad: ISOLATED (24-bit WAV)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (separatingState == "IDLE") {
                                separatingState = "PROCESSING"
                                coroutineScope.launch {
                                    for (p in 1..100) {
                                        delay(25)
                                        separatorProgress = p.toFloat() / 100f
                                    }
                                    separatingState = "DONE"
                                    viewModel.addNotification("Karaoke vocals stem extraction built successfully.")
                                }
                            } else if (separatingState == "DONE") {
                                karaokeSplitterOpen = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (separatingState == "IDLE") "START STEM EXTRACTION" else if (separatingState == "PROCESSING") "PROCESSING NEURAL MASKS..." else "IMPORT SPLIT TRACKS",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. AI Music progressions composer
        if (aiMusicComposerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { aiMusicComposerOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var outputChords by remember { mutableStateOf("Click COMPOSE to generate chord models...") }
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF131521), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GoldenAmber, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI SONGWRITING & CHORDS MATRIX", color = GoldenAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { aiMusicComposerOpen = false })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(outputChords, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                outputChords = "AI Output Matrix (Key: F# Minor)\nProgression: F#m - Dmaj7 - Amaj - E5\nTempo Match: 114 BPM\nSynthesizer patches populated."
                                viewModel.addNotification("AI Progressive chord nodes built.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("COMPOSE PROGRESSION", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                aiMusicComposerOpen = false
                                viewModel.setCurrentTab("TOOLS")
                                viewModel.setSelectedToolId("SYNTH")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassCardBg),
                            modifier = Modifier.border(0.5.dp, GoldenAmber, RoundedCornerShape(50))
                        ) {
                            Text("PLAY KEYS", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 6. Audio Repair & gate de-noiser popup
        if (audioRepairOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { audioRepairOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var clickRemoval by remember { mutableStateOf(true) }
                var hissRemoval by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF090C15), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI DE-NOSER & AUDIO REPAIR", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { audioRepairOpen = false })
                    }

                    Text("Attenuate background room air hiss, AC hums, and microphone crackles automatically.", color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    ListItem(
                        headlineContent = { Text("Attenuate Transient Click Artifacts", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text("Detect and mute pops and crackles", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                        trailingContent = { Checkbox(checked = clickRemoval, onCheckedChange = { clickRemoval = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text("Absolute Squelch Noise Gate Hiss", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text("Attenuates constant hum background air", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                        trailingContent = { Checkbox(checked = hissRemoval, onCheckedChange = { hissRemoval = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    Button(
                        onClick = {
                            audioRepairOpen = false
                            viewModel.addNotification("Audio Repair processing complete.")
                            Toast.makeText(context, "Denoised and declicked wave sample successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("APPLY CO-PROCESSED MASTER FILTERS", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 7. HD Recorder advanced settings configuration overlay page
        if (advancedRecorderSettingsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { advancedRecorderSettingsOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var selectedFormat by remember { mutableStateOf("WAV") }
                var selectedSampleRate by remember { mutableStateOf("96kHz") }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0F0F1A), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("HD AUDIO RECODER SPECIFICATIONS", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { advancedRecorderSettingsOpen = false })
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Format:", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WAV", "FLAC", "MP3").forEach { fmt ->
                                val active = selectedFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) NeonCyan else GlassCardBg)
                                        .clickable { selectedFormat = fmt }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(fmt, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sample Rate:", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("44.1kHz", "48kHz", "96kHz").forEach { rate ->
                                val active = selectedSampleRate == rate
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) NeonCyan else GlassCardBg)
                                        .clickable { selectedSampleRate = rate }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(rate, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            advancedRecorderSettingsOpen = false
                            viewModel.addNotification("HD Recorder settings locked: WAV @ 96kHz.")
                            Toast.makeText(context, "Configurations loaded to audio recorder module.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LOCK RECORDING HARDWARE GRID", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenViewDuplicate_NotUsed(viewModel: SoundLabViewModel) {
    val activeProject by viewModel.selectedProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    
    // UI Local Stateful overlays for advanced DAW Tools
    var visSettingsOpen by remember { mutableStateOf(false) }
    var visDecayPeriod by remember { mutableStateOf(0.7f) }
    var visScaleDb by remember { mutableStateOf(48f) }
    
    var fileImporterOpen by remember { mutableStateOf(false) }
    var audioCropperOpen by remember { mutableStateOf(false) }
    var karaokeSplitterOpen by remember { mutableStateOf(false) }
    var aiMusicComposerOpen by remember { mutableStateOf(false) }
    var audioRepairOpen by remember { mutableStateOf(false) }
    var advancedRecorderSettingsOpen by remember { mutableStateOf(false) }
    
    // AI Assistant inputs & simulated output console
    var aiAssistantInput by remember { mutableStateOf("") }
    var aiAssistantOutput by remember { mutableStateOf("Co-Creator Ready. Choose a fast command below or type details to orchestrate models.") }
    var aiIsThinking by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Infinite animation loops for visualization dynamics
    val transition = rememberInfiniteTransition(label = "daw_oscilloscope")
    val sweepFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "oscilloscopeSweep"
    )
    
    val bounceFloat by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscilloscopeBounce"
    )

    // Layout Root Container with Floating Record FAB overlaid
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceObsidian)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_scrollable_container"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // Section 1: Dynamic Greeting & Audio Tip Panel
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0x0E7B2CBF), Color(0x0300E5FF))))
                        .border(1.dp, GlassBorder.copy(0.18f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(ElectricIndigo, HotViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Achmad Tohirin • Producer Console",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MASTERING TIP: Saturating the 16kHz vocal high-end band with 12ms delay feedbacks generates gorgeous digital air. Try custom EQ profiles below.",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ==========================================
            // Section 2: Realtime Live Audio Visualization Deck
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LIVE AUDIO VISUALIZER (TAP ANALYZER // LONG-PRESS CFG)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF070912))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        // Tap opens the analyzer tab tool
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("ANALYZER")
                                        Toast.makeText(context, "Navigated to Analyzer diagnostic deck.", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongPress = {
                                        visSettingsOpen = true
                                    }
                                )
                            }
                    ) {
                        // Drawing grid lines, FFT spectrum columns, VU led lights, and Soundwave oscillographs
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle horizontal grid ticks (Classic DAW cathode scopes look)
                            val gridLines = 5
                            val gridStep = h / gridLines
                            for (i in 0 until gridLines) {
                                drawLine(
                                    color = Color.White.copy(0.04f),
                                    start = Offset(0f, i * gridStep),
                                    end = Offset(w, i * gridStep),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // 1. Draw Simulated 16-Band FFT Columns
                            val barCount = 18
                            val barGap = 6.dp.toPx()
                            val totalGapWidth = barGap * (barCount - 1)
                            val barWidth = (w * 0.7f - totalGapWidth) / barCount

                            for (i in 0 until barCount) {
                                val amplitudeMultiplier = sin(sweepFloat + (i * 0.45f)) * 0.35f + 0.55f
                                val barHeight = h * 0.75f * amplitudeMultiplier * bounceFloat
                                val startX = i * (barWidth + barGap)

                                // Dual-color glowing metallic gradient for columns
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(HotViolet, NeonCyan)
                                    ),
                                    topLeft = Offset(startX, h - barHeight),
                                    size = Size(barWidth, barHeight),
                                    alpha = 0.82f
                                )
                            }

                            // 2. Draw Simulated Stereo VU Level Meter (L / R Channel)
                            val vuStartX = w * 0.75f
                            val vuWidth = 12.dp.toPx()
                            val segmentCount = 10
                            val segmentHeight = (h * 0.85f) / segmentCount
                            val segmentGap = 2.dp.toPx()

                            for (ch in 0..1) { // Left = 0, Right = 1
                                val currentChStartX = vuStartX + ch * (vuWidth + 14.dp.toPx())
                                val chRandomAmp = if (ch == 0) bounceFloat else (bounceFloat * 0.9f + 0.05f)

                                for (s in 0 until segmentCount) {
                                    val isLit = (segmentCount - s).toFloat() / segmentCount <= chRandomAmp
                                    val segmentColor = when {
                                        s < 2 -> Color.Red // peaks
                                        s < 5 -> GoldenAmber // high warnings
                                        else -> MintGreen // normal safety levels
                                    }
                                    val segmentAlpha = if (isLit) 0.95f else 0.08f

                                    drawRoundRect(
                                        color = segmentColor,
                                        topLeft = Offset(currentChStartX, s * (segmentHeight + segmentGap)),
                                        size = Size(vuWidth, segmentHeight),
                                        cornerRadius = CornerRadius(1.dp.toPx()),
                                        alpha = segmentAlpha
                                    )
                                }
                            }

                            // 3. Draw overlay continuous Waveform Line
                            val points = 64
                            val path = androidx.compose.ui.graphics.Path()
                            val waveWidth = w * 0.7f
                            val waveYCenter = h * 0.4f

                            for (p in 0..points) {
                                val x = (p.toFloat() / points) * waveWidth
                                val y = waveYCenter + sin(sweepFloat + (p * 0.35f)) * 14.dp.toPx() * bounceFloat

                                if (p == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = NeonCyan,
                                style = Stroke(width = 2.dp.toPx()),
                                alpha = 0.9f
                            )
                        }

                        // Top overlays displaying real-time parameters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(Color.Black.copy(0.4f))
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DECAY: ${(visDecayPeriod * 100).toInt()}% • SCALE: ${visScaleDb.toInt()}dB SPL",
                                color = Color.White.copy(0.55f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MintGreen)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // Section 3: Quick Studio Tools (Haptic Grid of 10)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK STUDIO TOOLS (TOUCH RESPONSIVE CORES)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // 10 buttons aligned inside a premium multi-item row grid
                    val toolsList = listOf(
                        Quadruple("RECORD", "🎙️", { viewModel.startRecording() }, NeonCyan),
                        Quadruple("IMPORT", "📥", { fileImporterOpen = true }, NeonCyan),
                        Quadruple("EQ", "🎛️", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("EQUALIZER")
                        }, ElectricIndigo),
                        Quadruple("AI MASTER", "🪄", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("AI_COCREATOR")
                        }, ElectricIndigo),
                        Quadruple("MIXER", "🎚️", {
                            viewModel.setCurrentTab("TOOLS")
                            viewModel.setSelectedToolId("MIXER")
                        }, HotViolet),
                        Quadruple("EDITOR", "✂️", { audioCropperOpen = true }, HotViolet),
                        Quadruple("KARAOKE", "🎤", { karaokeSplitterOpen = true }, MintGreen),
                        Quadruple("PODCAST", "🗣️", {
                            // Apply broadcast podcast preset and notify
                            viewModel.savePreset("Broadcast Vocal")
                            viewModel.addNotification("Podcast vocal preset applied instantly.")
                            Toast.makeText(context, "Applied broadcast profile preset to Equalizer.", Toast.LENGTH_SHORT).show()
                        }, MintGreen),
                        Quadruple("AI MUSIC", "🎵", { aiMusicComposerOpen = true }, GoldenAmber),
                        Quadruple("REPAIR", "🩹", { audioRepairOpen = true }, GoldenAmber)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 5,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        toolsList.forEach { tool ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.2f), RoundedCornerShape(10.dp))
                                    .clickable { tool.action() }
                                    .testTag("quick_studio_tool_${tool.label}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(tool.unicode, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tool.label,
                                        color = tool.color,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 4: Continue Project (Focus Hub)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONTINUE RECENT WORKSPACE SESSION",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Target last project dynamically or show placeholder
                    val focalProj = allProjects.firstOrNull() ?: AudioProjectEntity(
                        id = 9999,
                        title = "Atmospheric Void Intro Synth",
                        bpm = 114,
                        keySignature = "F# Minor",
                        notes = "Analog saturation model tracks with heavy multi-stage tape delays.",
                        lyrics = "Silent soundwaves breathing inside the electronic neon void..."
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.radialGradient(listOf(Color(0x2A131521), Color(0xFF04060B))))
                            .border(1.5.dp, HotViolet.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .background(HotViolet.copy(0.18f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LAST WORKSPACE", color = HotViolet, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = focalProj.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "⏱️ ${focalProj.bpm} BPM // ${focalProj.keySignature}",
                                color = GoldenAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Simulated Soundwave artwork inside focal session
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.02f))
                                .border(0.5.dp, GlassBorder.copy(0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                                val w = size.width
                                val h = size.height
                                val lines = 44
                                val lineGap = w / lines

                                for (i in 0 until lines) {
                                    val valSin = sin(i * 0.18f) * cos(i * 0.4f)
                                    val finalLineHeight = (h * 0.8f) * abs(valSin)

                                    drawLine(
                                        color = if (i < lines * 0.35f) HotViolet else NeonCyan.copy(0.5f),
                                        start = Offset(i * lineGap, (h - finalLineHeight) / 2f),
                                        end = Offset(i * lineGap, (h + finalLineHeight) / 2f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved: " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(focalProj.lastSaved)),
                                color = Color.White.copy(0.4f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Delete/Discard button
                                Button(
                                    onClick = {
                                        if (allProjects.isNotEmpty() && focalProj.id != 9999) {
                                            viewModel.deleteProject(focalProj.id)
                                            Toast.makeText(context, "Workspace discarded: ${focalProj.title}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Cannot discard system core templates.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.12f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).border(0.5.dp, Color.Red.copy(0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Text("DISCARD", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                // Duplicate/Clone button
                                Button(
                                    onClick = {
                                        viewModel.createNewProject(focalProj.title + " (Copy)")
                                        Toast.makeText(context, "Project duplicated in Database.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassCardBg),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).border(0.5.dp, GlassBorder.copy(0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Text("CLONE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                // Continue/Edit button
                                Button(
                                    onClick = {
                                        viewModel.selectProject(focalProj)
                                        viewModel.setCurrentTab("TOOLS")
                                        viewModel.setSelectedToolId("MIXER")
                                        Toast.makeText(context, "Loaded DAW: ${focalProj.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("EDIT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 5: AI sound assistant card
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI CO-CREATOR STUDIO COMMAND CENTER",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F0B18))
                            .border(1.5.dp, HotViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Blinking AI core avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.radialGradient(listOf(HotViolet.copy(0.5f), Color.Transparent)))
                                    .border(1.dp, HotViolet, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔮", fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                            }

                            Column {
                                Text("AI SOUND ASSISTANT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("CO-CREATION ENGINES ACTIVE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // AI Console output
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(0.35f))
                                .padding(8.dp)
                        ) {
                            if (aiIsThinking) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = NeonCyan)
                                    Text("Synthesizing neural audio parameters...", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                Text(
                                    text = aiAssistantOutput,
                                    color = Color.White.copy(0.85f),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Text input & Voice mic button row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = aiAssistantInput,
                                onValueChange = { aiAssistantInput = it },
                                placeholder = { Text("Ask Gemini to isolate sound, write chords, split bass...", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("ai_prompt_text_field"),
                                textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HotViolet,
                                    unfocusedBorderColor = GlassBorder.copy(0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Voice dictation microphone button
                            IconButton(
                                onClick = {
                                    aiAssistantInput = "Record broadcast profile mic vocal track"
                                    Toast.makeText(context, "Voice dictation audio recorded.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(HotViolet.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, HotViolet.copy(0.4f), RoundedCornerShape(10.dp))
                                    .testTag("voice_prompt_mic_button")
                            ) {
                                Text("🎙️", fontSize = 16.sp)
                            }

                            // Run button
                            Button(
                                onClick = {
                                    if (aiAssistantInput.isNotEmpty()) {
                                        keyboardController?.hide()
                                        aiIsThinking = true
                                        aiAssistantOutput = ""
                                        // Execute custom mock response based on input
                                        val prompt = aiAssistantInput
                                        aiAssistantInput = ""
                                        viewModel.viewModelScope.launch {
                                            delay(1500)
                                            aiIsThinking = false
                                            aiAssistantOutput = when {
                                                prompt.contains("Vocal", ignoreCase = true) || prompt.contains("vocal", ignoreCase = true) ->
                                                    "AI Separation: Vocal track successfully isolated from 'Beat Stem 1'. Separation fidelity is computed at 98.4dB SNR."
                                                prompt.contains("Chords", ignoreCase = true) || prompt.contains("chords", ignoreCase = true) ->
                                                    "AI Chords Composer: Generated (i - VI - III - VII) progression in F# Minor. Playback mapped to analog synth module."
                                                prompt.contains("Master", ignoreCase = true) || prompt.contains("master", ignoreCase = true) ->
                                                    "AI Mastering Suite: Set graphic EQ decay parameters. Added +2.4dB tube compression saturation at 250Hz. Lows gated."
                                                else -> "AI Engine: Executed pipeline for the requested action successfully. Equalizer registers configured."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("RUN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Presets Task pills (Interactive pills)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pills = listOf(
                                "Remove Vocal" to "Isolating vocal stems...",
                                "Master This Song" to "Applying heavy solid-state tube limiter and analog EQ outputs...",
                                "Clean My Voice" to "Noise Gate threshold mapped to -45dB. Attenuating hiss values...",
                                "Generate Beat" to "Generated 118 BPM deep ambient house grid...",
                                "Fix Noise" to "Attenuating clicks, high-frequency hiss, and low-end AC rumble..."
                            )

                            pills.forEach { (label, actionResponse) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(0.04f))
                                        .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(50))
                                        .clickable {
                                            aiIsThinking = true
                                            coroutineScope.launch {
                                                delay(1200)
                                                aiIsThinking = false
                                                aiAssistantOutput = "AI Suite: $actionResponse"
                                            }
                                        }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = NeonCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 6: Recent Projects Carousel (Unbounded Horizontal Scrolling)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RECENT PROJECTS (UNLIMITED HORIZONTAL CAROUSEL)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    if (allProjects.isEmpty()) {
                        // Empty state indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassCardBg)
                                .border(1.dp, GlassBorder.copy(0.12f), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.createNewProject("New Atmospheric Track")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📂 No tracks inside DB workspace.", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("CLICK TO GENERATE FIRST DAW TEMPLATE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allProjects) { proj ->
                                Column(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassCardBg)
                                        .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectProject(proj)
                                            viewModel.setCurrentTab("TOOLS")
                                            viewModel.setSelectedToolId("MIXER")
                                            Toast.makeText(context, "Loaded: ${proj.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = proj.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("☁️", fontSize = 8.sp)
                                            Text("⭐️", fontSize = 8.sp, color = GoldenAmber)
                                        }
                                    }

                                    // Minimal wave shape inside card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(0.01f))
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val steps = 18

                                            for (k in 0 until steps) {
                                                val hFraction = sin((k + proj.id) * 0.45f) * cos((k + proj.id) * 0.9f)
                                                val barH = h * 0.7f * abs(hFraction)
                                                drawRect(
                                                    color = if (proj.id % 2 == 0) NeonCyan.copy(0.6f) else HotViolet.copy(0.6f),
                                                    topLeft = Offset(k * (w / steps), (h - barH) / 2f),
                                                    size = Size((w / steps) * 0.7f, barH)
                                                )
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${proj.bpm} BPM // ${proj.keySignature}", color = Color.White.copy(0.5f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                        Box(
                                            modifier = Modifier
                                                .background(HotViolet.copy(0.12f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("STUDIO", color = HotViolet, fontSize = 6.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 7: AI Suggestions (Category Filtering Chips)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI CLASSIFICATION CHIPS",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val suggestionModels = listOf(
                            "Offline AI" to MintGreen,
                            "Cloud AI" to NeonCyan,
                            "Trending AI" to HotViolet,
                            "Recommended AI" to GoldenAmber,
                            "Recently Used" to Color.White
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(suggestionModels) { (lbl, clr) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(0.04f))
                                        .border(0.5.dp, clr.copy(0.35f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.addNotification("AI profile matched to: $lbl")
                                            Toast.makeText(context, "$lbl module selected.", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(50)).background(clr))
                                        Text(lbl, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 8: Favorite Presets Carousel (Instant EQ Modifiers)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FAVORITE HARDWARE PRESETS (TAP TO APPLY EQ MIX)",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Standard presets target lists
                    val favoritePresets = listOf(
                        Triple("Bass Boost", "Solid low-end sub boost (+6dB @ 60Hz)", listOf(6f, 4f, 2f, 0f, 0f, 0f, 1f, 2f, 3f, 4f)),
                        Triple("Podcast", "Vocal clarity emphasis mid-range", listOf(-2f, -1f, 1f, 4f, 5f, 3f, 2f, 1f, 3f, 1f)),
                        Triple("Studio Vocal", "High air dynamic compression", listOf(1f, 2f, 1f, 2f, 4f, 5f, 5f, 6f, 8f, 7f)),
                        Triple("Live Streaming", "Broad-stage ambient gating", listOf(3f, 3f, 2f, 1f, 2f, 3f, 4f, 5f, 4f, 3f)),
                        Triple("Gaming", "Expanded surround localization", listOf(5f, 2f, -1f, -1f, 1f, 3f, 4f, 5f, 6f, 8f)),
                        Triple("Car Audio", "Punchy solid acoustics limiter", listOf(6f, 6f, 4f, 2f, 0f, 1f, 2f, 3f, 4f, 6f)),
                        Triple("Headphones", "Attenuated harsh-high parameters", listOf(1f, 1f, 2f, 2f, 0f, -1f, -2f, -3f, 0f, 2f))
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoritePresets) { (name, bDesc, eqLevels) ->
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, NeonCyan.copy(0.18f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        // Update viewmodel EQ bands dynamically
                                        for (i in 0 until 10) {
                                            viewModel.setEqBandLevel(i, eqLevels[i])
                                        }
                                        viewModel.addNotification("$name physical EQ settings loaded.")
                                        Toast.makeText(context, "Loaded EQ Target Mix: $name", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(name, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(bDesc, color = Color.White.copy(0.45f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, lineHeight = 9.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // High visual EQ curve sparkline inside preset
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .background(Color.Black.copy(0.2f), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cW = size.width
                                        val cH = size.height
                                        val eStep = cW / 10

                                        for (e in 0 until 10) {
                                            val eqFraction = (eqLevels[e] + 12f) / 24f // Map -12..12 DB to scale 0..1
                                            val eqLineH = cH * eqFraction
                                            drawRect(
                                                color = HotViolet,
                                                topLeft = Offset(e * eStep, cH - eqLineH),
                                                size = Size(eStep * 0.7f, eqLineH)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 9: Recent Audio Files
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "IMPORTED AUDIO LIBRARY",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val audioLibrary = listOf(
                        Triple("vocal_lead_raw.wav", "00:44", "PCM 24-bit 96kHz Stereo"),
                        Triple("synthesizer_poly_pad.wav", "02:18", "PCM 24-bit 96kHz Stereo"),
                        Triple("acoustic_drum_break_120.wav", "00:08", "PCM 24-bit 96kHz Mono"),
                        Triple("recorded_ambient_hiss.wav", "01:10", "MP3 320kbps Stereo")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        audioLibrary.forEach { (aName, aDur, aSpec) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.12f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎵", fontSize = 16.sp)
                                    Column {
                                        Text(aName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("$aDur • $aSpec", color = Color.White.copy(0.4f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Open file
                                    IconButton(
                                        onClick = {
                                            viewModel.addNotification("Loaded $aName as Active Mastering Input.")
                                            Toast.makeText(context, "$aName selected.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp).background(Color.White.copy(0.04f), RoundedCornerShape(50))
                                    ) {
                                        Text("▶", fontSize = 10.sp, color = NeonCyan)
                                    }

                                    // Share Node file
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "System share callback generated for: $aName", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp).background(Color.White.copy(0.04f), RoundedCornerShape(50))
                                    ) {
                                        Text("📤", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Section 10: Learning Center Cards
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LEARNING CENTER",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val tutorials = listOf(
                        Triple("PRO AUDIO MASTERING SECRETS", "Unlock the hidden solid-state saturator algorithms to double vocal loudness safely.", "LEARN PRO MASTER"),
                        Triple("OPTIMIZING 10-BAND EQ PARAMETERS", "Visual guide mapping raw frequency spectra to professional car and headphone speakers.", "VIEW EQ GUIDE"),
                        Triple("AI CREATIVE LYRICS GENERATORS", "How to trigger Gemini LLMs inside our lyrics generation matrix effectively.", "OPEN GUIDE")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tutorials) { (titleText, descText, actionLabel) ->
                            Column(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassCardBg)
                                    .border(1.dp, GlassBorder.copy(0.24f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(titleText, color = GoldenAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(descText, color = Color.White.copy(0.6f), fontSize = 9.sp, lineHeight = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.addNotification("Tutorial loaded: $titleText")
                                        Toast.makeText(context, "Redirecting to $titleText", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(actionLabel, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Margin bottom to allow scroll clearance above PersistentMiniPlayer
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        // ==========================================
        // Section 11: Heartbeat-pulsing Red Record FAB
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
        ) {
            val recordActive by viewModel.isRecordingActive.collectAsState()
            
            // Pulse glow size animation
            val fabTransition = rememberInfiniteTransition(label = "fab_pulse")
            val pulseScale by fabTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fabScale"
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer(
                        scaleX = if (recordActive) 1.0f else pulseScale,
                        scaleY = if (recordActive) 1.0f else pulseScale
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Red,
                                if (recordActive) Color.Red.copy(0.4f) else Color.Red.copy(0.12f)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.startRecording()
                            },
                            onLongPress = {
                                // Save a fast Voice Memo directly
                                val name = "Voice_Memo_" + System.currentTimeMillis() / 1000
                                viewModel.startRecording()
                                viewModel.viewModelScope.launch {
                                    delay(2000)
                                    viewModel.stopAndSaveRecording(name)
                                    Toast.makeText(context, "Voice Memo saved automatically: $name", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDoubleTap = {
                                advancedRecorderSettingsOpen = true
                            }
                        )
                    }
                    .testTag("floating_live_audio_record_fab"),
                contentAlignment = Alignment.Center
            ) {
                // Glow boundary circles
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF04060B))
                        .border(2.dp, Color.Red, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔴", fontSize = 18.sp)
                }
            }
        }

        // ==========================================
        // LOCAL STATE MODALS & TELEMETRY DIALOGS
        // ==========================================

        // 1. Interactive Visualization settings overlay panel
        if (visSettingsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.8f))
                    .clickable { visSettingsOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0A0C16), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("VISUALIZATION ENGINE VALUES", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { visSettingsOpen = false })
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Decay Coefficient: ${(visDecayPeriod * 100).toInt()}%", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = visDecayPeriod,
                            onValueChange = { visDecayPeriod = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("FFT Scale Power: ${visScaleDb.toInt()} dB SPL", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Slider(
                            value = visScaleDb,
                            onValueChange = { visScaleDb = it },
                            valueRange = 12f..96f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Button(
                        onClick = { visSettingsOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("APPLY PHYSICAL GRID EFFECTS", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Beautiful File Importer Dialog
        if (fileImporterOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.8f))
                    .clickable { fileImporterOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF090B12), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SELECT AUDIO SOURCE FILE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { fileImporterOpen = false })
                    }

                    val samples = listOf(
                        "guitar_strum_clean_E.wav" to "00:15 • 24-bit PCM",
                        "rhythm_trap_drums_808.wav" to "00:48 • 24-bit PCM",
                        "synth_wave_space_intro.wav" to "02:04 • 24-bit PCM",
                        "ambient_rain_sound_effect.mp3" to "01:30 • 320kbps MP3"
                    )

                    samples.forEach { (sName, sSpec) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.02f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, GlassBorder.copy(0.12f), RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.addNotification("Imported $sName successfully.")
                                    fileImporterOpen = false
                                    Toast.makeText(context, "$sName added to project tracks.", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 14.sp)
                                Column {
                                    Text(sName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(sSpec, color = Color.White.copy(0.5f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("SELECT", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 3. Audio Cropper/Editor Waveform Dialog
        if (audioCropperOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { audioCropperOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0F0B18), RoundedCornerShape(16.dp))
                        .border(1.5.dp, HotViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AUDIO CROPPING WAVEFORM TOOL", color = HotViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { audioCropperOpen = false })
                    }

                    Text("Drag gates to prune the active sample duration.", color = Color.White.copy(0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    // Simulated Cropper Area with left/right seek gates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, HotViolet, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            val w = size.width
                            val h = size.height
                            val lines = 36
                            val lineGap = w / lines

                            for (i in 0 until lines) {
                                val valSin = sin(i * 0.25f) * cos(i * 0.12f)
                                val finalLineHeight = (h * 0.64f) * abs(valSin)

                                drawLine(
                                    color = NeonCyan,
                                    start = Offset(i * lineGap, (h - finalLineHeight) / 2f),
                                    end = Offset(i * lineGap, (h + finalLineHeight) / 2f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        // Left select crop handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(HotViolet)
                                .align(Alignment.CenterStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", color = Color.White, fontSize = 6.sp)
                        }

                        // Right select crop handle
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(HotViolet)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, fontSize = 6.sp)
                        }
                    }

                    Button(
                        onClick = {
                            audioCropperOpen = false
                            viewModel.addNotification("Audio file cropped successfully.")
                            Toast.makeText(context, "Pruned sample successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE CROP SELECTION", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 4. Vocal Splitter / Karaoke processor dialogue
        if (karaokeSplitterOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { karaokeSplitterOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var separatorProgress by remember { mutableStateOf(0f) }
                var separatingState by remember { mutableStateOf("IDLE") }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF070912), RoundedCornerShape(16.dp))
                        .border(1.5.dp, MintGreen, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI MULTI-STEM SPLITTER", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { karaokeSplitterOpen = false })
                    }

                    Text("Process current track into independent Vocal, Beat, Bass, and Synth stems using DSP separations models.", color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    if (separatingState == "PROCESSING") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(progress = separatorProgress, color = MintGreen, modifier = Modifier.fillMaxWidth())
                            Text("Neural Stem Isolations: ${(separatorProgress * 100).toInt()}% completed", color = MintGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else if (separatingState == "DONE") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.04f)).padding(8.dp)) {
                                Text("Vocal Stem: ISOLATED (24-bit WAV)\nBeat Drum Stem: ISOLATED (24-bit WAV)\nMelodics Pad: ISOLATED (24-bit WAV)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (separatingState == "IDLE") {
                                separatingState = "PROCESSING"
                                viewModel.viewModelScope.launch {
                                    for (p in 1..100) {
                                        delay(25)
                                        separatorProgress = p.toFloat() / 100f
                                    }
                                    separatingState = "DONE"
                                    viewModel.addNotification("Karaoke vocals stem extraction built successfully.")
                                }
                            } else if (separatingState == "DONE") {
                                karaokeSplitterOpen = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (separatingState == "IDLE") "START STEM EXTRACTION" else if (separatingState == "PROCESSING") "PROCESSING NEURAL MASKS..." else "IMPORT SPLIT TRACKS",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. AI Music progressions composer
        if (aiMusicComposerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { aiMusicComposerOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var outputChords by remember { mutableStateOf("Click COMPOSE to generate chord models...") }
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF131521), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GoldenAmber, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI SONGWRITING & CHORDS MATRIX", color = GoldenAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { aiMusicComposerOpen = false })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(outputChords, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                outputChords = "AI Output Matrix (Key: F# Minor)\nProgression: F#m - Dmaj7 - Amaj - E5\nTempo Match: 114 BPM\nSynthesizer patches populated."
                                viewModel.addNotification("AI Progressive chord nodes built.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("COMPOSE PROGRESSION", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                aiMusicComposerOpen = false
                                viewModel.setCurrentTab("TOOLS")
                                viewModel.setSelectedToolId("SYNTH")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassCardBg),
                            modifier = Modifier.border(0.5.dp, GoldenAmber, RoundedCornerShape(50))
                        ) {
                            Text("PLAY KEYS", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 6. Audio Repair & gate de-noiser popup
        if (audioRepairOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { audioRepairOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var clickRemoval by remember { mutableStateOf(true) }
                var hissRemoval by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF090C15), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI DE-NOSER & AUDIO REPAIR", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { audioRepairOpen = false })
                    }

                    Text("Attenuate background room air hiss, AC hums, and microphone crackles automatically.", color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                    ListItem(
                        headlineContent = { Text("Attenuate Transient Click Artifacts", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text("Detect and mute pops and crackles", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                        trailingContent = { Checkbox(checked = clickRemoval, onCheckedChange = { clickRemoval = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text("Absolute Squelch Noise Gate Hiss", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text("Attenuates constant hum background air", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                        trailingContent = { Checkbox(checked = hissRemoval, onCheckedChange = { hissRemoval = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    Button(
                        onClick = {
                            audioRepairOpen = false
                            viewModel.addNotification("Audio Repair processing complete.")
                            Toast.makeText(context, "Denoised and declicked wave sample successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("APPLY CO-PROCESSED MASTER FILTERS", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 7. HD Recorder advanced settings configuration overlay page
        if (advancedRecorderSettingsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.85f))
                    .clickable { advancedRecorderSettingsOpen = false },
                contentAlignment = Alignment.Center
            ) {
                var selectedFormat by remember { mutableStateOf("WAV") }
                var selectedSampleRate by remember { mutableStateOf("96kHz") }

                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color(0xFF0F0F1A), RoundedCornerShape(16.dp))
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { /* Block dismiss */ }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("HD AUDIO RECODER SPECIFICATIONS", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❌", color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { advancedRecorderSettingsOpen = false })
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Format:", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WAV", "FLAC", "MP3").forEach { fmt ->
                                val active = selectedFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) NeonCyan else GlassCardBg)
                                        .clickable { selectedFormat = fmt }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(fmt, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sample Rate:", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("44.1kHz", "48kHz", "96kHz").forEach { rate ->
                                val active = selectedSampleRate == rate
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) NeonCyan else GlassCardBg)
                                        .clickable { selectedSampleRate = rate }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(rate, color = if (active) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            advancedRecorderSettingsOpen = false
                            viewModel.addNotification("HD Recorder settings locked: WAV @ 96kHz.")
                            Toast.makeText(context, "Configurations loaded to audio recorder module.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LOCK RECORDING HARDWARE GRID", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom data container for tool keys
data class Quadruple<A, B, C, D>(
    val label: A,
    val unicode: B,
    val action: C,
    val color: D
)

@Composable
fun OldHomeScreenView(viewModel: SoundLabViewModel) {
    val activeProject by viewModel.selectedProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val timeString = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.getDefault()).format(Date(1781513332567L)) // UTC timestamp
    val hour = 2 // Simulated early morning Hour

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        // A. Interactive Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardBg, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder.copy(0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GOOD MORNING, CREATOR",
                        color = HotViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Achmad Tohirin",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Thin
                    )
                    Text(
                        text = "System time check: $timeString",
                        color = Color.White.copy(0.4f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.radialGradient(listOf(NeonCyan.copy(0.2f), Color.Transparent))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍🎤", fontSize = 20.sp)
                }
            }
        }

        // B. Active Current Session Card (Preserve Editor Link)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF0F1223), Color(0xFF0A0C16))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PREVALENT COCREATOR SESSION",
                        color = GoldenAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(modifier = Modifier.background(MintGreen.copy(0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ACTIVE CORE", color = MintGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Text(
                    text = activeProject?.title ?: "DASHBOARD SANDBOX MASTER",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "BPM Tracker: ${activeProject?.bpm ?: 120} | Custom Key Signature: ${activeProject?.keySignature ?: "C Major"}\nAssociated preset parameters: ${if (activeProject?.selectedPresetId ?: 0 > 0) "Custom Node Asset" else "Baseline Factory Analog"}",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Button(
                    onClick = {
                        viewModel.setCurrentTab("TOOLS")
                        viewModel.setSelectedToolId("MIXER") // Direct edit route!
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("continue_editing_home_button")
                ) {
                    Text("CONTINUE EDITING IN STUDIO", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        // C. Preset Fast-Loader list
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DYNAMIC ANALOG PRESETS",
                    color = Color.White.copy(0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allPresets) { preset ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .background(GlassCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorder.copy(0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.loadPresetToDsp(preset)
                                    viewModel.addNotification("Loaded Preset: ${preset.name}")
                                }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(preset.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(
                                text = "Overdrive: ${(preset.drive * 100).toInt()}%\nReverb: ${(preset.reverbDecay * 100).toInt()}%",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // D. Sound Lab Quick Actions Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RAPID CONTROL PANELS",
                    color = Color.White.copy(0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                val quickActions = listOf(
                    Triple("🎛️ Equalizer Tuning", "EQ RACK", "EQUALIZER"),
                    Triple("🎚️ Multi-band Mixer", "MIX RACK", "MIXER"),
                    Triple("🎹 Live VCO Synth Keys", "VCO SAND", "SYNTH"),
                    Triple("📊 FFT Waveform Scope", "ANALYSER", "ANALYZER"),
                    Triple("🤖 AI Mastering Node", "GEMINI ENG", "AI_COCREATOR"),
                    Triple("📂 Audio Sessions archive", "ROOM DB", "DB_PROJECTS")
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(quickActions) { (label, sub, toolId) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBg, RoundedCornerShape(10.dp))
                                .border(1.dp, GlassBorder.copy(0.3f), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setCurrentTab("TOOLS")
                                    viewModel.setSelectedToolId(toolId)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column {
                                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(sub, color = HotViolet, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // E. AI Suggestions & Audio Tips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.01f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder.copy(0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 SYSTEM INTELLIGENCE & DSP ADVICE",
                    color = GoldenAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Did you know that saturating your high-end vocals (16k band) with low delay feedbacks creates high-contrast, airy digital spacing? Ask the Gemini mastering advisor tab for instant multi-band configurations customized to your track's vibe.",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ==========================================
// 9. TAB AREA 2: PROJECTS MANAGEMENT
// ==========================================

@Composable
fun ProjectsScreenView(viewModel: SoundLabViewModel) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProject by viewModel.selectedProject.collectAsState()
    val isProjGridView by viewModel.isProjectGridView.collectAsState()
    val activeCategory by viewModel.selectedProjectCategory.collectAsState()
    val queryText by viewModel.searchQuery.collectAsState()

    val categories = listOf("Recent", "Favorites", "Cloud", "Local", "Shared", "Archived", "Deleted")

    // Filter projects based on query and category
    val filteredProjects = projects.filter {
        val matchesQuery = it.title.contains(queryText, ignoreCase = true) || it.lyrics.contains(queryText, ignoreCase = true)
        val matchesCategory = when (activeCategory) {
            "Recent" -> true
            "Favorites" -> it.id % 2 == 0 // Mock favorite index for demo persistence
            "Cloud" -> it.id % 3 == 0
            "Local" -> !it.isRecordingSample
            "Shared" -> false
            "Archived" -> false
            "Deleted" -> false
            else -> true
        }
        matchesQuery && matchesCategory
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Style Filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STORED PROJECTS ARCHIVE (${filteredProjects.size})",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            // Grid / List Toggler UI Button
            IconButton(
                onClick = { viewModel.toggleProjectGridView() },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(0.04f), RoundedCornerShape(50))
                    .testTag("layout_toggle_button")
            ) {
                Text(if (isProjGridView) "☰" else "▦", color = NeonCyan, fontSize = 14.sp)
            }
        }

        // Horizontal Category Tab selection
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSel = cat == activeCategory
                Box(
                    modifier = Modifier
                        .background(
                            if (isSel) ElectricIndigo.copy(0.4f) else Color.White.copy(0.03f),
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            if (isSel) NeonCyan else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .clickable { viewModel.setProjectCategory(cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("category_tab_$cat")
                ) {
                    Text(cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Project Cards list or grid
        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📂", fontSize = 36.sp)
                    Text("No records match the category parameters", color = Color.White.copy(0.4f), fontSize = 11.sp)
                }
            }
        } else {
            if (isProjGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(bottom = 120.dp)
                ) {
                    items(filteredProjects) { proj ->
                        val isCurrent = proj.id == activeProject?.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) ElectricIndigo.copy(0.2f) else GlassCardBg,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) NeonCyan else GlassBorder.copy(0.4f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectProject(proj) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(proj.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text("BPM: ${proj.bpm} | Key: ${proj.keySignature}", color = NeonCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (proj.isRecordingSample) "🎙️ RECS" else "🎹 DAW", color = Color.White.copy(0.4f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { viewModel.deleteProject(proj.id) }, modifier = Modifier.size(20.dp)) {
                                    Text("❌", fontSize = 7.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProjects) { proj ->
                        val isCurrent = proj.id == activeProject?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) ElectricIndigo.copy(0.2f) else GlassCardBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) NeonCyan else GlassBorder.copy(0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectProject(proj) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(proj.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("BPM: ${proj.bpm} | Key Signature: ${proj.keySignature}", color = NeonCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (proj.isRecordingSample) "🎙️ Clip" else "🎹 Grid", color = Color.White.copy(0.4f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { viewModel.deleteProject(proj.id) }, modifier = Modifier.size(24.dp).testTag("delete_proj_${proj.id}")) {
                                    Text("❌", fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. TAB AREA 3: PROFESSIONAL TOOLS
// ==========================================

@Composable
fun ToolsScreenView(viewModel: SoundLabViewModel) {
    val selectedToolId by viewModel.selectedToolId.collectAsState()

    if (selectedToolId != null) {
        // Render detailed focused subscreen with a back button
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.setSelectedToolId(null) },
                    modifier = Modifier.testTag("back_to_tools_grid_button")
                ) {
                    Text("← BACK TO TOOLS LIST", color = HotViolet, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedToolId) {
                    "EQUALIZER" -> TactileEqRack(viewModel)
                    "MIXER" -> DspMasterRack(viewModel)
                    "SYNTH" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { LiveVisualizerRack(viewModel) }
                        SynthesizerKeyboardController(viewModel)
                    }
                    "ANALYZER" -> Column {
                        LiveVisualizerRack(viewModel)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Live cathode oscilloscope grids track precise floating-point VCO waveforms. Play visual keys below or toggle the master generator to inspect stereo phasing values.",
                                color = Color.White.copy(0.6f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    "AI_COCREATOR" -> AiStudioAssistantPanel(viewModel)
                    "DB_PROJECTS" -> SessionArchivePanel(viewModel)
                }
            }
        }
    } else {
        // Standard interactive tools explorer categories grid
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
        ) {
            // Category A: Audio Utilities
            item {
                ToolsCategorySection(
                    title = "PROFESSIONAL AUDIO UTILITIES",
                    items = listOf(
                        Triple("Tactile EQ Desk", "Continuous 10-band parameters sliders", "EQUALIZER"),
                        Triple("Multi-channel Mixer", "Synthesized Vocals vs Beat gains", "MIXER"),
                        Triple("Digital VCO Synthesizer", "VCO live piano keys oscillator console", "SYNTH"),
                        Triple("Fourier FFT Analyzer", "Spectrograph and dual CRT Oscilloscopes", "ANALYZER")
                    ),
                    viewModel = viewModel
                )
            }

            // Category B: AI Sound Engineering
            item {
                ToolsCategorySection(
                    title = "AI ASSISTANTS & GENERATORS",
                    items = listOf(
                        Triple("Gemini AI Masters Advisor", "Auto dynamic settings advisor matrix", "AI_COCREATOR"),
                        Triple("Interactive Song Lyricist", "Vocal sheets and metrics chord builder", "AI_COCREATOR"),
                        Triple("Sample Voice Vocals isolator", "Algorithmic divider split tracks", "AI_COCREATOR")
                    ),
                    viewModel = viewModel
                )
            }

            // Category C: Utility Indexes
            item {
                ToolsCategorySection(
                    title = "METRIC BENCHMARKS & UTILITIES",
                    items = listOf(
                        Triple("System Project Archive", "Local SQLite database list nodes", "DB_PROJECTS"),
                        Triple("Plugins Mixer Extensions", "Add continuous latency buffers", "DB_PROJECTS")
                    ),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ToolsCategorySection(
    title: String,
    items: List<Triple<String, String, String>>,
    viewModel: SoundLabViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = NeonCyan,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(140.dp)
        ) {
            items(items) { (label, desc, toolId) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassCardBg, RoundedCornerShape(14.dp))
                        .border(1.dp, GlassBorder.copy(0.4f), RoundedCornerShape(14.dp))
                        .clickable { viewModel.setSelectedToolId(toolId) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = Color.White.copy(0.5f), fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ==========================================
// 11. TAB AREA 4: USER PROFILE SCREEN
// ==========================================

@Composable
fun ProfileScreenView(viewModel: SoundLabViewModel) {
    var devOptionToggle by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
    ) {
        // Standard User Header Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardBg, RoundedCornerShape(20.dp))
                    .border(2.dp, NeonCyan.copy(0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Achmad Tohirin", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("achmadtohirin123@gmail.com", color = Color.White.copy(0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.background(HotViolet.copy(0.18f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("PRO SOUND LAB ACCORD v1.4", color = HotViolet, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }

                Text("🥇", fontSize = 32.sp)
            }
        }

        // Horizontal billing / cloud charts
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardBg, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder.copy(0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cloud Sync Backup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("74.2 GB / 100 GB used", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(0.08f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.74f).background(NeonCyan)
                    )
                }
            }
        }

        // Appearance & Configuration Settings
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "HARDWARE & CONSOLES PREFERENCES",
                    color = Color.White.copy(0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                val prefs = listOf(
                    Pair("Audio Output Interface", "ASIO Stereo Core (Low Latency)"),
                    Pair("Buffer Block Size Engine", "256 samples (2.9ms feedback)"),
                    Pair("Workspace Core Theme", "Cyber Deep Obsidian Glassmorphism"),
                    Pair("Global Language Locale", "English International (US-UTC)")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    prefs.forEach { (label, stateVal) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBg, RoundedCornerShape(10.dp))
                                .border(1.dp, GlassBorder.copy(0.2f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.addNotification("Configured: $label") }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp)
                            Text(stateVal, color = GoldenAmber, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Developer Settings Toggle View
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardBg, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ASIO Developer Core Options", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Show analytical low-level latency measurements", color = Color.White.copy(0.4f), fontSize = 8.5.sp)
                    }

                    Switch(
                        checked = devOptionToggle,
                        onCheckedChange = { devOptionToggle = it },
                        modifier = Modifier.testTag("dev_options_toggle")
                    )
                }

                if (devOptionToggle) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Core Engine: 32-Bit Dual Float Pipeline", color = MintGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Local SQLite Version: SQLite-3.41-Session", color = MintGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Active REST Endpoint: api.gemini.ai-v1beta", color = MintGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Button(
                            onClick = {
                                viewModel.createNewProject("Prefilled Benchmark Project")
                                viewModel.addNotification("Spawned system performance index stats")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("RUN INTERNALS BENCHMARK", color = Color.White, fontSize = 8.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 12. FLOATING MINI AUDIO PLAYER & WAVE SYSTEM
// ==========================================

@Composable
fun PersistentMiniPlayer(viewModel: SoundLabViewModel) {
    val activeProject by viewModel.selectedProject.collectAsState()
    val isPlaying by viewModel.miniPlayerPlaying.collectAsState()
    val isExpanded by viewModel.miniPlayerExpanded.collectAsState()
    val sliderPos by viewModel.miniPlayerPosition.collectAsState()
    val volumeSetting by viewModel.miniPlayerVolume.collectAsState()

    // Neon glowing waveform animation
    val waveOffset = rememberInfiniteTransition(label = "wave")
    val phaseState by waveOffset.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color(0xFF0C0E18).copy(0.95f), RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonCyan, HotViolet)),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        if (isExpanded) {
            // Highly robust expanded player
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EXPANDED MULTI-PIECE STUDIO SYSTEM",
                        color = GoldenAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = { viewModel.setMiniPlayerExpanded(false) }) {
                        Text("▼", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Rotating cassette/vinyl drawing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        // cassette grid body
                        drawRoundRect(
                            color = GlassBorder.copy(0.5f),
                            size = Size(64.dp.toPx(), 44.dp.toPx()),
                            topLeft = Offset(0f, 10.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        // Spinning rolls
                        val spinFreq = if (isPlaying) phaseState else 1f
                        drawCircle(
                            color = NeonCyan,
                            radius = 8.dp.toPx(),
                            center = Offset(20.dp.toPx(), 32.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = HotViolet,
                            radius = 8.dp.toPx(),
                            center = Offset(44.dp.toPx(), 32.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeProject?.title ?: "DASHBOARD STEREO FEED",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "BPM Indicator: ${activeProject?.bpm ?: 120} | Vocal sheet signature: ${activeProject?.keySignature ?: "C Major"}",
                            color = Color.White.copy(0.5f),
                            fontSize = 9.sp
                        )
                    }
                }

                // Continuous seek bar slider and volume mix
                Slider(
                    value = sliderPos,
                    onValueChange = { viewModel.setMiniPlayerPosition(it) },
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.height(28.dp).testTag("miniplayer_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 12.sp)
                        Slider(
                            value = volumeSetting,
                            onValueChange = { viewModel.setMiniPlayerVolume(it) },
                            colors = SliderDefaults.colors(thumbColor = HotViolet, activeTrackColor = HotViolet),
                            modifier = Modifier.width(100.dp).height(24.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { viewModel.toggleMiniPlayerPlaying() }, modifier = Modifier.testTag("miniplayer_play_pause_exp")) {
                            Text(if (isPlaying) "⏸" else "▶", color = NeonCyan, fontSize = 16.sp)
                        }
                    }
                }
            }
        } else {
            // Standard compact bar mini player with bouncing waveform preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Small animated Canvas Waveform
                    Canvas(modifier = Modifier.size(width = 60.dp, height = 24.dp).testTag("miniplayer_waveform_canvas")) {
                        val strokeW = 1.5.dp.toPx()
                        val cy = size.height / 2f
                        val path = Path()
                        val subdivisions = 20
                        val stepW = size.width / subdivisions
                        for (i in 0..subdivisions) {
                            val px = i * stepW
                            val angle = (i.toFloat() / subdivisions) * 3f * PI.toFloat() - phaseState
                            val ampMultiplier = if (isPlaying) 10.dp.toPx() else 2.dp.toPx()
                            val py = cy + sin(angle) * ampMultiplier
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Brush.horizontalGradient(listOf(NeonCyan, HotViolet)), style = Stroke(strokeW))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeProject?.title ?: "BASELINE MONITORS SANDBOX",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPlaying) "STEREO FEED ACTIVE" else "MONITORS STANDBY",
                            color = if (isPlaying) MintGreen else Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Tap trigger triggers synth
                    IconButton(onClick = { viewModel.toggleMiniPlayerPlaying() }, modifier = Modifier.testTag("mini_player_play_button")) {
                        Text(if (isPlaying) "⏸" else "▶", color = NeonCyan, fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.setMiniPlayerExpanded(true) }, modifier = Modifier.testTag("mini_player_expand_button")) {
                        Text("▲", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    // Floating recording prompt modal dialog if active
    val isRecordingActive by viewModel.isRecordingActive.collectAsState()
    val isRecordingPaused by viewModel.isRecordingPaused.collectAsState()
    val recordingSecs by viewModel.recordingSeconds.collectAsState()
    val activeInput by viewModel.recordingInputDevice.collectAsState()

    if (isRecordingActive) {
        var recordingTitle by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable { /* Block dismissions */ }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(Color(0xFF0A0C16), RoundedCornerShape(20.dp))
                    .border(2.dp, HotViolet, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("LIVE RECORDING MONITOR", color = HotViolet, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Box(modifier = Modifier.background(Color.Red.copy(0.18f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("REC", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Clock timer
                val mins = recordingSecs / 60
                val secs = recordingSecs % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("recording_timer_clock")
                )

                Text(
                    text = "Source: $activeInput",
                    color = Color.White.copy(0.5f),
                    fontSize = 10.sp
                )

                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { recordingTitle = it },
                    placeholder = { Text("E.g., Guitar Loop #1", color = Color.Gray, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("recording_title_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.pauseToggleRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isRecordingPaused) "RESUME" else "PAUSE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.stopAndSaveRecording(recordingTitle) },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f).testTag("stop_recording_save_button")
                    ) {
                        Text("STOP & SAVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                TextButton(onClick = { viewModel.discardRecording() }) {
                    Text("Discard and close channel", color = Color.White.copy(0.35f), fontSize = 10.sp)
                }
            }
        }
    }
}

// ==========================================
// 13. PROFESSIONAL ANALOG AUDIO CONTROLLER COMPOSABLES
// ==========================================

@Composable
fun TactileEqRack(viewModel: SoundLabViewModel) {
    val eqBands by viewModel.eqBandsState.collectAsState()
    var presetNameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "TACTILE 10-BAND GRAPHIC EQUALIZER",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val freqLabels = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
            for (i in 0 until 10) {
                val dbVal = eqBands.getOrElse(i) { 0.0f }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(46.dp)
                        .background(Color.White.copy(0.01f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%+1.1f", dbVal),
                        color = if (dbVal != 0.0f) HotViolet else Color.Gray,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Vertical Slider Track
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(18.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = dbVal,
                            onValueChange = { viewModel.setEqBandLevel(i, it) },
                            valueRange = -12.0f..12.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color.White.copy(0.08f)
                            ),
                            // Rotates standard slider vertically
                            modifier = Modifier
                                .graphicsLayer(
                                    rotationZ = -90f,
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                )
                                .width(120.dp)
                        )
                    }

                    Text(
                        text = freqLabels[i],
                        color = Color.White.copy(0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Divider(color = Color.White.copy(0.08f))

        // Preset Saving Core Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = presetNameText,
                onValueChange = { presetNameText = it },
                placeholder = { Text("E.g., Ultra Vocal Polish", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("save_preset_input_name"),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )

            Button(
                onClick = {
                    if (presetNameText.isNotBlank()) {
                        viewModel.savePreset(presetNameText)
                        viewModel.addNotification("Saved Preset node '$presetNameText' to Room DB")
                        presetNameText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("save_preset_submit_button")
            ) {
                Text("SAVE DYNAMIC PRESET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Text(
                text = "CONTINUOUS DSP OVERDRIVE & DELAY SLIDERS RACKS",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }

        // Two Stem Controls (Vocals vs Beats)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.01f)),
                border = BorderStroke(1.dp, GlassBorder.copy(0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("STEM CHANNELS MIX DESK", color = GoldenAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Synth Vocals Gain", color = Color.White, fontSize = 10.sp)
                                Text(String.format("%.1fx", vocalsGain), color = NeonCyan, fontSize = 10.sp)
                            }
                            Slider(value = vocalsGain, onValueChange = { viewModel.setVocalsVolume(it) }, valueRange = 0f..1.5f, colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Seq Beats Volume", color = Color.White, fontSize = 10.sp)
                                Text(String.format("%.1fx", beatsGain), color = NeonCyan, fontSize = 10.sp)
                            }
                            Slider(value = beatsGain, onValueChange = { viewModel.setBeatsVolume(it) }, valueRange = 0f..1.5f, colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan))
                        }
                    }
                }
            }
        }

        // Overdrive & Stereo Wideness
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Vacuum Valve Overdrive Drive", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f SAT", drive), color = HotViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = drive,
                    onValueChange = { viewModel.setDriveLevel(it) },
                    valueRange = 0f..1.2f,
                    colors = SliderDefaults.colors(thumbColor = HotViolet, activeTrackColor = HotViolet),
                    modifier = Modifier.testTag("slider_drive")
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Stereo Spatial Wideness", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%d%% EXP", (wideness * 100).toInt()), color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = wideness,
                    onValueChange = { viewModel.setWidenessLevel(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.testTag("slider_wideness")
                )
            }
        }

        // Limiter compressor
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Digital Dynamics Compressor Limiter", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1f dB", compressThreshold), color = GoldenAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = compressThreshold,
                    onValueChange = { viewModel.setCompressorThreshold(it) },
                    valueRange = -40f..0f,
                    colors = SliderDefaults.colors(thumbColor = GoldenAmber, activeTrackColor = GoldenAmber),
                    modifier = Modifier.testTag("slider_compress")
                )
            }
        }

        // Reverb & Delay
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Digital Delay Echo Feedback", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%d%% FBCK", (delayFeedback * 100).toInt()), color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = delayFeedback,
                    onValueChange = { viewModel.setDelayFeedbackLevel(it) },
                    valueRange = 0f..0.6f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.testTag("slider_delay")
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Physical Reverb Room Space Decay", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f SIZE", reverbDecay), color = HotViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = reverbDecay,
                    onValueChange = { viewModel.setReverbDecayLevel(it) },
                    valueRange = 0f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = HotViolet, activeTrackColor = HotViolet),
                    modifier = Modifier.testTag("slider_reverb")
                )
            }
        }

        // World-class XY Pad warper
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "XY MASTER SOUNDSTAGE WARPER INTERNALS",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                XyPerformancePad(
                    delayFeedback = delayFeedback,
                    reverbDecay = reverbDecay,
                    onXChange = { viewModel.setDelayFeedbackLevel(it) },
                    onYChange = { viewModel.setReverbDecayLevel(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }
    }
}

@Composable
fun XyPerformancePad(
    delayFeedback: Float,
    reverbDecay: Float,
    onXChange: (Float) -> Unit,
    onYChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var padWidth by remember { mutableStateOf(1) }
    var padHeight by remember { mutableStateOf(1) }

    Box(
        modifier = modifier
            .background(Color.Black.copy(0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .onSizeChanged {
                padWidth = it.width.coerceAtLeast(1)
                padHeight = it.height.coerceAtLeast(1)
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val posX = (change.position.x / padWidth).coerceIn(0f, 1f)
                    val posY = (1f - (change.position.y / padHeight)).coerceIn(0f, 1f)
                    onXChange(posX * 0.6f) // Map to actual Delay feedback limit (0.6f)
                    onYChange(posY * 0.8f) // Map to actual Reverb decay limit (0.8f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val posX = (offset.x / padWidth).coerceIn(0f, 1f)
                    val posY = (1f - (offset.y / padHeight)).coerceIn(0f, 1f)
                    onXChange(posX * 0.6f)
                    onYChange(posY * 0.8f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = GlassBorder.copy(alpha = 0.15f)
            
            // Draw crossgrids
            for (x in 1..4) {
                val posX = (x / 5f) * size.width
                drawLine(gridColor, Offset(posX, 0f), Offset(posX, size.height))
            }
            for (y in 1..4) {
                val posY = (y / 5f) * size.height
                drawLine(gridColor, Offset(0f, posY), Offset(size.width, posY))
            }

            // Draw current active crosshead spot
            val cx = (delayFeedback / 0.6f) * size.width
            val cy = (1f - (reverbDecay / 0.8f)) * size.height

            drawCircle(
                color = NeonCyan,
                radius = 16.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = HotViolet,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun LiveVisualizerRack(viewModel: SoundLabViewModel) {
    val spectrum by viewModel.audioEngine.spectrumFlow.collectAsState()
    val isSynthPlaying by viewModel.isSynthPlaying.collectAsState()
    val activeMidiNote = viewModel.audioEngine.activeMidiNote

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("60FPS FOURIER SPECTROGRAPH ANALYZER", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Box(modifier = Modifier.background(MintGreen.copy(0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("LIVE ANALYZER", color = MintGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // FFT Spectrum Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .testTag("fft_analyzer_display_canvas")
        ) {
            val brickW = size.width / 10f
            val spacing = 4.dp.toPx()
            
            for (i in 0 until 10) {
                val dbScalar = spectrum.getOrElse(i) { 0.1f }
                val heightPx = dbScalar * size.height
                val rx = i * brickW + spacing / 2f
                val ry = size.height - heightPx
                val widthPx = brickW - spacing

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(NeonCyan, HotViolet)),
                    topLeft = Offset(rx, ry),
                    size = Size(widthPx, heightPx),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }

        // Oscilloscope Phasing Cathode CRT
        Spacer(modifier = Modifier.height(4.dp))
        Text("STEREO PHASING CRT OSCILLOSCOPE", color = Color.White.copy(0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF020408), RoundedCornerShape(8.dp))
                .border(0.5.dp, GlassBorder.copy(0.3f), RoundedCornerShape(8.dp))
        ) {
            val strokeW = 1.2f.dp.toPx()
            val cy = size.height / 2f
            val path = Path()
            val subdivisions = 120
            val stepW = size.width / subdivisions
            
            for (i in 0..subdivisions) {
                val px = i * stepW
                val anglePrivate = (i.toFloat() / subdivisions) * 8f * PI.toFloat()
                val phaseOffset = if (isSynthPlaying) (System.currentTimeMillis() % 1000) / 1000f * 2.0f * PI.toFloat() else 0f
                var waveVal = sin(anglePrivate - phaseOffset) * 0.4f
                
                if (activeMidiNote > 0f) {
                    waveVal += sin(anglePrivate * 2f - phaseOffset) * 0.2f
                }
                
                val py = cy + waveVal * size.height * 0.8f
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(HotViolet, NeonCyan)),
                style = Stroke(width = strokeW)
            )
        }
    }
}

@Composable
fun SynthesizerKeyboardController(viewModel: SoundLabViewModel) {
    val isSynthPlaying by viewModel.isSynthPlaying.collectAsState()
    val activeMidiNote = viewModel.audioEngine.activeMidiNote
    val keyboardKeys = viewModel.keyFrequencies

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.3f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TACTILE VOLTAGE-CONTROLLED OSCILLATOR KEYS", color = HotViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Interactive digital oscillator hardware keys", color = Color.White.copy(0.4f), fontSize = 8.sp)
            }

            IconButton(
                onClick = { viewModel.toggleSynthesizer() },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isSynthPlaying) NeonCyan else Color.White.copy(0.04f), RoundedCornerShape(50))
            ) {
                Text("⚡", color = if (isSynthPlaying) Color.Black else Color.White, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            keyboardKeys.forEach { (noteName, freqValue) ->
                val isPressed = activeMidiNote == freqValue
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(
                            if (isPressed) {
                                Brush.verticalGradient(listOf(NeonCyan, ElectricIndigo))
                            } else {
                                Brush.verticalGradient(listOf(Color.White, Color.White.copy(0.85f)))
                            }
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .pointerInput(noteName) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.playNoteOnPress(freqValue)
                                    try {
                                        awaitRelease()
                                    } finally {
                                        viewModel.stopNoteOnRelease()
                                    }
                                }
                            )
                        }
                        .testTag("key_$noteName"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = noteName,
                        color = if (isPressed) Color.White else Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiStudioAssistantPanel(viewModel: SoundLabViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProject by viewModel.selectedProject.collectAsState()
    var masteringPromptText by remember { mutableStateOf("") }
    var songwritingThemeText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "GEMINI 3.5-FLASH AI CO-CREATION ENGINE",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )

        // UI States Loader View
        when (uiState) {
            is SoundLabUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(HotViolet.copy(0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = HotViolet, strokeWidth = 2.dp)
                        Text("AI engine query in workflow. Allocating matrix...", color = HotViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is SoundLabUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MintGreen.copy(0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "SUCCESS: ${(uiState as SoundLabUiState.Success).message}",
                        color = MintGreen,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            is SoundLabUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ALERT: ${(uiState as SoundLabUiState.Error).throwable.message}",
                        color = Color.Red,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            else -> {}
        }

        // Section A: AI Intelligent Mastering advisor
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.01f)),
            border = BorderStroke(1.dp, GlassBorder.copy(0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI MASTERING ASSISTANT PROTOCOL", color = GoldenAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                
                OutlinedTextField(
                    value = masteringPromptText,
                    onValueChange = { masteringPromptText = it },
                    placeholder = { Text("E.g., Airy jazz vocals with tight compressor and warm delay...", color = Color.Gray, fontSize = 10.5.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ai_master_prompt_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                Button(
                    onClick = {
                        if (masteringPromptText.isNotBlank()) {
                            viewModel.runAiMasteringAssistant(masteringPromptText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("apply_master_advice_button")
                ) {
                    Text("GENERATE TECHNICAL DSP PRESET", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            }
        }

        // Section B: Lyrics Sheet Builder
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.01f)),
            border = BorderStroke(1.dp, GlassBorder.copy(0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INTERACTIVE SONG LYRICIST ASSISTANT", color = GoldenAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                
                OutlinedTextField(
                    value = songwritingThemeText,
                    onValueChange = { songwritingThemeText = it },
                    placeholder = { Text("E.g., Cyberpunk rain city heartbreak progressive house verse...", color = Color.Gray, fontSize = 10.5.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("songwrite_prompt_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                Button(
                    onClick = {
                        if (songwritingThemeText.isNotBlank()) {
                            viewModel.runAiLyricsGenerator(songwritingThemeText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("generate_lyrics_button")
                ) {
                    Text("AUTO GENERATE VOCAL LYRICS SHEET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }

                if (activeProject != null && activeProject?.lyrics?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SONG SHEET LYRICS:", color = Color.White.copy(0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Color.Black.copy(0.2f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, GlassBorder.copy(0.2f), RoundedCornerShape(6.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = activeProject?.lyrics ?: "",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
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
    var newProjectNameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassCardBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM SQLITE SESSION CHANNELS",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newProjectNameText,
                onValueChange = { newProjectNameText = it },
                placeholder = { Text("E.g., Hyperpop Stereo Phasing", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("input_new_project_name"),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )

            Button(
                onClick = {
                    if (newProjectNameText.isNotBlank()) {
                        viewModel.createNewProject(newProjectNameText)
                        viewModel.addNotification("Created Project node '$newProjectNameText' inside Room DB")
                        newProjectNameText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotViolet),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("create_project_button")
            ) {
                Text("CREATE Fresh SESSION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        Divider(color = Color.White.copy(0.08f))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(projects) { project ->
                val isCurrent = project.id == activeProject?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) ElectricIndigo.copy(0.2f) else Color.White.copy(0.01f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isCurrent) NeonCyan else GlassBorder.copy(0.2f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.selectProject(project) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(project.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("BPM: ${project.bpm} // Key: ${project.keySignature}", color = NeonCyan, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    }

                    IconButton(
                        onClick = { viewModel.deleteProject(project.id) },
                        modifier = Modifier.size(24.dp).testTag("delete_btn_${project.id}")
                    ) {
                        Text("❌", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

