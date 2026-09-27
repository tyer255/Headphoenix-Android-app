package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.remote.models.TrackDto
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        const val CHANNEL_ID = "spotify_playback_channel"
        const val CHANNEL_NAME = "Music Playback"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "PlaybackService"

        const val ROOT_ID = "spotiz_root"
        const val SUGGESTED_ROOT_ID = "spotiz_suggested_root"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = PlayerManager.getPlayerForSession(applicationContext)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = CustomMediaLibrarySessionCallback()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(sessionActivityPendingIntent)
            .setId("HeadphonixMediaLibrarySession")
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        PlayerManager.onRecommendationsUpdatedListener = {
            try {
                mediaLibrarySession?.let { session ->
                    val recs = PlayerManager.getRecommendedTracks()
                    val extras = Bundle().apply {
                        putBoolean("android.service.media.extra.SUGGESTED", true)
                        putBoolean("android.service.media.extra.RECENT", true)
                    }
                    val params = LibraryParams.Builder()
                        .setExtras(extras)
                        .setSuggested(true)
                        .setRecent(true)
                        .build()
                    session.notifyChildrenChanged(ROOT_ID, recs.size, params)
                    session.notifyChildrenChanged(SUGGESTED_ROOT_ID, recs.size, params)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify children changed", e)
            }
        }

        Log.d(TAG, "MediaLibrarySession created and attached with recommendation support")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Active music playback and lock screen controls"
                    setShowBadge(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        PlayerManager.onRecommendationsUpdatedListener = null
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private inner class CustomMediaLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean("android.service.media.extra.SUGGESTED", true)
                putBoolean("android.service.media.extra.RECENT", true)
                putBoolean("android.service.media.extra.OFFLINE", false)
            }

            val isSuggested = params?.isSuggested == true ||
                params?.extras?.getBoolean("android.service.media.extra.SUGGESTED") == true

            val rootId = if (isSuggested) SUGGESTED_ROOT_ID else ROOT_ID

            val rootItem = MediaItem.Builder()
                .setMediaId(rootId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Recommended Songs")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                        .setExtras(rootExtras)
                        .build()
                )
                .build()

            val libraryParams = LibraryParams.Builder()
                .setSuggested(true)
                .setRecent(true)
                .setExtras(rootExtras)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val recs = PlayerManager.getRecommendedTracks()
            val mediaItems = recs.take(6).map { track ->
                buildMediaItemFromTrack(track)
            }

            val extras = Bundle().apply {
                putBoolean("android.service.media.extra.SUGGESTED", true)
                putBoolean("android.service.media.extra.RECENT", true)
            }
            val returnParams = LibraryParams.Builder()
                .setExtras(extras)
                .setSuggested(true)
                .setRecent(true)
                .build()

            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), returnParams)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val track = PlayerManager.findTrackById(mediaId)
            return if (track != null) {
                Futures.immediateFuture(LibraryResult.ofItem(buildMediaItemFromTrack(track), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(SessionResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val requestedItem = mediaItems.firstOrNull()
            if (requestedItem != null) {
                val trackId = requestedItem.mediaId
                val track = PlayerManager.findTrackById(trackId)
                if (track != null) {
                    PlayerManager.playTrack(track, PlayerManager.getRecommendedTracks())
                }
            }
            return Futures.immediateFuture(mediaItems)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val current = PlayerManager.currentTrack.value
            if (current != null) {
                val item = buildMediaItemFromTrack(current)
                val pos = PlayerManager.currentPositionMs.value
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(listOf(item), 0, pos)
                )
            }
            val rec = PlayerManager.getRecommendedTracks().firstOrNull()
            if (rec != null) {
                val item = buildMediaItemFromTrack(rec)
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(listOf(item), 0, 0L)
                )
            }
            return super.onPlaybackResumption(mediaSession, controller)
        }
    }

    private fun buildMediaItemFromTrack(track: TrackDto): MediaItem {
        val artworkUrl = track.images?.large
            ?: track.images?.medium
            ?: track.images?.small
            ?: ""

        val artworkBytes = PlayerManager.getArtworkDataForTrack(track.id)

        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setAlbumArtist(track.artist)
            .setAlbumTitle(track.album ?: "")
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

        if (artworkUrl.isNotBlank()) {
            metaBuilder.setArtworkUri(Uri.parse(artworkUrl))
        }
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        val itemExtras = Bundle().apply {
            putString("track_id", track.id)
            putString("title", track.title)
            putString("artist", track.artist)
        }
        metaBuilder.setExtras(itemExtras)

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl ?: "spotiz://track/${track.id}")
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(track.streamUrl ?: "spotiz://track/${track.id}"))
                    .build()
            )
            .setMediaMetadata(metaBuilder.build())
            .build()
    }
}
