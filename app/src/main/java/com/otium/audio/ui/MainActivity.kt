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
import androidx.compose.runtime.Composable
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
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipToNext,
                            onSkipPrevious = viewModel::skipToPrevious,
                            onOpenNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                            onSeekTo = viewModel::seekTo,
                            onOpenEqualizer = { showHomeEqualizer = true },
                            onEditTagClick = viewModel::openTagEditor,
                            onSaveMetadata = viewModel::saveSongMetadata,
                            onDismissTagEditor = { viewModel.openTagEditor(null) },
                            onDismissUpdate = viewModel::dismissUpdate,
                            onSongLongPress = { songToDelete = it }
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
                                onToggleEnabled = viewModel::toggleEqualizer,
                                onBandLevelChange = viewModel::setEqualizerBandLevel,
                                onBassBoostChange = viewModel::setBassBoost,
                                onVirtualizerChange = viewModel::setVirtualizer,
                                onPresetSelected = viewModel::applyEqualizerPreset,
                                onPreAmpChange = viewModel::setPreAmpGain,
                                onReverbChange = viewModel::setReverb,
                                onReverbPresetChange = viewModel::setReverbPreset,
                                onChorusChange = viewModel::setChorus,
                                onClarityChange = viewModel::setClarity,
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
                                    onCollapse = { viewModel.setNowPlayingExpanded(false) },
                                    onPlayPause = viewModel::togglePlayPause,
                                    onSeekTo = viewModel::seekTo,
                                    onSeekBy = viewModel::seekBy,
                                    onSkipNext = viewModel::skipToNext,
                                    onSkipPrevious = viewModel::skipToPrevious,
                                    onToggleRepeat = viewModel::toggleRepeatMode,
                                    onToggleShuffle = viewModel::toggleShuffle,
                                    onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                                    onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                                    onToggleEqualizer = viewModel::toggleEqualizer,
                                    onEqualizerBandChange = viewModel::setEqualizerBandLevel,
                                    onBassBoostChange = viewModel::setBassBoost,
                                    onVirtualizerChange = viewModel::setVirtualizer,
                                    onEqualizerPresetSelected = viewModel::applyEqualizerPreset,
                                    onPreAmpChange = viewModel::setPreAmpGain,
                                    onReverbChange = viewModel::setReverb,
                                    onReverbPresetChange = viewModel::setReverbPreset,
                                    onChorusChange = viewModel::setChorus,
                                    onClarityChange = viewModel::setClarity,
                                    onPlayQueueIndex = viewModel::playQueueIndex,
                                    onRemoveQueueItem = viewModel::removeQueueItem,
                                    onMoveQueueItem = viewModel::moveQueueItem,
                                    onStartSleepTimer = viewModel::startSleepTimer,
                                    onStartSleepEndOfTrack = viewModel::startSleepEndOfTrack,
                                    onCancelSleepTimer = viewModel::cancelSleepTimer,
                                    onEditTagClick = { viewModel.openTagEditor(currentSong) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have granted the permission from system settings while the
        // app was backgrounded; without this the library stays empty forever.
        if (hasRequiredAudioPermission(this)) viewModel.onPermissionGranted()
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
