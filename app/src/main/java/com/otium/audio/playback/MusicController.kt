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
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
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

    // Position updates four times a second. Keeping it out of MusicPlaybackState means
    // the library screen no longer recomposes on every tick just to move a 2 dp bar.
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private var currentPlaylistSongs: List<Song> = emptyList()

    /** Set by the application so the sleep timer can stop at the end of a track. */
    var onTrackEnded: (() -> Unit)? = null

    private val persistence = PlaybackPersistence(context)

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
        _position.value = controller.currentPosition.coerceAtLeast(0L)

        _playbackState.update {
            it.copy(
                currentSong = currentSong ?: it.currentSong,
                isPlaying = controller.isPlaying,
                durationMs = duration,
                repeatMode = controller.repeatMode,
                isShuffleEnabled = controller.shuffleModeEnabled,
                playbackSpeed = controller.playbackParameters.speed,
                queue = currentPlaylistSongs
            )
        }

        if (controller.isPlaying) {
            startProgressTracker()
        } else {
            stopProgressTracker()
            persistence.savePosition(_position.value)
        }
        persistQueue()
    }

    private fun persistQueue() {
        val controller = mediaController ?: return
        val ids = currentPlaylistSongs.map { it.id }
        if (ids.isEmpty()) return
        persistence.saveQueue(ids, controller.currentMediaItemIndex)
    }

    /** Rebuilds the last queue from the library, paused where the user left it. */
    fun restoreQueue(library: List<Song>): Boolean {
        val controller = mediaController ?: return false
        if (controller.mediaItemCount > 0) return false
        val session = persistence.load() ?: return false

        val byId = library.associateBy { it.id }
        val queue = session.songIds.mapNotNull { byId[it] }
        if (queue.isEmpty()) {
            persistence.clearQueue()
            return false
        }

        val index = session.index.coerceIn(0, queue.lastIndex)
        currentPlaylistSongs = queue
        controller.setMediaItems(queue.map { it.toMediaItem() }, index, session.positionMs)
        controller.prepare()
        _position.value = session.positionMs
        _playbackState.update {
            it.copy(currentSong = queue[index], durationMs = queue[index].durationMs, queue = queue)
        }
        return true
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = coroutineScope.launch(Dispatchers.Main) {
            var ticks = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _position.value = controller.currentPosition.coerceAtLeast(0L)
                    if (ticks % POSITION_SAVE_TICKS == 0) {
                        persistence.savePosition(_position.value)
                    }
                }
                ticks++
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
                applyOutputVolume()
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
        _position.value = positionMs
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

    private var headroomLinear = 1f
    private var fadeLinear = 1f

    /** Sleep timer and micro fades go through here so they never cancel the headroom. */
    fun setVolume(volume: Float) {
        fadeLinear = volume.coerceIn(0f, 1f)
        applyOutputVolume()
    }

    /**
     * The pre-amp, for real. Negative values buy headroom before an equalizer boost,
     * positive values push the signal up and rely on the device to stay clean.
     */
    fun setHeadroomDb(db: Float) {
        headroomLinear = 10f.pow(db.coerceIn(-20f, 6f) / 20f)
        applyOutputVolume()
    }

    private fun applyOutputVolume() {
        mediaController?.volume = (headroomLinear * fadeLinear).coerceIn(0f, 1f)
    }

    fun playNext(song: Song) {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val targetIndex = (currentIndex + 1).coerceAtMost(currentPlaylistSongs.size)

        val updated = currentPlaylistSongs.toMutableList()
        updated.add(targetIndex, song)
        currentPlaylistSongs = updated

        controller.addMediaItem(targetIndex, song.toMediaItem())
        _playbackState.update { it.copy(queue = updated) }
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        val updated = currentPlaylistSongs.toMutableList()
        updated.add(song)
        currentPlaylistSongs = updated

        controller.addMediaItem(song.toMediaItem())
        _playbackState.update { it.copy(queue = updated) }
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index !in currentPlaylistSongs.indices) return

        val updated = currentPlaylistSongs.toMutableList()
        updated.removeAt(index)
        currentPlaylistSongs = updated

        controller.removeMediaItem(index)
        _playbackState.update { it.copy(queue = updated) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex !in currentPlaylistSongs.indices || toIndex !in currentPlaylistSongs.indices) return

        val updated = currentPlaylistSongs.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        currentPlaylistSongs = updated

        controller.moveMediaItem(fromIndex, toIndex)
        _playbackState.update { it.copy(queue = updated) }
    }

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in currentPlaylistSongs.indices) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    private companion object {
        /** 250 ms per tick, so twenty ticks is roughly five seconds. */
        const val POSITION_SAVE_TICKS = 20
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
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) onTrackEnded?.invoke()
            val mediaId = mediaItem?.mediaId
            val newSong = currentPlaylistSongs.find { it.id.toString() == mediaId }
            if (newSong != null) {
                _playbackState.update {
                    it.copy(
                        currentSong = newSong,
                        durationMs = newSong.durationMs
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
