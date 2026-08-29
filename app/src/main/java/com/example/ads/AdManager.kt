package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdConstants {
    // 100% Real Live Production AdMob IDs (Play Store Monetization)
    const val APP_ID = "ca-app-pub-1859648502281028~7809732470"
    const val APP_OPEN_AD_ID = "ca-app-pub-1859648502281028/3997929605"
    const val BANNER_AD_ID = "ca-app-pub-1859648502281028/6432521251"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-1859648502281028/2435285123"

    // Frequency cap: Minimum 30 seconds between interstitial ads to protect user experience and maximize fill
    const val INTERSTITIAL_MIN_INTERVAL_MS = 30_000L
}

class AdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var appOpenLoadedTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

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
                Log.d(TAG, "AdMob Live SDK Initialized: $initializationStatus")
                loadAppOpenAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob SDK", e)
        }
    }

    // ==================== REAL APP OPEN AD ====================

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
                    Log.d(TAG, "Live AppOpenAd loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAppOpenLoading = false
                    appOpenAd = null
                    Log.w(TAG, "Live AppOpenAd failed to load: code=${loadAdError.code}, msg=${loadAdError.message}")
                    // Retry loading live ad after a short delay (15 seconds)
                    mainHandler.postDelayed({
                        loadAppOpenAd()
                    }, 15_000L)
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
                Log.d(TAG, "Live AppOpenAd displayed")
            }
        }

        appOpenAd?.show(activity)
    }

    // ==================== REAL INTERSTITIAL AD ====================

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
                    Log.d(TAG, "Live InterstitialAd loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                    Log.w(TAG, "Live InterstitialAd failed to load: code=${loadAdError.code}, msg=${loadAdError.message}")
                    // Retry loading live interstitial ad after 20 seconds
                    mainHandler.postDelayed({
                        loadInterstitialAd()
                    }, 20_000L)
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
                    Log.d(TAG, "Live InterstitialAd displayed")
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd()
            onDismissed()
        }
    }
}

// ==================== REAL LIVE BANNER AD ====================

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.BANNER_AD_ID
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
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("SmartCounterAds", "Live Banner loaded successfully: $adUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("SmartCounterAds", "Live Banner failed: code=${error.code}, msg=${error.message}")
                            // Auto retry live ad after 15 seconds
                            postDelayed({
                                loadAd(AdRequest.Builder().build())
                            }, 15_000L)
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
