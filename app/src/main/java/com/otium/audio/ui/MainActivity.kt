package com.otium.audio.ui

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.otium.audio.OtiumApplication
import com.otium.audio.ui.components.DeleteSongDialog
import com.otium.audio.ui.components.EqualizerSheet
import com.otium.audio.ui.screens.HomeScreen
import com.otium.audio.ui.screens.NowPlayingScreen
import com.otium.audio.ui.screens.PermissionScreen
import com.otium.audio.ui.theme.asReadableAccent
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.OtiumAccent
import com.otium.audio.ui.theme.OtiumAudioTheme
import com.otium.audio.ui.theme.PaletteManager
import com.otium.audio.ui.viewmodel.MusicViewModel
import com.otium.audio.ui.actions.PlaybackActions
import com.otium.audio.ui.actions.EqualizerActions
import com.otium.audio.ui.actions.QueueActions
import com.otium.audio.ui.actions.SleepTimerActions
import com.otium.audio.ui.actions.LibraryActions

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        val app = application as OtiumApplication
        MusicViewModel.Factory(
            repository = app.repository,
            musicController = app.musicController,
            equalizerController = app.equalizerController,
            sleepTimerController = app.sleepTimerController
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.decorView.post { enableHighRefreshRate() }

        setContent {
            OtiumAudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSurface
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // One colour, taken from whatever is playing. Everything that means
                    // "this is active" reads it, everything else stays neutral.
                    val playing = uiState.playbackState.currentSong
                    LaunchedEffect(playing?.albumArtUri) {
                        val art = playing?.albumArtUri ?: return@LaunchedEffect
                        val colors = PaletteManager.extractColors(this@MainActivity, art)
                        OtiumAccent.color = colors.vibrant.asReadableAccent()
                    }
                    var showHomeEqualizer by remember { mutableStateOf(false) }
                    var songToDelete by remember { mutableStateOf<com.otium.audio.data.model.Song?>(null) }

                    LaunchedEffect(Unit) {
                        if (hasRequiredAudioPermission(this@MainActivity)) {
                            viewModel.onPermissionGranted()
                        }
                    }

                    val playbackActions = remember(viewModel) {
                        PlaybackActions(
                            onPlayPause = viewModel::togglePlayPause,
                            onSeekTo = viewModel::seekTo,
                            onSeekBy = viewModel::seekBy,
                            onSkipNext = viewModel::skipToNext,
                            onSkipPrevious = viewModel::skipToPrevious,
                            onToggleRepeat = viewModel::toggleRepeatMode,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                            onToggleSkipSilence = viewModel::setSkipSilence
                        )
                    }

                    val equalizerActions = remember(viewModel) {
                        EqualizerActions(
                            onToggle = viewModel::toggleEqualizer,
                            onToggleAutoPreAmp = viewModel::setAutoPreAmp,
                            onBandLevelChange = viewModel::setEqualizerBandLevel,
                            onBassBoostChange = viewModel::setBassBoost,
                            onVirtualizerChange = viewModel::setVirtualizer,
                            onPresetSelected = viewModel::applyEqualizerPreset,
                            onPreAmpChange = viewModel::setPreAmpGain,
                            onReverbChange = viewModel::setReverb,
                            onReverbPresetChange = viewModel::setReverbPreset,
                            onChorusChange = viewModel::setChorus,
                            onClarityChange = viewModel::setClarity
                        )
                    }

                    val queueActions = remember(viewModel) {
                        QueueActions(
                            onPlayQueueIndex = viewModel::playQueueIndex,
                            onRemoveQueueItem = viewModel::removeQueueItem,
                            onMoveQueueItem = viewModel::moveQueueItem
                        )
                    }

                    val sleepTimerActions = remember(viewModel) {
                        SleepTimerActions(
                            onStartTimer = viewModel::startSleepTimer,
                            onStartEndOfTrack = viewModel::startSleepEndOfTrack,
                            onCancelTimer = viewModel::cancelSleepTimer
                        )
                    }

                    val libraryActions = remember(viewModel) {
                        LibraryActions(
                            onTabSelected = viewModel::selectTab,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onSongSelected = viewModel::playSong,
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onPlaylistSelected = viewModel::selectPlaylist,
                            onClearSelectedPlaylist = viewModel::clearSelectedPlaylist,
                            onFolderSelected = viewModel::selectFolder,
                            onClearSelectedFolder = viewModel::clearSelectedFolder,
                            onCreatePlaylist = viewModel::createPlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onEditTagClick = viewModel::openTagEditor,
                            onSaveMetadata = viewModel::saveSongMetadata,
                            onSongLongPress = { songToDelete = it },
                            onDismissTagEditor = { viewModel.openTagEditor(null) },
                            onDismissUpdate = viewModel::dismissUpdate,
                            onRescanLibrary = viewModel::rescanLibrary,
                            onSortOrderChanged = viewModel::setSortOrder
                        )
                    }

                    if (!uiState.hasStoragePermission) {
                        PermissionScreen(
                            onPermissionGranted = {
                                viewModel.onPermissionGranted()
                            }
                        )
                    } else {
                        HomeScreen(
                            uiState = uiState,
                            positionFlow = viewModel.position,
                            libraryActions = libraryActions,
                            playbackActions = playbackActions,
                            equalizerActions = equalizerActions,
                            onOpenNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                            onOpenEqualizer = { showHomeEqualizer = true }
                        )

                        DeleteSongDialog(
                            song = songToDelete,
                            onDismiss = { songToDelete = null },
                            onSongDeleted = { deletedSong ->
                                if (uiState.playbackState.currentSong?.id == deletedSong.id) {
                                    viewModel.skipToNext()
                                }
                            }
                        )

                        if (showHomeEqualizer) {
                            EqualizerSheet(
                                state = uiState.equalizerState,
                                onToggleEnabled = equalizerActions.onToggle,
                                onBandLevelChange = equalizerActions.onBandLevelChange,
                                onBassBoostChange = equalizerActions.onBassBoostChange,
                                onVirtualizerChange = equalizerActions.onVirtualizerChange,
                                onPresetSelected = equalizerActions.onPresetSelected,
                                onPreAmpChange = equalizerActions.onPreAmpChange,
                                onReverbChange = equalizerActions.onReverbChange,
                                onReverbPresetChange = equalizerActions.onReverbPresetChange,
                                onChorusChange = equalizerActions.onChorusChange,
                                onClarityChange = equalizerActions.onClarityChange,
                                onDismiss = { showHomeEqualizer = false }
                            )
                        }

                        BackHandler(enabled = uiState.selectedFolder != null && !uiState.isNowPlayingExpanded) {
                            viewModel.clearSelectedFolder()
                        }

                        BackHandler(enabled = uiState.selectedPlaylist != null && !uiState.isNowPlayingExpanded) {
                            viewModel.clearSelectedPlaylist()
                        }

                        BackHandler(enabled = uiState.searchQuery.isNotBlank() && !uiState.isNowPlayingExpanded) {
                            viewModel.onSearchQueryChanged("")
                        }

                        // Smooth modal expansion for NowPlayingScreen
                        val currentSong = uiState.playbackState.currentSong
                        AnimatedVisibility(
                            visible = uiState.isNowPlayingExpanded && currentSong != null,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                            ),
                            exit = fadeOut(animationSpec = tween(durationMillis = 140))
                        ) {
                            if (currentSong != null) {
                                BackHandler {
                                    viewModel.setNowPlayingExpanded(false)
                                }

                                NowPlayingScreen(
                                    song = currentSong,
                                    isPlaying = uiState.playbackState.isPlaying,
                                    positionFlow = viewModel.position,
                                    durationMs = uiState.playbackState.durationMs,
                                    repeatMode = uiState.playbackState.repeatMode,
                                    isShuffleEnabled = uiState.playbackState.isShuffleEnabled,
                                    playbackSpeed = uiState.playbackState.playbackSpeed,
                                    equalizerState = uiState.equalizerState,
                                    queue = if (uiState.playbackState.queue.isNotEmpty()) uiState.playbackState.queue else uiState.songs,
                                    sleepTimerState = uiState.sleepTimerState,
                                    playbackActions = playbackActions,
                                    equalizerActions = equalizerActions,
                                    queueActions = queueActions,
                                    sleepTimerActions = sleepTimerActions,
                                    onCollapse = { viewModel.setNowPlayingExpanded(false) },
                                    onEditTagClick = { viewModel.openTagEditor(currentSong) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enableHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableHighRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
        // The user may have granted the permission from system settings while the
        // app was backgrounded; without this the library stays empty forever.
        if (hasRequiredAudioPermission(this)) viewModel.onPermissionGranted()
    }

    private fun enableHighRefreshRate() {
        try {
            val targetDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    window.decorView.display ?: display
                } catch (_: Throwable) {
                    window.decorView.display
                }
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val modes = targetDisplay.supportedModes ?: emptyArray()
            val highest = modes.maxByOrNull { it.refreshRate }
            if (highest != null && highest.refreshRate >= 85f) {
                val lp = window.attributes
                var changed = false
                if (lp.preferredDisplayModeId != highest.modeId) {
                    lp.preferredDisplayModeId = highest.modeId
                    changed = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (lp.preferredRefreshRate != highest.refreshRate) {
                        lp.preferredRefreshRate = highest.refreshRate
                        changed = true
                    }
                }
                if (changed) {
                    window.attributes = lp
                }
            }
        } catch (_: Throwable) {
            // Graceful fallback if device vendor locks display attributes
        }
    }

    private fun hasRequiredAudioPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
