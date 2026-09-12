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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
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
import java.util.concurrent.atomic.AtomicBoolean

object AdConstants {
    // 100% Real Live Production AdMob IDs (from AdMob Console)
    const val PROD_APP_ID = "ca-app-pub-1859648502281028~7809732470"
    const val PROD_APP_OPEN_AD_ID = "ca-app-pub-1859648502281028/2338291242"
    const val PROD_BANNER_AD_ID = "ca-app-pub-1859648502281028/8764134185"
    const val PROD_INTERSTITIAL_AD_ID = "ca-app-pub-1859648502281028/8413579150"

    // Backward-compatibility constants preserving exact original references
    const val APP_ID = PROD_APP_ID
    const val APP_OPEN_AD_ID = PROD_APP_OPEN_AD_ID
    const val BANNER_AD_ID = PROD_BANNER_AD_ID
    const val INTERSTITIAL_AD_ID = PROD_INTERSTITIAL_AD_ID

    // Official Google Android Test Ad Unit IDs
    const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // Optional runtime override for testing/diagnostics
    var testModeOverride: Boolean? = null

    val isTestMode: Boolean
        get() = testModeOverride ?: BuildConfig.ADMOB_TEST_MODE

    val appOpenAdId: String
        get() = if (isTestMode) TEST_APP_OPEN_AD_ID else PROD_APP_OPEN_AD_ID

    val bannerAdId: String
        get() = if (isTestMode) TEST_BANNER_AD_ID else PROD_BANNER_AD_ID

    val interstitialAdId: String
        get() = if (isTestMode) TEST_INTERSTITIAL_AD_ID else PROD_INTERSTITIAL_AD_ID

    // Minimum interval between interstitials to protect UX
    const val INTERSTITIAL_MIN_INTERVAL_MS = 30_000L
}

class AdManager private constructor(private val context: Context) {

    private val isInitializing = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val initCallbacks = mutableListOf<() -> Unit>()

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var appOpenLoadedTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime = 0L

    private var isShowingFullScreenAd = false
    private var isSplashCompleted = false

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ADMOB_DEBUG"

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
        if (!isInitializing.compareAndSet(false, true)) {
            Log.d(TAG, "AdMob initialization already in progress or completed")
            return
        }

        Log.d(TAG, "MobileAds initialization started (testMode=${AdConstants.isTestMode})")
        Log.d(TAG, "Active App Open ID: ${AdConstants.appOpenAdId}")
        Log.d(TAG, "Active Banner ID: ${AdConstants.bannerAdId}")
        Log.d(TAG, "Active Interstitial ID: ${AdConstants.interstitialAdId}")

        try {
            // Configure test request configuration if needed
            val reqConfigBuilder = RequestConfiguration.Builder()
            MobileAds.setRequestConfiguration(reqConfigBuilder.build())

            MobileAds.initialize(context) { initializationStatus ->
                isInitialized.set(true)
                Log.d(TAG, "MobileAds initialization completed")
                val statusMap = initializationStatus.adapterStatusMap
                for ((adapterClass, status) in statusMap) {
                    Log.d(TAG, "Adapter: $adapterClass -> state=${status.initializationState}, description=${status.description}, latency=${status.latency}ms")
                }

                // Run any queued callbacks waiting for initialization
                synchronized(initCallbacks) {
                    initCallbacks.forEach { it.invoke() }
                    initCallbacks.clear()
                }

                // Preload primary full-screen formats
                loadAppOpenAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds SDK", e)
        }
    }

    fun doWhenInitialized(action: () -> Unit) {
        if (isInitialized.get()) {
            action()
        } else {
            synchronized(initCallbacks) {
                if (isInitialized.get()) {
                    action()
                } else {
                    initCallbacks.add(action)
                }
            }
        }
    }

    fun setSplashCompleted(completed: Boolean) {
        isSplashCompleted = completed
        Log.d(TAG, "Splash screen completed marked: $completed")
    }

    fun onAppForeground(activity: Activity) {
        Log.d("ADMOB_APP_OPEN", "onAppForeground called, isSplashCompleted=$isSplashCompleted")
        if (isSplashCompleted && !isShowingFullScreenAd) {
            showAppOpenAdIfAvailable(activity)
        }
    }

    // ==================== APP OPEN ADS ====================

