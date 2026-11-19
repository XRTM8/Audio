package com.otium.audio.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioAlbum
import com.otium.audio.data.model.AudioArtist
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.model.SortOrder
import com.otium.audio.playback.EqualizerState
import com.otium.audio.playback.MusicPlaybackState
import com.otium.audio.playback.SleepTimerState

enum class LibraryTab {
    TRACKS,
    ARTISTS,
    ALBUMS,
    FAVORITES,
    WEEKLY_ROTATION,
    FORGOTTEN_GEMS,
    FOLDERS,
    RECENTLY_ADDED,
    PLAYLISTS,
    MOST_PLAYED,
    HISTORY
}

@Immutable
data class MusicUiState(
    val selectedTab: LibraryTab = LibraryTab.TRACKS,
    val sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC,
    val songs: List<Song> = emptyList(),
    val artists: List<AudioArtist> = emptyList(),
    val albums: List<AudioAlbum> = emptyList(),
    val folders: List<AudioFolder> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val weeklyRotation: List<Song> = emptyList(),
    val forgottenGems: List<Song> = emptyList(),
    val selectedArtist: AudioArtist? = null,
    val selectedAlbum: AudioAlbum? = null,
    val selectedFolder: AudioFolder? = null,
    val selectedPlaylist: PlaylistEntity? = null,
    val selectedPlaylistSongs: List<Song> = emptyList(),
    val songToEdit: Song? = null,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val isNowPlayingExpanded: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val isScanning: Boolean = false,
    val playbackState: MusicPlaybackState = MusicPlaybackState(),
    val equalizerState: EqualizerState = EqualizerState(),
    val sleepTimerState: SleepTimerState = SleepTimerState(),
    val isSelectionMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    val availableUpdate: com.otium.audio.data.update.UpdateInfo? = null,
    val minDurationSeconds: Int = 0,
    val isLoudnessNormalizationEnabled: Boolean = false,
    val isDarkMode: Boolean = true
)
