package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.PlaybackDevice
import com.example.data.remote.models.LyricsDto
import com.example.data.remote.models.TrackDto
import com.example.service.PlayerManager
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    init {
        PlayerManager.init(application)
    }

    val currentTrack: StateFlow<TrackDto?> = PlayerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = PlayerManager.isPlaying
    val isLoading: StateFlow<Boolean> = PlayerManager.isLoading
    val isShuffle: StateFlow<Boolean> = PlayerManager.isShuffle
    val repeatMode: StateFlow<Int> = PlayerManager.repeatMode
    val savedTracks: StateFlow<Set<String>> = PlayerManager.savedTracks
    val currentPositionSec: StateFlow<Int> = PlayerManager.currentPositionSec
    val currentPositionMs: StateFlow<Long> = PlayerManager.currentPositionMs
    val durationSec: StateFlow<Int> = PlayerManager.durationSec
    val volume: StateFlow<Float> = PlayerManager.volume
    val queue: StateFlow<List<TrackDto>> = PlayerManager.queue
    val currentQueueIndex: StateFlow<Int> = PlayerManager.currentQueueIndex
    val devices: StateFlow<List<PlaybackDevice>> = PlayerManager.devices
    val currentDevice: StateFlow<PlaybackDevice> = PlayerManager.currentDevice
    val sleepTimerMinutes: StateFlow<Int?> = PlayerManager.sleepTimerMinutes
    val playbackSpeed: StateFlow<Float> = PlayerManager.playbackSpeed
    val currentCanvasUrl: StateFlow<String?> = PlayerManager.currentCanvasUrl
    val currentLyrics: StateFlow<LyricsDto?> = PlayerManager.currentLyrics
    val recommendedTracks: StateFlow<List<TrackDto>> = PlayerManager.recommendedTracks

    fun playTrack(track: TrackDto, newQueue: List<TrackDto>? = null) {
        PlayerManager.playTrack(track, newQueue)
    }

    fun togglePlayPause() {
        PlayerManager.togglePlayPause()
    }

    fun pause() {
        PlayerManager.pause()
    }

    fun play() {
        PlayerManager.play()
    }

    fun seekToMs(ms: Long) {
        PlayerManager.seekToMs(ms)
    }

    fun seekTo(seconds: Int) {
        PlayerManager.seekTo(seconds)
    }

    fun setVolume(volumeLevel: Float) {
        PlayerManager.setVolume(volumeLevel)
    }

    fun toggleMute() {
        PlayerManager.toggleMute()
    }

    fun skipNext() {
        PlayerManager.skipNext()
    }

    fun skipPrevious() {
        PlayerManager.skipPrevious()
    }

    fun toggleShuffle() {
        PlayerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        PlayerManager.toggleRepeat()
    }

    fun toggleSave(track: TrackDto) {
        PlayerManager.toggleSave(track)
    }

    fun toggleSave(trackId: String) {
        PlayerManager.toggleSave(trackId)
    }

    fun setPlaybackSpeed(speed: Float) {
        PlayerManager.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        PlayerManager.setSleepTimer(minutes)
    }

    fun selectDevice(device: PlaybackDevice) {
        PlayerManager.selectDevice(device)
    }

    fun addToQueue(track: TrackDto) {
        PlayerManager.addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        PlayerManager.removeFromQueue(index)
    }

    fun playFromQueue(index: Int) {
        PlayerManager.playFromQueue(index)
    }

    fun clearQueue() {
        PlayerManager.clearQueue()
    }
}
