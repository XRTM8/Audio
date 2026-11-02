package com.otium.audio.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.otium.audio.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicController(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressTrackingJob: Job? = null

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    private var currentPlaylistSongs: List<Song> = emptyList()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(PlayerListener())
                    syncState()
                }
                _playbackState.update { it.copy(isConnected = true) }
            } catch (e: Exception) {
                _playbackState.update { it.copy(isConnected = false) }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun syncState() {
        val controller = mediaController ?: return
        val currentMediaId = controller.currentMediaItem?.mediaId
        val currentSong = currentPlaylistSongs.find { it.id.toString() == currentMediaId }

        val duration = if (controller.duration > 0) controller.duration else (currentSong?.durationMs ?: 0L)
        val position = controller.currentPosition.coerceAtLeast(0L)

        _playbackState.update {
            it.copy(
                currentSong = currentSong ?: it.currentSong,
                isPlaying = controller.isPlaying,
                currentPositionMs = position,
                durationMs = duration,
                repeatMode = controller.repeatMode,
                isShuffleEnabled = controller.shuffleModeEnabled
            )
        }

        if (controller.isPlaying) {
            startProgressTracker()
        } else {
            stopProgressTracker()
        }
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val position = controller.currentPosition.coerceAtLeast(0L)
                    val duration = if (controller.duration > 0) controller.duration else _playbackState.value.durationMs
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = position,
                            durationMs = duration
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun playSong(song: Song, playlist: List<Song>) {
        val controller = mediaController ?: return
        currentPlaylistSongs = playlist

        val songIndex = playlist.indexOfFirst { it.id == song.id }
        if (songIndex == -1) return

        val mediaItems = playlist.map { it.toMediaItem() }

        controller.setMediaItems(mediaItems, songIndex, 0L)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs
            )
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val targetShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = targetShuffle
        _playbackState.update { it.copy(isShuffleEnabled = targetShuffle) }
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId
            val newSong = currentPlaylistSongs.find { it.id.toString() == mediaId }
            if (newSong != null) {
                _playbackState.update {
                    it.copy(
                        currentSong = newSong,
                        durationMs = newSong.durationMs,
                        currentPositionMs = 0L
                    )
                }
            }
            syncState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }
    }
}
