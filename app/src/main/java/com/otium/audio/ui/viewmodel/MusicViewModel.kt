package com.otium.audio.ui.viewmodel

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.otium.audio.BuildConfig
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.model.SortOrder
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.data.update.UpdateChecker
import com.otium.audio.playback.EqualizerController
import com.otium.audio.playback.MusicController
import com.otium.audio.playback.SleepTimerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MusicViewModel(
    private val repository: AudioRepository,
    private val musicController: MusicController,
    private val equalizerController: EqualizerController,
    private val sleepTimerController: SleepTimerController,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val initialTab = savedStateHandle?.get<String>("selected_tab")
        ?.let { runCatching { LibraryTab.valueOf(it) }.getOrNull() }
        ?: LibraryTab.TRACKS

    private val initialSortOrder = savedStateHandle?.get<String>("sort_order")
        ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
        ?: SortOrder.DATE_ADDED_DESC

    private val _uiState = MutableStateFlow(
        MusicUiState(
            selectedTab = initialTab,
            sortOrder = initialSortOrder
        )
    )
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    /** Ticks four times a second while playing; collected only by the progress widgets. */
    val position: StateFlow<Long> = musicController.position

    /** Fine-grained flows for isolated UI recomposition */
    val currentSong: StateFlow<Song?> = musicController.playbackState
        .map { it.currentSong }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPlaying: StateFlow<Boolean> = musicController.playbackState
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isScanning: StateFlow<Boolean> = repository.isScanningFlow()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val searchQuery = MutableStateFlow("")

    private var libraryLoaded = false
    private var lastRecordedSongId: Long? = null
    private var sessionRestored = false
    private var playlistSongsJob: Job? = null

    init {
        restoreSession()
        observePlaybackState()
        observeEqualizer()
        observeSleepTimer()
        observeSearch()
        observeScanningState()
        checkForUpdates()
    }

    // --- Library -----------------------------------------------------------

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasStoragePermission = true) }
        if (libraryLoaded) return
        libraryLoaded = true
        // Immediately scan now that permission is granted so songs appear
        // on first launch without needing an app restart.
        repository.refresh()
        loadLibrary()
    }

    // Seven independent collectors used to drive seven separate state writes, and every
    // derived flow re-opened its own MediaStore observer upstream. One combine, one write.
    private fun loadLibrary() {
        viewModelScope.launch {
            combine(
                repository.getAllSongsFlow(),
                repository.getFavoritesFlow(),
                repository.getFoldersFlow(),
                repository.getPlaylistsFlow()
            ) { songs, favorites, folders, playlists ->
                Library(songs, favorites, folders, playlists)
            }
                .combine(repository.getRecentlyPlayedFlow()) { lib, played ->
                    lib.copy(recentlyPlayed = played)
                }
                .combine(repository.getMostPlayedFlow(MOST_PLAYED_LIMIT)) { lib, top ->
                    lib.copy(mostPlayed = top)
                }
                .combine(repository.getRecentlyAddedFlow()) { lib, added ->
                    lib.copy(recentlyAdded = added)
                }
                .collect { lib ->
                    _uiState.update {
                        it.copy(
                            songs = lib.songs,
                            favorites = lib.favorites,
                            folders = lib.folders,
                            playlists = lib.playlists,
                            history = lib.recentlyPlayed,
                            mostPlayed = lib.mostPlayed,
                            recentlyAdded = lib.recentlyAdded
                        )
                    }
                }
        }
    }

    /**
     * The queue can only be rebuilt once two separate things are ready: the library, and
     * the media controller's connection to the service. On a cold start the connection is
     * usually the slower of the two, so this waits for both and keeps trying; the earlier
     * version gave up on the first attempt and marked itself done.
     */
    private fun restoreSession() {
        if (!musicController.hasSavedSession()) return
        viewModelScope.launch {
            combine(
                repository.getAllSongsFlow(),
                musicController.playbackState.map { it.isConnected }.distinctUntilChanged()
            ) { songs, connected -> songs to connected }
                .collect { (songs, connected) ->
                    if (sessionRestored || !connected || songs.isEmpty()) return@collect
                    sessionRestored = musicController.restoreQueue(songs)
                }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            musicController.playbackState.collectLatest { playback ->
                _uiState.update { it.copy(playbackState = playback) }

                // This used to run on every position tick — four history rows per second,
                // colliding with the media scanner. Record once per track instead.
                val songId = playback.currentSong?.id ?: return@collectLatest
                if (songId != lastRecordedSongId) {
                    lastRecordedSongId = songId
                    repository.recordHistory(songId)
                }
            }
        }
    }

    private fun observeEqualizer() {
        viewModelScope.launch {
            equalizerController.state.collectLatest { state ->
                _uiState.update { it.copy(equalizerState = state) }
            }
        }
    }

    private fun observeSleepTimer() {
        viewModelScope.launch {
            sleepTimerController.state.collectLatest { state ->
                _uiState.update { it.copy(sleepTimerState = state) }
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    } else {
                        _uiState.update { it.copy(isSearching = true) }
                        repository.searchSongs(query).collectLatest { results ->
                            _uiState.update { it.copy(searchResults = results) }
                        }
                    }
                }
        }
    }

    private fun observeScanningState() {
        viewModelScope.launch {
            repository.isScanningFlow().collectLatest { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            // Not on the first frame: a network round trip should never race the UI.
            delay(UPDATE_CHECK_DELAY_MS)
            UpdateChecker.checkLatestRelease(BuildConfig.VERSION_NAME)?.let { update ->
                _uiState.update { it.copy(availableUpdate = update) }
            }
        }
    }

    fun dismissUpdate() = _uiState.update { it.copy(availableUpdate = null) }

    // --- Navigation --------------------------------------------------------

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        savedStateHandle?.set("sort_order", order.name)
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun selectTab(tab: LibraryTab) {
        savedStateHandle?.set("selected_tab", tab.name)
        _uiState.update {
            it.copy(selectedTab = tab, selectedPlaylist = null, selectedFolder = null)
        }
        playlistSongsJob?.cancel()
    }

    fun selectFolder(folder: AudioFolder) = _uiState.update { it.copy(selectedFolder = folder) }

    fun clearSelectedFolder() = _uiState.update { it.copy(selectedFolder = null) }

    fun selectPlaylist(playlist: PlaylistEntity) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        playlistSongsJob?.cancel()
        playlistSongsJob = viewModelScope.launch {
            repository.getSongsForPlaylistFlow(playlist.playlistId).collect { songs ->
                _uiState.update { it.copy(selectedPlaylistSongs = songs) }
            }
        }
    }

    fun clearSelectedPlaylist() {
        playlistSongsJob?.cancel()
        playlistSongsJob = null
        _uiState.update { it.copy(selectedPlaylist = null, selectedPlaylistSongs = emptyList()) }
    }

    fun setNowPlayingExpanded(expanded: Boolean) =
        _uiState.update { it.copy(isNowPlayingExpanded = expanded) }

    fun openTagEditor(song: Song?) = _uiState.update { it.copy(songToEdit = song) }

    // --- Playback ----------------------------------------------------------

    fun playSong(song: Song, playlist: List<Song>) {
        sessionRestored = true
        musicController.playSong(song, playlist)
    }
    fun togglePlayPause() = musicController.togglePlayPause()
    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = musicController.seekBy(deltaMs)
    fun skipToNext() = musicController.skipToNext()
    fun skipToPrevious() = musicController.skipToPrevious()
    fun toggleRepeatMode() = musicController.toggleRepeatMode()
    fun toggleShuffle() = musicController.toggleShuffle()
    fun setPlaybackSpeed(speed: Float) = musicController.setPlaybackSpeed(speed)
    fun setSkipSilence(enabled: Boolean) = musicController.setSkipSilence(enabled)

    fun playNext(song: Song) = musicController.playNext(song)
    fun addToQueue(song: Song) = musicController.addToQueue(song)
    fun removeQueueItem(index: Int) = musicController.removeQueueItem(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) =
        musicController.moveQueueItem(fromIndex, toIndex)
    fun playQueueIndex(index: Int) = musicController.playQueueIndex(index)

    // --- Library mutation --------------------------------------------------

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { repository.toggleFavorite(song.id) }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createPlaylist(trimmed) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_uiState.value.selectedPlaylist?.playlistId == playlistId) clearSelectedPlaylist()
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun saveSongMetadata(songId: Long, title: String, artist: String, album: String) {
        viewModelScope.launch {
            repository.updateSongMetadata(songId, title, artist, album)
            _uiState.update { it.copy(songToEdit = null) }
        }
    }

    fun rescanLibrary() = repository.refresh()

    // --- Equalizer ---------------------------------------------------------

    fun toggleEqualizer(enabled: Boolean) = equalizerController.setEnabled(enabled)
    fun setAutoPreAmp(enabled: Boolean) = equalizerController.setAutoPreAmp(enabled)
    fun setPreAmpGain(gainDb: Float) {
        // Only the DSP applies gain. Doing it here as well made every boost twice as loud,
        // which is exactly what a clipped, dirty sound feels like.
        equalizerController.setPreAmpGain(gainDb)
    }
    fun setEqualizerBandLevel(bandIndex: Short, level: Float) =
        equalizerController.setBandLevel(bandIndex, level)
    fun setBassBoost(strength: Int) = equalizerController.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = equalizerController.setVirtualizer(strength)
    fun applyEqualizerPreset(preset: String) = equalizerController.applyPreset(preset)
    fun setReverb(strength: Int, preset: String? = null) = equalizerController.setReverb(strength, preset)
    fun setReverbPreset(preset: String) = equalizerController.setReverbPreset(preset)
    fun setChorus(strength: Int) = equalizerController.setChorus(strength)
    fun setClarity(strength: Int) = equalizerController.setClarity(strength)

    // --- Sleep timer -------------------------------------------------------

    fun startSleepTimer(minutes: Int) = sleepTimerController.startTimer(minutes)
    fun startSleepEndOfTrack() = sleepTimerController.startEndOfTrackTimer()
    fun cancelSleepTimer() = sleepTimerController.cancelTimer()

    private data class Library(
        val songs: List<Song>,
        val favorites: List<Song>,
        val folders: List<AudioFolder>,
        val playlists: List<PlaylistEntity>,
        val recentlyPlayed: List<Song> = emptyList(),
        val mostPlayed: List<Song> = emptyList(),
        val recentlyAdded: List<Song> = emptyList()
    )

    class Factory(
        private val repository: AudioRepository,
        private val musicController: MusicController,
        private val equalizerController: EqualizerController,
        private val sleepTimerController: SleepTimerController
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return MusicViewModel(
                repository,
                musicController,
                equalizerController,
                sleepTimerController,
                extras.createSavedStateHandle()
            ) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MusicViewModel(repository, musicController, equalizerController, sleepTimerController) as T
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 60L
        const val MOST_PLAYED_LIMIT = 50
        const val UPDATE_CHECK_DELAY_MS = 3_000L
    }
}
