package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Headphonix", appName)
  }

  @Test
  fun `cookie repository saves and retrieves cookies`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.CookieRepository.initialize(context)
    com.example.data.CookieRepository.saveCookies(
      rawCookie = "SID=test_cookie_123; HSID=test_hsid;",
      customUserAgent = "TestUserAgent/1.0",
      isEnabled = true
    )
    val header = com.example.data.CookieRepository.getCookieHeader()
    assertEquals("SID=test_cookie_123; HSID=test_hsid;", header)
    assertEquals("TestUserAgent/1.0", com.example.data.CookieRepository.getUserAgent())
  }

  @Test
  fun `media processing engine supported codecs discovery`() {
    val codecs = com.example.data.MediaProcessingEngine.getSupportedAudioCodecs()
    org.junit.Assert.assertTrue(codecs.isNotEmpty())
    org.junit.Assert.assertTrue(codecs.any { it.contains("AAC") || it.contains("MP3") || it.contains("MPEG") })
  }

  @Test
  fun `netscape cookie file generation and secure cleanup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rawCookie = "PREF=f1=50000000; VISITOR_INFO1_LIVE=test_token_xyz; SID=test_session_id"
    val cookieFile = com.example.data.MediaProcessingEngine.createNetscapeCookieFile(context, rawCookie)

    org.junit.Assert.assertNotNull("Cookie file should be generated", cookieFile)
    org.junit.Assert.assertTrue("Cookie file should exist in cacheDir", cookieFile!!.exists())
    org.junit.Assert.assertTrue("Cookie file must be in app-private cacheDir", cookieFile.parentFile == context.cacheDir)

    val content = cookieFile.readText()
    org.junit.Assert.assertTrue("Must contain Netscape header", content.contains("# Netscape HTTP Cookie File"))
    org.junit.Assert.assertTrue("Must contain PREF cookie entry", content.contains("PREF\tf1=50000000"))
    org.junit.Assert.assertTrue("Must contain SID cookie entry", content.contains("SID\ttest_session_id"))
    org.junit.Assert.assertTrue("Must contain tab separated format", content.lines().any { it.startsWith(".youtube.com\tTRUE\t/\tTRUE") })

    // Verify cleanup
    val deleted = cookieFile.delete()
    org.junit.Assert.assertTrue("Cookie file must be cleanly deletable", deleted)
    org.junit.Assert.assertFalse("Cookie file must no longer exist after cleanup", cookieFile.exists())
  }

  @Test
  fun `engine status initial state and flow validation`() {
    val status = com.example.data.MediaProcessingEngine.engineStatus.value
    org.junit.Assert.assertNotNull(status)
    org.junit.Assert.assertNotNull(status.statusMessage)
    org.junit.Assert.assertFalse("Should not be updating by default", status.isUpdating)
  }

  @Test
  fun `test legally accessible sample download and verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.CookieRepository.initialize(context)
    val targetFile = java.io.File(context.filesDir, "test_sample_audio.mp3")
    if (targetFile.exists()) targetFile.delete()

    val sampleUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    com.example.data.AppDownloadManager.downloadStreamToFile(
      sourceUrl = sampleUrl,
      targetFile = targetFile,
      bufferSize = 32768,
      onProgress = null
    )

    org.junit.Assert.assertTrue("Target file must exist after download", targetFile.exists())
    org.junit.Assert.assertTrue("File size must be greater than 10KB, was ${targetFile.length()} bytes", targetFile.length() > 10000L)
    val isValidAudio = com.example.data.MediaProcessingEngine.validateAudioFile(targetFile)
    org.junit.Assert.assertTrue("MediaProcessingEngine must validate audio container format", isValidAudio)
    targetFile.delete()
  }
}