    fun loadAppOpenAd() {
        if (isAppOpenLoading) {
            Log.d("ADMOB_APP_OPEN", "AppOpen load already in progress, skipping request")
            return
        }
        if (isAppOpenAdAvailable()) {
            Log.d("ADMOB_APP_OPEN", "AppOpen ad already loaded and valid (age: ${(System.currentTimeMillis() - appOpenLoadedTime) / 1000}s), skipping load")
            return
        }

        isAppOpenLoading = true
        val adUnitId = AdConstants.appOpenAdId
        Log.d("ADMOB_APP_OPEN", "AppOpen load started: unitId=$adUnitId (testMode=${AdConstants.isTestMode})")

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
                    Log.d("ADMOB_APP_OPEN", "AppOpen loaded: unitId=$adUnitId")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAppOpenLoading = false
                    appOpenAd = null
                    Log.e("ADMOB_APP_OPEN", "AppOpen failed to load: code=${loadAdError.code}, message=${loadAdError.message}")
                    AdDiagnostics.logError("ADMOB_APP_OPEN", "App Open", loadAdError, adUnitId)

                    // Safe delayed retry
                    mainHandler.postDelayed({
                        if (!isAppOpenAdAvailable() && !isAppOpenLoading) {
                            Log.d("ADMOB_APP_OPEN", "Retrying AppOpen load after failure...")
                            loadAppOpenAd()
                        }
                    }, 20_000L)
                }
            }
        )
    }

    fun isAppOpenAdAvailable(): Boolean {
        // App open ads expire after 4 hours
        val isNotExpired = (System.currentTimeMillis() - appOpenLoadedTime) < (4 * 3600 * 1000)
        return appOpenAd != null && isNotExpired
    }

    fun showAppOpenAdIfAvailable(activity: Activity?, onComplete: () -> Unit = {}) {
        Log.d("ADMOB_APP_OPEN", "AppOpen show requested")

        if (isShowingFullScreenAd) {
            Log.d("ADMOB_APP_OPEN", "AppOpen suppressed: another full screen ad is currently active")
            onComplete()
            return
        }

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w("ADMOB_APP_OPEN", "AppOpen failed to show: Activity is null, finishing, or destroyed")
            onComplete()
            return
        }

        if (!isAppOpenAdAvailable()) {
            Log.d("ADMOB_APP_OPEN", "AppOpen ad not currently available; initiating background preload")
            loadAppOpenAd()
            onComplete()
            return
        }

        val ad = appOpenAd
        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                Log.d("ADMOB_APP_OPEN", "AppOpen shown: displayed to user")
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingFullScreenAd = false
                appOpenAd = null
                Log.d("ADMOB_APP_OPEN", "AppOpen dismissed: preloading next AppOpen ad")
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingFullScreenAd = false
                appOpenAd = null
                Log.e("ADMOB_APP_OPEN", "AppOpen failed to show: code=${adError.code}, message=${adError.message}, domain=${adError.domain}")
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdImpression() {
                Log.d("ADMOB_APP_OPEN", "AppOpen impression recorded")
            }

            override fun onAdClicked() {
                Log.d("ADMOB_APP_OPEN", "AppOpen clicked")
            }
        }

        ad?.show(activity)
    }

    // ==================== INTERSTITIAL ADS ====================

    fun loadInterstitialAd() {
        if (isInterstitialLoading) {
            Log.d("ADMOB_INTERSTITIAL", "Interstitial load already in progress, skipping request")
            return
        }
        if (interstitialAd != null) {
            Log.d("ADMOB_INTERSTITIAL", "Interstitial already loaded and available, skipping request")
            return
        }

        isInterstitialLoading = true
        val adUnitId = AdConstants.interstitialAdId
        Log.d("ADMOB_INTERSTITIAL", "Interstitial load started: unitId=$adUnitId (testMode=${AdConstants.isTestMode})")

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d("ADMOB_INTERSTITIAL", "Interstitial loaded: unitId=$adUnitId")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                    Log.e("ADMOB_INTERSTITIAL", "Interstitial failed: code=${loadAdError.code}, message=${loadAdError.message}")
                    AdDiagnostics.logError("ADMOB_INTERSTITIAL", "Interstitial", loadAdError, adUnitId)

                    // Safe delayed retry
                    mainHandler.postDelayed({
                        if (interstitialAd == null && !isInterstitialLoading) {
                            Log.d("ADMOB_INTERSTITIAL", "Retrying Interstitial load after failure...")
                            loadInterstitialAd()
                        }
                    }, 20_000L)
                }
            }
        )
    }

    fun isInterstitialAdLoaded(): Boolean {
        return interstitialAd != null
    }

    fun showInterstitialIfAllowed(
        activity: Activity?,
        forceShow: Boolean = false,
        onDismissed: () -> Unit = {}
    ) {
        Log.d("ADMOB_INTERSTITIAL", "Interstitial show requested (forceShow=$forceShow)")

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w("ADMOB_INTERSTITIAL", "Interstitial failed to show: Activity is null, finishing, or destroyed")
            onDismissed()
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialShownTime
        if (!forceShow && elapsed < AdConstants.INTERSTITIAL_MIN_INTERVAL_MS) {
            Log.d("ADMOB_INTERSTITIAL", "Interstitial throttled: ${elapsed / 1000}s elapsed since last ad (min interval ${AdConstants.INTERSTITIAL_MIN_INTERVAL_MS / 1000}s)")
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.w("ADMOB_INTERSTITIAL", "Interstitial failed to show: ad is null (not loaded yet). Preloading...")
            loadInterstitialAd()
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
                lastInterstitialShownTime = System.currentTimeMillis()
                Log.d("ADMOB_INTERSTITIAL", "Interstitial shown: displayed to user")
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingFullScreenAd = false
                interstitialAd = null
                lastInterstitialShownTime = System.currentTimeMillis()
                Log.d("ADMOB_INTERSTITIAL", "Interstitial dismissed: preloading next interstitial")
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingFullScreenAd = false
                interstitialAd = null
                Log.e("ADMOB_INTERSTITIAL", "Interstitial failed to show: code=${adError.code}, message=${adError.message}, domain=${adError.domain}")
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdImpression() {
                Log.d("ADMOB_INTERSTITIAL", "Interstitial impression recorded")
            }

            override fun onAdClicked() {
                Log.d("ADMOB_INTERSTITIAL", "Interstitial clicked")
            }
        }

        ad.show(activity)
    }

    // ==================== AD INSPECTOR ====================

    fun openAdInspector(activity: Activity, onClosed: (error: String?) -> Unit = {}) {
        Log.d(TAG, "Ad Inspector open requested")
        doWhenInitialized {
            MobileAds.openAdInspector(activity) { error ->
                if (error != null) {
                    Log.e(TAG, "Ad Inspector failed to open: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    onClosed("Ad Inspector error: ${error.message} (code ${error.code})")
                } else {
                    Log.d(TAG, "Ad Inspector opened and completed successfully")
                    onClosed(null)
                }
            }
        }
    }
}

