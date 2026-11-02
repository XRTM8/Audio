package com.otium.audio.ui.viewmodel

import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.playback.EqualizerState
import com.otium.audio.playback.MusicPlaybackState
import com.otium.audio.playback.SleepTimerState

enum class LibraryTab {
    TRACKS,
    FOLDERS,
    FAVORITES,
    PLAYLISTS,
    HISTORY
}

data class MusicUiState(
    val selectedTab: LibraryTab = LibraryTab.TRACKS,
    val songs: List<Song> = emptyList(),
    val folders: List<AudioFolder> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val selectedFolder: AudioFolder? = null,
    val selectedPlaylist: PlaylistEntity? = null,
    val selectedPlaylistSongs: List<Song> = emptyList(),
    val songToEdit: Song? = null,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val isNowPlayingExpanded: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val playbackState: MusicPlaybackState = MusicPlaybackState(),
    val equalizerState: EqualizerState = EqualizerState(),
    val sleepTimerState: SleepTimerState = SleepTimerState()
)
