package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object PlaybackHistoryRepository {
    private const val TAG = "PlaybackHistory"
    private const val FILE_HISTORY = "playback_history.json"

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var appContext: Context? = null

    private val _recentlyPlayed = MutableStateFlow<List<TrackDto>>(emptyList())
    val recentlyPlayed: StateFlow<List<TrackDto>> = _recentlyPlayed.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadHistory()
    }

    private fun loadHistory() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, FILE_HISTORY)
            if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val list = json.decodeFromString<List<TrackDto>>(content)
                    _recentlyPlayed.value = list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playback history", e)
        }
    }

    private fun persistHistory() {
        val ctx = appContext ?: return
        val current = _recentlyPlayed.value
        scope.launch {
            try {
                val jsonStr = json.encodeToString(current)
                val file = File(ctx.filesDir, FILE_HISTORY)
                file.writeText(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save playback history", e)
            }
        }
    }

    fun addTrackToHistory(track: TrackDto) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)
        if (current.size > 25) {
            current.removeLast()
        }
        _recentlyPlayed.value = current
        persistHistory()
    }
}
