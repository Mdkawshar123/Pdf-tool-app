package com.example.ads

class AdFrequencyController(
    val interstitialCooldownMs: Long = 120_000L, // 2 minutes
    val maxInterstitialsPerSession: Int = 3,
    val appOpenCooldownMs: Long = 180_000L // 3 minutes
) {
    private var sessionInterstitialCount: Int = 0
    private var lastInterstitialShownTime: Long = 0L
    // Initialize to current time so app open ad never triggers on cold launch
    private var lastAppOpenShownTime: Long = System.currentTimeMillis()
    @Volatile
    var isFullScreenAdShowing: Boolean = false

    @Synchronized
    fun canShowInterstitial(): Boolean {
        if (isFullScreenAdShowing) return false
        if (sessionInterstitialCount >= maxInterstitialsPerSession) return false
        val now = System.currentTimeMillis()
        return (now - lastInterstitialShownTime) >= interstitialCooldownMs
    }

    @Synchronized
    fun onInterstitialShown() {
        isFullScreenAdShowing = true
        sessionInterstitialCount++
        lastInterstitialShownTime = System.currentTimeMillis()
    }

    @Synchronized
    fun onFullScreenAdDismissed() {
        isFullScreenAdShowing = false
        val now = System.currentTimeMillis()
        lastInterstitialShownTime = now
        lastAppOpenShownTime = now
    }

    @Synchronized
    fun canShowAppOpen(): Boolean {
        if (isFullScreenAdShowing) return false
        val now = System.currentTimeMillis()
        return (now - lastAppOpenShownTime) >= appOpenCooldownMs
    }

    @Synchronized
    fun onAppOpenShown() {
        isFullScreenAdShowing = true
        lastAppOpenShownTime = System.currentTimeMillis()
    }

    @Synchronized
    fun resetSession() {
        sessionInterstitialCount = 0
        lastInterstitialShownTime = 0L
        lastAppOpenShownTime = System.currentTimeMillis()
        isFullScreenAdShowing = false
    }
}
