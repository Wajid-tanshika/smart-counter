package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdConstants {
    const val APP_ID = "ca-app-pub-1859648502281028~7809732470"
    const val APP_OPEN_AD_ID = "ca-app-pub-1859648502281028/2338291242"
    const val BANNER_AD_ID = "ca-app-pub-1859648502281028/8764134185"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-1859648502281028/8413579150"

    // Cooldown duration between interstitials to keep ads non-intrusive (e.g. 3 minutes)
    const val INTERSTITIAL_MIN_INTERVAL_MS = 180_000L
}

class AdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var appOpenLoadedTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime = 0L

    companion object {
        private const val TAG = "SmartCounterAds"

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext).also {
                    instance = it
                    it.init()
                }
            }
        }
    }

    private fun init() {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob Initialized: $initializationStatus")
                loadAppOpenAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob", e)
        }
    }

    // ==================== APP OPEN AD ====================

    fun loadAppOpenAd() {
        if (isAppOpenLoading || isAppOpenAdAvailable()) return

        isAppOpenLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdConstants.APP_OPEN_AD_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                    appOpenLoadedTime = System.currentTimeMillis()
                    Log.d(TAG, "AppOpenAd loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAppOpenLoading = false
                    appOpenAd = null
                    Log.w(TAG, "AppOpenAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAppOpenAdAvailable(): Boolean {
        // App open ads expire after 4 hours
        val isNotExpired = (System.currentTimeMillis() - appOpenLoadedTime) < (4 * 3600 * 1000)
        return appOpenAd != null && isNotExpired
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        if (!isAppOpenAdAvailable()) {
            loadAppOpenAd()
            onComplete()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AppOpenAd shown")
            }
        }

        appOpenAd?.show(activity)
    }

    // ==================== INTERSTITIAL AD ====================

    fun loadInterstitialAd() {
        if (isInterstitialLoading || interstitialAd != null) return

        isInterstitialLoading = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdConstants.INTERSTITIAL_AD_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "InterstitialAd loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Non-intrusive interstitial display:
     * Checks if enough time has passed since the last ad (rate-limited / frequency capped)
     * so user is not interrupted repeatedly.
     */
    fun showInterstitialIfAllowed(
        activity: Activity,
        forceShow: Boolean = false,
        onDismissed: () -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialShownTime

        if (!forceShow && elapsed < AdConstants.INTERSTITIAL_MIN_INTERVAL_MS) {
            // Respect the user: do not show ad too frequently
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShownTime = System.currentTimeMillis()
                    loadInterstitialAd()
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    lastInterstitialShownTime = System.currentTimeMillis()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd()
            onDismissed()
        }
    }
}

// ==================== JETPACK COMPOSE BANNER AD ====================

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.BANNER_AD_ID
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
