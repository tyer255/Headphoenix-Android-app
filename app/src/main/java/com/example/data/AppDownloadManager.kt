package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.example.data.remote.ApiClient
import com.example.data.remote.models.PlaybackResolveRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val localPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

object AppDownloadManager {
    private val _downloadStatus = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadState>> = _downloadStatus.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val downloadedTracks: StateFlow<List<TrackDto>> = _downloadedTracks.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    fun getDownloadedTracks(context: Context): List<TrackDto> {
        val file = File(context.filesDir, "downloaded_tracks.json")
        if (!file.exists()) return emptyList()
        return try {
            val jsonText = file.readText()
            val list = jsonConfig.decodeFromString<List<TrackDto>>(jsonText)
            _downloadedTracks.value = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDownloadedTrack(context: Context, track: TrackDto, localPath: String) {
        val current = getDownloadedTracks(context).toMutableList()
        val index = current.indexOfFirst { it.id == track.id }
        val newTrack = track.copy(streamUrl = "file://$localPath")
        if (index != -1) {
            current[index] = newTrack
        } else {
            current.add(0, newTrack)
        }
        val jsonText = jsonConfig.encodeToString(current)
        File(context.filesDir, "downloaded_tracks.json").writeText(jsonText)
        _downloadedTracks.value = current
    }
    
    fun removeDownloadedTrack(context: Context, trackId: String) {
        val current = getDownloadedTracks(context).toMutableList()
        current.removeAll { it.id == trackId }
        val jsonText = jsonConfig.encodeToString(current)
        File(context.filesDir, "downloaded_tracks.json").writeText(jsonText)
        _downloadedTracks.value = current
        
        val file = File(context.filesDir, "$trackId.m4a")
        if (file.exists()) file.delete()
        
        val newMap = _downloadStatus.value.toMutableMap()
        newMap.remove(trackId)
        _downloadStatus.value = newMap
    }

    fun downloadTrack(context: Context, track: TrackDto) {
        if (_downloadedTracks.value.any { it.id == track.id }) return
        if (_downloadStatus.value[track.id] is DownloadState.Completed) return
        if (_downloadStatus.value[track.id] is DownloadState.Downloading) return

        _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
            put(track.id, DownloadState.Downloading(0.1f))
        }

        scope.launch {
            try {
                // Resolve url
                var urlToDownload = track.streamUrl
                if (urlToDownload.isNullOrEmpty() || !urlToDownload.startsWith("http")) {
                    if (track.title.equals("Khat", ignoreCase = true) && track.artist.contains("Navjot", ignoreCase = true)) {
                        urlToDownload = "https://archive.org/download/khat-navjot-ahuja-320-kbps/Khat%20Navjot%20Ahuja%20320%20Kbps.mp3"
                    } else {
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
                            if (rawUrl.startsWith("youtube:")) {
                                val ytId = rawUrl.removePrefix("youtube:").trim()
                                urlToDownload = com.example.YouTubeResolver.resolveStreamUrl(ytId)
                            } else if (rawUrl.startsWith("http")) {
                                urlToDownload = rawUrl
                            }
                        }
                    }
                }
                
                if (urlToDownload.isNullOrEmpty()) {
                    throw Exception("No stream URL available")
                }

                val outputFile = File(context.filesDir, "${track.id}.m4a")
                val url = URL(urlToDownload)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()
                
                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(outputFile)
                
                val data = ByteArray(8192)
                var total = 0L
                var count: Int
                var lastUpdate = System.currentTimeMillis()
                
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100 || total == fileLength.toLong()) {
                        lastUpdate = now
                        val progress = if (fileLength > 0) (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f) else 0.5f
                        _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                            put(track.id, DownloadState.Downloading(progress))
                        }
                    }
                }
                
                output.flush()
                output.close()
                input.close()

                saveDownloadedTrack(context, track, outputFile.absolutePath)

                _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                    put(track.id, DownloadState.Completed(outputFile.absolutePath))
                }
                
            } catch (e: Exception) {
                Log.e("AppDownloadManager", "Error downloading ${track.title}", e)
                _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                    put(track.id, DownloadState.Failed(e.message ?: "Unknown error"))
                }
            }
        }
    }
    
    fun initCompleted(context: Context) {
        val tracks = getDownloadedTracks(context)
        _downloadedTracks.value = tracks
        val newMap = _downloadStatus.value.toMutableMap()
        for (track in tracks) {
            val localPath = track.streamUrl?.replace("file://", "") ?: ""
            val file = File(localPath)
            if (file.exists()) {
                newMap[track.id] = DownloadState.Completed(file.absolutePath)
            }
        }
        _downloadStatus.value = newMap
    }
}
