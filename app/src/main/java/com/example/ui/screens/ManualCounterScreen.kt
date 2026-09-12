package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CounterSubScreen
import com.example.ui.SmartCounterViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ManualCounterScreen(
    viewModel: SmartCounterViewModel,
    onNavigateBack: () -> Unit,
    onOpenVoiceCounter: () -> Unit,
    onOpenObjectCounter: () -> Unit
) {
    val count by viewModel.currentCount.collectAsStateWithLifecycle()
    val target by viewModel.targetCount.collectAsStateWithLifecycle()
    val sessionTitle by viewModel.activeSessionTitle.collectAsStateWithLifecycle()
    val category by viewModel.activeCategory.collectAsStateWithLifecycle()
    val customIncrement by viewModel.customIncrement.collectAsStateWithLifecycle()
    val isSoundOn by viewModel.settings.isSoundEnabled.collectAsStateWithLifecycle()
    val isVibeOn by viewModel.settings.isVibrationEnabled.collectAsStateWithLifecycle()

    var showTargetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomStepDialog by remember { mutableStateOf(false) }

    val progressFraction = if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val progressPercent = (progressFraction * 100).toInt()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Category: $category",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Voice Counter shortcut
                    IconButton(onClick = onOpenVoiceCounter) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Counter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Object Counter shortcut
                    IconButton(onClick = onOpenObjectCounter) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Object Counter"
                        )
                    }

                    // Target Dialog shortcut
                    IconButton(onClick = { showTargetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Set Target",
                            tint = if (target > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Save Session
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Session"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Target Progress Card (if set)
            if (target > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target: $target ($progressPercent%)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (count >= target) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                            if (count >= target) {
                                Text(
                                    text = "TARGET REACHED! 🎉",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (count >= target) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Quick Target hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTargetDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Set a count target",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Massive Clickable Display Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.increment() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$count",
                        fontSize = 88.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap anywhere to add (+$customIncrement)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Increments Bar (+1, +5, +10, +50, +100, Step)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val increments = listOf(1, 5, 10, 50, 100)
                increments.forEach { step ->
                    val isCurrent = customIncrement == step
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.setCustomIncrement(step)
                                viewModel.increment(step)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "+$step",
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Custom Step button
                IconButton(
                    onClick = { showCustomStepDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Custom Step",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Primary Big Controller Controls (+ and - and RESET)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement Button
                Button(
                    onClick = { viewModel.decrement() },
                    modifier = Modifier
                        .size(76.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Subtract",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // RESET Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET")
                }

                // Giant Immediate Increment Button
                Button(
                    onClick = { viewModel.increment() },
                    modifier = Modifier
                        .size(96.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Toggles Footer: Sound, Vibration, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { viewModel.settings.setSoundEnabled(!isSoundOn) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = "Sound",
                        tint = if (isSoundOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSoundOn) "Sound ON" else "Sound OFF",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable { viewModel.settings.setVibrationEnabled(!isVibeOn) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Vibration",
                        tint = if (isVibeOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVibeOn) "Vibe ON" else "Vibe OFF",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showSaveDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Session", fontSize = 12.sp)
                }
            }
        }
    }

    // ==================== DIALOGS ====================

    // Set Target Dialog
    if (showTargetDialog) {
        var inputTarget by remember { mutableStateOf(if (target > 0) target.toString() else "") }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Set Count Target") },
            text = {
                Column {
                    Text(
                        text = "Enter a goal number to monitor progress in real-time.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputTarget,
                        onValueChange = { inputTarget = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val num = inputTarget.toIntOrNull() ?: 0
                    viewModel.setTarget(num)
                    showTargetDialog = false
                }) {
                    Text("Set Target")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setTarget(0)
                    showTargetDialog = false
                }) {
                    Text("Clear Target")
                }
            }
        )
    }

    // Custom Step Dialog
    if (showCustomStepDialog) {
        var inputStep by remember { mutableStateOf(customIncrement.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomStepDialog = false },
            title = { Text("Set Increment Step") },
            text = {
                Column {
                    Text("Choose how much each tap adds or subtracts:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputStep,
                        onValueChange = { inputStep = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Step value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val step = inputStep.toIntOrNull() ?: 1
                    viewModel.setCustomIncrement(step)
                    showCustomStepDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomStepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Counter?") },
            text = { Text("Are you sure you want to reset the count to 0? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCounter()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset to 0")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Session Dialog
    if (showSaveDialog) {
        var titleText by remember { mutableStateOf(sessionTitle) }
        var categoryText by remember { mutableStateOf(category) }
        var notesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Counting Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Count: $count items",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Session Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        label = { Text("Category (e.g. Boxes, Bottles, Inventory)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setSessionTitle(titleText)
                    viewModel.setCategory(categoryText)
                    viewModel.saveActiveSession(notes = notesText) {
                        showSaveDialog = false
                        onNavigateBack()
                    }
                }) {
                    Text("Save to History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
