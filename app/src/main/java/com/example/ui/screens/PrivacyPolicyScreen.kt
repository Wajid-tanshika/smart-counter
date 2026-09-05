package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Smart Counter Privacy Policy",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Last updated: September 2026 • Version 2.0.0 (Build 3)\nPackage: com.smartcounter.ai",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Your privacy is of utmost importance to us. Smart Counter is designed as an offline-first utility application. We do not require account registration or collect your personal data.",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            PrivacySectionCard(
                icon = Icons.Default.CloudOff,
                title = "1. Offline Data Storage",
                content = "All counting numbers, sessions, barcode scans, and inventory stock quantities are stored locally on your device using an encrypted local SQLite/Room database. No tally numbers or inventory records are ever uploaded to external servers."
            )

            PrivacySectionCard(
                icon = Icons.Default.CameraAlt,
                title = "2. Camera Permission",
                content = "Camera access is used strictly on-device for scanning QR codes and barcodes using Google ML Kit, and for camera-assisted object counting. Images and video feeds are processed entirely in memory in real time and are never recorded, saved, or uploaded to any cloud server."
            )

            PrivacySectionCard(
                icon = Icons.Default.Mic,
                title = "3. Microphone Permission",
                content = "Microphone access is optional and used solely for voice counting commands (such as 'add one', 'subtract one', 'reset') using the standard Android on-device SpeechRecognizer. The microphone is only active while the Voice Counter is explicitly open and running; there is no background recording."
            )

            PrivacySectionCard(
                icon = Icons.Default.Public,
                title = "4. Advertising (Google AdMob)",
                content = "Smart Counter uses Google AdMob to display banner and interstitial ads in accordance with Google Play Developer Policies. AdMob may collect standard device identifiers (such as Advertising ID) and non-personal diagnostic data to serve appropriate advertisements. You can reset your Advertising ID anytime in your Android device settings."
            )

            PrivacySectionCard(
                icon = Icons.Default.Security,
                title = "5. User Control and Data Deletion",
                content = "You retain complete control over your data. You can export your data to CSV or PDF at any time, or permanently clear all counting and inventory history via the Settings screen with a single tap."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PrivacySectionCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
