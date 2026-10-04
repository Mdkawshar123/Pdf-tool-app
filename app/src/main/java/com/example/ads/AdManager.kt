package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(
    private val context: Context,
    val frequencyController: AdFrequencyController = AdFrequencyController()
) {
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading: Boolean = false

    fun loadInterstitial() {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdMobConfig.interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d("AdManager", "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w("AdManager", "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAdActivity(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name.contains("AdActivity") || name.contains("com.google.android.gms.ads")
    }

    fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || !frequencyController.canShowInterstitial()) {
            onClosed()
            loadInterstitial()
            return
        }

        if (activity.isFinishing || activity.isDestroyed || isAdActivity(activity)) {
            onClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                frequencyController.onInterstitialShown()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                frequencyController.onFullScreenAdDismissed()
                onClosed()
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                frequencyController.onFullScreenAdDismissed()
                Log.w("AdManager", "Interstitial failed to show: ${adError.message}")
                onClosed()
                loadInterstitial()
            }
        }
        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e("AdManager", "Exception showing InterstitialAd", e)
            interstitialAd = null
            frequencyController.onFullScreenAdDismissed()
            onClosed()
            loadInterstitial()
        }
    }

    fun loadRewardedAd() {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdMobConfig.rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d("AdManager", "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w("AdManager", "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onClosed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null || frequencyController.isFullScreenAdShowing) {
            onClosed()
            loadRewardedAd()
            return
        }

        if (activity.isFinishing || activity.isDestroyed || isAdActivity(activity)) {
            onClosed()
            return
        }

        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                frequencyController.isFullScreenAdShowing = true
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                frequencyController.onFullScreenAdDismissed()
                if (rewardGranted) {
                    onRewarded()
                }
                onClosed()
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                frequencyController.onFullScreenAdDismissed()
                Log.w("AdManager", "Rewarded ad failed to show: ${adError.message}")
                onClosed()
                loadRewardedAd()
            }
        }

        try {
            ad.show(activity) { _ ->
                rewardGranted = true
            }
        } catch (e: Exception) {
            Log.e("AdManager", "Exception showing RewardedAd", e)
            rewardedAd = null
            frequencyController.onFullScreenAdDismissed()
            onClosed()
            loadRewardedAd()
        }
    }
}
