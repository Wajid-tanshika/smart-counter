package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdmobBanner
import com.example.data.AppThemeMode
import com.example.ui.SmartCounterViewModel

@Composable
fun SettingsScreen(
    viewModel: SmartCounterViewModel,
    onOpenPrivacyPolicy: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val settings = viewModel.settings

    val currentTheme by settings.themeMode.collectAsStateWithLifecycle()
    val isSoundOn by settings.isSoundEnabled.collectAsStateWithLifecycle()
    val isVibeOn by settings.isVibrationEnabled.collectAsStateWithLifecycle()
    val autoSaveScans by settings.autoSaveScans.collectAsStateWithLifecycle()
    val lowStockAlerts by settings.lowStockAlerts.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearInventoryDialog by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Title
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 1. Appearance Section
                SettingsGroupHeader(title = "Appearance")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                                Column {
                                    Text("Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(
                                        when (currentTheme) {
                                            AppThemeMode.LIGHT -> "Light Theme"
                                            AppThemeMode.DARK -> "Dark Theme"
                                            AppThemeMode.SYSTEM -> "System Default"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { settings.setThemeMode(AppThemeMode.LIGHT) }) {
                                    Text("Light", fontWeight = if (currentTheme == AppThemeMode.LIGHT) FontWeight.Bold else FontWeight.Normal)
                                }
                                TextButton(onClick = { settings.setThemeMode(AppThemeMode.DARK) }) {
                                    Text("Dark", fontWeight = if (currentTheme == AppThemeMode.DARK) FontWeight.Bold else FontWeight.Normal)
                                }
                                TextButton(onClick = { settings.setThemeMode(AppThemeMode.SYSTEM) }) {
                                    Text("Auto", fontWeight = if (currentTheme == AppThemeMode.SYSTEM) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                // 2. Counter Feedback Section
                SettingsGroupHeader(title = "Counter Feedback")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 14.dp)) {
                        SettingsSwitchRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "Sound Effects",
                            subtitle = "Play short click sound on every count",
                            checked = isSoundOn,
                            onCheckedChange = { settings.setSoundEnabled(it) }
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.Vibration,
                            title = "Haptic Vibration",
                            subtitle = "Vibrate briefly on every increment/decrement",
                            checked = isVibeOn,
                            onCheckedChange = { settings.setVibrationEnabled(it) }
                        )
                    }
                }

                // 3. Scanner Preferences
                SettingsGroupHeader(title = "Scanner")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 14.dp)) {
                        SettingsSwitchRow(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Auto-save Barcode Scans",
                            subtitle = "Automatically record scanned codes into History",
                            checked = autoSaveScans,
                            onCheckedChange = { settings.setAutoSaveScans(it) }
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Low Stock Indicators",
                            subtitle = "Highlight items reaching minimum stock threshold",
                            checked = lowStockAlerts,
                            onCheckedChange = { settings.setLowStockAlerts(it) }
                        )
                    }
                }

                // 4. Data Export & Storage
                SettingsGroupHeader(title = "Data & Export")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.TableChart,
                            title = "Export to CSV",
                            subtitle = "Export all counts and inventory into spreadsheet",
                            onClick = { viewModel.exportCsv(context) }
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.PictureAsPdf,
                            title = "Export to PDF",
                            subtitle = "Generate executive PDF summary report",
                            onClick = { viewModel.exportPdf(context) }
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear Activity History",
                            subtitle = "Permanently remove all past counting logs",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearHistoryDialog = true }
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear All Inventory",
                            subtitle = "Permanently remove all product items",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showClearInventoryDialog = true }
                        )
                    }
                }

                // 5. About & Support
                SettingsGroupHeader(title = "About & Legal")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.Policy,
                            title = "Privacy Policy",
                            subtitle = "Read our offline-first data protection terms",
                            onClick = onOpenPrivacyPolicy
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.Refresh,
                            title = "Replay Onboarding",
                            subtitle = "View intro features walkthrough again",
                            onClick = onReplayOnboarding
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.Share,
                            title = "Share Smart Counter",
                            subtitle = "Recommend this app to friends or colleagues",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "Count objects, scan barcodes & track inventory with Smart Counter: https://play.google.com/store/apps/details?id=com.smartcounter.ai")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Smart Counter"))
                            }
                        )

                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            title = "App Information",
                            subtitle = "Smart Counter v2.0.0 (Build 3) • com.smartcounter.ai",
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Responsible AdMob Banner at bottom
            AdmobBanner(modifier = Modifier.fillMaxWidth())
        }
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("Are you sure you want to delete all activity and counting records? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Inventory Confirmation Dialog
    if (showClearInventoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearInventoryDialog = false },
            title = { Text("Clear All Inventory?") },
            text = { Text("Are you sure you want to delete all inventory items and barcodes? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllInventory()
                        showClearInventoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Inventory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearInventoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != MaterialTheme.colorScheme.onSurface) titleColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
