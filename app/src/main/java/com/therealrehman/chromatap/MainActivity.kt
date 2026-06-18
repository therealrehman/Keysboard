package com.therealrehman.chromatap

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.therealrehman.chromatap.ui.theme.ChromaTapTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// High-fidelity active key states matching Samsung KeysCafe's neon transitions
enum class DynamicKeyState {
    NORMAL, WHITE, CYAN, PINK, FADE
}

// Spark/Glow particle represents propagated rgb neon background splash ripples
data class GlowParticle(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val createdAt: Long = System.currentTimeMillis(),
    // 3-color radial rings: inner, mid, outer
    val colorInner: Color = Color(0xFFFFFFFF),
    val colorMid: Color = Color(0xFF00FFFF),
    val colorOuter: Color = Color(0xFFFF00D4)
)

// Active layout switching mode
enum class KeyboardLayoutMode {
    LETTERS, SYMBOLS, EMOJIS
}

// Definition of each physical/rendered key button
data class KeyDef(
    val code: String,
    val label: String,
    val weight: Float = 1f,
    val isWide: Boolean = false,
    val isExtraWide: Boolean = false,
    val isSpace: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChromaTapTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentWindowInsets = WindowInsets.systemBars
                ) { innerPadding ->
                    KeysCafeScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun KeysCafeScreen(modifier: Modifier = Modifier) {
    var typedText by remember { mutableStateOf("") }
    var isShifted by remember { mutableStateOf(false) }
    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.LETTERS) }
    
    // Suggestion system lists
    val currentWord = remember(typedText) {
        val lastWord = typedText.split(Regex("\\s+")).lastOrNull() ?: ""
        lastWord.lowercase()
    }

    // Expanded prefix completeness word suggestions dictionary
    val expandedWordsDict = remember {
        listOf(
            "hello", "happy", "have", "heart", "help", "here", "home", "hope", "how",
            "love", "like", "laugh", "life", "live", "light", "look", "local",
            "fire", "funny", "family", "find", "first", "free", "friend", "focus",
            "cool", "crazy", "cute", "come", "coffee", "code", "cloud", "cat",
            "good", "great", "gaming", "green", "grow", "gold", "gift", "game",
            "awesome", "amazing", "angry", "app", "apple", "art", "active",
            "thanks", "today", "think", "there", "time", "thank", "together",
            "smile", "star", "sad", "smart", "small", "super", "system", "space",
            "party", "people", "play", "pizza", "car", "cry", "cake", "game",
            "wow", "nice", "number", "new", "night", "now", "name", "nature"
        )
    }

    // Emoji completion matches
    val emojiSuggestionsMap = remember {
        mapOf(
            "hello" to "👋",
            "happy" to "😊",
            "heart" to "❤️",
            "love" to "💖",
            "fire" to "🔥",
            "cool" to "😎",
            "funny" to "😂",
            "smile" to "😄",
            "star" to "🌟",
            "thanks" to "👍",
            "today" to "📅",
            "think" to "🤔",
            "awesome" to "✨",
            "sad" to "😢",
            "laugh" to "😆",
            "gaming" to "🎮",
            "green" to "🟢",
            "gold" to "💛",
            "gift" to "🎁",
            "pizza" to "🍕",
            "car" to "🚗",
            "cake" to "🍰",
            "party" to "🥳",
            "cloud" to "☁️",
            "cat" to "🐱",
            "cry" to "😭",
            "active" to "⚡"
        )
    }

    // Compute dynamic suggestions built dynamically on type prefix
    val activeSuggestions = remember(currentWord) {
        if (currentWord.isEmpty()) {
            listOf("hello", "awesome", "cool", "fire", "happy", "love")
        } else {
            expandedWordsDict.filter { it.startsWith(currentWord) }.take(6)
        }
    }

    // Color definitions corresponding to each keypress code (brighter neon shades)
    fun getKeyColor(code: String): Color {
        return when (code.lowercase()) {
            "a" -> Color(0xFFFF1111) // Ultra Neon Red
            "b" -> Color(0xFF007FFF) // Brighter Cobalt Blue
            "c" -> Color(0xFFFFEA00) // Electric Yellow
            "d" -> Color(0xFF00FF55) // Bright Lime Green
            "e" -> Color(0xFFFF00FF) // Glowing Magenta Orchid
            "f" -> Color(0xFFFF5500) // Neon Tangerine
            "g" -> Color(0xFF8800FF) // Premium Violet Purple
            "h" -> Color(0xFF00FFFF) // Ice Cyan Glow
            "i" -> Color(0xFFFF3366) // Hot Pinkish Red
            "j" -> Color(0xFF33FF33) // Vivid Green
            "k" -> Color(0xFFFFFF00) // Super Laser Yellow
            "l" -> Color(0xFFFF3399) // Laser Rose Pink
            "m" -> Color(0xFF33CCFF) // Ocean Cyan
            "n" -> Color(0xFFFFE600) // Gold Spark
            "o" -> Color(0xFFB333FF) // Royal Purple Neon
            "p" -> Color(0xFF00FFCC) // Mint Cyan Glow
            "q" -> Color(0xFFFF0033) // Radiant Rose Red
            "r" -> Color(0xFF00FFAA) // Electric Turquoise
            "s" -> Color(0xFFFFD700) // Golden Aura
            "t" -> Color(0xFF9400D3) // Deep Purple Star
            "u" -> Color(0xFF00FA9A) // Medium Spring Green
            "v" -> Color(0xFFFF4500) // Tomato Lava Orange
            "w" -> Color(0xFF1E90FF) // Dodgy Blue
            "x" -> Color(0xFFFFD700) // Imperial Yellow
            "y" -> Color(0xFFFF1493) // Deep Pink Glow
            "z" -> Color(0xFF20B2AA) // Sea Green Neon
            "0" -> Color(0xFFFFFFFF)
            "1" -> Color(0xFFFF5000)
            "2" -> Color(0xFF00FF00)
            "3" -> Color(0xFFFFCC00)
            "4" -> Color(0xFF007FFF)
            "5" -> Color(0xFFFF00AA)
            "6" -> Color(0xFF7FFF00)
            "7" -> Color(0xFFFF7F00)
            "8" -> Color(0xFF00FFFF)
            "9" -> Color(0xFF9E00FF)
            " " -> Color(0xFFFFB400) // Warm Radiant Sun Amber
            "shift" -> Color(0xFFFFFFFF)
            "backspace" -> Color(0xFFFF2222)
            "enter" -> Color(0xFF00FF77)
            "symbols", ",", "." -> Color(0xFFDDDDDD)
            else -> Color(0xFFFF8800)
        }
    }

    // Dynamic glows tracking keypress-triggered ambient backgrounds
    val activeGlows = remember { mutableStateListOf<GlowParticle>() }
    var glowIdCounter by remember { mutableStateOf(0L) }
    
    // Absolute position offsets of the main keyboard layout container
    var keyboardContainerOffset by remember { mutableStateOf(Offset.Zero) }

    // Maps each key to its measured bounding box coordinates on screen
    val keyBounds = remember { mutableStateMapOf<String, Rect>() }
    // Maps key codes to programmatic trigger functions for swipe-based RGB activations
    val keyTriggerCallbacks = remember { mutableStateMapOf<String, () -> Unit>() }
    var activeHoverKey by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    // Trigger localized physical touch response
    val triggerHaptic = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // Append standard character with Shift capitalization reset handler
    val handleNormalCharacterInput: (String) -> Unit = { character ->
        var char = character
        if (isShifted) {
            char = char.uppercase()
            isShifted = false
        }
        typedText += char
        triggerHaptic()
    }

    // Triggers full layout glows
    val addGlowEffect: (String, Pair<Float, Float>) -> Unit = { code, fractions ->
        val colFraction = fractions.first
        val rowFraction = fractions.second
        val baseColor = getKeyColor(code)
        
        val particle = GlowParticle(
            id = glowIdCounter++,
            xFraction = colFraction,
            yFraction = rowFraction,
            color = baseColor
        )
        activeGlows.add(particle)
    }

    // Background permanent fire breathing oscillation animation (opacity shift)
    val infiniteTransition = rememberInfiniteTransition(label = "fireBreath")
    val fireOpacity by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientFire"
    )

    // Blink cursor
    val cursorOpacity by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        
        // HEADING & TITLE BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAMSUNG KEYBOARD REPLICA",
                    color = Color(0xFFFF9900).copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "KeysCafe Premium",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = Color(0xFFFF4500).copy(alpha = 0.20f),
                border = BorderStroke(1.2.dp, Color(0xFFFF4500).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = "SWIPE MODE ACTIVE",
                    color = Color(0xFFFF8800),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // OUTPUT DISPLAY AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
                .background(Color(0xFF070707), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFF1E1E1E)), RoundedCornerShape(12.dp))
                .padding(14.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 40.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typedText,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 22.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 2.dp, height = 18.dp)
                            .background(Color(0xFFFFAA00).copy(alpha = cursorOpacity))
                            .align(Alignment.CenterVertically)
                    )
                }
            }

            // CLEAR BUTTON
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF3300).copy(alpha = 0.18f))
                    .border(BorderStroke(1.dp, Color(0xFFFF3300).copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                    .clickable {
                        triggerHaptic()
                        typedText = ""
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("clear_button")
            ) {
                Text(
                    text = "Clear",
                    color = Color(0xFFFF6F00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // INTELLIGENT WORD & EMOJI SUGGESTION BAR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            border = BorderStroke(1.dp, Color(0xFF161616)),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(activeSuggestions) { suggestionWord ->
                    val suggestedEmoji = emojiSuggestionsMap[suggestionWord]

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(6.dp))
                            .clickable {
                                triggerHaptic()
                                // Replace current typing prefix word with completed word
                                val words = typedText.trimEnd().split(Regex("\\s+")).toMutableList()
                                if (words.isNotEmpty() && typedText.lastOrNull()?.isWhitespace() == false) {
                                    words.removeAt(words.size - 1)
                                }
                                words.add(suggestionWord)
                                typedText = words.joinToString(" ") + " "
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestionWord,
                            color = Color(0xFFFFB400),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (suggestedEmoji != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = suggestedEmoji,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // MAIN KEYBOARD WRAPPER & GESTURE INTERCEPTOR (CAPTURES SWIPES & TYPE RIPPLES INSTANTLY)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF030303), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.2.dp, Color(0xFF111111)), RoundedCornerShape(16.dp))
                .padding(top = 10.dp, bottom = 14.dp, start = 4.dp, end = 4.dp)
                .onGloballyPositioned { coords ->
                    keyboardContainerOffset = coords.localToWindow(Offset.Zero)
                }
                .pointerInput(Unit) {
                    // Unified pointer intercept pass to catch fast swipe typing slide-throughs
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                if (change.pressed) {
                                    val position = change.position
                                    // Linear search of coordinate rectangle bounds relative to container
                                    val detectedCode = keyBounds.entries.firstOrNull { it.value.contains(position) }?.key
                                    if (detectedCode != null) {
                                        if (detectedCode != activeHoverKey) {
                                            activeHoverKey = detectedCode
                                            // Execute visual animations & physical type appending callback
                                            keyTriggerCallbacks[detectedCode]?.invoke()
                                        }
                                    } else {
                                        activeHoverKey = null
                                    }
                                } else {
                                    activeHoverKey = null
                                }
                                change.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            
            // 1. PERMANENT BURSTING FIRE GLOW CANVAS (COVERING ABSOLUTE KEYBOARD WIDTH)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .drawBehind {
                        val brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3C00).copy(alpha = 0.60f * fireOpacity),
                                Color(0xFFFF7A00).copy(alpha = 0.40f * fireOpacity),
                                Color(0xFFFFB300).copy(alpha = 0.25f * fireOpacity),
                                Color(0xFFFFD200).copy(alpha = 0.10f * fireOpacity),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height * 0.9f),
                            radius = size.width * 1.2f
                        )
                        drawRect(brush = brush)
                    }
                    .zIndex(0f)
            )

            // 2. DYNAMIC RGB BACKGROUND EXPLOSIONS (EXPANDS GIGANTIC TO COVER WHOLE KEYBOARD)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .zIndex(1f)
            ) {
                activeGlows.forEach { glow ->
                    KeyNeonRippleOverlay(glow = glow) {
                        activeGlows.remove(glow)
                    }
                }
            }

            // 3. PHYSICAL RENDER ROWS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                when (layoutMode) {
                    KeyboardLayoutMode.LETTERS -> {
                        // ROW 0: NUMBERS
                        KeyboardRowDef(
                            keys = listOf(
                                KeyDef("1", "1"), KeyDef("2", "2"), KeyDef("3", "3"),
                                KeyDef("4", "4"), KeyDef("5", "5"), KeyDef("6", "6"),
                                KeyDef("7", "7"), KeyDef("8", "8"), KeyDef("9", "9"), KeyDef("0", "0")
                            ),
                            rowIdx = 0,
                            isShifted = isShifted,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onKeyPress = { code -> handleNormalCharacterInput(code) },
                            onRegisterBounds = { code, offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[code] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { code, cb -> keyTriggerCallbacks[code] = cb },
                            addGlow = addGlowEffect
                        )

                        // ROW 1: QWERTY
                        KeyboardRowDef(
                            keys = listOf(
                                KeyDef("q", "q"), KeyDef("w", "w"), KeyDef("e", "e"),
                                KeyDef("r", "r"), KeyDef("t", "t"), KeyDef("y", "y"),
                                KeyDef("u", "u"), KeyDef("i", "i"), KeyDef("o", "o"), KeyDef("p", "p")
                            ),
                            rowIdx = 1,
                            isShifted = isShifted,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onKeyPress = { code -> handleNormalCharacterInput(code) },
                            onRegisterBounds = { code, offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[code] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { code, cb -> keyTriggerCallbacks[code] = cb },
                            addGlow = addGlowEffect
                        )

                        // ROW 2: ASDFG
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val row2Keys = listOf(
                                KeyDef("a", "a"), KeyDef("s", "s"), KeyDef("d", "d"),
                                KeyDef("f", "f"), KeyDef("g", "g"), KeyDef("h", "h"),
                                KeyDef("j", "j"), KeyDef("k", "k"), KeyDef("l", "l")
                            )
                            row2Keys.forEachIndexed { kIdx, keyDef ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KeysCafeHardwareKey(
                                        keyDef = keyDef,
                                        isShifted = isShifted,
                                        xFraction = (kIdx + 1f) / 10f,
                                        yFraction = 2.5f / 5f,
                                        keyboardContainerOffset = keyboardContainerOffset,
                                        onClick = { handleNormalCharacterInput(keyDef.code) },
                                        onRegisterBounds = { offset, size ->
                                            val relativeLeft = offset.x - keyboardContainerOffset.x
                                            val relativeTop = offset.y - keyboardContainerOffset.y
                                            keyBounds[keyDef.code] = Rect(
                                                left = relativeLeft,
                                                top = relativeTop,
                                                right = relativeLeft + size.width,
                                                bottom = relativeTop + size.height
                                            )
                                        },
                                        onRegisterCallback = { cb -> keyTriggerCallbacks[keyDef.code] = cb },
                                        addGlow = addGlowEffect
                                    )
                                }
                            }
                        }

                        // ROW 3: ZXCVB + Shift & Backspace
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val totalWeight = 9.6f
                            
                            // Shift Button
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                KeysCafeHardwareKey(
                                    keyDef = KeyDef("shift", "⇧", weight = 1.3f, isWide = true),
                                    isShifted = isShifted,
                                    xFraction = 0.65f / totalWeight,
                                    yFraction = 3.5f / 5f,
                                    keyboardContainerOffset = keyboardContainerOffset,
                                    onClick = {
                                        triggerHaptic()
                                        isShifted = !isShifted
                                    },
                                    onRegisterBounds = { offset, size ->
                                        val relativeLeft = offset.x - keyboardContainerOffset.x
                                        val relativeTop = offset.y - keyboardContainerOffset.y
                                        keyBounds["shift"] = Rect(
                                            left = relativeLeft,
                                            top = relativeTop,
                                            right = relativeLeft + size.width,
                                            bottom = relativeTop + size.height
                                        )
                                    },
                                    onRegisterCallback = { cb -> keyTriggerCallbacks["shift"] = cb },
                                    addGlow = addGlowEffect,
                                    labelColorOverride = if (isShifted) Color(0xFFFF5000) else null
                                )
                            }

                            val row3Keys = listOf(
                                KeyDef("z", "z"), KeyDef("x", "x"), KeyDef("c", "c"),
                                KeyDef("v", "v"), KeyDef("b", "b"), KeyDef("n", "n"), KeyDef("m", "m")
                            )
                            row3Keys.forEachIndexed { idx, keyDef ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KeysCafeHardwareKey(
                                        keyDef = keyDef,
                                        isShifted = isShifted,
                                        xFraction = (1.3f + idx + 0.5f) / totalWeight,
                                        yFraction = 3.5f / 5f,
                                        keyboardContainerOffset = keyboardContainerOffset,
                                        onClick = { handleNormalCharacterInput(keyDef.code) },
                                        onRegisterBounds = { offset, size ->
                                            val relativeLeft = offset.x - keyboardContainerOffset.x
                                            val relativeTop = offset.y - keyboardContainerOffset.y
                                            keyBounds[keyDef.code] = Rect(
                                                left = relativeLeft,
                                                top = relativeTop,
                                                right = relativeLeft + size.width,
                                                bottom = relativeTop + size.height
                                            )
                                        },
                                        onRegisterCallback = { cb -> keyTriggerCallbacks[keyDef.code] = cb },
                                        addGlow = addGlowEffect
                                    )
                                }
                            }

                            // Backspace Button
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                KeysCafeHardwareKey(
                                    keyDef = KeyDef("backspace", "⌫", weight = 1.3f, isWide = true),
                                    isShifted = isShifted,
                                    xFraction = (totalWeight - 0.65f) / totalWeight,
                                    yFraction = 3.5f / 5f,
                                    keyboardContainerOffset = keyboardContainerOffset,
                                    onClick = {
                                        triggerHaptic()
                                        if (typedText.isNotEmpty()) {
                                            typedText = typedText.substring(0, typedText.length - 1)
                                        }
                                    },
                                    onRegisterBounds = { offset, size ->
                                        val relativeLeft = offset.x - keyboardContainerOffset.x
                                        val relativeTop = offset.y - keyboardContainerOffset.y
                                        keyBounds["backspace"] = Rect(
                                            left = relativeLeft,
                                            top = relativeTop,
                                            right = relativeLeft + size.width,
                                            bottom = relativeTop + size.height
                                        )
                                    },
                                    onRegisterCallback = { cb -> keyTriggerCallbacks["backspace"] = cb },
                                    addGlow = addGlowEffect
                                )
                            }
                        }
                    }

                    KeyboardLayoutMode.SYMBOLS -> {
                        // SYMBOLS LAYOUT SCREEN
                        val symbolsRow0 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                        KeyboardRowDef(
                            keys = symbolsRow0.map { KeyDef(it, it) },
                            rowIdx = 0,
                            isShifted = false,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onKeyPress = { typedText += it; triggerHaptic() },
                            onRegisterBounds = { code, offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[code] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { code, cb -> keyTriggerCallbacks[code] = cb },
                            addGlow = addGlowEffect
                        )

                        val symbolsRow1 = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
                        KeyboardRowDef(
                            keys = symbolsRow1.map { KeyDef(it, it) },
                            rowIdx = 1,
                            isShifted = false,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onKeyPress = { typedText += it; triggerHaptic() },
                            onRegisterBounds = { code, offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[code] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { code, cb -> keyTriggerCallbacks[code] = cb },
                            addGlow = addGlowEffect
                        )

                        val symbolsRow2 = listOf("+", "-", "=", "_", "[", "]", "{", "}", "\\", "|")
                        KeyboardRowDef(
                            keys = symbolsRow2.map { KeyDef(it, it) },
                            rowIdx = 2,
                            isShifted = false,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onKeyPress = { typedText += it; triggerHaptic() },
                            onRegisterBounds = { code, offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[code] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { code, cb -> keyTriggerCallbacks[code] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    KeyboardLayoutMode.EMOJIS -> {
                        // EMOJIS GRID SELECTOR PANEL
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFF070707), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFF161616)), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            val popularEmojis = listOf(
                                "😊", "😂", "🔥", "❤️", "👍", "💡", "😭", "😍", "🎉", "✨",
                                "😎", "🤝", "🚀", "🌟", "😘", "🥺", "🙏", "👏", "💩", "💯",
                                "🎨", "🎵", "🍕", "🦾", "👑", "🌈", "⚡", "🍀", "💥", "🥳",
                                "🎁", "🚗", "🍰", "🍕", "☁️", "🐱", "🟢", "💛", "😄", "🤔"
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(popularEmojis) { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF141414))
                                            .clickable {
                                                triggerHaptic()
                                                typedText += emoji
                                            }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM PERSISTENT SWITCHING NAV BAR ROW (COMPATIBLE ACROSS ALL LAYOUT MODES)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val totalWeight = 9f

                    // Layout Toggle Button (!#1)
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val label = when (layoutMode) {
                            KeyboardLayoutMode.LETTERS -> "!#1"
                            KeyboardLayoutMode.SYMBOLS -> "ABC"
                            KeyboardLayoutMode.EMOJIS -> "ABC"
                        }
                        KeysCafeHardwareKey(
                            keyDef = KeyDef("symbols", label, weight = 1.5f, isWide = true),
                            isShifted = isShifted,
                            xFraction = 0.75f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                layoutMode = if (layoutMode == KeyboardLayoutMode.LETTERS) {
                                    KeyboardLayoutMode.SYMBOLS
                                } else {
                                    KeyboardLayoutMode.LETTERS
                                }
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds["symbols"] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks["symbols"] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    // Emoji switching tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconLabel = "😊"
                        KeysCafeHardwareKey(
                            keyDef = KeyDef("emojis_btn", iconLabel),
                            isShifted = false,
                            xFraction = 2.0f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                layoutMode = if (layoutMode == KeyboardLayoutMode.EMOJIS) {
                                    KeyboardLayoutMode.LETTERS
                                } else {
                                    KeyboardLayoutMode.EMOJIS
                                }
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds["emojis_btn"] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks["emojis_btn"] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    // Comma key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KeysCafeHardwareKey(
                            keyDef = KeyDef(",", ","),
                            isShifted = isShifted,
                            xFraction = 3.0f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                typedText += ","
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[","] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks[","] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    // Space (Weight 3.5f)
                    Box(
                        modifier = Modifier
                            .weight(3.5f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KeysCafeHardwareKey(
                            keyDef = KeyDef(" ", "English (US)", weight = 3.5f, isSpace = true),
                            isShifted = isShifted,
                            xFraction = 5.25f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                typedText += " "
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds[" "] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks[" "] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    // Dot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KeysCafeHardwareKey(
                            keyDef = KeyDef(".", "."),
                            isShifted = isShifted,
                            xFraction = 7.5f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                typedText += "."
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds["."] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks["."] = cb },
                            addGlow = addGlowEffect
                        )
                    }

                    // Enter Key (Weight 1.5f)
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KeysCafeHardwareKey(
                            keyDef = KeyDef("enter", "⏎", weight = 1.5f, isExtraWide = true),
                            isShifted = isShifted,
                            xFraction = 8.5f / totalWeight,
                            yFraction = 4.5f / 5f,
                            keyboardContainerOffset = keyboardContainerOffset,
                            onClick = {
                                triggerHaptic()
                                typedText += "\n"
                            },
                            onRegisterBounds = { offset, size ->
                                val relativeLeft = offset.x - keyboardContainerOffset.x
                                val relativeTop = offset.y - keyboardContainerOffset.y
                                keyBounds["enter"] = Rect(
                                    left = relativeLeft,
                                    top = relativeTop,
                                    right = relativeLeft + size.width,
                                    bottom = relativeTop + size.height
                                )
                            },
                            onRegisterCallback = { cb -> keyTriggerCallbacks["enter"] = cb },
                            addGlow = addGlowEffect
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardRowDef(
    keys: List<KeyDef>,
    rowIdx: Int,
    isShifted: Boolean,
    keyboardContainerOffset: Offset,
    onKeyPress: (String) -> Unit,
    onRegisterBounds: (String, Offset, IntSize) -> Unit,
    onRegisterCallback: (String, () -> Unit) -> Unit,
    addGlow: (String, Pair<Float, Float>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEachIndexed { colIdx, keyDef ->
            Box(
                modifier = Modifier
                    .weight(keyDef.weight)
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                KeysCafeHardwareKey(
                    keyDef = keyDef,
                    isShifted = isShifted,
                    xFraction = (colIdx + 0.5f) / keys.size.toFloat(),
                    yFraction = (rowIdx + 0.5f) / 5f,
                    keyboardContainerOffset = keyboardContainerOffset,
                    onClick = { onKeyPress(keyDef.code) },
                    onRegisterBounds = { offset, size -> onRegisterBounds(keyDef.code, offset, size) },
                    onRegisterCallback = { cb -> onRegisterCallback(keyDef.code, cb) },
                    addGlow = addGlow
                )
            }
        }
    }
}

@Composable
fun KeysCafeHardwareKey(
    keyDef: KeyDef,
    isShifted: Boolean,
    xFraction: Float,
    yFraction: Float,
    keyboardContainerOffset: Offset,
    onClick: () -> Unit,
    onRegisterBounds: (Offset, IntSize) -> Unit,
    onRegisterCallback: (() -> Unit) -> Unit,
    addGlow: (String, Pair<Float, Float>) -> Unit,
    labelColorOverride: Color? = null
) {
    val scope = rememberCoroutineScope()
    var transitionState by remember { mutableStateOf(DynamicKeyState.NORMAL) }
    var isPopupActive by remember { mutableStateOf(false) }

    // Synchronize background changes across white -> cyan -> pink -> fade sequence
    val backgroundColor = when (transitionState) {
        DynamicKeyState.NORMAL -> Color(0xFF0C0C0C).copy(alpha = 0.88f)
        DynamicKeyState.WHITE -> Color(0xFFFFFFFF)
        DynamicKeyState.CYAN -> Color(0xFF00FFFF)
        DynamicKeyState.PINK -> Color(0xFFFF00D4)
        DynamicKeyState.FADE -> Color(0xFFFF5500)
    }

    val textColor = when (transitionState) {
        DynamicKeyState.NORMAL -> labelColorOverride ?: Color(0xFFFF9900)
        DynamicKeyState.WHITE, DynamicKeyState.CYAN -> Color(0xFF000000)
        DynamicKeyState.PINK, DynamicKeyState.FADE -> Color(0xFFFFFFFF)
    }

    val glowColor = when (transitionState) {
        DynamicKeyState.NORMAL -> Color.Transparent
        DynamicKeyState.WHITE -> Color(0xFFFFFFFF).copy(alpha = 0.95f)
        DynamicKeyState.CYAN -> Color(0xFF00FFFF).copy(alpha = 0.95f)
        DynamicKeyState.PINK -> Color(0xFFFF00D4).copy(alpha = 0.95f)
        DynamicKeyState.FADE -> Color(0xFFFF5500).copy(alpha = 0.70f)
    }

    // Interactive circular expansions inside keys
    var rippleScale by remember { mutableStateOf(0f) }
    var rippleAlpha by remember { mutableStateOf(0f) }

    val playTapAnimation: () -> Unit = {
        scope.launch {
            // Coordinate background glows expansion
            addGlow(keyDef.code, Pair(xFraction, yFraction))

            isPopupActive = true
            transitionState = DynamicKeyState.WHITE
            
            // Neon ripple circle expansion
            launch {
                rippleScale = 0f
                rippleAlpha = 0.7f
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(400, easing = LinearOutSlowInEasing)
                ) { scaleVal, _ ->
                    rippleScale = scaleVal
                    rippleAlpha = 0.7f * (1f - scaleVal)
                }
            }

            // Key interactive states (matching timing of high speed color changes)
            delay(70)
            transitionState = DynamicKeyState.CYAN
            delay(70)
            transitionState = DynamicKeyState.PINK
            delay(70)
            transitionState = DynamicKeyState.FADE
            delay(200)
            transitionState = DynamicKeyState.NORMAL
            delay(100)
            isPopupActive = false
        }
    }

    // Register active keyboard sliding callback listener
    LaunchedEffect(keyDef.code) {
        onRegisterCallback {
            playTapAnimation()
            onClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val offsetInWindow = coordinates.localToWindow(Offset.Zero)
                onRegisterBounds(offsetInWindow, coordinates.size)
            },
        contentAlignment = Alignment.Center
    ) {
        
        // FLOATING MAGNIFYING POPUP OVERLAY
        if (isPopupActive) {
            Box(
                modifier = Modifier
                    .offset(y = (-55).dp)
                    .width(62.dp)
                    .height(55.dp)
                    .zIndex(100f)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.5.dp, Color(0xFFFF9900).copy(alpha = 0.7f)), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isShifted && keyDef.code.length == 1) keyDef.label.uppercase() else keyDef.label,
                    color = Color(0xFFFFCC00),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // PHYSICAL KEY SURFACE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    BorderStroke(
                        if (transitionState == DynamicKeyState.NORMAL) 1.dp else 2.dp, 
                        if (transitionState == DynamicKeyState.NORMAL) Color(0xFF1F1F1F) else glowColor
                    ), 
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    playTapAnimation()
                    onClick()
                }
                .drawBehind {
                    // White flash burst on the key surface itself
                    if (rippleAlpha > 0f) {
                        val maxRadius = size.width * 1.2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = rippleAlpha),
                                    Color(0xFF00FFFF).copy(alpha = rippleAlpha * 0.6f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = maxRadius * rippleScale
                            ),
                            radius = maxRadius * rippleScale,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                }
                .testTag("key_${keyDef.code}"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isShifted && keyDef.code.length == 1) keyDef.label.uppercase() else keyDef.label,
                color = textColor,
                fontSize = if (keyDef.isSpace) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Full keyboard expanding neon explosion overlay
@Composable
fun KeyNeonRippleOverlay(
    glow: GlowParticle,
    onAnimationEnd: () -> Unit
) {
    // 3 separate ring animatables - staggered start for layered effect
    val scaleInner  = remember { Animatable(0f) }
    val scaleMid    = remember { Animatable(0f) }
    val scaleOuter  = remember { Animatable(0f) }
    val alphaInner  = remember { Animatable(0f) }
    val alphaMid    = remember { Animatable(0f) }
    val alphaOuter  = remember { Animatable(0f) }

    val endCallback = rememberUpdatedState(onAnimationEnd)

    LaunchedEffect(glow.id) {
        // INNER RING — White, fastest, leads
        launch {
            alphaInner.snapTo(0.95f)
            scaleInner.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            alphaInner.animateTo(0f, tween(480, easing = LinearOutSlowInEasing))
        }

        // MID RING — Cyan, 60ms after inner
        launch {
            delay(60)
            alphaMid.snapTo(0.85f)
            scaleMid.animateTo(1f, tween(540, easing = FastOutSlowInEasing))
        }
        launch {
            delay(60)
            alphaMid.animateTo(0f, tween(540, easing = LinearOutSlowInEasing))
        }

        // OUTER RING — Pink/Magenta, 120ms after inner, slowest
        launch {
            delay(120)
            alphaOuter.snapTo(0.75f)
            scaleOuter.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(120)
            alphaOuter.animateTo(0f, tween(600, easing = LinearOutSlowInEasing))
            endCallback.value()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = glow.xFraction * size.width
        val cy = glow.yFraction * size.height

        // Max radius = diagonal of keyboard so it covers every corner from any key
        val maxRadius = kotlin.math.sqrt(
            (size.width * size.width + size.height * size.height).toDouble()
        ).toFloat()

        // Draw 3 rings from outer to inner (painter's algorithm)
        // OUTER — Pink/Magenta ring
        if (alphaOuter.value > 0f) {
            val r = maxRadius * scaleOuter.value
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to glow.colorOuter.copy(alpha = 0f),
                    0.55f to glow.colorOuter.copy(alpha = 0.10f * alphaOuter.value),
                    0.75f to glow.colorOuter.copy(alpha = 0.65f * alphaOuter.value),
                    0.88f to glow.colorOuter.copy(alpha = 0.80f * alphaOuter.value),
                    0.95f to glow.colorOuter.copy(alpha = 0.40f * alphaOuter.value),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // MID — Cyan ring
        if (alphaMid.value > 0f) {
            val r = maxRadius * 0.80f * scaleMid.value
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to glow.colorMid.copy(alpha = 0f),
                    0.50f to glow.colorMid.copy(alpha = 0.08f * alphaMid.value),
                    0.72f to glow.colorMid.copy(alpha = 0.60f * alphaMid.value),
                    0.87f to glow.colorMid.copy(alpha = 0.75f * alphaMid.value),
                    0.95f to glow.colorMid.copy(alpha = 0.35f * alphaMid.value),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // INNER — White ring, brightest core
        if (alphaInner.value > 0f) {
            val r = maxRadius * 0.55f * scaleInner.value
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to glow.colorInner.copy(alpha = 0.90f * alphaInner.value),
                    0.30f to glow.colorInner.copy(alpha = 0.55f * alphaInner.value),
                    0.65f to glow.colorInner.copy(alpha = 0.70f * alphaInner.value),
                    0.85f to glow.colorInner.copy(alpha = 0.80f * alphaInner.value),
                    0.95f to glow.colorInner.copy(alpha = 0.30f * alphaInner.value),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}
