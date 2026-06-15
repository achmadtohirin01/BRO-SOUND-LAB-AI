package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// ==========================================
// CENTRALIZED DESIGN LANGUAGE PALETTE
// ==========================================
val BroSpaceObsidian = Color(0xFF04060B)
val BroGlassCardBg = Color(0x1F1A2235)
val BroGlassBorder = Color(0x336C63FF)
val BroNeonCyan = Color(0xFF00E5FF)
val BroElectricIndigo = Color(0xFF7B2CBF)
val BroHotViolet = Color(0xFFE040FB)
val BroMintGreen = Color(0xFF00E676)
val BroGoldenAmber = Color(0xFFFFB300)
val BroDarkGunmetal = Color(0xFF131521)
val BroDangerRed = Color(0xFFFF5252)

// ==========================================
// 3. PRIMARY BUTTON COMPONENT
// ==========================================
enum class BroButtonVariant {
    Primary, Secondary, Outline, Ghost, Danger, Success, Recording, AI
}

@Composable
fun BroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BroButtonVariant = BroButtonVariant.Primary,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "btnScale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val containerColor = when (variant) {
        BroButtonVariant.Primary -> BroElectricIndigo
        BroButtonVariant.Secondary -> BroGlassCardBg
        BroButtonVariant.Outline -> Color.Transparent
        BroButtonVariant.Ghost -> Color.Transparent
        BroButtonVariant.Danger -> BroDangerRed
        BroButtonVariant.Success -> BroMintGreen
        BroButtonVariant.Recording -> BroDangerRed.copy(alpha = pulseGlow)
        BroButtonVariant.AI -> Color.Transparent // Powered by gradient custom draw
    }

    val contentColor = when (variant) {
        BroButtonVariant.Primary -> Color.White
        BroButtonVariant.Secondary -> Color.White.copy(0.9f)
        BroButtonVariant.Outline -> BroNeonCyan
        BroButtonVariant.Ghost -> Color.White.copy(0.7f)
        BroButtonVariant.Danger -> Color.White
        BroButtonVariant.Success -> Color.Black
        BroButtonVariant.Recording -> Color.White
        BroButtonVariant.AI -> Color.White
    }

    val borderStroke = when (variant) {
        BroButtonVariant.Outline -> BorderStroke(1.dp, BroNeonCyan)
        BroButtonVariant.Secondary -> BorderStroke(1.dp, BroGlassBorder)
        BroButtonVariant.AI -> BorderStroke(1.dp, Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)))
        else -> null
    }

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (variant == BroButtonVariant.AI) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                BroElectricIndigo.copy(alpha = 0.8f),
                                BroHotViolet.copy(alpha = 0.8f)
                            )
                        )
                    )
                } else {
                    Modifier.background(containerColor)
                }
            )
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(12.dp)) else Modifier)
            .pointerInput(isEnabled, isLoading) {
                if (isEnabled && !isLoading) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            isPressed = false
                            onClick()
                        }
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.animateContentSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = if (isLoading) "PROCESSING..." else text.uppercase(),
                color = if (isEnabled) contentColor else Color.White.copy(0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==========================================
// 4. ICON BUTTON
// ==========================================
@Composable
fun BroIconButton(
    unicodeSymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    glowColor: Color = BroNeonCyan,
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1.0f, label = "iconScale")
    
    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(BroGlassCardBg)
            .border(1.dp, BroGlassBorder, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = unicodeSymbol,
            fontSize = (sizeDp.value * 0.42f).sp,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// 5. FLOATING ACTION BUTTON
// ==========================================
@Composable
fun BroFAB(
    iconSymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuItems: List<Pair<String, () -> Unit>> = emptyList(),
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fabRotate")
    
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .background(BroSpaceObsidian.copy(0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, BroGlassBorder, RoundedCornerShape(12.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                menuItems.forEach { (label, action) ->
                    Text(
                        text = label.uppercase(),
                        color = BroNeonCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                action()
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BroHotViolet, BroElectricIndigo)
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(0.15f),
                        radius = size.minDimension / 2f
                    )
                }
                .clickable {
                    if (menuItems.isNotEmpty()) {
                        expanded = !expanded
                    } else {
                        onClick()
                    }
                }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconSymbol,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier.graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

// ==========================================
// 6. AUDIO SLIDER
// ==========================================
@Composable
fun BroAudioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "VOLUME",
    valueDisplay: String = "${(value * 100).toInt()}%",
    isVertical: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier
            .background(BroGlassCardBg, RoundedCornerShape(10.dp))
            .border(1.dp, BroGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(36.dp)
                        .pointerInput(onValueChange) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val fraction = dragAmount.y / size.height
                                val newValue = (value - fraction).coerceIn(0f, 1f)
                                onValueChange(newValue)
                            }
                        }
                        .drawBehind {
                            // Track background line
                            val trackW = 4.dp.toPx()
                            val cornerR = 2.dp.toPx()
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.1f),
                                topLeft = Offset((size.width - trackW) / 2f, 0f),
                                size = Size(trackW, size.height),
                                cornerRadius = CornerRadius(cornerR, cornerR)
                            )
                            // Clean indicator line
                            val filledH = size.height * value
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(BroNeonCyan, BroHotViolet)),
                                topLeft = Offset((size.width - trackW) / 2f, size.height - filledH),
                                size = Size(trackW, filledH),
                                cornerRadius = CornerRadius(cornerR, cornerR)
                            )
                            // Draw metallic fader thumb handle
                            val thumbH = 12.dp.toPx()
                            val thumbW = 28.dp.toPx()
                            val thumbY = (size.height - filledH).coerceIn(0f, size.height)
                            drawRoundRect(
                                color = Color(0xFF1E2135),
                                topLeft = Offset((size.width - thumbW) / 2f, thumbY - thumbH / 2f),
                                size = Size(thumbW, thumbH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRect(
                                color = BroNeonCyan,
                                topLeft = Offset((size.width - thumbW) / 2f + 2.dp.toPx(), thumbY - 1.dp.toPx()),
                                size = Size(thumbW - 4.dp.toPx(), 2.dp.toPx())
                            )
                        }
                )
                
                Text(
                    text = valueDisplay,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = BroNeonCyan
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.width(60.dp)) {
                    Text(
                        label,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(0.6f)
                    )
                    Text(
                        valueDisplay,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BroNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .pointerInput(onValueChange) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val fraction = dragAmount.x / size.width
                                val newValue = (value + fraction).coerceIn(0f, 1f)
                                onValueChange(newValue)
                            }
                        }
                        .drawBehind {
                            // Horizontal slider track
                            val trackH = 4.dp.toPx()
                            val cornerR = 2.dp.toPx()
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.1f),
                                topLeft = Offset(0f, (size.height - trackH) / 2f),
                                size = Size(size.width, trackH),
                                cornerRadius = CornerRadius(cornerR, cornerR)
                            )
                            val filledW = size.width * value
                            drawRoundRect(
                                brush = Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)),
                                topLeft = Offset(0f, (size.height - trackH) / 2f),
                                size = Size(filledW, trackH),
                                cornerRadius = CornerRadius(cornerR, cornerR)
                            )
                            // Handle
                            val thumbW = 12.dp.toPx()
                            val thumbH = 24.dp.toPx()
                            val thumbX = filledW.coerceIn(0f, size.width)
                            drawRoundRect(
                                color = Color(0xFF1E2135),
                                topLeft = Offset(thumbX - thumbW / 2f, (size.height - thumbH) / 2f),
                                size = Size(thumbW, thumbH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRect(
                                color = BroNeonCyan,
                                topLeft = Offset(thumbX - 1.dp.toPx(), (size.height - thumbH) / 2f + 3.dp.toPx()),
                                size = Size(2.dp.toPx(), thumbH - 6.dp.toPx())
                            )
                        }
                )
            }
        }
    }
}

