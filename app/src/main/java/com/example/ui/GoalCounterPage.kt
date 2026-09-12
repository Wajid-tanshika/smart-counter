package com.example.ui

import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoalEntity
import java.util.Locale

@Composable
fun GoalCounterPage(
    goal: GoalEntity,
    goalsViewModel: GoalsViewModel,
    counterViewModel: CounterViewModel,
    onNavigateToGoals: () -> Unit,
    onOpenEdit: (GoalEntity) -> Unit,
    onOpenHistory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeChoice by counterViewModel.theme.collectAsStateWithLifecycle()
    val colors = getAppThemeColors(themeChoice, goal.currentCount)
    val goalColor = parseColorSafe(goal.colorHex, colors.headerAccentColor)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isLocked by remember { mutableStateOf(false) }
    var stepSize by remember { mutableStateOf(1) }
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
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

    val lastFeedbackTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val playFeedback = { isIncrement: Boolean?, isCompleted: Boolean ->
        val now = System.currentTimeMillis()
        val prev = lastFeedbackTime.get()
        if (now - prev >= 100L) {
            lastFeedbackTime.set(now)
            if (soundEnabled) {
                if (isCompleted) {
                    AudioSynth.playTone(800.0, 150)
                } else if (isIncrement == true) {
                    AudioSynth.playTone(600.0, 50)
                } else if (isIncrement == false) {
                    AudioSynth.playTone(400.0, 50)
                } else {
                    AudioSynth.playTone(500.0, 80)
                }
            }
            if (vibrationEnabled) {
                try {
                    if (isCompleted) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (isIncrement == true) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                } catch (e: Throwable) {
                }
            }
        }
    }

    val lastSpeakTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val speakGoalNumber = { number: Int, isCompleted: Boolean ->
        if (voiceEnabled) {
            val now = System.currentTimeMillis()
            if (now - lastSpeakTime.get() >= 500L) {
                lastSpeakTime.set(now)
                val text = if (isCompleted) {
                    "Goal ${goal.name} completed! $number ${goal.unit}"
                } else {
                    "$number ${goal.unit}"
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    val handleIncrement = {
        if (!isLocked && !goal.isPaused) {
            val wasCompleted = goal.isCompleted
            goalsViewModel.incrementGoal(goal.id, stepSize) { updated ->
                val isNowCompleted = updated?.isCompleted == true
                playFeedback(true, isNowCompleted && !wasCompleted)
                if (updated != null) {
                    speakGoalNumber(updated.currentCount, isNowCompleted && !wasCompleted)
                    if (isNowCompleted && !wasCompleted) {
                        showCelebration = true
                    }
                }
            }
        }
    }

    val handleDecrement = {
        if (!isLocked && !goal.isPaused && goal.currentCount > 0) {
            goalsViewModel.decrementGoal(goal.id, stepSize) { updated ->
                playFeedback(false, false)
                if (updated != null) {
                    speakGoalNumber(updated.currentCount, false)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.2f))
                        .border(1.dp, goalColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GOAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = goalColor
                        )
                        if (goal.isDaily) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAILY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = goalColor,
                                modifier = Modifier
                                    .background(goalColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = goal.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val shareText = "🎯 Goal: ${goal.name}\nProgress: ${goal.currentCount}/${goal.targetCount} ${goal.unit} (${goal.progressPercent}%)\nTracked via SMART Counter!"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Goal Progress"))
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = colors.iconContainerColor),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, colors.iconBorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = colors.iconTintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { isLocked = !isLocked },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = colors.iconContainerColor),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, colors.iconBorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (isLocked) goalColor else colors.iconTintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = colors.iconContainerColor),
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, colors.iconBorderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = colors.iconTintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            onClick = { showMenu = false; onOpenEdit(goal) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View History") },
                            onClick = { showMenu = false; onOpenHistory(goal.id) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (goal.isPaused) "Resume Goal" else "Pause Goal") },
                            onClick = { showMenu = false; goalsViewModel.togglePause(goal.id) },
                            leadingIcon = { Icon(if (goal.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("All Goals Dashboard") },
                            onClick = { showMenu = false; onNavigateToGoals() },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = goalColor) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (soundEnabled) "Disable Sounds" else "Enable Sounds") },
                            onClick = { soundEnabled = !soundEnabled; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (voiceEnabled) "Disable Voice" else "Enable Voice") },
                            onClick = { voiceEnabled = !voiceEnabled; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (vibrationEnabled) "Disable Vibration" else "Enable Vibration") },
                            onClick = { vibrationEnabled = !vibrationEnabled; showMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = Color(0xFFEF4444)) },
                            onClick = { showMenu = false; showDeleteDialog = true },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                        )
                    }
                }
            }
        }

        // Center Counter Display
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
                    .shadow(24.dp, shape = RoundedCornerShape(32.dp), ambientColor = goalColor.copy(alpha = 0.4f), spotColor = goalColor.copy(alpha = 0.4f))
                    .background(color = colors.glassBackgroundColor, shape = RoundedCornerShape(32.dp))
                    .border(
                        1.5.dp,
                        if (goal.isCompleted) Color(0xFF22C55E).copy(alpha = 0.8f) else goalColor.copy(alpha = 0.5f),
                        RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = !isLocked && !goal.isPaused) {
                        handleIncrement()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = goal.currentCount,
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
                        label = "GoalCounterValueAnimation"
                    ) { displayedCount ->
                        Text(
                            text = String.format("%01d", displayedCount),
                            fontSize = if (displayedCount > 9999) 70.sp else 100.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-2).sp,
                            color = if (goal.isCompleted) Color(0xFF22C55E) else goalColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("goal_counter_display_${goal.id}")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "OF ${goal.targetCount} ${goal.unit.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = colors.secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { goal.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (goal.isCompleted) Color(0xFF22C55E) else goalColor,
                        trackColor = colors.secondaryTextColor.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${goal.progressPercent}% Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (goal.isCompleted) Color(0xFF22C55E) else goalColor
                        )
                        if (goal.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            val remaining = (goal.targetCount - goal.currentCount).coerceAtLeast(0)
                            Text(
                                text = "• $remaining left",
                                fontSize = 11.sp,
                                color = colors.secondaryTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = colors.secondaryTextColor,
                    modifier = Modifier.padding(end = 12.dp)
                )
                listOf(1, 5, 10, 25).forEach { s ->
                    val isSelected = stepSize == s
                    Surface(
                        onClick = {
                            if (!isLocked) {
                                stepSize = s
                                playFeedback(null, false)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) goalColor else colors.glassBackgroundColor,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) goalColor else colors.glassBorderColor
                        ),
                        shadowElevation = if (isSelected) 4.dp else 0.dp,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(38.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "+$s",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else colors.primaryTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            Button(
                onClick = {
                    if (!isLocked) {
                        playFeedback(null, false)
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
                    .height(42.dp)
                    .widthIn(min = 130.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RESET GOAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Action Controls (- and + buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
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
                        ambientColor = if (goal.currentCount > 0) Color(0xFFEF4444).copy(alpha = 0.3f) else Color.Transparent,
                        spotColor = if (goal.currentCount > 0) Color(0xFFEF4444).copy(alpha = 0.3f) else Color.Transparent
                    )
                    .background(
                        color = if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.glassBackgroundColor,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444).copy(alpha = 0.6f) else colors.secondaryTextColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .repeatingClickable(
                        interactionSource = minusInteractionSource,
                        enabled = !isLocked && !goal.isPaused && goal.currentCount > 0,
                        initialDelay = 400,
                        interval = 100,
                        onClick = handleDecrement
                    )
                    .testTag("goal_decrement_${goal.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease count",
                    tint = if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444) else colors.secondaryTextColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }

            val plusInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, shape = CircleShape, ambientColor = goalColor, spotColor = goalColor)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(goalColor, goalColor.copy(alpha = 0.75f))
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, goalColor.copy(alpha = 0.9f), CircleShape)
                    .repeatingClickable(
                        interactionSource = plusInteractionSource,
                        enabled = !isLocked && !goal.isPaused,
                        initialDelay = 400,
                        interval = 100,
                        onClick = handleIncrement
                    )
                    .testTag("goal_increment_${goal.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase count",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

    // Celebration Dialog
    if (showCelebration) {
        AlertDialog(
            onDismissRequest = { showCelebration = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Goal Target Reached! 🎉",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Superb! You reached ${goal.targetCount} ${goal.unit} for \"${goal.name}\"! Keep up the great consistency!",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCelebration = false },
                    colors = ButtonDefaults.buttonColors(containerColor = goalColor)
                ) {
                    Text("Keep Going!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.secondaryTextColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Goal Progress?") },
            text = {
                Text("Are you sure you want to reset \"${goal.name}\" to ${goal.startingCount} ${goal.unit}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalsViewModel.resetGoal(goal.id)
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = goalColor, fontWeight = FontWeight.Bold)
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

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Goal?") },
            text = {
                Text("Are you sure you want to delete \"${goal.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalsViewModel.deleteGoal(goal.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.secondaryTextColor)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.primaryTextColor
        )
    }
}
