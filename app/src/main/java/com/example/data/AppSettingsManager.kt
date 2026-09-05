package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_counter_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _defaultIncrement = MutableStateFlow(prefs.getInt("default_increment", 1))
    val defaultIncrement: StateFlow<Int> = _defaultIncrement.asStateFlow()

    private val _isAutoSaveEnabled = MutableStateFlow(prefs.getBoolean("auto_save_enabled", true))
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    private val _autoSaveScans = MutableStateFlow(prefs.getBoolean("auto_save_scans", true))
    val autoSaveScans: StateFlow<Boolean> = _autoSaveScans.asStateFlow()

    private val _lowStockAlerts = MutableStateFlow(prefs.getBoolean("low_stock_alerts", true))
    val lowStockAlerts: StateFlow<Boolean> = _lowStockAlerts.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private fun loadThemeMode(): AppThemeMode {
        return when (prefs.getString("theme_mode", "SYSTEM")) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _soundEnabled(enabled)
    }
    private fun _soundEnabled(enabled: Boolean) { _isSoundEnabled.value = enabled }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _isVibrationEnabled.value = enabled
    }

    fun setDefaultIncrement(increment: Int) {
        val safe = if (increment in 1..1000) increment else 1
        prefs.edit().putInt("default_increment", safe).apply()
        _defaultIncrement.value = safe
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_enabled", enabled).apply()
        _isAutoSaveEnabled.value = enabled
    }

    fun setAutoSaveScans(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_scans", enabled).apply()
        _autoSaveScans.value = enabled
    }

    fun setLowStockAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("low_stock_alerts", enabled).apply()
        _lowStockAlerts.value = enabled
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_done", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarding_done", false).apply()
        _isOnboardingCompleted.value = false
    }
}