// ==========================================
// 7. ROTARY KNOB
// ==========================================
@Composable
fun BroRotaryKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "GAIN",
    valueDisplay: String = "${(value * 24 - 12).toInt()} dB",
    sizeDp: Dp = 72.dp
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier.width(sizeDp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(BroGlassCardBg)
                .border(1.5.dp, BroGlassBorder, CircleShape)
                .pointerInput(onValueChange) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Vertical screen drag changes values logically
                        val sensitivity = 180f // Smaller = faster, larger = slower
                        val delta = -dragAmount.y / sensitivity
                        val newValue = (value + delta).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onValueChange(0.5f) // Reset to middle
                        }
                    )
                }
                .drawBehind {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeW = 4.dp.toPx()
                    
                    // Arc meter path
                    drawCircle(
                        color = Color.White.copy(0.05f),
                        radius = radius - 8.dp.toPx(),
                        style = Stroke(width = strokeW)
                    )
                    
                    // Active sweep arc (270 degrees sweep)
                    val startAngle = 135f
                    val sweepAngle = 270f * value
                    drawArc(
                        brush = Brush.sweepGradient(listOf(BroNeonCyan, BroHotViolet)),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    
                    // Central physical dial cap
                    drawCircle(
                        color = Color(0xFF0F111E),
                        radius = radius - 14.dp.toPx()
                    )
                    
                    // Directional dot line fader fader marker
                    val angleOffset = startAngle + sweepAngle
                    val angleRad = Math.toRadians(angleOffset.toDouble())
                    val markerDist = radius - 20.dp.toPx()
                    val targetX = center.x + markerDist * cos(angleRad).toFloat()
                    val targetY = center.y + markerDist * sin(angleRad).toFloat()
                    
                    drawLine(
                        color = BroNeonCyan,
                        start = center,
                        end = Offset(targetX, targetY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    drawCircle(
                        color = BroNeonCyan,
                        center = Offset(targetX, targetY),
                        radius = 2.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Can render empty or metadata inside knob
        }
        
        Text(
            text = valueDisplay,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = BroNeonCyan
        )
    }
}

// ==========================================
// 8. EQUALIZER BAND
// ==========================================
@Composable
fun BroEqBand(
    frequencyText: String,
    gainDb: Float, // -15f to +15f
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = ((gainDb + 15f) / 30f).coerceIn(0f, 1f)
    
    Column(
        modifier = modifier.width(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = frequencyText,
            color = Color.White.copy(0.6f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth()
                .pointerInput(onGainChange) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val unitDelta = -dragAmount.y / size.height
                        val newProgress = (progress + unitDelta).coerceIn(0f, 1f)
                        onGainChange(newProgress * 30f - 15f)
                    }
                }
                .drawBehind {
                    val midY = size.height / 2f
                    // Center zero line
                    drawLine(
                        color = Color.White.copy(0.12f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Track Line
                    drawLine(
                        color = Color.White.copy(0.08f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    // Active filled fader
                    val currentY = size.height * (1f - progress)
                    drawLine(
                        brush = Brush.verticalGradient(
                            if (gainDb >= 0) listOf(BroNeonCyan, BroElectricIndigo)
                            else listOf(BroElectricIndigo, BroHotViolet)
                        ),
                        start = Offset(size.width / 2f, midY),
                        end = Offset(size.width / 2f, currentY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Thumb slider bed
                    drawRect(
                        color = if (gainDb >= 0) BroNeonCyan else BroHotViolet,
                        topLeft = Offset(4.dp.toPx(), currentY - 3.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), 6.dp.toPx())
                    )
                }
        )
        
        Text(
            text = String.format("%.1fdB", gainDb),
            color = if (gainDb == 0f) Color.White.copy(0.5f) else if (gainDb > 0) BroNeonCyan else BroHotViolet,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// 9. TOGGLE SWITCH
// ==========================================
@Composable
fun BroToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val thumbOffset by animateFloatAsState(targetValue = if (checked) 20f else 0f, label = "toggleOffset")
    val activeColor by animateColorAsState(targetValue = if (checked) BroNeonCyan else Color.White.copy(0.2f), label = "toggleColor")

    Row(
        modifier = modifier.clickable {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onCheckedChange(!checked)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label.uppercase(),
                color = Color.White.copy(0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        Box(
            modifier = Modifier
                .size(44.dp, 24.dp)
                .clip(CircleShape)
                .background(BroSpaceObsidian)
                .border(1.dp, activeColor.copy(0.5f), CircleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) Brush.linearGradient(listOf(BroNeonCyan, BroElectricIndigo))
                        else Brush.linearGradient(listOf(Color.White.copy(0.4f), Color.White.copy(0.6f)))
                    )
            )
        }
    }
}

// ==========================================
// 10. CARD COMPONENT
// ==========================================
@Composable
fun BroGlassCard(
    modifier: Modifier = Modifier,
    borderGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BroGlassCardBg)
            .border(
                1.dp,
                if (borderGlow) Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet))
                else Brush.verticalGradient(listOf(BroGlassBorder, Color.Transparent)),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        content = content
    )
}

// ==========================================
// 11. DIALOG COMPONENT
// ==========================================
@Composable
fun BroDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.75f))
                .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BroDarkGunmetal)
                    .border(1.5.dp, BroGlassBorder, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) { /* Consume taps */ }
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "✕",
                        color = Color.White.copy(0.5f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismissRequest() }
                            .padding(4.dp)
                    )
                }
                
                content()
            }
        }
    }
}

