package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InventoryItemEntity
import com.example.scanner.BarcodeAnalyzer
import com.example.ui.SmartCounterViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

enum class ScannerMode(val title: String) {
    SINGLE("Single Scan"),
    BULK("Bulk Scan"),
    OBJECT("Object Counter")
}

@Composable
fun ScannerScreen(
    viewModel: SmartCounterViewModel,
    onNavigateToInventory: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var currentMode by remember { mutableStateOf(ScannerMode.SINGLE) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // Last scanned result in Single mode
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var lastScannedFormat by remember { mutableStateOf<String?>(null) }
    var linkedInventoryItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showAddToInventoryDialog by remember { mutableStateOf(false) }

    // Bulk scan items
    val bulkItems by viewModel.bulkScanItems.collectAsStateWithLifecycle()

    // Object Counting mode state
    val objectCategories = listOf("Bottles", "Boxes", "Coins", "Fruits", "Packages", "Other")
    var selectedCategory by remember { mutableStateOf("Boxes") }
    var objectCount by remember { mutableIntStateOf(0) }

    // Check inventory when barcode is scanned
    LaunchedEffect(lastScannedValue) {
        val code = lastScannedValue
        if (!code.isNullOrBlank()) {
            val item = withContext(Dispatchers.IO) {
                viewModel.findInventoryByBarcode(code)
            }
            linkedInventoryItem = item
        } else {
            linkedInventoryItem = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Mode Switcher Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScannerMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentMode = mode
                                lastScannedValue = null
                            },
                            label = { Text(mode.title, fontSize = 12.sp) }
                        )
                    }
                }

                // Flashlight toggle button
                IconButton(
                    onClick = {
                        val newFlashState = !isFlashlightOn
                        cameraControl?.cameraControl?.enableTorch(newFlashState)
                        isFlashlightOn = newFlashState
                    }
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (isFlashlightOn) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Camera Preview Area or Permission Denial Placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is needed to scan QR codes, barcodes, and count objects on your device.",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            } else {
                // Live CameraX View
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        try {
                            cameraControl?.cameraControl?.enableTorch(false)
                        } catch (_: Exception) {}
                        try {
                            cameraProvider?.unbindAll()
                        } catch (e: Exception) {
                            Log.e("ScannerScreen", "Camera unbind error on dispose", e)
                        }
                        try {
                            cameraExecutor.shutdown()
                        } catch (e: Exception) {
                            Log.e("ScannerScreen", "Camera executor shutdown error", e)
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(
                                cameraExecutor,
                                BarcodeAnalyzer { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        val firstBarcode = barcodes.first()
                                        val rawValue = firstBarcode.rawValue ?: return@BarcodeAnalyzer
                                        val formatName = getBarcodeFormatName(firstBarcode.format)

                                        when (currentMode) {
                                            ScannerMode.SINGLE -> {
                                                if (lastScannedValue == null) {
                                                    lastScannedValue = rawValue
                                                    lastScannedFormat = formatName
                                                    viewModel.recordScan(
                                                        type = if (firstBarcode.format == Barcode.FORMAT_QR_CODE) "QR" else "BARCODE",
                                                        title = "Scan: $rawValue",
                                                        rawValue = rawValue
                                                    )
                                                }
                                            }
                                            ScannerMode.BULK -> {
                                                viewModel.onBarcodeScannedInBulk(rawValue)
                                            }
                                            ScannerMode.OBJECT -> {
                                                // Object Mode uses manual or visual tap
                                            }
                                        }
                                    }
                                }
                            )

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                provider.unbindAll()
                                cameraControl = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                Log.e("ScannerScreen", "Camera binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = {
                        try {
                            cameraProvider?.unbindAll()
                        } catch (_: Exception) {}
                    }
                )

                // Scanner Target Reticle Frame Overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 2.dp,
                                color = if (currentMode == ScannerMode.BULK) Color(0xFF10B981) else Color(0xFF3B82F6),
                                shape = RoundedCornerShape(20.dp)
                            )
                    )
                }
            }
        }

        // Bottom Result Card or Mode Specific Controls
        when (currentMode) {
            ScannerMode.SINGLE -> {
                SingleScanResultBottomCard(
                    scannedValue = lastScannedValue,
                    scannedFormat = lastScannedFormat,
                    linkedItem = linkedInventoryItem,
                    onScanAgain = { lastScannedValue = null },
                    onAddToInventory = { showAddToInventoryDialog = true },
                    onAdjustInventory = { delta ->
                        linkedInventoryItem?.let { item ->
                            viewModel.adjustInventoryQty(item, delta)
                            linkedInventoryItem = item.copy(quantity = (item.quantity + delta).coerceAtLeast(0))
                        }
                    }
                )
            }
            ScannerMode.BULK -> {
                BulkScanBottomCard(
                    bulkItems = bulkItems,
                    onAdjustQuantity = { barcode, delta ->
                        viewModel.adjustBulkItemQuantity(barcode, delta)
                    },
                    onClear = { viewModel.clearBulkScan() },
                    onSaveSession = {
                        viewModel.saveBulkScanSession {
                            Toast.makeText(context, "Bulk scan saved to history!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            ScannerMode.OBJECT -> {
                ObjectCountingBottomCard(
                    count = objectCount,
                    selectedCategory = selectedCategory,
                    categories = objectCategories,
                    onCategoryChange = { selectedCategory = it },
                    onIncrement = { objectCount += 1 },
                    onDecrement = { if (objectCount > 0) objectCount -= 1 },
                    onReset = { objectCount = 0 },
                    onSave = {
                        viewModel.saveActiveSession(notes = "Counted with camera assist: $selectedCategory") {
                            Toast.makeText(context, "Object count saved to history!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Add To Inventory Dialog for freshly scanned barcode
    if (showAddToInventoryDialog && lastScannedValue != null) {
        var productName by remember { mutableStateOf("") }
        var productQty by remember { mutableStateOf("1") }
        var minStock by remember { mutableStateOf("5") }
        var category by remember { mutableStateOf("General") }

        AlertDialog(
            onDismissRequest = { showAddToInventoryDialog = false },
            title = { Text("Add to Inventory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Barcode: ${lastScannedValue ?: ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = productQty,
                            onValueChange = { productQty = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Initial Qty") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Min Stock") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val qty = productQty.toIntOrNull() ?: 1
                    val min = minStock.toIntOrNull() ?: 5
                    viewModel.saveInventoryItem(
                        name = productName.ifBlank { "Scanned Product" },
                        barcode = lastScannedValue ?: "",
                        quantity = qty,
                        minStock = min,
                        category = category,
                        notes = "Scanned with barcode reader"
                    )
                    showAddToInventoryDialog = false
                    Toast.makeText(context, "Added to inventory!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToInventoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SingleScanResultBottomCard(
    scannedValue: String?,
    scannedFormat: String?,
    linkedItem: InventoryItemEntity?,
    onScanAgain: () -> Unit,
    onAddToInventory: () -> Unit,
    onAdjustInventory: (Int) -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (scannedValue == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Align QR code or Barcode inside the box to scan",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val isUrl = scannedValue.startsWith("http://", ignoreCase = true) ||
                        scannedValue.startsWith("https://", ignoreCase = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Format: ${scannedFormat ?: "Barcode"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onScanAgain) {
                        Text("Scan Again")
                    }
                }

                Text(
                    text = scannedValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Actions: Copy & Open URL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Barcode Result", scannedValue)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 13.sp)
                    }

                    if (isUrl) {
                        Button(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedValue))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open URL", fontSize = 13.sp)
                        }
                    }
                }

                // Inventory Link Integration (Requirement 13)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    if (linkedItem != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "In Inventory: ${linkedItem.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Stock: ${linkedItem.quantity} units",
                                    fontSize = 12.sp,
                                    color = if (linkedItem.isLowStock) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onAdjustInventory(-1) }) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                IconButton(onClick = { onAdjustInventory(+1) }) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item not yet in inventory",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onAddToInventory,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add to Inventory", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkScanBottomCard(
    bulkItems: Map<String, Int>,
    onAdjustQuantity: (String, Int) -> Unit,
    onClear: () -> Unit,
    onSaveSession: () -> Unit
) {
    val totalCount = bulkItems.values.sum()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bulk Scanned Items ($totalCount total)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${bulkItems.size} unique SKUs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (bulkItems.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text("Clear")
                        }
                        Button(
                            onClick = onSaveSession,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save (${totalCount})")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (bulkItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scan barcodes repeatedly. Each scan increments the quantity automatically.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bulkItems.entries.toList(), key = { it.key }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.key,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onAdjustQuantity(entry.key, -1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Subtract", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${entry.value}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { onAdjustQuantity(entry.key, +1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectCountingBottomCard(
    count: Int,
    selectedCategory: String,
    categories: List<String>,
    onCategoryChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Category selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategoryChange(cat) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Count display and controller
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$count $selectedCategory",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Camera-assisted counter (Manual override enabled)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDecrement,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Subtract")
                    }

                    Button(
                        onClick = onIncrement,
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Count")
                }
            }
        }
    }
}

fun getBarcodeFormatName(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_PDF417 -> "PDF-417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        else -> "Barcode"
    }
}
