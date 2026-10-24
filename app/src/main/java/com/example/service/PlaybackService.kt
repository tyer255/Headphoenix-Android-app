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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.remote.models.TrackDto
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "spotify_playback_channel"
        const val CHANNEL_NAME = "Music Playback"
        const val NOTIFICATION_ID = 1001
        const val CUSTOM_CARD_NOTIFICATION_ID = 1002
        private const val TAG = "PlaybackService"

        const val ROOT_ID = "spotiz_root"
        const val SUGGESTED_ROOT_ID = "spotiz_suggested_root"
        const val RECENT_ROOT_ID = "spotiz_recent_root"

        const val ACTION_TOGGLE_LIKE = "com.example.action.TOGGLE_LIKE"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PLAY_REC_0 = "com.example.action.PLAY_REC_0"
        const val ACTION_PLAY_REC_1 = "com.example.action.PLAY_REC_1"
        const val ACTION_PLAY_REC_2 = "com.example.action.PLAY_REC_2"
        const val ACTION_PLAY_REC_3 = "com.example.action.PLAY_REC_3"
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
            .setCustomLayout(buildSpotifyCustomLayout())
            .build()

        val baseProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        // Custom MediaNotification.Provider that attaches our 4-thumbnail RemoteViews bigContentView
        // AND keeps standard MediaSession compatibility for Android OS Notification Shade
        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val baseNotif = baseProvider.createNotification(
                    mediaSession,
                    customLayout,
                    actionFactory,
                    onNotificationChangedCallback
                )
                try {
                    val remoteViews = buildSpotifyExpandedRemoteViews()
                    baseNotif.notification.bigContentView = remoteViews
                } catch (e: Exception) {
                    Log.w(TAG, "Error attaching custom bigContentView", e)
                }
                return baseNotif
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean {
                return baseProvider.handleCustomCommand(session, action, extras)
            }
        })

        PlayerManager.onLikeStateChangedListener = {
            try {
                mediaLibrarySession?.setCustomLayout(buildSpotifyCustomLayout())
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating custom layout", e)
            }
        }

        PlayerManager.onPlaybackStateUpdatedListener = {
            // MediaSession automatically updates the system notification
        }

        PlayerManager.onRecommendationsUpdatedListener = {
            try {
                mediaLibrarySession?.let { session ->
                    session.setCustomLayout(buildSpotifyCustomLayout())
                    val recs = PlayerManager.getRecommendedTracks()
                    val extras = Bundle().apply {
                        putBoolean("android.service.media.extra.SUGGESTED", true)
                        putBoolean("android.service.media.extra.RECENT", true)
                        putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                        putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2) // 2 = Grid item hint for 4 thumbnails!
                        putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
                        putBoolean("androidx.media.contentstyle.CONTENT_STYLE_SUPPORTED", true)
                        putInt("androidx.media.contentstyle.CONTENT_STYLE_PLAYABLE_HINT", 2)
                        putInt("androidx.media.contentstyle.CONTENT_STYLE_BROWSABLE_HINT", 2)
                    }
                    val params = LibraryParams.Builder()
                        .setExtras(extras)
                        .setSuggested(true)
                        .build()
                    val count = recs.size.coerceAtLeast(4)
                    session.notifyChildrenChanged(ROOT_ID, count, params)
                    session.notifyChildrenChanged(SUGGESTED_ROOT_ID, count, params)
                    session.notifyChildrenChanged(RECENT_ROOT_ID, 1, params)
                    session.notifyChildrenChanged("@media_suggested@", count, params)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify children changed", e)
            }
        }

        Log.d(TAG, "MediaLibrarySession created with Spotify 4-thumbnail & RemoteViews support")
    }

    private fun buildServiceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildSpotifyExpandedRemoteViews(): RemoteViews {
        val rv = RemoteViews(packageName, R.layout.notification_spotify_expanded)
        val currentTrack = PlayerManager.currentTrack.value
        val recs = PlayerManager.getRecommendedTracks()
        val isPlaying = PlayerManager.isPlaying.value
        val posSec = PlayerManager.currentPositionSec.value
        val durSec = PlayerManager.durationSec.value.coerceAtLeast(1)
        val isLiked = currentTrack != null && PlayerManager.savedTracks.value.contains(currentTrack.id)

        rv.setTextViewText(R.id.notif_title, currentTrack?.title ?: "DOPAMINE")
        rv.setTextViewText(R.id.notif_artist, currentTrack?.artist ?: "Guru Randhawa")
        rv.setTextViewText(R.id.notif_time_current, "%02d:%02d".format(posSec / 60, posSec % 60))
        rv.setTextViewText(R.id.notif_time_total, "%02d:%02d".format(durSec / 60, durSec % 60))
        val progressPct = ((posSec.toFloat() / durSec.toFloat()) * 100f).toInt().coerceIn(0, 100)
        rv.setProgressBar(R.id.notif_progress_bar, 100, progressPct, false)

        if (currentTrack != null) {
            val mainBmp = PlayerManager.getArtworkBitmapForTrack(currentTrack, 0)
            rv.setImageViewBitmap(R.id.notif_album_art, mainBmp)
        }

        rv.setImageViewResource(
            R.id.notif_btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        rv.setImageViewResource(
            R.id.notif_btn_like,
            if (isLiked) R.drawable.ic_notif_check_circle else R.drawable.ic_notif_add_circle
        )

        val openAppIntent = PendingIntent.getActivity(
            this,
            200,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        rv.setOnClickPendingIntent(R.id.notif_root_card, openAppIntent)
        rv.setOnClickPendingIntent(R.id.notif_btn_devices, openAppIntent)
        rv.setOnClickPendingIntent(R.id.notif_btn_prev, buildServiceActionPendingIntent(ACTION_PREV, 201))
        rv.setOnClickPendingIntent(R.id.notif_btn_play_pause, buildServiceActionPendingIntent(ACTION_PLAY_PAUSE, 202))
        rv.setOnClickPendingIntent(R.id.notif_btn_next, buildServiceActionPendingIntent(ACTION_NEXT, 203))
        rv.setOnClickPendingIntent(R.id.notif_btn_like, buildServiceActionPendingIntent(ACTION_TOGGLE_LIKE, 204))

        val imgIds = intArrayOf(
            R.id.notif_rec_img_0,
            R.id.notif_rec_img_1,
            R.id.notif_rec_img_2,
            R.id.notif_rec_img_3
        )
        val titleIds = intArrayOf(
            R.id.notif_rec_title_0,
            R.id.notif_rec_title_1,
            R.id.notif_rec_title_2,
            R.id.notif_rec_title_3
        )
        val boxIds = intArrayOf(
            R.id.notif_rec_box_0,
            R.id.notif_rec_box_1,
            R.id.notif_rec_box_2,
            R.id.notif_rec_box_3
        )
        val recActions = arrayOf(
            ACTION_PLAY_REC_0,
            ACTION_PLAY_REC_1,
            ACTION_PLAY_REC_2,
            ACTION_PLAY_REC_3
        )

        for (i in 0 until 4) {
            val recTrack = recs.getOrNull(i)
            if (recTrack != null) {
                val bmp = PlayerManager.getArtworkBitmapForTrack(recTrack, i + 1)
                rv.setImageViewBitmap(imgIds[i], bmp)
                rv.setTextViewText(titleIds[i], recTrack.title)
                rv.setOnClickPendingIntent(
                    boxIds[i],
                    buildServiceActionPendingIntent(recActions[i], 210 + i)
                )
            }
        }

        return rv
    }

    private fun postCompanion4ThumbnailNotification() {
        // Handled directly by MediaLibrarySession notification
    }

    private fun buildSpotifyCustomLayout(): ImmutableList<CommandButton> {
        val currentId = PlayerManager.currentTrack.value?.id
        val isLiked = currentId != null && PlayerManager.savedTracks.value.contains(currentId)
        val likeButton = CommandButton.Builder()
            .setDisplayName(if (isLiked) "Saved to Liked Songs" else "Add to Liked Songs")
            .setIconResId(if (isLiked) R.drawable.ic_notif_check_circle else R.drawable.ic_notif_add_circle)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .setEnabled(true)
            .build()
        return ImmutableList.of(likeButton)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                PlayerManager.togglePlayPause()
                postCompanion4ThumbnailNotification()
            }
            ACTION_PREV -> {
                PlayerManager.skipPrevious()
                postCompanion4ThumbnailNotification()
            }
            ACTION_NEXT -> {
                PlayerManager.skipNext()
                postCompanion4ThumbnailNotification()
            }
            ACTION_TOGGLE_LIKE -> {
                PlayerManager.currentTrack.value?.let {
                    PlayerManager.toggleSave(it)
                    postCompanion4ThumbnailNotification()
                }
            }
            ACTION_PLAY_REC_0 -> playRecommendationAtIndex(0)
            ACTION_PLAY_REC_1 -> playRecommendationAtIndex(1)
            ACTION_PLAY_REC_2 -> playRecommendationAtIndex(2)
            ACTION_PLAY_REC_3 -> playRecommendationAtIndex(3)
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun playRecommendationAtIndex(index: Int) {
        val recs = PlayerManager.getRecommendedTracks()
        val target = recs.getOrNull(index) ?: return
        PlayerManager.playTrack(target, recs)
        postCompanion4ThumbnailNotification()
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
        PlayerManager.onLikeStateChangedListener = null
        PlayerManager.onPlaybackStateUpdatedListener = null
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private inner class CustomMediaLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val defaultResult = super.onConnect(session, controller)
            val sessionCommands = defaultResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(defaultResult.availablePlayerCommands)
                .setCustomLayout(buildSpotifyCustomLayout())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_TOGGLE_LIKE) {
                PlayerManager.currentTrack.value?.let { track ->
                    PlayerManager.toggleSave(track)
                    mediaLibrarySession?.setCustomLayout(buildSpotifyCustomLayout())
                    postCompanion4ThumbnailNotification()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val isRecentRequest = params?.isRecent == true && params.isSuggested != true
            val isSuggestedRequest = params?.isSuggested == true ||
                params?.extras?.getBoolean("android.service.media.extra.SUGGESTED") == true

            // Hint 2 = CONTENT_STYLE_GRID_ITEM_HINT_VALUE (tells Android SystemUI / ColorOS to render 4 grid thumbnails!)
            val rootExtras = Bundle().apply {
                putBoolean("android.service.media.extra.SUGGESTED", true)
                putBoolean("android.service.media.extra.RECENT", isRecentRequest)
                putBoolean("android.service.media.extra.OFFLINE", false)
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
                putBoolean("androidx.media.contentstyle.CONTENT_STYLE_SUPPORTED", true)
                putInt("androidx.media.contentstyle.CONTENT_STYLE_PLAYABLE_HINT", 2)
                putInt("androidx.media.contentstyle.CONTENT_STYLE_BROWSABLE_HINT", 2)
            }

            val rootId = when {
                isRecentRequest -> RECENT_ROOT_ID
                isSuggestedRequest -> SUGGESTED_ROOT_ID
                else -> ROOT_ID
            }

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
                .setRecent(isRecentRequest)
                .setExtras(rootExtras)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            val callerPkg = browser.packageName

            serviceScope.launch {
                try {
                    val recs = PlayerManager.ensureRecommendationArtworksReady()
                    val isRecentOnly = parentId == RECENT_ROOT_ID && params?.isRecent == true && params.isSuggested != true
                    val itemsToReturn = if (isRecentOnly) {
                        val current = PlayerManager.currentTrack.value ?: recs.firstOrNull()
                        if (current != null) listOf(current) else emptyList()
                    } else {
                        recs.take(4)
                    }

                    val mediaItems = itemsToReturn.map { track ->
                        buildMediaItemFromTrack(track, callerPkg)
                    }

                    val extras = Bundle().apply {
                        putBoolean("android.service.media.extra.SUGGESTED", true)
                        putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                        putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2)
                        putBoolean("androidx.media.contentstyle.CONTENT_STYLE_SUPPORTED", true)
                        putInt("androidx.media.contentstyle.CONTENT_STYLE_PLAYABLE_HINT", 2)
                    }
                    val returnParams = LibraryParams.Builder()
                        .setExtras(extras)
                        .setSuggested(true)
                        .setRecent(isRecentOnly)
                        .build()

                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), returnParams))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onGetChildren", e)
                    future.set(LibraryResult.ofItemList(ImmutableList.of(), null))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val recs = PlayerManager.getRecommendedTracks()
            val mediaItems = recs.take(4).map { track ->
                buildMediaItemFromTrack(track, browser.packageName)
            }
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val track = PlayerManager.findTrackById(mediaId)
            return if (track != null) {
                Futures.immediateFuture(LibraryResult.ofItem(buildMediaItemFromTrack(track, browser.packageName), null))
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

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val requestedItem = mediaItems.getOrNull(startIndex) ?: mediaItems.firstOrNull()
            if (requestedItem != null) {
                val trackId = requestedItem.mediaId
                val track = PlayerManager.findTrackById(trackId)
                if (track != null) {
                    PlayerManager.playTrack(track, PlayerManager.getRecommendedTracks())
                }
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            )
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val current = PlayerManager.currentTrack.value
            if (current != null) {
                val item = buildMediaItemFromTrack(current, controller.packageName)
                val pos = PlayerManager.currentPositionMs.value
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(listOf(item), 0, pos)
                )
            }
            val rec = PlayerManager.getRecommendedTracks().firstOrNull()
            if (rec != null) {
                val item = buildMediaItemFromTrack(rec, controller.packageName)
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(listOf(item), 0, 0L)
                )
            }
            return super.onPlaybackResumption(mediaSession, controller)
        }
    }

    private fun buildMediaItemFromTrack(track: TrackDto, callerPackage: String? = null): MediaItem {
        val artworkBytes = PlayerManager.getArtworkDataForTrack(track.id)
        val contentUri = PlayerManager.getArtworkContentUriForTrack(track.id, callerPackage)

        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setSubtitle(track.artist)
            .setArtist(track.artist)
            .setAlbumArtist(track.artist)
            .setAlbumTitle(track.album ?: "")
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

        if (contentUri != null) {
            metaBuilder.setArtworkUri(contentUri)
        }
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        val itemExtras = Bundle().apply {
            putString("track_id", track.id)
            putString("title", track.title)
            putString("artist", track.artist)
            putBoolean("android.service.media.extra.SUGGESTED", true)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2)
            putInt("androidx.media.contentstyle.CONTENT_STYLE_PLAYABLE_HINT", 2)
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
