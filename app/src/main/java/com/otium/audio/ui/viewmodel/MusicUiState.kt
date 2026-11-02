package com.otium.audio.ui.viewmodel

import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.Song
import com.otium.audio.playback.MusicPlaybackState

enum class LibraryTab {
    TRACKS,
    FAVORITES,
    PLAYLISTS,
    HISTORY
}

data class MusicUiState(
    val selectedTab: LibraryTab = LibraryTab.TRACKS,
    val songs: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<Song> = emptyList(),
    val selectedPlaylist: PlaylistEntity? = null,
    val selectedPlaylistSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val isNowPlayingExpanded: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val playbackState: MusicPlaybackState = MusicPlaybackState()
)
