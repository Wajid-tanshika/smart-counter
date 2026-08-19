package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdConstants {
    // Production AdMob IDs
    const val APP_ID = "ca-app-pub-1859648502281028~7809732470"
    const val APP_OPEN_AD_ID = "ca-app-pub-1859648502281028/2338291242"
    const val BANNER_AD_ID = "ca-app-pub-1859648502281028/8764134185"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-1859648502281028/8413579150"

    // Google Official Test Ad IDs (Reliable fallback when live account is warming up / pending fill)
    const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // Frequency cap: Minimum 45 seconds between interstitial ads to protect user experience
    const val INTERSTITIAL_MIN_INTERVAL_MS = 45_000L
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
            // Enable test device configurations if applicable
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

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

    fun loadAppOpenAd(useTestFallback: Boolean = false) {
        if (isAppOpenLoading || isAppOpenAdAvailable()) return

        isAppOpenLoading = true
        val adUnitId = if (useTestFallback) AdConstants.TEST_APP_OPEN_AD_ID else AdConstants.APP_OPEN_AD_ID
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                    appOpenLoadedTime = System.currentTimeMillis()
                    Log.d(TAG, "AppOpenAd loaded successfully ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAppOpenLoading = false
                    appOpenAd = null
                    Log.w(TAG, "AppOpenAd failed to load ($adUnitId): code=${loadAdError.code}, msg=${loadAdError.message}")
                    // If live ad failed with No-Fill (Code 3) or Error, fallback to test ad ID once
                    if (!useTestFallback) {
                        Log.d(TAG, "Retrying AppOpenAd with Test Ad Unit...")
                        loadAppOpenAd(useTestFallback = true)
                    }
                }
            }
        )
    }

    fun isAppOpenAdAvailable(): Boolean {
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

    fun loadInterstitialAd(useTestFallback: Boolean = false) {
        if (isInterstitialLoading || interstitialAd != null) return

        isInterstitialLoading = true
        val adUnitId = if (useTestFallback) AdConstants.TEST_INTERSTITIAL_AD_ID else AdConstants.INTERSTITIAL_AD_ID
        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "InterstitialAd loaded successfully ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                    Log.w(TAG, "InterstitialAd failed to load ($adUnitId): code=${loadAdError.code}, msg=${loadAdError.message}")
                    // If live ad had no fill, fallback to test ad ID to verify setup
                    if (!useTestFallback) {
                        Log.d(TAG, "Retrying InterstitialAd with Test Ad Unit...")
                        loadInterstitialAd(useTestFallback = true)
                    }
                }
            }
        )
    }

    fun showInterstitialIfAllowed(
        activity: Activity,
        forceShow: Boolean = false,
        onDismissed: () -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialShownTime

        if (!forceShow && elapsed < AdConstants.INTERSTITIAL_MIN_INTERVAL_MS) {
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
    primaryAdUnitId: String = AdConstants.BANNER_AD_ID,
    fallbackAdUnitId: String = AdConstants.TEST_BANNER_AD_ID
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder in Android Studio / Compose Preview
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text("AdMob Banner Preview", color = Color.White, fontSize = 12.sp)
        }
        return
    }

    var currentAdUnitId by remember { mutableStateOf(primaryAdUnitId) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle events to manage AdView state properly
    DisposableEffect(lifecycleOwner, adViewInstance) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adViewInstance?.resume()
                Lifecycle.Event.ON_PAUSE -> adViewInstance?.pause()
                Lifecycle.Event.ON_DESTROY -> adViewInstance?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adViewInstance?.destroy()
        }
    }

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
                    this.adUnitId = currentAdUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("SmartCounterAds", "Banner loaded successfully: $currentAdUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("SmartCounterAds", "Banner failed: code=${error.code}, msg=${error.message}")
                            // Fallback to Test ID if production ID has no fill
                            if (currentAdUnitId != fallbackAdUnitId) {
                                currentAdUnitId = fallbackAdUnitId
                                this@apply.adUnitId = fallbackAdUnitId
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                    adViewInstance = this
                }
            },
            update = { adView ->
                adViewInstance = adView
            }
        )
    }
}
