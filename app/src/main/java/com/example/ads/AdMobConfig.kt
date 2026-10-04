package com.example.ads

import com.example.BuildConfig

object AdMobConfig {
    // Official Google AdMob Test Ad Unit IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"
    const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    // Resolves ad unit id from BuildConfig or fallback to official test ID
    val bannerAdUnitId: String
        get() = BuildConfig.BANNER_AD_UNIT_ID.ifBlank { TEST_BANNER_ID }

    val interstitialAdUnitId: String
        get() = BuildConfig.INTERSTITIAL_AD_UNIT_ID.ifBlank { TEST_INTERSTITIAL_ID }

    val rewardedAdUnitId: String
        get() = BuildConfig.REWARDED_AD_UNIT_ID.ifBlank { TEST_REWARDED_ID }

    val appOpenAdUnitId: String
        get() = BuildConfig.APP_OPEN_AD_UNIT_ID.ifBlank { TEST_APP_OPEN_ID }

    val nativeAdUnitId: String
        get() = BuildConfig.NATIVE_AD_UNIT_ID.ifBlank { TEST_NATIVE_ID }
}

object AdTestConfig {
    // Developers can add their hashed test device IDs here (obtained from Logcat)
    val testDeviceIds: List<String> = listOf(
        // e.g. "33BE2250B43518CCDA7DE426D04EE231"
    )
}
