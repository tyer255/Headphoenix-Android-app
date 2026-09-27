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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val localPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

object AppDownloadManager {
    private const val TAG = "AppDownloadManager"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

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
            put(track.id, DownloadState.Downloading(0.05f))
        }

        scope.launch {
            try {
                // Step 1: Resolve valid stream URL
                var urlToDownload = track.streamUrl
                if (urlToDownload.isNullOrEmpty() || !urlToDownload.startsWith("http")) {
                    if (track.title.equals("Khat", ignoreCase = true) && track.artist.contains("Navjot", ignoreCase = true)) {
                        urlToDownload = "https://archive.org/download/khat-navjot-ahuja-320-kbps/Khat%20Navjot%20Ahuja%20320%20Kbps.mp3"
                    } else {
                        // Try 1: Remote resolve endpoint
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
                            Log.w(TAG, "Failed resolving via backend for download: ${e.message}")
                        }

                        // Try 2: Direct query YouTube / Piped resolver
                        if (urlToDownload.isNullOrEmpty()) {
                            try {
                                urlToDownload = YouTubeResolver.searchAndResolve("${track.title} ${track.artist}".trim())
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed YouTube searchAndResolve: ${e.message}")
                            }
                        }
                    }
                }

                if (urlToDownload.isNullOrEmpty()) {
                    throw Exception("Could not resolve stream URL for ${track.title}")
                }

                // Step 2: Download audio file with proper headers and redirect handling
                val targetAudioFile = getAudioFile(context, track.id)
                val tempAudioFile = File(context.filesDir, "temp_${targetAudioFile.name}")
                if (tempAudioFile.exists()) tempAudioFile.delete()

                downloadHttpUrlToFile(urlToDownload, tempAudioFile) { progress ->
                    _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                        put(track.id, DownloadState.Downloading(progress * 0.85f))
                    }
                }

                if (!tempAudioFile.exists() || tempAudioFile.length() < 1024) {
                    throw Exception("Downloaded file is empty or too small")
                }

                if (targetAudioFile.exists()) targetAudioFile.delete()
                tempAudioFile.renameTo(targetAudioFile)

                // Step 3: Download track artwork for offline display
                var localArtPath: String? = null
                val artUrl = track.images?.large ?: track.images?.medium ?: track.images?.small
                if (!artUrl.isNullOrBlank() && artUrl.startsWith("http")) {
                    try {
                        val targetArtFile = getArtworkFile(context, track.id)
                        val tempArtFile = File(context.filesDir, "temp_${targetArtFile.name}")
                        downloadHttpUrlToFile(artUrl, tempArtFile, null)
                        if (tempArtFile.exists() && tempArtFile.length() > 500) {
                            if (targetArtFile.exists()) targetArtFile.delete()
                            tempArtFile.renameTo(targetArtFile)
                            localArtPath = targetArtFile.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Artwork download optional failed: ${e.message}")
                    }
                }

                // Step 4: Save metadata into local json list
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

    private fun downloadHttpUrlToFile(
        sourceUrl: String,
        targetFile: File,
        onProgress: ((Float) -> Unit)?
    ) {
        var currentUrl = sourceUrl
        var redirects = 0
        var conn: HttpURLConnection? = null

        while (redirects < 6) {
            val url = URL(currentUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "*/*")
            conn.connectTimeout = 20000
            conn.readTimeout = 30000
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
                val newUrl = conn.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    currentUrl = newUrl
                    conn.disconnect()
                    redirects++
                    continue
                }
            }

            if (responseCode !in 200..299) {
                throw Exception("HTTP Error: $responseCode from $currentUrl")
            }
            break
        }

        val connection = conn ?: throw Exception("Failed to open connection")
        val fileLength = connection.contentLength
        val input = connection.inputStream
        val output = FileOutputStream(targetFile)

        val data = ByteArray(16384)
        var total = 0L
        var count: Int
        var lastUpdate = System.currentTimeMillis()

        while (input.read(data).also { count = it } != -1) {
            total += count
            output.write(data, 0, count)

            val now = System.currentTimeMillis()
            if (now - lastUpdate > 150 || (fileLength > 0 && total == fileLength.toLong())) {
                lastUpdate = now
                if (fileLength > 0 && onProgress != null) {
                    val progress = (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                }
            }
        }

        output.flush()
        output.close()
        input.close()
        connection.disconnect()
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
