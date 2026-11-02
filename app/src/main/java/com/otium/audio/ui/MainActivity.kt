package com.otium.audio.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.otium.audio.OtiumApplication
import com.otium.audio.ui.components.EqualizerSheet
import com.otium.audio.ui.screens.HomeScreen
import com.otium.audio.ui.screens.NowPlayingScreen
import com.otium.audio.ui.screens.PermissionScreen
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.OtiumAudioTheme
import com.otium.audio.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        val app = application as OtiumApplication
        MusicViewModel.Factory(
            repository = app.repository,
            musicController = app.musicController,
            equalizerController = app.equalizerController
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
                    var showHomeEqualizer by remember { mutableStateOf(false) }

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
                            onTabSelected = viewModel::selectTab,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onSongSelected = viewModel::playSong,
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onPlaylistSelected = viewModel::selectPlaylist,
                            onClearSelectedPlaylist = viewModel::clearSelectedPlaylist,
                            onCreatePlaylist = viewModel::createPlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipToNext,
                            onOpenNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                            onOpenEqualizer = { showHomeEqualizer = true }
                        )

                        if (showHomeEqualizer) {
                            EqualizerSheet(
                                state = uiState.equalizerState,
                                onToggleEnabled = viewModel::toggleEqualizer,
                                onBandLevelChange = viewModel::setEqualizerBandLevel,
                                onBassBoostChange = viewModel::setBassBoost,
                                onVirtualizerChange = viewModel::setVirtualizer,
                                onPresetSelected = viewModel::applyEqualizerPreset,
                                onDismiss = { showHomeEqualizer = false }
                            )
                        }

                        // Smooth modal expansion for NowPlayingScreen
                        val currentSong = uiState.playbackState.currentSong
                        AnimatedVisibility(
                            visible = uiState.isNowPlayingExpanded && currentSong != null,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            if (currentSong != null) {
                                BackHandler {
                                    viewModel.setNowPlayingExpanded(false)
                                }

                                NowPlayingScreen(
                                    song = currentSong,
                                    isPlaying = uiState.playbackState.isPlaying,
                                    currentPositionMs = uiState.playbackState.currentPositionMs,
                                    durationMs = uiState.playbackState.durationMs,
                                    repeatMode = uiState.playbackState.repeatMode,
                                    isShuffleEnabled = uiState.playbackState.isShuffleEnabled,
                                    playbackSpeed = uiState.playbackState.playbackSpeed,
                                    equalizerState = uiState.equalizerState,
                                    onCollapse = { viewModel.setNowPlayingExpanded(false) },
                                    onPlayPause = viewModel::togglePlayPause,
                                    onSeekTo = viewModel::seekTo,
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
                                    onEqualizerPresetSelected = viewModel::applyEqualizerPreset
                                )
                            }
                        }
                    }
                }
            }
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
