package com.otium.audio.playback

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.otium.audio.R
import com.otium.audio.ui.MainActivity

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var activeAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "otium_playback_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "PlaybackService"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
        setupNotificationProvider()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            .build()

        // 32-Bit Float PCM Audiophile Audio Sink
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true) // Studio-grade 32-bit floating point PCM output
                    .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain())
                    .build()
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                addListener(PlayerEventListener())
            }

        // Bind initial audio session immediately if already available
        val initialSessionId = player.audioSessionId
        if (initialSessionId > 0 && initialSessionId != C.AUDIO_SESSION_ID_UNSET) {
            openSystemAudioEffectSession(initialSessionId)
        }
    }

    private fun initializeMediaSession() {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            pendingIntentFlags
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun setupNotificationProvider() {
        createNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Otium Audio playback controls"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            com.otium.audio.ui.widget.OtiumAppWidgetProvider.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            com.otium.audio.ui.widget.OtiumAppWidgetProvider.ACTION_SKIP_NEXT -> {
                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            }
            com.otium.audio.ui.widget.OtiumAppWidgetProvider.ACTION_SKIP_PREVIOUS -> {
                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidgetState() {
        val currentMediaItem = player.currentMediaItem
        val song = currentMediaItem?.let { item ->
            val metadata = item.mediaMetadata
            com.otium.audio.data.model.Song(
                id = item.mediaId.toLongOrNull() ?: 0L,
                title = metadata.title?.toString() ?: "Unknown Track",
                artist = metadata.artist?.toString() ?: "Unknown Artist",
                album = metadata.albumTitle?.toString() ?: "Unknown Album",
                durationMs = player.duration.coerceAtLeast(0L),
                contentUri = item.requestMetadata.mediaUri ?: android.net.Uri.EMPTY,
                albumArtUri = metadata.artworkUri ?: android.net.Uri.EMPTY,
                size = 0L,
                relativePath = "",
                dateAdded = 0L
            )
        }
        com.otium.audio.ui.widget.OtiumAppWidgetProvider.updateWidget(this, song, player.isPlaying)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = mediaSession?.player
        if (activePlayer == null || !activePlayer.playWhenReady || activePlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        closeSystemAudioEffectSession(activeAudioSessionId)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun openSystemAudioEffectSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        activeAudioSessionId = audioSessionId

        Log.i(TAG, "Opening AudioEffect session: $audioSessionId")

        // 1. Bind internal Equalizer engine
        (application as? com.otium.audio.OtiumApplication)?.equalizerController?.bindAudioSession(audioSessionId)

        // 2. Broadcast to system DSP (Dolby Atmos, Dirac, Sony Sound, etc.)
        try {
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Error broadcasting ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION", e)
        }
    }

    private fun closeSystemAudioEffectSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        try {
            val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Error broadcasting ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION", e)
        }
        activeAudioSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId > 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                if (activeAudioSessionId != audioSessionId) {
                    closeSystemAudioEffectSession(activeAudioSessionId)
                }
                openSystemAudioEffectSession(audioSessionId)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            val currentId = player.audioSessionId
            if (currentId > 0 && currentId != C.AUDIO_SESSION_ID_UNSET && currentId != activeAudioSessionId) {
                openSystemAudioEffectSession(currentId)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updateWidgetState()
            val currentId = player.audioSessionId
            if (currentId > 0 && currentId != C.AUDIO_SESSION_ID_UNSET && currentId != activeAudioSessionId) {
                openSystemAudioEffectSession(currentId)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateWidgetState()
            val currentId = player.audioSessionId
            if (currentId > 0 && currentId != C.AUDIO_SESSION_ID_UNSET && currentId != activeAudioSessionId) {
                openSystemAudioEffectSession(currentId)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
            player.pause()
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map { item ->
                val resolvedUri = item.requestMetadata.mediaUri
                    ?: item.localConfiguration?.uri
                    ?: ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        item.mediaId.toLongOrNull() ?: 0L
                    )

                item.buildUpon()
                    .setUri(resolvedUri)
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(resolvedUri)
                            .build()
                    )
                    .build()
            }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
