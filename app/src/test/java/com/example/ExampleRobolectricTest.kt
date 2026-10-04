package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdFrequencyController
import com.example.core.pdf.PdfProcessingEngine
import com.example.core.storage.FileUtils
import com.example.domain.model.PdfToolsList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Master PDF Tool", appName)
    }

    @Test
    fun `verify page range parsing`() {
        val range = PdfProcessingEngine.parsePageRanges("1-3, 5, 7-8", maxPages = 10)
        // 0-based: 0, 1, 2, 4, 6, 7
        assertEquals(listOf(0, 1, 2, 4, 6, 7), range)

        val outOfBounds = PdfProcessingEngine.parsePageRanges("9-15", maxPages = 10)
        assertEquals(listOf(8, 9), outOfBounds)

        val invalid = PdfProcessingEngine.parsePageRanges("abc, - , ??", maxPages = 10)
        assertTrue(invalid.isEmpty())
    }

    @Test
    fun `verify ad frequency controller limits`() {
        val controller = AdFrequencyController(
            interstitialCooldownMs = 1000L,
            maxInterstitialsPerSession = 2
        )

        // Initial state allows first ad
        assertTrue(controller.canShowInterstitial())
        controller.onInterstitialShown()

        // Immediate subsequent request fails due to cooldown
        assertFalse(controller.canShowInterstitial())

        // Simulate reset session
        controller.resetSession()
        assertTrue(controller.canShowInterstitial())
    }

    @Test
    fun `verify file size formatting`() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("500 B", FileUtils.formatFileSize(500))
        assertEquals("1 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `verify tool catalog registered`() {
        assertTrue(PdfToolsList.allTools.size >= 15)
        val merge = PdfToolsList.getToolById("merge")
        assertTrue(merge != null)
        val split = PdfToolsList.getToolById("split")
        assertTrue(split != null)
    }
}