// ==================== BANNER AD COMPOSABLE ====================

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.bannerAdId
) {
    if (LocalInspectionMode.current) {
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Remember a single AdView instance for this composable so recomposition does not trigger reloads
    val adView = remember(adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("ADMOB_BANNER", "Banner onAdLoaded successfully: unitId=$adUnitId, size=${adSize}")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Log.e(
                        "ADMOB_BANNER",
                        "Banner failed: code=${error.code}, message=${error.message}, domain=${error.domain}, cause=${error.cause}"
                    )
                    error.responseInfo?.let {
                        Log.d("ADMOB_BANNER", "Banner responseInfo: $it")
                    }
                    AdDiagnostics.logError("ADMOB_BANNER", "Banner", error, adUnitId)
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Log.d("ADMOB_BANNER", "Banner onAdOpened: user clicked ad and full screen overlay opened")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.d("ADMOB_BANNER", "Banner onAdClosed: overlay closed, returned to application")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d("ADMOB_BANNER", "Banner onAdClicked: user clicked banner")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d("ADMOB_BANNER", "Banner onAdImpression: impression recorded")
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ADMOB_BANNER", "Banner lifecycle ON_RESUME: resuming adView")
                    adView.resume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("ADMOB_BANNER", "Banner lifecycle ON_PAUSE: pausing adView")
                    adView.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d("ADMOB_BANNER", "Banner lifecycle ON_DESTROY: destroying adView")
                    adView.destroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Load ad safely once MobileAds is initialized
        AdManager.getInstance(context).doWhenInitialized {
            try {
                Log.d("ADMOB_BANNER", "Loading banner with unitId=$adUnitId (testMode=${AdConstants.isTestMode})")
                adView.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e("ADMOB_BANNER", "Exception while loading banner ad", e)
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
            Log.d("ADMOB_BANNER", "Banner composable onDispose: adView destroyed")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { adView }
        )
    }
}
