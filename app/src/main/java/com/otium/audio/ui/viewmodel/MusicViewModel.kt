package com.otium.audio.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.playback.EqualizerController
import com.otium.audio.playback.MusicController
import com.otium.audio.playback.SleepTimerController
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
    private val musicController: MusicController,
    private val equalizerController: EqualizerController,
    private val sleepTimerController: SleepTimerController,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val initialTab = savedStateHandle?.get<String>("selected_tab")?.let {
        runCatching { LibraryTab.valueOf(it) }.getOrNull()
    } ?: LibraryTab.TRACKS

    private val _uiState = MutableStateFlow(MusicUiState(selectedTab = initialTab))
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        observePlaybackState()
        observeEqualizer()
        observeSleepTimer()
        observeSearch()
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            val update = com.otium.audio.data.update.UpdateChecker.checkLatestRelease("1.5.0")
            if (update != null) {
                _uiState.update { it.copy(availableUpdate = update) }
            }
        }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(availableUpdate = null) }
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
            repository.getFoldersFlow().collectLatest { folders ->
                _uiState.update { it.copy(folders = folders) }
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

        viewModelScope.launch {
            repository.getMostPlayedFlow().collectLatest { mostPlayed ->
                _uiState.update { it.copy(mostPlayed = mostPlayed) }
            }
        }

        viewModelScope.launch {
            repository.getRecentlyAddedFlow().collectLatest { recentlyAdded ->
                _uiState.update { it.copy(recentlyAdded = recentlyAdded) }
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

    private fun observeEqualizer() {
        viewModelScope.launch {
            equalizerController.state.collectLatest { eqState ->
                _uiState.update { it.copy(equalizerState = eqState) }
            }
        }
    }

    private fun observeSleepTimer() {
        viewModelScope.launch {
            sleepTimerController.state.collectLatest { timerState ->
                _uiState.update { it.copy(sleepTimerState = timerState) }
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
        savedStateHandle?.set("selected_tab", tab.name)
        _uiState.update { it.copy(selectedTab = tab, selectedPlaylist = null, selectedFolder = null) }
    }

    fun selectFolder(folder: AudioFolder) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun clearSelectedFolder() {
        _uiState.update { it.copy(selectedFolder = null) }
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

    fun setPlaybackSpeed(speed: Float) {
        musicController.setPlaybackSpeed(speed)
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    fun setPreAmpGain(gainDb: Float) {
        equalizerController.setPreAmpGain(gainDb)
    }

    fun setEqualizerBandLevel(bandIndex: Short, level: Float) {
        equalizerController.setBandLevel(bandIndex, level)
    }

    fun setBassBoost(strength: Int) {
        equalizerController.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        equalizerController.setVirtualizer(strength)
    }

    fun applyEqualizerPreset(preset: String) {
        equalizerController.applyPreset(preset)
    }

    fun openTagEditor(song: Song?) {
        _uiState.update { it.copy(songToEdit = song) }
    }

    fun saveSongMetadata(songId: Long, title: String, artist: String, album: String) {
        viewModelScope.launch {
            repository.updateSongMetadata(songId, title, artist, album)
            _uiState.update { it.copy(songToEdit = null) }
        }
    }

    // --- Sleep Timer ---
    fun startSleepTimer(minutes: Int) = sleepTimerController.startTimer(minutes)
    fun startSleepEndOfTrack() = sleepTimerController.startEndOfTrackTimer()
    fun cancelSleepTimer() = sleepTimerController.cancelTimer()

    // --- Queue Management ---
    fun playNext(song: Song) = musicController.playNext(song)
    fun addToQueue(song: Song) = musicController.addToQueue(song)
    fun removeQueueItem(index: Int) = musicController.removeQueueItem(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = musicController.moveQueueItem(fromIndex, toIndex)
    fun playQueueIndex(index: Int) = musicController.playQueueIndex(index)

    class Factory(
        private val repository: AudioRepository,
        private val musicController: MusicController,
        private val equalizerController: EqualizerController,
        private val sleepTimerController: SleepTimerController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                val savedStateHandle = extras.createSavedStateHandle()
                return MusicViewModel(repository, musicController, equalizerController, sleepTimerController, savedStateHandle) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MusicViewModel(repository, musicController, equalizerController, sleepTimerController, null) as T
        }
    }
}
