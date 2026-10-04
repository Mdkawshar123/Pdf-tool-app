package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.ads.AdFrequencyController
import com.example.ads.AdManager
import com.example.ads.AdsInitializer
import com.example.ads.AppOpenAdManager
import com.example.ads.ConsentManager
import com.example.data.local.AppDatabase
import com.example.data.repository.HistoryRepository

class PdfMasterApplication : Application(), Application.ActivityLifecycleCallbacks {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }

    val adFrequencyController: AdFrequencyController by lazy { AdFrequencyController() }
    val adManager: AdManager by lazy { AdManager(this, adFrequencyController) }
    val appOpenAdManager: AppOpenAdManager by lazy { AppOpenAdManager(this, adFrequencyController) }
    val consentManager: ConsentManager by lazy { ConsentManager(this) }

    private var currentActivity: Activity? = null
    var isProcessingPdf: Boolean = false // Global flag to suppress App Open Ads during critical operations

    private var startedActivityCount: Int = 0
    private var isAppInForeground: Boolean = false
    private var isColdStart: Boolean = true

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Initialize Google Mobile Ads SDK safely
        AdsInitializer.initialize(this) {
            adManager.loadInterstitial()
            appOpenAdManager.loadAd()
        }
    }

    private fun isAdActivity(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name.contains("AdActivity") || name.contains("com.google.android.gms.ads")
    }

    override fun onActivityStarted(activity: Activity) {
        if (!isAdActivity(activity)) {
            currentActivity = activity
        }

        startedActivityCount++
        if (startedActivityCount == 1 && !isAppInForeground) {
            isAppInForeground = true
            // If it's cold start, don't show app open ad; let the main UI render smoothly
            if (isColdStart) {
                isColdStart = false
            } else if (!isProcessingPdf && currentActivity != null && !isAdActivity(currentActivity!!)) {
                // App returning to foreground from background
                appOpenAdManager.showAdIfAvailable(currentActivity!!)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isAdActivity(activity)) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount <= 0) {
            startedActivityCount = 0
            isAppInForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
}
