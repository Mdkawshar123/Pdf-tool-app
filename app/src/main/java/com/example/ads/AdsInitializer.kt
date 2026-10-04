package com.example.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean

object AdsInitializer {
    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                if (AdTestConfig.testDeviceIds.isNotEmpty()) {
                    val config = RequestConfiguration.Builder()
                        .setTestDeviceIds(AdTestConfig.testDeviceIds)
                        .build()
                    MobileAds.setRequestConfiguration(config)
                }

                MobileAds.initialize(context) { initializationStatus ->
                    Log.d("AdsInitializer", "Mobile Ads SDK initialized: $initializationStatus")
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e("AdsInitializer", "Failed to initialize MobileAds", e)
                onComplete?.invoke()
            }
        } else {
            onComplete?.invoke()
        }
    }
}
