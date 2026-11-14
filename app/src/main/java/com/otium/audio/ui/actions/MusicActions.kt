package com.otium.audio.ui.actions

import androidx.compose.runtime.Immutable
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioAlbum
import com.otium.audio.data.model.AudioArtist
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.model.SortOrder
import com.otium.audio.ui.viewmodel.LibraryTab

/**
 * Domain-scoped action contracts for playback, equalizer, queue, sleep timer, and library navigation.
 * Eliminates callback drilling and groups UI events into clean, cohesive, immutable bundles.
 */

@Immutable
data class PlaybackActions(
    val onPlayPause: () -> Unit = {},
    val onSeekTo: (Long) -> Unit = {},
    val onSeekBy: (Long) -> Unit = {},
    val onSkipNext: () -> Unit = {},
    val onSkipPrevious: () -> Unit = {},
    val onToggleRepeat: () -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onToggleFavorite: (Song) -> Unit = {},
    val onSetPlaybackSpeed: (Float) -> Unit = {},
    val onToggleSkipSilence: (Boolean) -> Unit = {}
)

@Immutable
data class EqualizerActions(
    val onToggle: (Boolean) -> Unit = {},
    val onToggleAutoPreAmp: (Boolean) -> Unit = {},
    val onBandLevelChange: (Short, Float) -> Unit = { _, _ -> },
    val onBassBoostChange: (Int) -> Unit = {},
    val onVirtualizerChange: (Int) -> Unit = {},
    val onPresetSelected: (String) -> Unit = {},
    val onPreAmpChange: (Float) -> Unit = {},
    val onReverbChange: (Int) -> Unit = {},
    val onReverbPresetChange: (String) -> Unit = {},
    val onChorusChange: (Int) -> Unit = {},
    val onClarityChange: (Int) -> Unit = {}
)

@Immutable
data class QueueActions(
    val onPlayQueueIndex: (Int) -> Unit = {},
    val onRemoveQueueItem: (Int) -> Unit = {},
    val onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> }
)

@Immutable
data class SleepTimerActions(
    val onStartTimer: (Int) -> Unit = {},
    val onStartEndOfTrack: () -> Unit = {},
    val onCancelTimer: () -> Unit = {}
)

@Immutable
data class LibraryActions(
    val onTabSelected: (LibraryTab) -> Unit = {},
    val onSearchQueryChanged: (String) -> Unit = {},
    val onSongSelected: (Song, List<Song>) -> Unit = { _, _ -> },
    val onFavoriteToggle: (Song) -> Unit = {},
    val onPlaylistSelected: (PlaylistEntity) -> Unit = {},
    val onClearSelectedPlaylist: () -> Unit = {},
    val onFolderSelected: (AudioFolder) -> Unit = {},
    val onClearSelectedFolder: () -> Unit = {},
    val onArtistSelected: (AudioArtist) -> Unit = {},
    val onClearSelectedArtist: () -> Unit = {},
    val onAlbumSelected: (AudioAlbum) -> Unit = {},
    val onClearSelectedAlbum: () -> Unit = {},
    val onAddToPlaylist: (Long, Long) -> Unit = { _, _ -> },
    val onPlayNext: (Song) -> Unit = {},
    val onAddToQueue: (Song) -> Unit = {},
    val onCreatePlaylist: (String) -> Unit = {},
    val onDeletePlaylist: (Long) -> Unit = {},
    val onEditTagClick: (Song) -> Unit = {},
    val onSaveMetadata: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    val onSongLongPress: (Song) -> Unit = {},
    val onDismissTagEditor: () -> Unit = {},
    val onDismissUpdate: () -> Unit = {},
    val onRescanLibrary: () -> Unit = {},
    val onSortOrderChanged: (SortOrder) -> Unit = {}
)
