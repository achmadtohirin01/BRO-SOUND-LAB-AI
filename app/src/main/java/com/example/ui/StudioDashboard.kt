package com.example.ui

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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.sin

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
    val currentTab by viewModel.currentTab.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceObsidian.copy(0.9f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Interactive Hamburger menu opens Side Drawer
                IconButton(
                    onClick = { viewModel.setSideDrawerOpen(true) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(0.04f), RoundedCornerShape(50))
                        .border(1.dp, GlassBorder.copy(0.4f), RoundedCornerShape(50))
                        .testTag("hamburger_menu_button")
                ) {
                    Text("☰", color = NeonCyan, fontSize = 16.sp)
                }

                Column {
                    Text(
                        text = "BRO SOUND LAB AI",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "DAW CONSOLE CORE // $currentTab",
                        color = Color.White.copy(0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Operations Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Real-time notification log toggle
                var showPromoAlerts by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showPromoAlerts = !showPromoAlerts },
                    modifier = Modifier.size(40.dp).background(Color.White.copy(0.03f), RoundedCornerShape(50))
                ) {
                    BadgedBox(
                        badge = {
                            if (notifications.isNotEmpty()) {
                                Badge(containerColor = HotViolet) {
                                    Text(notifications.size.toString(), color = Color.White, fontSize = 8.sp)
                                }
                            }
                        }
                    ) {
                        Text("🔔", fontSize = 16.sp)
                    }
                }

                // Global Interactive Search trigger
                IconButton(
                    onClick = { globalSearchOpen = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(GlassCardBg, RoundedCornerShape(50))
                        .border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(50))
                        .testTag("global_search_icon_button")
                ) {
                    Text("🔍", color = Color.White, fontSize = 16.sp)
                }

                // Notification quick-dropdown panel
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
            Triple("TOOLS", "🎛️", "Tools"),
            Triple("PROFILE", "👤", "Profile")
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
            Triple("TOOLS", "🎛️", "Tools"),
            Triple("PROFILE", "👤", "Profile")
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

