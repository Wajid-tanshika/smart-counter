package com.example.ui

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GoalEntity
import com.example.data.GoalHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Pre-defined Preset Templates for 1-tap Goal Creation
data class GoalPreset(
    val name: String,
    val target: Int,
    val unit: String,
    val iconName: String,
    val colorHex: String,
    val isDaily: Boolean,
    val description: String = ""
)

val GOAL_PRESETS = listOf(
    GoalPreset("Water", 8, "glasses", "water", "#0EA5E9", true, "Stay hydrated throughout the day"),
    GoalPreset("Push-ups", 50, "reps", "fitness", "#EF4444", true, "Build strength and fitness"),
    GoalPreset("Study", 30, "questions", "study", "#8B5CF6", true, "Study topics, questions or lessons"),
    GoalPreset("Reading", 30, "pages", "book", "#F59E0B", true, "Read book pages or chapters"),
    GoalPreset("Walking", 10000, "steps", "walk", "#22C55E", true, "Reach your daily step milestone"),
    GoalPreset("Meditation", 20, "minutes", "meditate", "#6366F1", true, "Mindfulness and breathing"),
    GoalPreset("Dhikr", 100, "counts", "prayer", "#10B981", true, "Spiritual daily remembrance & tasbeeh"),
    GoalPreset("Coding", 5, "tasks", "code", "#EC4899", true, "Complete daily programming tasks"),
    GoalPreset("Running", 5, "km", "walk", "#F97316", true, "Cardio and outdoor running")
)

fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "water" -> Icons.Default.WaterDrop
        "fitness" -> Icons.Default.FitnessCenter
        "book" -> Icons.Default.MenuBook
        "prayer" -> Icons.Default.SelfImprovement
        "walk" -> Icons.Default.DirectionsWalk
        "study" -> Icons.Default.School
        "star" -> Icons.Default.Star
        "flame" -> Icons.Default.LocalFireDepartment
        "trophy" -> Icons.Default.EmojiEvents
        "code" -> Icons.Default.Code
        "meditate" -> Icons.Default.Spa
        else -> Icons.Default.Flag
    }
}

