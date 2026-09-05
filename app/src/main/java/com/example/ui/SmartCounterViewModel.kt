package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityHistoryEntity
import com.example.data.AppSettingsManager
import com.example.data.CountingSessionEntity
import com.example.data.InventoryItemEntity
import com.example.data.SmartCounterDatabase
import com.example.data.SmartCounterRepository
import com.example.util.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainNavigationTab {
    HOME,
    SCAN,
    HISTORY,
    INVENTORY,
    SETTINGS
}

enum class CounterSubScreen {
    NONE,
    MANUAL_COUNTER,
    VOICE_COUNTER,
    OBJECT_COUNTER,
    BULK_SCAN,
    PRIVACY_POLICY
}

class SmartCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartCounterRepository
    val settings: AppSettingsManager

    // Tone & Vibration feedback
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator?

    init {
        val db = SmartCounterDatabase.getDatabase(application)
        repository = SmartCounterRepository(db.smartCounterDao())
        settings = AppSettingsManager(application)

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            toneGenerator = null
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // ==================== NAVIGATION STATE ====================
    private val _currentTab = MutableStateFlow(MainNavigationTab.HOME)
    val currentTab: StateFlow<MainNavigationTab> = _currentTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow(CounterSubScreen.NONE)
    val activeSubScreen: StateFlow<CounterSubScreen> = _activeSubScreen.asStateFlow()

    fun selectTab(tab: MainNavigationTab) {
        _currentTab.value = tab
        _activeSubScreen.value = CounterSubScreen.NONE
    }

    fun openSubScreen(screen: CounterSubScreen) {
        _activeSubScreen.value = screen
    }

    fun closeSubScreen() {
        _activeSubScreen.value = CounterSubScreen.NONE
    }

    // ==================== MANUAL & ACTIVE COUNTER STATE ====================
    private val _currentCount = MutableStateFlow(0)
    val currentCount: StateFlow<Int> = _currentCount.asStateFlow()

    private val _targetCount = MutableStateFlow(0)
    val targetCount: StateFlow<Int> = _targetCount.asStateFlow()

    private val _activeSessionTitle = MutableStateFlow("Counter Session")
    val activeSessionTitle: StateFlow<String> = _activeSessionTitle.asStateFlow()

    private val _activeCategory = MutableStateFlow("General")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _customIncrement = MutableStateFlow(1)
    val customIncrement: StateFlow<Int> = _customIncrement.asStateFlow()

    fun increment(step: Int = _customIncrement.value) {
        _currentCount.value += step
        triggerFeedback()
    }

    fun decrement(step: Int = _customIncrement.value) {
        if (_currentCount.value - step >= 0) {
            _currentCount.value -= step
        } else {
            _currentCount.value = 0
        }
        triggerFeedback()
    }

    fun resetCounter() {
        _currentCount.value = 0
        triggerFeedback()
    }

    fun setTarget(target: Int) {
        _targetCount.value = if (target >= 0) target else 0
    }

    fun setSessionTitle(title: String) {
        _activeSessionTitle.value = title
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    fun setCustomIncrement(step: Int) {
        if (step > 0) _customIncrement.value = step
    }

    fun startNewCountSession(initialTitle: String = "Item Count", category: String = "General") {
        _currentCount.value = 0
        _targetCount.value = 0
        _activeSessionTitle.value = initialTitle
        _activeCategory.value = category
        _activeSubScreen.value = CounterSubScreen.MANUAL_COUNTER
    }

    fun saveActiveSession(notes: String = "", onSaved: () -> Unit = {}) {
        val count = _currentCount.value
        val title = _activeSessionTitle.value.ifBlank { "Counting Session" }
        val category = _activeCategory.value
        val target = _targetCount.value

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSession(
                title = title,
                count = count,
                target = target,
                category = category,
                notes = notes
            )
            launch(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    // Repeat Last Count feature
    fun repeatLastCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val last = repository.getLatestSession()
            launch(Dispatchers.Main) {
                if (last != null) {
                    _activeSessionTitle.value = "Repeat: ${last.title}"
                    _activeCategory.value = last.category
                    _targetCount.value = last.target
                    _currentCount.value = 0
                    _activeSubScreen.value = CounterSubScreen.MANUAL_COUNTER
                } else {
                    startNewCountSession("Quick Count")
                }
            }
        }
    }

    private fun triggerFeedback() {
        if (settings.isSoundEnabled.value) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            } catch (e: Exception) {
                // safely ignore
            }
        }
        if (settings.isVibrationEnabled.value && vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            } catch (e: Exception) {
                // safely ignore
            }
        }
    }

    // ==================== BULK SCANNING STATE ====================
    private val _bulkScanItems = MutableStateFlow<Map<String, Int>>(emptyMap())
    val bulkScanItems: StateFlow<Map<String, Int>> = _bulkScanItems.asStateFlow()

    fun onBarcodeScannedInBulk(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        val current = _bulkScanItems.value.toMutableMap()
        val existing = current[clean] ?: 0
        current[clean] = existing + 1
        _bulkScanItems.value = current
        triggerFeedback()
    }

    fun adjustBulkItemQuantity(barcode: String, delta: Int) {
        val current = _bulkScanItems.value.toMutableMap()
        val existing = current[barcode] ?: 0
        val updated = existing + delta
        if (updated > 0) {
            current[barcode] = updated
        } else {
            current.remove(barcode)
        }
        _bulkScanItems.value = current
    }

    fun clearBulkScan() {
        _bulkScanItems.value = emptyMap()
    }

    fun saveBulkScanSession(onSaved: () -> Unit) {
        val items = _bulkScanItems.value
        if (items.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val totalItems = items.values.sum()
            val summary = items.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            repository.saveSession(
                title = "Bulk Scan (${items.size} SKUs)",
                count = totalItems,
                target = 0,
                category = "Bulk Scan",
                notes = summary
            )
            repository.recordScan(
                type = "BULK_SCAN",
                title = "Bulk Scan Batch",
                rawValue = "${items.size} unique codes",
                quantity = totalItems,
                notes = summary
            )
            launch(Dispatchers.Main) {
                clearBulkScan()
                onSaved()
            }
        }
    }

    // ==================== SESSIONS STATE ====================
    val allSessions: StateFlow<List<CountingSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== INVENTORY STATE ====================
    val allInventory: StateFlow<List<InventoryItemEntity>> = repository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inventorySearchQuery = MutableStateFlow("")
    val inventorySearchQuery: StateFlow<String> = _inventorySearchQuery.asStateFlow()

    private val _inventoryFilter = MutableStateFlow("ALL") // "ALL", "LOW_STOCK", "OUT_OF_STOCK", "IN_STOCK"
    val inventoryFilter: StateFlow<String> = _inventoryFilter.asStateFlow()

    fun setInventorySearch(query: String) {
        _inventorySearchQuery.value = query
    }

    fun setInventoryFilter(filter: String) {
        _inventoryFilter.value = filter
    }

    val filteredInventory: StateFlow<List<InventoryItemEntity>> = combine(
        allInventory,
        _inventorySearchQuery,
        _inventoryFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.barcode.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "LOW_STOCK" -> item.isLowStock
                "OUT_OF_STOCK" -> item.isOutOfStock
                "IN_STOCK" -> !item.isLowStock && !item.isOutOfStock
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveInventoryItem(
        id: Long = 0,
        name: String,
        barcode: String,
        quantity: Int,
        minStock: Int,
        category: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveInventoryItem(
                id = id,
                name = name,
                barcode = barcode,
                quantity = quantity,
                minStock = minStock,
                category = category,
                notes = notes
            )
        }
    }

    fun adjustInventoryQty(item: InventoryItemEntity, delta: Int) {
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            repository.adjustInventoryQuantity(item.id, newQty, item.name)
        }
    }

    fun setExactInventoryQty(item: InventoryItemEntity, newQty: Int) {
        val safe = newQty.coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            repository.adjustInventoryQuantity(item.id, safe, item.name)
        }
    }

    fun deleteInventory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventory(id)
        }
    }

    fun clearAllInventory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllInventory()
        }
    }

    suspend fun findInventoryByBarcode(barcode: String): InventoryItemEntity? {
        return repository.findInventoryByBarcode(barcode)
    }

    // ==================== ACTIVITY HISTORY STATE ====================
    val allHistory: StateFlow<List<ActivityHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<ActivityHistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow("ALL") // "ALL", "COUNT", "SCAN", "INVENTORY"
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    fun setHistorySearch(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    val filteredHistory: StateFlow<List<ActivityHistoryEntity>> = combine(
        allHistory,
        _historySearchQuery,
        _historyFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.result.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "COUNT" -> item.type == "COUNT"
                "SCAN" -> item.type in listOf("BARCODE", "QR", "BULK_SCAN")
                "INVENTORY" -> item.type == "INVENTORY"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordScan(type: String, title: String, rawValue: String, quantity: Int = 1, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordScan(type, title, rawValue, quantity, notes)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllHistory()
        }
    }

    // ==================== DASHBOARD & STATS ====================
    val totalSessionsCount: StateFlow<Int> = repository.totalSessionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalItemsCounted: StateFlow<Int> = repository.totalItemsCounted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalScansCount: StateFlow<Int> = repository.totalScansCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalInventoryCount: StateFlow<Int> = repository.totalInventoryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Export helpers
    fun exportCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val hist = allHistory.value
            val inv = allInventory.value
            val uri = ExportHelper.exportToCsv(context, hist, inv)
            launch(Dispatchers.Main) {
                if (uri != null) {
                    ExportHelper.shareExportedFile(context, uri, "text/csv", "Share Smart Counter CSV")
                }
            }
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val hist = allHistory.value
            val inv = allInventory.value
            val counted = totalItemsCounted.value
            val scans = totalScansCount.value
            val uri = ExportHelper.exportToPdf(context, hist, inv, counted, scans)
            launch(Dispatchers.Main) {
                if (uri != null) {
                    ExportHelper.shareExportedFile(context, uri, "application/pdf", "Share Smart Counter PDF")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // safely ignore
        }
    }
}