// ==========================================
// 12. BOTTOM SHEET
// ==========================================
@Composable
fun BroBottomSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(BroDarkGunmetal)
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(BroGlassBorder, Color.Transparent)),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

// ==========================================
// 13. WAVEFORM COMPONENT
// ==========================================
@Composable
fun BroWaveform(
    modifier: Modifier = Modifier,
    amplitudePoints: List<Float> = List(40) { abs(sin(it * 0.2f) * cos(it * 0.4f)) },
    playheadFraction: Float = 0.4f,
    onSeek: (Float) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BroGlassCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, BroGlassBorder.copy(0.12f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = 4.dp.toPx()
            val barWidth = (width / amplitudePoints.size) - spacing
            
            for (i in amplitudePoints.indices) {
                val amp = amplitudePoints[i].coerceIn(0.05f, 1f)
                val barHeight = height * amp * 0.8f
                val x = i * (barWidth + spacing) + spacing / 2f
                val y = (height - barHeight) / 2f
                
                val currentFraction = i.toFloat() / amplitudePoints.size
                val isPlayed = currentFraction <= playheadFraction
                
                drawRoundRect(
                    brush = if (isPlayed) Brush.verticalGradient(listOf(BroNeonCyan, BroHotViolet))
                            else Brush.verticalGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.18f))),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
            
            // Glowing Playhead Needle
            val playheadX = width * playheadFraction
            drawLine(
                color = BroNeonCyan,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = BroNeonCyan,
                radius = 4.dp.toPx(),
                center = Offset(playheadX, 0f)
            )
        }
    }
}

