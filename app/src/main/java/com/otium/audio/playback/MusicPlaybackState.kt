package com.otium.audio.playback

import androidx.media3.common.Player
import com.otium.audio.data.model.Song

data class MusicPlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffleEnabled: Boolean = false,
    val isConnected: Boolean = false
)