fun parseColorSafe(hex: String, fallback: Color = Color(0xFF3B82F6)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(
    goalsViewModel: GoalsViewModel,
    counterViewModel: CounterViewModel,
    onNavigateBack: () -> Unit,
    onSelectGoalForCounter: (GoalEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        goalsViewModel.resetSelectedState()
    }

    val goals by goalsViewModel.filteredGoals.collectAsStateWithLifecycle()
    val allGoals by goalsViewModel.allGoals.collectAsStateWithLifecycle()
    val selectedFilter by goalsViewModel.selectedFilter.collectAsStateWithLifecycle()
    val stats by goalsViewModel.stats.collectAsStateWithLifecycle()
    val selectedGoalIdForHistory by goalsViewModel.selectedGoalForHistory.collectAsStateWithLifecycle()
    val goalHistoryList by goalsViewModel.goalHistoryList.collectAsStateWithLifecycle()

    val themeChoice by counterViewModel.theme.collectAsStateWithLifecycle()
    val colors = getAppThemeColors(themeChoice, 0)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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
    val speakGoalProgress = { goalName: String, count: Int, unit: String, isCompleted: Boolean ->
        if (voiceEnabled) {
            val now = System.currentTimeMillis()
            if (now - lastSpeakTime.get() >= 500L) {
                lastSpeakTime.set(now)
                val text = if (isCompleted) {
                    "Goal $goalName completed!"
                } else {
                    "$count $unit"
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToReset by remember { mutableStateOf<GoalEntity?>(null) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var celebrationGoalName by remember { mutableStateOf<String?>(null) }

    val gradientBackground = Brush.verticalGradient(colors = colors.backgroundGradient)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.headerAccentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Goal") },
                text = { Text("Add Goal", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_goal_fab")
            )
        }
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, colors.iconBorderColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Counter",
                                tint = colors.iconTintColor
                            )
                        }

                        Column {
                            Text(
                                text = "HABITS & TARGETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = colors.headerAccentColor
                            )
                            Text(
                                text = "Goals",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryTextColor
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showStatsDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, colors.iconBorderColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Goal Statistics",
                                tint = colors.iconTintColor
                            )
                        }

                        IconButton(
                            onClick = { voiceEnabled = !voiceEnabled },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.iconContainerColor
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, colors.iconBorderColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (voiceEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = "Toggle Voice",
                                tint = if (voiceEnabled) colors.headerAccentColor else colors.iconTintColor
                            )
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { goalsViewModel.setFilter(filter) },
                            label = {
                                val count = when (filter) {
                                    GoalFilter.ALL -> allGoals.size
                                    GoalFilter.ACTIVE -> allGoals.count { !it.isPaused && !it.isCompleted }
                                    GoalFilter.DAILY -> allGoals.count { it.isDaily }
                                    GoalFilter.COMPLETED -> allGoals.count { it.isCompleted }
                                }
                                Text(
                                    text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.headerAccentColor.copy(alpha = 0.2f),
                                selectedLabelColor = colors.headerAccentColor,
                                containerColor = colors.glassBackgroundColor,
                                labelColor = colors.secondaryTextColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = colors.glassBorderColor,
                                selectedBorderColor = colors.headerAccentColor,
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Summary Stats Overview Card (Mini)
                if (allGoals.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(colors.glassBackgroundColor, RoundedCornerShape(20.dp))
                            .border(1.dp, colors.glassBorderColor, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = colors.headerAccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Progress Overview",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryTextColor
                                    )
                                }
                                Text(
                                    text = "${stats.totalProgressPercent}% Overall",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.headerAccentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (stats.totalProgressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = colors.headerAccentColor,
                                trackColor = colors.secondaryTextColor.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Active: ${stats.activeGoals}",
                                    fontSize = 11.sp,
                                    color = colors.secondaryTextColor
                                )
                                Text(
                                    text = "Completed: ${stats.completedGoals}",
                                    fontSize = 11.sp,
                                    color = colors.secondaryTextColor
                                )
                                Text(
                                    text = "Daily: ${stats.dailyGoalsCount}",
                                    fontSize = 11.sp,
                                    color = colors.secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Goals List or Empty State
                if (goals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = colors.headerAccentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (allGoals.isEmpty()) "No Goals Yet" else "No Goals in this Filter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryTextColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (allGoals.isEmpty()) "Choose a template below or tap Add Goal to get started!" else "Try switching the filter chips above.",
                                fontSize = 13.sp,
                                color = colors.secondaryTextColor,
                                textAlign = TextAlign.Center
                            )

                            if (allGoals.isEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Quick Starter Presets:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.headerAccentColor,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    GOAL_PRESETS.take(6).forEach { preset ->
                                        AssistChip(
                                            onClick = {
                                                goalsViewModel.addGoal(
                                                    name = preset.name,
                                                    targetCount = preset.target,
                                                    startingCount = 0,
                                                    unit = preset.unit,
                                                    iconName = preset.iconName,
                                                    colorHex = preset.colorHex,
                                                    isDaily = preset.isDaily
                                                )
                                            },
                                            label = { Text("${preset.name} (${preset.target} ${preset.unit})", fontSize = 12.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = getGoalIcon(preset.iconName),
                                                    contentDescription = null,
                                                    tint = parseColorSafe(preset.colorHex),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = colors.glassBackgroundColor,
                                                labelColor = colors.primaryTextColor
                                            ),
                                            border = AssistChipDefaults.assistChipBorder(
                                                enabled = true,
                                                borderColor = colors.glassBorderColor
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(goals, key = { it.id }) { goal ->
                            GoalCard(
                                goal = goal,
                                colors = colors,
                                onOpenCounter = { onSelectGoalForCounter(goal) },
                                onIncrement = {
                                    val wasCompleted = goal.isCompleted
                                    goalsViewModel.incrementGoal(goal.id) { updated ->
                                        val isNowCompleted = updated?.isCompleted == true
                                        playFeedback(true, isNowCompleted && !wasCompleted)
                                        if (updated != null) {
                                            speakGoalProgress(updated.name, updated.currentCount, updated.unit, isNowCompleted && !wasCompleted)
                                            if (isNowCompleted && !wasCompleted) {
                                                celebrationGoalName = updated.name
                                            }
                                        }
                                    }
                                },
                                onDecrement = {
                                    goalsViewModel.decrementGoal(goal.id) { updated ->
                                        playFeedback(false, false)
                                        if (updated != null) {
                                            speakGoalProgress(updated.name, updated.currentCount, updated.unit, false)
                                        }
                                    }
                                },
                                onEdit = { editingGoal = goal },
                                onHistory = { goalsViewModel.selectGoalForHistory(goal.id) },
                                onTogglePause = { goalsViewModel.togglePause(goal.id) },
                                onReset = { goalToReset = goal },
                                onDelete = { goalToDelete = goal }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            com.example.ads.AdmobBanner()
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Celebration Snackbar / Dialog when a Goal is completed
    if (celebrationGoalName != null) {
        AlertDialog(
            onDismissRequest = { celebrationGoalName = null },
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
                    text = "Goal Completed! 🎉",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Congratulations! You reached your target for \"$celebrationGoalName\". Keep up the great work!",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { celebrationGoalName = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.headerAccentColor)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.secondaryTextColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Add Goal Dialog
    if (showAddDialog) {
        GoalEditorDialog(
            goalToEdit = null,
            colors = colors,
            onDismiss = { showAddDialog = false },
            onSave = { name, target, starting, unit, icon, colorHex, isDaily ->
                goalsViewModel.addGoal(
                    name = name,
                    targetCount = target,
                    startingCount = starting,
                    unit = unit,
                    iconName = icon,
                    colorHex = colorHex,
                    isDaily = isDaily
                )
                showAddDialog = false
            }
        )
    }

    // Edit Goal Dialog
    if (editingGoal != null) {
        GoalEditorDialog(
            goalToEdit = editingGoal,
            colors = colors,
            onDismiss = { editingGoal = null },
            onSave = { name, target, starting, unit, icon, colorHex, isDaily ->
                editingGoal?.let { current ->
                    goalsViewModel.updateGoal(
                        id = current.id,
                        name = name,
                        targetCount = target,
                        unit = unit,
                        iconName = icon,
                        colorHex = colorHex,
                        isDaily = isDaily
                    )
                }
                editingGoal = null
            }
        )
    }

    // Reset Goal Confirmation Dialog
    if (goalToReset != null) {
        AlertDialog(
            onDismissRequest = { goalToReset = null },
            title = { Text(text = "Reset Goal?") },
            text = {
                Text("Are you sure you want to reset \"${goalToReset?.name}\" back to ${goalToReset?.startingCount} ${goalToReset?.unit}? History will be saved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalToReset?.let { goalsViewModel.resetGoal(it.id) }
                        playFeedback(null, false)
                        goalToReset = null
                    }
                ) {
                    Text("Reset", color = colors.headerAccentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToReset = null }) {
                    Text("Cancel", color = colors.secondaryTextColor)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.primaryTextColor
        )
    }

    // Delete Goal Confirmation Dialog
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(text = "Delete Goal?") },
            text = {
                Text("Are you sure you want to delete \"${goalToDelete?.name}\"? All its tracking history will also be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalToDelete?.let { goalsViewModel.deleteGoal(it.id) }
                        goalToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel", color = colors.secondaryTextColor)
                }
            },
            containerColor = colors.sheetBackgroundColor,
            titleContentColor = colors.primaryTextColor,
            textContentColor = colors.primaryTextColor
        )
    }

    // Goal History Modal Sheet
    if (selectedGoalIdForHistory != null) {
        val selectedGoal = allGoals.find { it.id == selectedGoalIdForHistory }
        GoalHistorySheet(
            goal = selectedGoal,
            historyList = goalHistoryList,
            colors = colors,
            onDismiss = { goalsViewModel.selectGoalForHistory(null) },
            onClearHistory = {
                selectedGoalIdForHistory?.let { goalsViewModel.clearGoalHistory(it) }
            }
        )
    }

    // Goals Statistics Sheet / Dialog
    if (showStatsDialog) {
        GoalStatsSheet(
            stats = stats,
            colors = colors,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
fun GoalCard(
    goal: GoalEntity,
    colors: AppThemeColors,
    onOpenCounter: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onTogglePause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val goalColor = parseColorSafe(goal.colorHex, colors.headerAccentColor)
    val minusInteractionSource = remember { MutableInteractionSource() }
    val plusInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape = RoundedCornerShape(24.dp), ambientColor = goalColor.copy(alpha = 0.2f), spotColor = goalColor.copy(alpha = 0.2f))
            .background(colors.glassBackgroundColor, RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (goal.isCompleted) goalColor.copy(alpha = 0.8f) else colors.glassBorderColor,
                RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Header Row: Icon, Title, Badges, Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = goal.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (goal.isCompleted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (goal.isDaily) {
                                Text(
                                    text = "Daily Goal",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = goalColor,
                                    modifier = Modifier
                                        .background(goalColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            if (goal.isPaused) {
                                Text(
                                    text = "Paused",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.secondaryTextColor
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            onClick = { onEdit(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View History") },
                            onClick = { onHistory(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (goal.isPaused) "Resume Goal" else "Pause Goal") },
                            onClick = { onTogglePause(); showMenu = false },
                            leadingIcon = { Icon(if (goal.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset Progress") },
                            onClick = { onReset(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = Color(0xFFEF4444)) },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Count & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${goal.currentCount}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (goal.isCompleted) goalColor else colors.primaryTextColor
                    )
                    Text(
                        text = " / ${goal.targetCount} ${goal.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.secondaryTextColor,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                Text(
                    text = "${goal.progressPercent}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (goal.isCompleted) Color(0xFF22C55E) else goalColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (goal.isCompleted) Color(0xFF22C55E) else goalColor,
                trackColor = colors.secondaryTextColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Controls: Open Counter, Minus & Plus Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenCounter,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = goalColor),
                    border = BorderStroke(1.dp, goalColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = goalColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Full Counter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decrement Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.glassBackgroundColor
                        )
                        .border(
                            1.dp,
                            if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444).copy(alpha = 0.4f) else colors.iconBorderColor,
                            CircleShape
                        )
                        .repeatingClickable(
                            interactionSource = minusInteractionSource,
                            enabled = !goal.isPaused && goal.currentCount > 0,
                            initialDelay = 400,
                            interval = 120,
                            onClick = onDecrement
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement",
                        tint = if (goal.currentCount > 0 && !goal.isPaused) Color(0xFFEF4444) else colors.secondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Increment Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (!goal.isPaused) goalColor else colors.glassBackgroundColor
                        )
                        .border(
                            1.dp,
                            if (!goal.isPaused) goalColor else colors.iconBorderColor,
                            CircleShape
                        )
                        .repeatingClickable(
                            interactionSource = plusInteractionSource,
                            enabled = !goal.isPaused,
                            initialDelay = 400,
                            interval = 120,
                            onClick = onIncrement
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment",
                        tint = if (!goal.isPaused) Color.White else colors.secondaryTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
fun GoalEditorDialog(
    goalToEdit: GoalEntity?,
    colors: AppThemeColors,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Int, starting: Int, unit: String, icon: String, colorHex: String, isDaily: Boolean) -> Unit
) {
    var isCustomizing by remember { mutableStateOf(goalToEdit != null) }
    var selectedPresetName by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var targetText by remember { mutableStateOf(goalToEdit?.targetCount?.toString() ?: "10") }
    var startingText by remember { mutableStateOf(goalToEdit?.startingCount?.toString() ?: "0") }
    var unit by remember { mutableStateOf(goalToEdit?.unit ?: "times") }
    var iconName by remember { mutableStateOf(goalToEdit?.iconName ?: "flag") }
    var colorHex by remember { mutableStateOf(goalToEdit?.colorHex ?: "#3B82F6") }
    var isDaily by remember { mutableStateOf(goalToEdit?.isDaily ?: true) }

    val iconOptions = listOf(
        "water" to Icons.Default.WaterDrop,
        "fitness" to Icons.Default.FitnessCenter,
        "study" to Icons.Default.School,
        "prayer" to Icons.Default.SelfImprovement,
        "book" to Icons.Default.MenuBook,
        "walk" to Icons.Default.DirectionsWalk,
        "meditate" to Icons.Default.Spa,
        "code" to Icons.Default.Code,
        "flame" to Icons.Default.LocalFireDepartment,
        "star" to Icons.Default.Star,
        "trophy" to Icons.Default.EmojiEvents
    )

    val colorOptions = listOf(
        "#3B82F6", "#0EA5E9", "#10B981", "#22C55E", "#EF4444", "#F59E0B", "#8B5CF6", "#EC4899", "#F97316", "#06B6D4"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (goalToEdit != null) {
                            "Edit Goal"
                        } else if (!isCustomizing) {
                            "Select a Goal"
                        } else {
                            "Configure Goal"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primaryTextColor
                    )
                    Text(
                        text = if (!isCustomizing && goalToEdit == null) {
                            "Choose what you want to work on"
                        } else {
                            "Set your target & preferences"
                        },
                        fontSize = 12.sp,
                        color = colors.secondaryTextColor
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isCustomizing && goalToEdit == null) {
                    // STEP 1: PRESET SELECTION CATALOG
                    Text(
                        text = "POPULAR GOALS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = colors.headerAccentColor
                    )

                    GOAL_PRESETS.forEach { preset ->
                        val presetColor = parseColorSafe(preset.colorHex)
                        val isSelected = selectedPresetName == preset.name

                        Surface(
                            onClick = {
                                name = preset.name
                                targetText = preset.target.toString()
                                startingText = "0"
                                unit = preset.unit
                                iconName = preset.iconName
                                colorHex = preset.colorHex
                                isDaily = preset.isDaily
                                selectedPresetName = preset.name
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) presetColor.copy(alpha = 0.15f) else colors.glassBackgroundColor,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) presetColor else colors.glassBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(presetColor.copy(alpha = 0.2f))
                                        .border(1.dp, presetColor.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getGoalIcon(preset.iconName),
                                        contentDescription = null,
                                        tint = presetColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.primaryTextColor
                                    )
                                    if (preset.description.isNotBlank()) {
                                        Text(
                                            text = preset.description,
                                            fontSize = 11.sp,
                                            color = colors.secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Target: ${preset.target} ${preset.unit}${if (preset.isDaily) " / day" else ""}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = presetColor
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = presetColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Select",
                                        tint = colors.secondaryTextColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Goal Option Card
                    val isCustomSelected = selectedPresetName == "Custom Goal"
                    Surface(
                        onClick = {
                            selectedPresetName = "Custom Goal"
                            name = ""
                            targetText = "10"
                            startingText = "0"
                            unit = "times"
                            iconName = "star"
                            colorHex = "#3B82F6"
                            isDaily = true
                            isCustomizing = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCustomSelected) colors.headerAccentColor.copy(alpha = 0.15f) else colors.glassBackgroundColor,
                        border = BorderStroke(
                            1.dp,
                            if (isCustomSelected) colors.headerAccentColor else colors.glassBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.headerAccentColor.copy(alpha = 0.2f))
                                    .border(1.dp, colors.headerAccentColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.headerAccentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Goal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.primaryTextColor
                                )
                                Text(
                                    text = "Create your own custom goal & target",
                                    fontSize = 11.sp,
                                    color = colors.secondaryTextColor
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Create Custom",
                                tint = colors.headerAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // STEP 2 / EDIT MODE: FORM FIELDS
                    if (goalToEdit == null) {
                        TextButton(
                            onClick = { isCustomizing = false },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(14.dp),
                                tint = colors.headerAccentColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Choose a different goal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.headerAccentColor
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name (e.g., Water, Push-ups)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (e.g., glasses, reps)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    if (goalToEdit == null) {
                        OutlinedTextField(
                            value = startingText,
                            onValueChange = { startingText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Starting Count (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Select Icon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.secondaryTextColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { (key, vector) ->
                            val isSelected = iconName == key
                            val activeColor = parseColorSafe(colorHex)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor.copy(alpha = 0.25f) else colors.glassBackgroundColor)
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) activeColor else colors.iconBorderColor, CircleShape)
                                    .clickable { iconName = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = key,
                                    tint = if (isSelected) activeColor else colors.secondaryTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text("Select Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.secondaryTextColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            val color = parseColorSafe(hex)
                            val isSelected = colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { colorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Goal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.primaryTextColor)
                            Text("Resets or tracks as a daily routine", fontSize = 11.sp, color = colors.secondaryTextColor)
                        }
                        Switch(
                            checked = isDaily,
                            onCheckedChange = { isDaily = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isCustomizing && goalToEdit == null) {
                // Actions when viewing presets list
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedPresetName != null && selectedPresetName != "Custom Goal") {
                        OutlinedButton(
                            onClick = { isCustomizing = true },
                            border = BorderStroke(1.dp, colors.headerAccentColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Customize", fontSize = 12.sp, color = colors.headerAccentColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val targetInt = targetText.toIntOrNull() ?: 10
                            val startInt = startingText.toIntOrNull() ?: 0
                            if (name.isNotBlank()) {
                                onSave(name, targetInt, startInt, unit, iconName, colorHex, isDaily)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.headerAccentColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (selectedPresetName != null) "Add Goal" else "Select a Goal",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Actions in customize / edit mode
                Button(
                    onClick = {
                        val targetInt = targetText.toIntOrNull() ?: 10
                        val startInt = startingText.toIntOrNull() ?: 0
                        if (name.isNotBlank()) {
                            onSave(name, targetInt, startInt, unit, iconName, colorHex, isDaily)
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.headerAccentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (goalToEdit == null) "Start Tracking Goal" else "Save Changes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.secondaryTextColor)
            }
        },
        containerColor = colors.sheetBackgroundColor,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalHistorySheet(
    goal: GoalEntity?,
    historyList: List<GoalHistoryEntity>,
    colors: AppThemeColors,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBackgroundColor,
        contentColor = colors.primaryTextColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goal History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryTextColor
                    )
                    if (goal != null) {
                        Text(
                            text = "${goal.name} (Target: ${goal.targetCount} ${goal.unit})",
                            fontSize = 12.sp,
                            color = colors.secondaryTextColor
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.primaryTextColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyList.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear History", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history recorded yet for this goal.",
                        color = colors.secondaryTextColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.glassBackgroundColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.operation,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (entry.operation == "COMPLETED") Color(0xFF22C55E) else colors.primaryTextColor
                                    )
                                    if (entry.isCompleted) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dateFormatter.format(Date(entry.timestamp)),
                                    fontSize = 11.sp,
                                    color = colors.secondaryTextColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${entry.previousValue} ➔ ${entry.newValue}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = colors.headerAccentColor
                                )
                                Text(
                                    text = "${entry.progressPercent}% of target",
                                    fontSize = 10.sp,
                                    color = colors.secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStatsSheet(
    stats: GoalStats,
    colors: AppThemeColors,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBackgroundColor,
        contentColor = colors.primaryTextColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goals Statistics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryTextColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.primaryTextColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Goals",
                    value = "${stats.totalGoals}",
                    subtitle = "${stats.activeGoals} Active",
                    color = colors.headerAccentColor,
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Completed",
                    value = "${stats.completedGoals}",
                    subtitle = if (stats.totalGoals > 0) "${(stats.completedGoals * 100) / stats.totalGoals}% completion" else "0%",
                    color = Color(0xFF22C55E),
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Avg Progress",
                    value = "${stats.totalProgressPercent}%",
                    subtitle = "Across all goals",
                    color = Color(0xFFF59E0B),
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Daily Routines",
                    value = "${stats.dailyGoalsCount}",
                    subtitle = "${stats.dailyProgressPercent}% avg progress",
                    color = Color(0xFF8B5CF6),
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.bestPerformingGoalName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.glassBackgroundColor, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.glassBorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Top Performing Goal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.secondaryTextColor
                            )
                            Text(
                                text = stats.bestPerformingGoalName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.primaryTextColor
                            )
                            Text(
                                text = "${stats.bestPerformingGoalProgress}% completed",
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    colors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(colors.glassBackgroundColor, RoundedCornerShape(16.dp))
            .border(1.dp, colors.glassBorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, fontSize = 12.sp, color = colors.secondaryTextColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = colors.secondaryTextColor)
        }
    }
}
