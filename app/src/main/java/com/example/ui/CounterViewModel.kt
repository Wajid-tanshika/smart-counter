package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CounterDatabase
import com.example.data.CounterState
import com.example.data.CounterRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTheme { LIGHT, DARK, NEON_BLUE, RED_GAMING, NEON_GREEN, GOLD, PURPLE }

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CounterRepository(CounterDatabase.getDatabase(application).counterDao())
    private val prefs = application.getSharedPreferences("counter_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(AppTheme.valueOf(prefs.getString("theme", AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name))
    val theme = _theme.asStateFlow()

    private val _step = MutableStateFlow(prefs.getInt("step_size", 1))
    val step = _step.asStateFlow()

    private val _target = MutableStateFlow(prefs.getInt("target_count", 0))
    val target = _target.asStateFlow()

    fun setStep(newStep: Int) {
        _step.value = newStep
        prefs.edit().putInt("step_size", newStep).apply()
    }

    fun setTarget(newTarget: Int) {
        _target.value = newTarget
        prefs.edit().putInt("target_count", newTarget).apply()
    }

    private val _highestCount = MutableStateFlow(prefs.getInt("highest", 0))
    val highestCount = _highestCount.asStateFlow()

    private val _lowestCount = MutableStateFlow(prefs.getInt("lowest", 0))
    val lowestCount = _lowestCount.asStateFlow()

    private val _totalTaps = MutableStateFlow(prefs.getInt("total_taps", 0))
    val totalTaps = _totalTaps.asStateFlow()
    
    fun setTheme(newTheme: AppTheme) {
        _theme.value = newTheme
        prefs.edit().putString("theme", newTheme.name).apply()
    }

    private fun updateStats(newCount: Int, isTap: Boolean) {
        if (newCount > _highestCount.value) {
            _highestCount.value = newCount
        }
        if (newCount < _lowestCount.value || (_lowestCount.value == 0 && newCount > 0 && prefs.getInt("lowest", -1) == -1)) {
            _lowestCount.value = newCount
        }
        if (isTap) {
            _totalTaps.value += 1
        }
    }

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    val history = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // A shared flow to buffer count updates and debounce database writes.
    // Extra buffer capacity ensures tryEmit won't block or drop if the collector is briefly busy.
    private val dbSaveChannel = MutableSharedFlow<Int>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // Migration safeguard for new themes in AppTheme
        try {
            AppTheme.valueOf(prefs.getString("theme", AppTheme.DARK.name) ?: AppTheme.DARK.name)
        } catch (e: Exception) {
            prefs.edit().putString("theme", AppTheme.DARK.name).apply()
        }
        // Load the initial stored count value from the database ONCE on startup
        viewModelScope.launch {
            try {
                val state = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.getCounterStateSync()
                }
                if (state != null) {
                    _count.value = state.countValue
                }
            } catch (t: Throwable) {
                // Fallback gracefully
                t.printStackTrace()
            }
        }

        // Collect dbSaveChannel with 300ms debounce.
        // This prevents disk I/O overload and thread lockups during rapid/repeating click gestures.
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            dbSaveChannel
                .debounce(300)
                .collect { value ->
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                            repository.saveCountValue(value)
                            prefs.edit()
                                .putInt("highest", _highestCount.value)
                                .putInt("lowest", _lowestCount.value)
                                .putInt("total_taps", _totalTaps.value)
                                .apply()
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
        }
    }

    fun increment() {
        val next = _count.value + _step.value
        _count.value = next
        dbSaveChannel.tryEmit(next)
        updateStats(next, true)
    }

    fun decrement() {
        val current = _count.value
        if (current > 0) {
            val next = (current - _step.value).coerceAtLeast(0)
            _count.value = next
            dbSaveChannel.tryEmit(next)
            updateStats(next, true)

        }
    }

    fun reset() {
        val currentCount = _count.value
        if (currentCount > 0) {
            viewModelScope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                    repository.saveSessionToHistory(currentCount)
                }
            }
        }
        _count.value = 0
        dbSaveChannel.tryEmit(0)
    }

    fun clearHistory() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                repository.clearHistory()
            }
        }
    }
}
