package com.example.ads

import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError

object AdDiagnostics {

    fun getErrorDescription(code: Int): String {
        return when (code) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR ->
                "ERROR_CODE_INTERNAL_ERROR (0) - Something happened internally in Google AdMob server or SDK (e.g. invalid response received from ad server)."
            AdRequest.ERROR_CODE_INVALID_REQUEST ->
                "ERROR_CODE_INVALID_REQUEST (1) - The ad request was invalid (e.g. ad unit ID incorrect, missing APPLICATION_ID in manifest, or improper configuration)."
            AdRequest.ERROR_CODE_NETWORK_ERROR ->
                "ERROR_CODE_NETWORK_ERROR (2) - The ad request was unsuccessful due to network connectivity / DNS / timeout."
            AdRequest.ERROR_CODE_NO_FILL ->
                "ERROR_CODE_NO_FILL (3) - The ad request was successful, but no ad was returned due to lack of ad inventory. Live production ad units frequently return No Fill for unverified, newly registered apps or non-Play-Store debug installs. Use official Google test IDs during development."
            else ->
                "UNKNOWN_ERROR_CODE ($code)"
        }
    }

    fun logError(tag: String, adType: String, error: LoadAdError, adUnitId: String) {
        val errorDesc = getErrorDescription(error.code)
        val responseInfoStr = error.responseInfo?.toString() ?: "None"
        val causeStr = error.cause?.message ?: "None"

        val report = buildString {
            appendLine("==================== ADMOB FAILURE DIAGNOSTIC ====================")
            appendLine("Ad Format    : $adType")
            appendLine("Ad Unit ID   : $adUnitId")
            appendLine("Test Mode    : ${AdConstants.isTestMode}")
            appendLine("Error Code   : ${error.code} -> $errorDesc")
            appendLine("Message      : ${error.message}")
            appendLine("Domain       : ${error.domain}")
            appendLine("Cause        : $causeStr")
            appendLine("ResponseInfo : $responseInfoStr")
            appendLine("==================================================================")
        }

        Log.e(tag, report)
    }
}
