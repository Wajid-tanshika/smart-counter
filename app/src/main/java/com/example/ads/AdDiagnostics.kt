package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AdDiagnostics {
    private const val PREFS_NAME = "smart_counter_prefs"
    private const val EVENTS_KEY = "ad_diagnostics_events"
    private const val MAX_EVENTS = 80

    private val lock = Any()
    @Volatile private var enabled = true
    private var prefs: SharedPreferences? = null

    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    fun initialize(context: Context) {
        synchronized(lock) {
            if (prefs != null) return
            prefs = context.applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
            )
            val saved = prefs?.getString(EVENTS_KEY, "") ?: ""
            _events.value = saved.split("\n").filter { it.isNotBlank() }.takeLast(MAX_EVENTS)
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        Log.i("ADMOB_DIAGNOSTICS", "Diagnostics recording enabled=$value")
    }

    fun logEvent(category: String, message: String) {
        if (!enabled) return

        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(Date())

        val cleanMessage = message
            .replace('\n', ' ')
            .replace('\r', ' ')
            .take(3500)

        val entry = "[$timestamp] [$category] $cleanMessage"

        synchronized(lock) {
            val store = prefs ?: return
            val updated = (_events.value + entry).takeLast(MAX_EVENTS)
            _events.value = updated
            store.edit().putString(EVENTS_KEY, updated.joinToString("\n")).apply()
        }
    }

    fun clearEvents() {
        synchronized(lock) {
            _events.value = emptyList()
            prefs?.edit()?.remove(EVENTS_KEY)?.apply()
        }
    }

    fun getErrorDescription(code: Int): String {
        return when (code) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR ->
                "INTERNAL_ERROR (0): Internal SDK/server error."
            AdRequest.ERROR_CODE_INVALID_REQUEST ->
                "INVALID_REQUEST (1): Invalid ad request or configuration."
            AdRequest.ERROR_CODE_NETWORK_ERROR ->
                "NETWORK_ERROR (2): Network, DNS or timeout problem."
            AdRequest.ERROR_CODE_NO_FILL ->
                "NO_FILL (3): Request succeeded but no ad was available."
            else -> "UNKNOWN_ERROR_CODE ($code)"
        }
    }

    fun logError(tag: String, adType: String, error: LoadAdError, adUnitId: String) {
        val report = buildString {
            append("Ad format=$adType")
            append(" | Ad unit=$adUnitId")
            append(" | Test mode=${AdConstants.isTestMode}")
            append(" | Error code=${error.code}")
            append(" | Description=${getErrorDescription(error.code)}")
            append(" | Message=${error.message}")
            append(" | Domain=${error.domain}")
            append(" | Cause=${error.cause}")
            append(" | Response=${error.responseInfo}")
        }
        Log.w(tag, report)
        logEvent("AD LOAD FAILURE", report)
    }
}
