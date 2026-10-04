package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.presentation.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PdfMasterApplication
        // Gather consent for European and personalized advertising regulations
        app.consentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                app.adManager.loadInterstitial()
            }
        }

        setContent {
            var themeMode by remember { mutableStateOf("system") }
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                AppNavigation(
                    currentThemeMode = themeMode,
                    onThemeModeChange = { themeMode = it }
                )
            }
        }
    }
}
