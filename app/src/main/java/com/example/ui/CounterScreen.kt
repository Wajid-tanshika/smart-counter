package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import android.content.Intent
import androidx.compose.ui.composed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.SoundEffectConstants
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.ToneGenerator

object AudioSynth {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (e: Exception) {
        }
    }

    fun playTone(frequency: Double, durationMs: Int) {
        try {
            if (frequency > 500) {
                // Increment tone
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
            } else if (frequency < 500) {
                // Decrement tone
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, durationMs)
            } else {
                // Other tone
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, durationMs)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}



@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    modifier: Modifier = Modifier
) {
    val count by viewModel.count.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val themeChoice by viewModel.theme.collectAsStateWithLifecycle()
    
    val highestCount by viewModel.highestCount.collectAsStateWithLifecycle()
    val lowestCount by viewModel.lowestCount.collectAsStateWithLifecycle()
    val totalTaps by viewModel.totalTaps.collectAsStateWithLifecycle()
    val stepSize by viewModel.step.collectAsStateWithLifecycle()
    val targetCount by viewModel.target.collectAsStateWithLifecycle()

    val colors = getAppThemeColors(themeChoice, count)

    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(false) }
    
    val tts = remember {
        var instance: TextToSpeech? = null
        val ttsContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.applicationContext.createAttributionContext("tts")
        } else {
            context.applicationContext
        }
        instance = TextToSpeech(ttsContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.getDefault()
            }
        }
        instance
    }
    
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val lastHapticTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    val playFeedback = { isIncrement: Boolean? ->
        val now = System.currentTimeMillis()
        val prev = lastHapticTime.get()
        if (now - prev >= 100L) { // Rate limit feedback, reduced to 100ms for more responsiveness
            lastHapticTime.set(now)
            if (soundEnabled) {
                if (isIncrement == true) {
                    AudioSynth.playTone(600.0, 50)
                } else if (isIncrement == false) {
                    AudioSynth.playTone(400.0, 50)
                } else {
                    AudioSynth.playTone(500.0, 80)
                }
            }
            if (vibrationEnabled) {
                try {
                    if (isIncrement == true) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (isIncrement == false) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                } catch (e: Throwable) {
                }
            }
        }
    }
    
    val lastSpeakTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val speakCurrentNumber = { number: Int ->
        if (voiceEnabled) {
            val now = System.currentTimeMillis()
            if (now - lastSpeakTime.get() >= 500L) {
                lastSpeakTime.set(now)
                tts?.speak(number.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    var showSettingsMenu by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var targetInput by remember { mutableStateOf("") }
    var isLocked by remember { mutableStateOf(false) }

    val gradientBackground = Brush.verticalGradient(
        colors = colors.backgroundGradient
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SMART",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = colors.headerAccentColor
                        )
                        Text(
                            text = "Counter",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryTextColor
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "🎯 I just reached ${count} (Target: ${if(targetCount > 0) targetCount else "None"}) with SMART Counter! Can you beat it?")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share your count")
                                context.startActivity(shareIntent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = 1.dp,
                                    color = colors.iconBorderColor,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Count",
                                tint = colors.iconTintColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { isLocked = !isLocked },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = 1.dp,
                                    color = colors.iconBorderColor,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Interface",
                                tint = if (isLocked) colors.headerAccentColor else colors.iconTintColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { 
                                val newTheme = if (themeChoice == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
                                viewModel.setTheme(newTheme)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = 1.dp,
                                    color = colors.iconBorderColor,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (themeChoice == AppTheme.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme",
                                tint = colors.iconTintColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { showHistorySheet = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = 1.dp,
                                    color = colors.iconBorderColor,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = colors.iconTintColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showSettingsMenu = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = colors.iconContainerColor
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(
                                        width = 1.dp,
                                        color = colors.iconBorderColor,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = colors.iconTintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                AppTheme.values().forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.name.replace("_", " ")) },
                                        onClick = {
                                            viewModel.setTheme(theme)
                                            showSettingsMenu = false
                                        },
                                        trailingIcon = {
                                            if (themeChoice == theme) Icon(Icons.Default.Check, "Selected")
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Set Target Counter") },
                                    onClick = { targetInput = targetCount.toString(); showTargetDialog = true; showSettingsMenu = false }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (soundEnabled) "Disable Sounds" else "Enable Sounds") },
                                    onClick = { soundEnabled = !soundEnabled; showSettingsMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (voiceEnabled) "Disable Voice" else "Enable Voice") },
                                    onClick = { voiceEnabled = !voiceEnabled; showSettingsMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (vibrationEnabled) "Disable Vibration" else "Enable Vibration") },
                                    onClick = { vibrationEnabled = !vibrationEnabled; showSettingsMenu = false }
                                )
                            }
                        }
                    }
                }

                // Center Display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .shadow(24.dp, shape = RoundedCornerShape(32.dp), ambientColor = colors.headerAccentColor, spotColor = colors.incrementShadow)
                            .background(
                                color = colors.glassBackgroundColor,
                                shape = RoundedCornerShape(32.dp)
                            )
                            .border(1.dp, colors.glassBorderColor, RoundedCornerShape(32.dp))
                            .clickable(enabled = !isLocked && count < 9999999) {
                                viewModel.increment()
                                playFeedback(true)
                                speakCurrentNumber(viewModel.count.value)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedContent(
                                targetState = count,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith
                                                (slideOutVertically { height -> -height } + fadeOut() + scaleOut(targetScale = 1.5f))
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn() + scaleIn(initialScale = 1.5f)) togetherWith
                                                (slideOutVertically { height -> height } + fadeOut() + scaleOut(targetScale = 0.5f))
                                    }.using(
                                        SizeTransform(clip = false)
                                    )
                                },
                                label = "CounterValueAnimation"
                            ) { displayedCount ->
                                Text(
                                    text = String.format("%01d", displayedCount),
                                    fontSize = 120.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = (-2).sp,
                                    color = colors.countTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("counter_display")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "CURRENT COUNT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = colors.secondaryTextColor
                            )

                            if (targetCount > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val progress = (count.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(6.dp)
                                        .background(colors.glassBackgroundColor, CircleShape)
                                        .border(0.5.dp, colors.glassBorderColor, CircleShape),
                                    color = if (count >= targetCount) colors.headerAccentColor else colors.countTextColor,
                                    trackColor = Color.Transparent,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Target: $targetCount",
                                    fontSize = 10.sp,
                                    color = colors.secondaryTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STEP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.secondaryTextColor,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        listOf(1, 5, 10, 100).forEach { s ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable(enabled = !isLocked) { viewModel.setStep(s) }
                                    .background(
                                        if (stepSize == s) colors.headerAccentColor.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (stepSize == s) colors.headerAccentColor else colors.iconBorderColor,
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stepSize == s) colors.headerAccentColor else colors.secondaryTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (!isLocked) {
                                playFeedback(null)
                                showResetDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.resetContainerColor,
                            contentColor = colors.resetContentColor
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, colors.resetBorderColor),
                        modifier = Modifier
                            .testTag("reset_button")
                            .height(44.dp)
                            .widthIn(min = 130.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset counter",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESET",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val minusInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = if (count > 0) colors.decrementShadow else Color.Transparent,
                                spotColor = if (count > 0) colors.decrementShadow else Color.Transparent
                            )
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (count > 0) colors.decrementGradientsActive else colors.decrementGradientsInactive
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (count > 0) colors.decrementShadow.copy(alpha=0.6f) else colors.secondaryTextColor.copy(alpha=0.3f),
                                shape = CircleShape
                            )
                            .repeatingClickable(
                                interactionSource = minusInteractionSource,
                                enabled = !isLocked && count > 0,
                                initialDelay = 500,
                                interval = 100,
                                onClick = {
                                    if (count > 0) {
                                        viewModel.decrement()
                                        playFeedback(false)
                                        speakCurrentNumber(viewModel.count.value)
                                    }
                                }
                            )
                            .testTag("decrement_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease counter by 1",
                            tint = colors.negativeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    val plusInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(16.dp, shape = CircleShape, ambientColor = colors.incrementShadow, spotColor = colors.incrementShadow)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = colors.incrementGradients
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, colors.incrementShadow, CircleShape)
                            .repeatingClickable(
                                interactionSource = plusInteractionSource,
                                enabled = !isLocked && count < 9999999,
                                initialDelay = 500,
                                interval = 100,
                                onClick = {
                                    viewModel.increment()
                                    playFeedback(true)
                                    speakCurrentNumber(viewModel.count.value)
                                }
                            )
                            .testTag("increment_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase counter by 1",
                            tint = colors.positiveColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Counter?") },
            text = { Text(text = "Are you sure you want to reset the counter to zero? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    playFeedback(null)
                    speakCurrentNumber(0)
                    showResetDialog = false
                }) {
                    Text("Reset", color = colors.headerAccentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = colors.secondaryTextColor)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.primaryTextColor
        )
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(text = "Target Counter") },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target value (0 to clear)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.primaryTextColor,
                        unfocusedTextColor = colors.primaryTextColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newVal = targetInput.toIntOrNull() ?: 0
                    viewModel.setTarget(newVal)
                    showTargetDialog = false
                }) {
                    Text("Save", color = colors.headerAccentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("Cancel", color = colors.secondaryTextColor)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor
        )
    }

    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = colors.sheetBackgroundColor,
            contentColor = colors.primaryTextColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryTextColor
                    )
                    IconButton(onClick = { showHistorySheet = false }) {
                        Icon(Icons.Default.Close, "Close", tint = colors.primaryTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Highest", color = colors.secondaryTextColor, fontSize = 12.sp)
                        Text("$highestCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryTextColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lowest", color = colors.secondaryTextColor, fontSize = 12.sp)
                        Text("$lowestCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryTextColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Taps", color = colors.secondaryTextColor, fontSize = 12.sp)
                        Text("$totalTaps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.primaryTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.iconBorderColor)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Sessions", color = colors.primaryTextColor, fontWeight = FontWeight.Bold)
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", modifier = Modifier.size(16.dp), tint = colors.headerAccentColor)
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = colors.headerAccentColor)
                        }
                    }
                }

                if (history.isEmpty()) {
                    Text(
                        text = "No history available.",
                        color = colors.secondaryTextColor,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(history.size) { index ->
                            val entry = history[index]
                            androidx.compose.material3.ListItem(
                                colors = androidx.compose.material3.ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                headlineContent = {
                                    Text(
                                        text = entry.operation,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primaryTextColor
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = dateFormatter.format(Date(entry.timestamp)),
                                        color = colors.secondaryTextColor
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        text = entry.valueAfter.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.headerAccentColor
                                    )
                                }
                            )
                            HorizontalDivider(color = colors.iconBorderColor)
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    initialDelay: Long = 500,
    interval: Long = 100,
    onClick: () -> Unit
): Modifier = composed {
    val currentClickListener by rememberUpdatedState(onClick)
    val isPressed by interactionSource.collectIsPressedAsState()
    var wasRepeating by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            wasRepeating = false
            delay(initialDelay)
            wasRepeating = true
            while (isPressed && enabled) {
                currentClickListener()
                delay(interval)
            }
        }
    }
    
    this.clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        enabled = enabled,
        onClick = {
            if (!wasRepeating) {
                currentClickListener()
            }
        }
    )
}