// ==========================================
// 14. FFT Spectrum Analyzer
// ==========================================
@Composable
fun BroFftAnalyzer(
    modifier: Modifier = Modifier,
    barsData: FloatArray = FloatArray(16) { 0.1f }
) {
    val peakHold = remember { FloatArray(barsData.size) }
    
    // Smooth transition simulation for peaks
    LaunchedEffect(barsData) {
        for (i in barsData.indices) {
            if (barsData[i] > peakHold[i]) {
                peakHold[i] = barsData[i]
            } else {
                peakHold[i] = (peakHold[i] - 0.02f).coerceAtLeast(0.02f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(BroSpaceObsidian)
            .border(1.dp, BroGlassBorder.copy(0.15f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = 3.dp.toPx()
            val barW = (width / barsData.size) - spacing
            
            for (i in barsData.indices) {
                val value = barsData[i].coerceIn(0.01f, 1f)
                val barH = height * value
                val x = i * (barW + spacing) + spacing / 2f
                
                // Draw audio analysis bar with dynamic spectral gradient color
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BroHotViolet, BroElectricIndigo, BroNeonCyan)
                    ),
                    topLeft = Offset(x, height - barH),
                    size = Size(barW, barH)
                )
                
                // Draw peak hold tick marks
                val peakY = height - (height * peakHold[i].coerceIn(0.02f, 1f))
                drawLine(
                    color = BroNeonCyan.copy(alpha = 0.8f),
                    start = Offset(x, peakY),
                    end = Offset(x + barW, peakY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

// ==========================================
// 16. STEREO VU METER
// ==========================================
@Composable
fun BroVuMeter(
    leftLevel: Float, // 0.0 to 1.0f
    rightLevel: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF070911), RoundedCornerShape(8.dp))
            .border(1.dp, BroGlassBorder.copy(0.15f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("L", color = Color.White.copy(0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Box(modifier = Modifier.weight(1f).height(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Background Track
                    drawRoundRect(Color.White.copy(0.05f), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                    
                    val filledW = w * leftLevel
                    val isClipping = leftLevel > 0.9f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(BroMintGreen, BroGoldenAmber, BroDangerRed)
                        ),
                        size = Size(filledW, h),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    
                    if (isClipping) {
                        drawCircle(BroDangerRed, radius = 3.dp.toPx(), center = Offset(w - 6.dp.toPx(), h / 2f))
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("R", color = Color.White.copy(0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Box(modifier = Modifier.weight(1f).height(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Background
                    drawRoundRect(Color.White.copy(0.05f), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                    
                    val filledW = w * rightLevel
                    val isClipping = rightLevel > 0.9f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(BroMintGreen, BroGoldenAmber, BroDangerRed)
                        ),
                        size = Size(filledW, h),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    
                    if (isClipping) {
                        drawCircle(BroDangerRed, radius = 3.dp.toPx(), center = Offset(w - 6.dp.toPx(), h / 2f))
                    }
                }
            }
        }
    }
}

// ==========================================
// 18. REALTIME OSCILLOSCOPE
// ==========================================
@Composable
fun BroOscilloscope(
    modifier: Modifier = Modifier,
    points: List<Float> = List(80) { sin(it * 0.3f).toFloat() }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(BroSpaceObsidian)
            .border(1.dp, BroGlassBorder.copy(0.2f), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val gridColor = BroGlassBorder.copy(alpha = 0.08f)
            
            // Subgrid lines
            for (x in 1..4) {
                drawLine(gridColor, Offset(w * (x / 5f), 0f), Offset(w * (x / 5f), h))
            }
            drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.5f)
            
            // Draw vector path
            if (points.isNotEmpty()) {
                val stepX = w / (points.size - 1)
                val path = Path()
                
                for (i in points.indices) {
                    val x = i * stepX
                    val level = points[i].coerceIn(-1f, 1f)
                    val y = h / 2f + (level * (h / 2.3f))
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ==========================================
// 19. AUDIO VISUALIZER (CIRCULAR RIPPLE)
// ==========================================
@Composable
fun BroAudioVisualizer(
    level: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vis")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim"
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .drawBehind {
                val r = (size.minDimension / 2.5f) * (1f + level * 0.4f) * waveAnim
                val center = Offset(size.width / 2f, size.height / 2f)
                
                // Secondary outer pulsing aura
                drawCircle(
                    brush = Brush.radialGradient(listOf(BroNeonCyan.copy(0.12f), Color.Transparent)),
                    radius = r + 24.dp.toPx(),
                    center = center
                )
                
                // Ring vector
                drawCircle(
                    brush = Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)),
                    radius = r,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Core pulse core emitter
                drawCircle(
                    brush = Brush.radialGradient(listOf(BroHotViolet, BroElectricIndigo)),
                    radius = (size.minDimension / 4.5f),
                    center = center
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("BRO AI", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 21. FLOAT TOAST
// ==========================================
@Composable
fun BroToast(
    message: String,
    unicodeIcon: String = "✓",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BroDarkGunmetal)
            .border(1.dp, BroNeonCyan.copy(0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(unicodeIcon, color = BroNeonCyan, fontSize = 14.sp)
            Text(
                message.uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// 23. PROGRESS RINGS & SPINNERS
// ==========================================
@Composable
fun BroProgressRing(
    modifier: Modifier = Modifier,
    labelText: String = "EXPORTING..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "prog")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer(rotationZ = rotation)
                .drawBehind {
                    drawArc(
                        brush = Brush.linearGradient(listOf(BroNeonCyan, BroHotViolet)),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
        )
        Text(
            text = labelText.uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// 24. SEARCH BAR
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "SEARCH PLUGINS & PRESETS...",
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BroGlassBorder.copy(0.2f), RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                placeholder.uppercase(),
                color = Color.White.copy(0.35f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        },
        leadingIcon = {
            Text("🔍", fontSize = 14.sp)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Text(
                    "✕",
                    color = Color.White.copy(0.5f),
                    modifier = Modifier.clickable { onValueChange("") },
                    fontSize = 12.sp
                )
            }
        },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BroGlassCardBg,
            unfocusedContainerColor = BroGlassCardBg,
            disabledContainerColor = BroGlassCardBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ==========================================
// 26. CHIPS & BADGES
// ==========================================
@Composable
fun BroChip(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) BroNeonCyan else BroGlassCardBg)
            .border(
                1.dp,
                if (isSelected) BroNeonCyan else BroGlassBorder.copy(0.3f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BroBadge(
    text: String,
    color: Color = BroHotViolet
) {
    Box(
        modifier = Modifier
            .background(color.copy(0.12f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
