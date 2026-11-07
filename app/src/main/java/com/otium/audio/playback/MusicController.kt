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

    fun hasSavedSession(): Boolean = persistence.hasSession()

    /** Rebuilds the last queue from the library, paused where the user left it. */
    fun restoreQueue(library: List<Song>): Boolean {
        val controller = mediaController ?: return false
        val byId = library.associateBy { it.id }

        if (controller.mediaItemCount > 0) {
            // Player is already active from background service!
            // Reconstruct in-memory queue so it is never empty on screen
            val session = persistence.load()
            val queue = if (session != null && session.songIds.isNotEmpty()) {
                session.songIds.mapNotNull { byId[it] }
            } else {
                (0 until controller.mediaItemCount).mapNotNull { idx ->
                    val mediaId = controller.getMediaItemAt(idx).mediaId.toLongOrNull()
                    byId[mediaId]
                }
            }
            if (queue.isNotEmpty()) {
                currentPlaylistSongs = queue
                val currentMediaId = controller.currentMediaItem?.mediaId
                val curSong = queue.find { it.id.toString() == currentMediaId }
                _playbackState.update {
                    it.copy(
                        currentSong = curSong ?: it.currentSong,
                        queue = queue
                    )
                }
            }
            return true
        }

        val session = persistence.load() ?: return false
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

        val songIndex = playlist.indexOfFirst { it.id == song.id }
        if (songIndex == -1) return

        // Fast path: if the requested song is in the already-loaded queue, seek directly without rebuilding the queue
        val isSameQueue = controller.mediaItemCount == playlist.size &&
            (currentPlaylistSongs === playlist || (currentPlaylistSongs.isNotEmpty() && currentPlaylistSongs.size == playlist.size && currentPlaylistSongs[0].id == playlist[0].id && currentPlaylistSongs.last().id == playlist.last().id))

        if (isSameQueue) {
            currentPlaylistSongs = playlist
            controller.seekTo(songIndex, 0L)
            controller.play()
            _playbackState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    durationMs = song.durationMs,
                    queue = playlist
                )
            }
            return
        }

        currentPlaylistSongs = playlist
        val mediaItems = playlist.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, songIndex, 0L)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                durationMs = song.durationMs,
                queue = playlist
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
        val duration = _playbackState.value.durationMs.coerceAtLeast(0L)
        val clamped = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _position.value = clamped
    }

    fun seekBy(deltaMs: Long) {
        val current = _position.value
        seekTo(current + deltaMs)
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekToDefaultPosition(0)
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

    /** Sleep timer and micro fades only. Gain belongs to the DSP, not to the player. */
    fun setVolume(volume: Float) {
        mediaController?.volume = volume.coerceIn(0f, 1f)
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

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val controller = mediaController ?: return
            _position.value = controller.currentPosition.coerceAtLeast(0L)
            syncState()
        }
    }
}
