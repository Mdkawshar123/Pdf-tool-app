package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(
    private val context: Context,
    private val frequencyController: AdFrequencyController
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd: Boolean = false
    private var isShowingAd: Boolean = false
    private var loadTime: Long = 0

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdMobConfig.appOpenAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d("AppOpenAdManager", "App open ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    appOpenAd = null
                    Log.w("AppOpenAdManager", "App open ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun wasLoadTimeLessThan4HoursAgo(): Boolean {
        val numMilliSecondsPerHour = 3600000L
        val dateDifference = Date().time - loadTime
        return dateDifference < numMilliSecondsPerHour * 4
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThan4HoursAgo()
    }

    private fun isAdActivity(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name.contains("AdActivity") || name.contains("com.google.android.gms.ads")
    }

    fun showAdIfAvailable(activity: Activity, onDismissed: () -> Unit = {}) {
        if (isShowingAd) {
            onDismissed()
            return
        }

        if (activity.isFinishing || activity.isDestroyed || isAdActivity(activity)) {
            onDismissed()
            return
        }

        if (!isAdAvailable() || !frequencyController.canShowAppOpen()) {
            loadAd()
            onDismissed()
            return
        }

        val ad = appOpenAd
        if (ad == null) {
            onDismissed()
            loadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                frequencyController.onFullScreenAdDismissed()
                onDismissed()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                frequencyController.onFullScreenAdDismissed()
                Log.w("AppOpenAdManager", "App open ad failed to show: ${adError.message}")
                onDismissed()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                frequencyController.onAppOpenShown()
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e("AppOpenAdManager", "Exception showing AppOpenAd", e)
            appOpenAd = null
            isShowingAd = false
            frequencyController.onFullScreenAdDismissed()
            onDismissed()
            loadAd()
        }
    }
}
