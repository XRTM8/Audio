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
                isShuffleEnabled = controller.shuffleModeEnabled,
                playbackSpeed = controller.playbackParameters.speed
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

        if (playlist.size <= 250) {
            val mediaItems = playlist.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, songIndex, 0L)
        } else {
            // Prevents TransactionTooLargeException across IPC Binder on giant 5,000+ track queues
            val chunkSize = 200
            val targetChunkStart = (songIndex - 50).coerceAtLeast(0)
            val targetChunkEnd = (targetChunkStart + chunkSize).coerceAtMost(playlist.size)
            val activeChunk = playlist.subList(targetChunkStart, targetChunkEnd).map { it.toMediaItem() }
            val chunkIndex = songIndex - targetChunkStart

            controller.setMediaItems(activeChunk, chunkIndex, 0L)

            // Stream remaining tracks smoothly into queue
            coroutineScope.launch(Dispatchers.Main) {
                if (targetChunkStart > 0) {
                    val preItems = playlist.subList(0, targetChunkStart).map { it.toMediaItem() }
                    controller.addMediaItems(0, preItems)
                }
                if (targetChunkEnd < playlist.size) {
                    var offset = targetChunkEnd
                    while (offset < playlist.size) {
                        val end = (offset + chunkSize).coerceAtMost(playlist.size)
                        val postItems = playlist.subList(offset, end).map { it.toMediaItem() }
                        controller.addMediaItems(postItems)
                        offset = end
                    }
                }
            }
        }

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
            // Micro-fade out (75ms) prevents digital audio pops / clicks on headphones
            coroutineScope.launch(Dispatchers.Main) {
                var vol = 1.0f
                while (vol > 0.15f) {
                    vol -= 0.35f
                    controller.volume = vol.coerceAtLeast(0f)
                    delay(25L)
                }
                controller.pause()
                controller.volume = 1.0f
            }
        } else {
            // Micro-fade in
            controller.volume = 0.25f
            controller.play()
            coroutineScope.launch(Dispatchers.Main) {
                var vol = 0.25f
                while (vol < 1.0f) {
                    vol += 0.35f
                    controller.volume = vol.coerceAtMost(1.0f)
                    delay(25L)
                }
            }
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

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.setPlaybackSpeed(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
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

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }
}
