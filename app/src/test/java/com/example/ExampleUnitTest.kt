package com.example

import com.example.data.MediaProcessingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun realFFmpegDownloadAndNativeExtraction_isVerified() {
        val tempDir = Files.createTempDirectory("headphonix_real_ffmpeg_test").toFile()
        try {
            val aarFile = File(tempDir, "ffmpeg-kit-https-8.1.7.aar")
            val jniDir = File(tempDir, "jni")
            var progressUpdates = 0
            var lastPercent = 0

            val (downloadedBytes, sha1Hex, ok) = MediaProcessingEngine.downloadRealBinaryWithProgress(
                url = MediaProcessingEngine.FFMPEG_MAVEN_AAR_URL,
                destFile = aarFile
            ) { bytesRead, totalBytes, _ ->
                progressUpdates++
                if (totalBytes > 0) {
                    lastPercent = ((bytesRead * 100L) / totalBytes).toInt()
                }
            }

            println("REAL FFMPEG DOWNLOAD BYTES: $downloadedBytes")
            println("REAL FFMPEG SHA-1: $sha1Hex")
            println("REAL PROGRESS UPDATES COUNT: $progressUpdates, FINAL PERCENT: $lastPercent%")

            assertTrue(ok)
            assertEquals(20_892_752L, downloadedBytes)
            assertEquals(MediaProcessingEngine.FFMPEG_EXPECTED_SHA1, sha1Hex)
            assertEquals(100, lastPercent)

            val (extractedLibs, _) = MediaProcessingEngine.extractNativeLibrariesFromArchive(aarFile, jniDir)
            println("EXTRACTED NATIVE .SO FILES (${extractedLibs.size}): $extractedLibs")

            assertTrue("Expected native .so libraries to be extracted", extractedLibs.isNotEmpty())
            assertTrue(extractedLibs.any { it.startsWith("libavcodec.so") })
            assertTrue(extractedLibs.any { it.startsWith("libavformat.so") })
            assertTrue(extractedLibs.any { it.startsWith("libffmpegkit.so") })
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
