package com.otium.audio.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.Song
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.playback.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: AudioRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        observePlaybackState()
        observeSearch()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasStoragePermission = true) }
        loadLibraryData()
    }

    private fun loadLibraryData() {
        viewModelScope.launch {
            repository.getAllSongsFlow().collectLatest { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }

        viewModelScope.launch {
            repository.getFavoritesFlow().collectLatest { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }

        viewModelScope.launch {
            repository.getPlaylistsFlow().collectLatest { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        viewModelScope.launch {
            repository.getRecentlyPlayedFlow().collectLatest { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            musicController.playbackState.collectLatest { playbackState ->
                _uiState.update { it.copy(playbackState = playbackState) }
                playbackState.currentSong?.let { song ->
                    repository.recordHistory(song.id)
                }
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(200L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    } else {
                        repository.searchSongs(query).collectLatest { results ->
                            _uiState.update { it.copy(searchResults = results, isSearching = true) }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedPlaylist = null) }
    }

    fun selectPlaylist(playlist: PlaylistEntity) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        viewModelScope.launch {
            repository.getSongsForPlaylistFlow(playlist.playlistId).collectLatest { songs ->
                _uiState.update { it.copy(selectedPlaylistSongs = songs) }
            }
        }
    }

    fun clearSelectedPlaylist() {
        _uiState.update { it.copy(selectedPlaylist = null, selectedPlaylistSongs = emptyList()) }
    }

    fun playSong(song: Song, playlist: List<Song>) {
        musicController.playSong(song, playlist)
    }

    fun togglePlayPause() {
        musicController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
    }

    fun skipToNext() {
        musicController.skipToNext()
    }

    fun skipToPrevious() {
        musicController.skipToPrevious()
    }

    fun toggleRepeatMode() {
        musicController.toggleRepeatMode()
    }

    fun toggleShuffle() {
        musicController.toggleShuffle()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim())
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_uiState.value.selectedPlaylist?.playlistId == playlistId) {
                clearSelectedPlaylist()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isNowPlayingExpanded = expanded) }
    }

    class Factory(
        private val repository: AudioRepository,
        private val musicController: MusicController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(repository, musicController) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
