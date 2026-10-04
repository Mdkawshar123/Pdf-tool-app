package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    fun gatherConsent(
        activity: Activity,
        onConsentComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED)
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w("ConsentManager", "Consent form error: ${formError.message}")
                    }
                    onConsentComplete(consentInformation.canRequestAds())
                }
            },
            { requestConsentError ->
                Log.w("ConsentManager", "Consent info update failed: ${requestConsentError.message}")
                onConsentComplete(consentInformation.canRequestAds())
            }
        )
    }
}
