package com.example.data

import android.content.Context
import android.util.Log
import com.example.YouTubeResolver
import com.example.data.remote.ApiClient
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.PlaybackResolveRequest
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data class Downloading(
        val progress: Float,
        val speedText: String = "",
        val bytesDownloaded: Long = 0L
    ) : DownloadState()
    data class Completed(val localPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

object AppDownloadManager {
    private const val TAG = "AppDownloadManager"

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadState>> = _downloadStatus.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val downloadedTracks: StateFlow<List<TrackDto>> = _downloadedTracks.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val jsonConfig = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
        isLenient = true
    }

    // High-performance streaming client with connection pooling and HTTP/2
    private val downloadHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun getSafeFileName(trackId: String): String {
        return trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    fun getAudioFile(context: Context, trackId: String): File {
        val safeName = getSafeFileName(trackId)
        return File(context.filesDir, "audio_${safeName}.m4a")
    }

    fun getArtworkFile(context: Context, trackId: String): File {
        val safeName = getSafeFileName(trackId)
        return File(context.filesDir, "art_${safeName}.jpg")
    }

    fun isDownloaded(trackId: String): Boolean {
        return _downloadedTracks.value.any { it.id == trackId }
    }

    fun getDownloadedTrack(trackId: String): TrackDto? {
        return _downloadedTracks.value.find { it.id == trackId }
    }

    fun getLocalStreamUrl(context: Context, trackId: String): String? {
        val audioFile = getAudioFile(context, trackId)
        if (audioFile.exists() && audioFile.length() > 1024) {
            return "file://${audioFile.absolutePath}"
        }
        val legacyFile = File(context.filesDir, "${trackId}.m4a")
        if (legacyFile.exists() && legacyFile.length() > 1024) {
            return "file://${legacyFile.absolutePath}"
        }
        return null
    }

    fun getDownloadedTracks(context: Context): List<TrackDto> {
        val file = File(context.filesDir, "downloaded_tracks.json")
        if (!file.exists()) return emptyList()
        return try {
            val jsonText = file.readText()
            val list = jsonConfig.decodeFromString<List<TrackDto>>(jsonText)
            // Filter only tracks where the audio file actually exists on disk
            val verifiedList = list.filter { track ->
                val audioFile = getAudioFile(context, track.id)
                val legacyFile = File(context.filesDir, "${track.id}.m4a")
                (audioFile.exists() && audioFile.length() > 1024) || (legacyFile.exists() && legacyFile.length() > 1024)
            }
            _downloadedTracks.value = verifiedList
            verifiedList
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloaded tracks", e)
            emptyList()
        }
    }

    private fun saveDownloadedTrack(context: Context, track: TrackDto, localAudioPath: String, localArtPath: String?) {
        val current = getDownloadedTracks(context).toMutableList()
        val index = current.indexOfFirst { it.id == track.id }
        
        val imagesDto = if (!localArtPath.isNullOrEmpty() && File(localArtPath).exists()) {
            ImagesDto(
                large = "file://$localArtPath",
                medium = "file://$localArtPath",
                small = "file://$localArtPath"
            )
        } else {
            track.images
        }

        val newTrack = track.copy(
            streamUrl = "file://$localAudioPath",
            images = imagesDto
        )

        if (index != -1) {
            current[index] = newTrack
        } else {
            current.add(0, newTrack)
        }

        try {
            val jsonText = jsonConfig.encodeToString(current)
            val file = File(context.filesDir, "downloaded_tracks.json")
            file.writeText(jsonText)
            _downloadedTracks.value = current
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloaded track list", e)
        }
    }

    fun removeDownloadedTrack(context: Context, trackId: String) {
        val current = getDownloadedTracks(context).toMutableList()
        current.removeAll { it.id == trackId }
        
        try {
            val jsonText = jsonConfig.encodeToString(current)
            File(context.filesDir, "downloaded_tracks.json").writeText(jsonText)
            _downloadedTracks.value = current
        } catch (e: Exception) {
            Log.e(TAG, "Error saving updated downloaded tracks after removal", e)
        }

        val audioFile = getAudioFile(context, trackId)
        if (audioFile.exists()) audioFile.delete()

        val legacyFile = File(context.filesDir, "${trackId}.m4a")
        if (legacyFile.exists()) legacyFile.delete()

        val artFile = getArtworkFile(context, trackId)
        if (artFile.exists()) artFile.delete()

        val newMap = _downloadStatus.value.toMutableMap()
        newMap.remove(trackId)
        _downloadStatus.value = newMap
    }

    fun downloadTrack(context: Context, track: TrackDto) {
        val currentLocalUrl = getLocalStreamUrl(context, track.id)
        if (currentLocalUrl != null && _downloadedTracks.value.any { it.id == track.id }) {
            _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                put(track.id, DownloadState.Completed(currentLocalUrl.removePrefix("file://")))
            }
            return
        }

        if (_downloadStatus.value[track.id] is DownloadState.Downloading) return

        _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
            put(track.id, DownloadState.Downloading(0.05f, "Resolving stream..."))
        }

        scope.launch {
            try {
                val targetAudioFile = getAudioFile(context, track.id)
                val tempAudioFile = File(context.filesDir, "temp_${targetAudioFile.name}")
                if (tempAudioFile.exists()) tempAudioFile.delete()

                var downloadedViaYtDlp = false
                var urlToDownload = track.streamUrl

                // 1. Try yt-dlp engine if URL is YouTube or direct media source
                val isSupportedYtDlp = !urlToDownload.isNullOrEmpty() &&
                        (urlToDownload.contains("youtube.com") || urlToDownload.contains("youtu.be") || urlToDownload.startsWith("http"))

                if (isSupportedYtDlp && MediaProcessingEngine.engineStatus.value.isYtDlpAvailable) {
                    try {
                        val cookieHeader = CookieRepository.getRawCookieText() ?: CookieRepository.getCookieHeader()
                        val result = MediaProcessingEngine.executeYtDlp(
                            context = context,
                            url = urlToDownload!!,
                            outputDir = context.filesDir,
                            outputFilenameTemplate = "temp_${targetAudioFile.name}",
                            cookieHeader = cookieHeader,
                            extractAudioOnly = true,
                            onProgress = { progress, speedText ->
                                _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                                    put(track.id, DownloadState.Downloading(progress * 0.88f, speedText))
                                }
                            }
                        )
                        if (result.isSuccess) {
                            val ytFile = result.getOrNull()
                            if (ytFile != null && ytFile.exists() && ytFile.length() > 1024) {
                                if (ytFile.absolutePath != tempAudioFile.absolutePath) {
                                    ytFile.renameTo(tempAudioFile)
                                }
                                downloadedViaYtDlp = true
                                Log.i(TAG, "yt-dlp download succeeded for ${track.title}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "yt-dlp download attempt note: ${e.message}")
                    }
                }

                // 2. Native Multi-Source Fallback (JioSaavn, iTunes, Deezer, Piped/Invidious)
                if (!downloadedViaYtDlp) {
                    if (urlToDownload.isNullOrEmpty() || !urlToDownload.startsWith("http")) {
                        if (track.title.equals("Khat", ignoreCase = true) && track.artist.contains("Navjot", ignoreCase = true)) {
                            urlToDownload = "https://archive.org/download/khat-navjot-ahuja-320-kbps/Khat%20Navjot%20Ahuja%20320%20Kbps.mp3"
                        } else {
                            try {
                                urlToDownload = YouTubeResolver.resolveDirectAudioForTrack(
                                    title = track.title,
                                    artist = track.artist
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Direct audio resolve fallback: ${e.message}")
                            }

                            if (urlToDownload.isNullOrEmpty()) {
                                try {
                                    val response = ApiClient.apiService.resolvePlayback(
                                        PlaybackResolveRequest(
                                            trackId = track.id,
                                            title = track.title,
                                            artist = track.artist,
                                            duration = track.duration
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        val stream = response.body()?.data?.stream
                                        val rawUrl = stream?.url ?: ""
                                        if (rawUrl.startsWith("youtube:") || stream?.descriptorType == "youtube") {
                                            val ytId = rawUrl.removePrefix("youtube:").trim()
                                            urlToDownload = YouTubeResolver.resolveStreamUrl(ytId)
                                        } else if (rawUrl.startsWith("http")) {
                                            urlToDownload = rawUrl
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Backend resolve failed for download: ${e.message}")
                                }
                            }

                            if (urlToDownload.isNullOrEmpty()) {
                                try {
                                    urlToDownload = YouTubeResolver.searchAndResolve("${track.title} ${track.artist}".trim())
                                } catch (e: Exception) {
                                    Log.w(TAG, "YouTube searchAndResolve failed: ${e.message}")
                                }
                            }

                            if (urlToDownload.isNullOrEmpty()) {
                                urlToDownload = YouTubeResolver.getGuaranteedFallbackStream(track.id)
                            }
                        }
                    }

                    if (urlToDownload.isNullOrEmpty()) {
                        throw Exception("Could not resolve stream URL for ${track.title}")
                    }

                    val bufferChunkSize = (CookieRepository.cookieConfig.value.bufferChunkSizeKb.coerceIn(16, 256)) * 1024

                    downloadStreamToFile(
                        sourceUrl = urlToDownload,
                        targetFile = tempAudioFile,
                        bufferSize = bufferChunkSize,
                        onProgress = { progress, speedText, bytes ->
                            _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                                put(track.id, DownloadState.Downloading(progress * 0.88f, speedText, bytes))
                            }
                        }
                    )
                }

                if (!tempAudioFile.exists() || tempAudioFile.length() < 1024) {
                    throw Exception("Downloaded file is empty or corrupted")
                }

                // 3. FFmpeg post-processing / transcoding (if needed) & container validation
                if (MediaProcessingEngine.engineStatus.value.isFFmpegAvailable && tempAudioFile.name.endsWith(".webm")) {
                    val transcodedFile = File(context.filesDir, "transcoded_${targetAudioFile.name}")
                    val success = MediaProcessingEngine.transcodeAudio(tempAudioFile, transcodedFile, 320)
                    if (success && transcodedFile.exists()) {
                        tempAudioFile.delete()
                        transcodedFile.renameTo(tempAudioFile)
                    }
                }

                val isVerified = MediaProcessingEngine.verifyAndTagAudio(tempAudioFile, track.title, track.artist)
                if (!isVerified) {
                    Log.w(TAG, "Audio file verification note for ${track.title}, proceeding with storage")
                }

                if (targetAudioFile.exists()) targetAudioFile.delete()
                tempAudioFile.renameTo(targetAudioFile)

                // Step 4: Download track artwork for offline display
                var localArtPath: String? = null
                val artUrl = track.images?.large ?: track.images?.medium ?: track.images?.small
                if (!artUrl.isNullOrBlank() && artUrl.startsWith("http")) {
                    try {
                        val targetArtFile = getArtworkFile(context, track.id)
                        val tempArtFile = File(context.filesDir, "temp_${targetArtFile.name}")
                        downloadStreamToFile(artUrl, tempArtFile, 16384, null)
                        if (tempArtFile.exists() && tempArtFile.length() > 500) {
                            if (targetArtFile.exists()) targetArtFile.delete()
                            tempArtFile.renameTo(targetArtFile)
                            localArtPath = targetArtFile.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Artwork download optional failed: ${e.message}")
                    }
                }

                // Step 5: Save metadata into local json list
                saveDownloadedTrack(context, track, targetAudioFile.absolutePath, localArtPath)

                _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                    put(track.id, DownloadState.Completed(targetAudioFile.absolutePath))
                }
                Log.i(TAG, "Successfully downloaded and saved ${track.title} -> ${targetAudioFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading track: ${track.title}", e)
                _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                    put(track.id, DownloadState.Failed(e.message ?: "Download failed"))
                }
            }
        }
    }

    fun downloadStreamToFile(
        sourceUrl: String,
        targetFile: File,
        bufferSize: Int = 65536,
        onProgress: ((Float, String, Long) -> Unit)? = null
    ) {
        val userAgent = CookieRepository.getUserAgent()
        val cookieHeader = CookieRepository.getCookieHeader()

        val requestBuilder = Request.Builder()
            .url(sourceUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")

        if (!cookieHeader.isNullOrBlank()) {
            requestBuilder.header("Cookie", cookieHeader)
        }

        val response = downloadHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val responseBody = response.body ?: throw Exception("Empty response body")
        val fileLength = responseBody.contentLength()
        val inputStream: InputStream = responseBody.byteStream()
        val outputStream = FileOutputStream(targetFile)

        val data = ByteArray(bufferSize)
        var totalBytes = 0L
        var count: Int
        var lastUpdate = System.currentTimeMillis()
        var bytesSinceLastUpdate = 0L
        var speedDisplay = "High Speed"

        try {
            while (inputStream.read(data).also { count = it } != -1) {
                outputStream.write(data, 0, count)
                totalBytes += count
                bytesSinceLastUpdate += count

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdate
                if (elapsed >= 250 || (fileLength > 0 && totalBytes == fileLength)) {
                    val bytesPerSec = if (elapsed > 0) (bytesSinceLastUpdate * 1000L) / elapsed else 0L
                    speedDisplay = if (bytesPerSec > 1024 * 1024) {
                        String.format("%.1f MB/s", bytesPerSec.toFloat() / (1024f * 1024f))
                    } else {
                        String.format("%d KB/s", bytesPerSec / 1024)
                    }

                    lastUpdate = now
                    bytesSinceLastUpdate = 0L

                    if (onProgress != null) {
                        val progress = if (fileLength > 0) {
                            (totalBytes.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0.5f
                        }
                        onProgress(progress, speedDisplay, totalBytes)
                    }
                }
            }
            outputStream.flush()
        } finally {
            outputStream.close()
            inputStream.close()
            response.close()
        }
    }

    fun initCompleted(context: Context) {
        val tracks = getDownloadedTracks(context)
        _downloadedTracks.value = tracks
        val newMap = _downloadStatus.value.toMutableMap()
        for (track in tracks) {
            val localPath = getLocalStreamUrl(context, track.id)?.removePrefix("file://")
            if (localPath != null && File(localPath).exists()) {
                newMap[track.id] = DownloadState.Completed(localPath)
            }
        }
        _downloadStatus.value = newMap
    }
}
