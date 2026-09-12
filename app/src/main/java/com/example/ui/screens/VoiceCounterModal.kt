package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SmartCounterViewModel
import com.example.util.VoiceCounterHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCounterModal(
    viewModel: SmartCounterViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val count by viewModel.currentCount.collectAsStateWithLifecycle()
    val target by viewModel.targetCount.collectAsStateWithLifecycle()

    var isListening by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf("Stopped") }
    var recognizedWord by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            voiceStatusText = "Microphone Permission Required"
            Toast.makeText(context, "Microphone permission is needed for voice counter", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper instance with continuous listening
    val voiceHelper = remember {
        VoiceCounterHelper(
            context = context,
            getCurrentCount = { viewModel.currentCount.value },
            onCommandRecognized = { command, spokenWord ->
                recognizedWord = spokenWord
                when (command) {
                    is VoiceCounterHelper.VoiceCommand.SetExact -> {
                        val current = viewModel.currentCount.value
                        val diff = command.value - current
                        if (diff != 0) {
                            if (diff > 0) viewModel.increment(diff) else viewModel.decrement(-diff)
                        }
                    }
                    is VoiceCounterHelper.VoiceCommand.Add -> {
                        viewModel.increment(command.amount)
                    }
                    is VoiceCounterHelper.VoiceCommand.Subtract -> {
                        viewModel.decrement(command.amount)
                    }
                    is VoiceCounterHelper.VoiceCommand.Reset -> {
                        viewModel.resetCounter()
                    }
                    is VoiceCounterHelper.VoiceCommand.SetTarget -> {
                        viewModel.setTarget(command.target)
                    }
                    is VoiceCounterHelper.VoiceCommand.Unknown -> {
                        // Spoken text didn't map to a command
                    }
                }
            },
            onStatusChanged = { status ->
                when (status) {
                    is VoiceCounterHelper.VoiceStatus.Listening -> {
                        isListening = true
                        voiceStatusText = "Listening..."
                        if (status.heardText.isNotBlank()) {
                            recognizedWord = status.heardText.removePrefix("Heard: ").removePrefix("Hearing: ")
                        }
                    }
                    is VoiceCounterHelper.VoiceStatus.Recognized -> {
                        isListening = true
                        voiceStatusText = "Listening..."
                        recognizedWord = status.text
                    }
                    is VoiceCounterHelper.VoiceStatus.Stopped -> {
                        isListening = false
                        voiceStatusText = "Voice Counter Stopped"
                    }
                    is VoiceCounterHelper.VoiceStatus.Error -> {
                        voiceStatusText = status.message
                    }
                    is VoiceCounterHelper.VoiceStatus.PermissionRequired -> {
                        isListening = false
                        hasAudioPermission = false
                        voiceStatusText = "Microphone Permission Required"
                    }
                }
            }
        )
    }

    // Safely stop and release recognizer when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.destroy()
        }
    }

    // Mic Pulsing Animation during active listening
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

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
                            text = "VOICE COUNTER",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Continuous Hands-Free Counting",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        voiceHelper.destroy()
                        onClose()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.recordVoiceCounterSession(count) {
                                Toast.makeText(context, "Voice session saved to History!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Save to History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Count Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Count",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$count",
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (target > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Target: $target (${if (target > 0) (count * 100 / target) else 0}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Status Indicator & Heard Word
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isListening) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFF10B981)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Status: ● Listening..." else "Status: ● Stopped",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (recognizedWord.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heard: \"$recognizedWord\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Big Central Microphone Animation Button
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(if (isListening) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isListening)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    .clickable {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isListening) {
                                voiceHelper.stopListening()
                            } else {
                                voiceHelper.startListening()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            // Primary Control Buttons (Start / Stop Voice Counter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isListening) {
                    Button(
                        onClick = {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                voiceHelper.startListening()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Voice Counter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { voiceHelper.stopListening() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop Voice Counter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { viewModel.resetCounter() },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontWeight = FontWeight.SemiBold)
                }
            }

            // Secondary Quick Adjusters & Save to History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.decrement(1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("-1")
                }

                OutlinedButton(
                    onClick = { viewModel.increment(1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+1")
                }

                Button(
                    onClick = {
                        viewModel.recordVoiceCounterSession(count) {
                            Toast.makeText(context, "Voice session saved to History!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            // Permission denied warning card
            if (!hasAudioPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Microphone Permission Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Smart Counter needs microphone access to continuously listen to your counting numbers.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Allow Microphone")
                        }
                    }
                }
            }

            // Voice Command Guide Card (English & Hindi support)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Continuous Voice Commands Guide",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "English Commands:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• Count sequentially: \"one\", \"two\", \"three\", \"four\"...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Add items: \"add one\", \"plus five\", \"count one\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Subtract items: \"subtract one\", \"minus five\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Set target: \"set target one hundred\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Reset: \"reset\" or \"clear to zero\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hindi Commands (हिंदी):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• गिनती: \"एक\", \"दो\", \"तीन\", \"चार\", \"पाँच\"...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• जोड़ें: \"एक जोड़ो\", \"दो जोड़ो\", \"पाँच जोड़ो\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• घटाएं: \"एक घटाओ\", \"कम करो\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• रीसेट: \"रीसेट\", \"शून्य\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
