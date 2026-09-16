package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.ads.AdManager

class SmartCounterApp : Application(), Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var isAppInForeground = false
    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        Log.d("ADMOB_DEBUG", "SmartCounterApp onCreate: initiating centralized MobileAds initialization")
        // Initialize AdManager early so MobileAds is initialized before any screen loads
        AdManager.getInstance(this)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        startedActivityCount++

        if (startedActivityCount == 1 && !isAppInForeground) {
            isAppInForeground = true
            Log.d("ADMOB_DEBUG", "App entered foreground with activity: ${activity.javaClass.simpleName}")
            if (activity is MainActivity) {
                AdManager.getInstance(this).onAppForeground(activity)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount <= 0) {
            startedActivityCount = 0
            isAppInForeground = false
            Log.d("ADMOB_DEBUG", "App sent to background")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
