package com.otium.audio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import com.otium.audio.ui.theme.AmbientBackdrop
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextTertiary
import kotlinx.coroutines.flow.StateFlow

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioAlbum
import com.otium.audio.data.model.AudioArtist
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.model.SortOrder
import com.otium.audio.ui.components.FastAlphabetScroller
import com.otium.audio.ui.components.MiniPlayer
import com.otium.audio.ui.components.PlaylistPickerBottomSheet
import com.otium.audio.ui.components.SettingsSheet
import com.otium.audio.ui.components.SongListItem
import com.otium.audio.ui.components.SortBottomSheet
import com.otium.audio.ui.components.TagEditorSheet
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.viewmodel.LibraryTab
import com.otium.audio.ui.viewmodel.MusicUiState
import com.otium.audio.ui.actions.PlaybackActions
import com.otium.audio.ui.actions.EqualizerActions
import com.otium.audio.ui.actions.LibraryActions
import com.otium.audio.ui.components.home.HomeTopBar
import com.otium.audio.ui.components.home.HomeSearchBar
import com.otium.audio.ui.components.home.HomeFilterPills
import com.otium.audio.ui.components.home.SelectionTopBar
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: MusicUiState,
    positionFlow: StateFlow<Long>,
    libraryActions: LibraryActions,
    playbackActions: PlaybackActions,
    equalizerActions: EqualizerActions = EqualizerActions(),
    onOpenNowPlaying: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBatchDeleteClick: (List<Song>) -> Unit = {},
    modifier: Modifier = Modifier
) = HomeScreen(
    uiState = uiState,
    positionFlow = positionFlow,
    onTabSelected = libraryActions.onTabSelected,
    onSearchQueryChanged = libraryActions.onSearchQueryChanged,
    onSongSelected = libraryActions.onSongSelected,
    onFavoriteToggle = libraryActions.onFavoriteToggle,
    onPlaylistSelected = libraryActions.onPlaylistSelected,
    onClearSelectedPlaylist = libraryActions.onClearSelectedPlaylist,
    onFolderSelected = libraryActions.onFolderSelected,
    onClearSelectedFolder = libraryActions.onClearSelectedFolder,
    onArtistSelected = libraryActions.onArtistSelected,
    onClearSelectedArtist = libraryActions.onClearSelectedArtist,
    onAlbumSelected = libraryActions.onAlbumSelected,
    onClearSelectedAlbum = libraryActions.onClearSelectedAlbum,
    onCreatePlaylist = libraryActions.onCreatePlaylist,
    onDeletePlaylist = libraryActions.onDeletePlaylist,
    onPlayPause = playbackActions.onPlayPause,
    onSkipNext = playbackActions.onSkipNext,
    onSkipPrevious = playbackActions.onSkipPrevious,
    onOpenNowPlaying = onOpenNowPlaying,
    onSeekTo = playbackActions.onSeekTo,
    onOpenEqualizer = onOpenEqualizer,
    onEditTagClick = libraryActions.onEditTagClick,
    onSongLongPress = libraryActions.onSongLongPress,
    onSaveMetadata = libraryActions.onSaveMetadata,
    onDismissTagEditor = libraryActions.onDismissTagEditor,
    onDismissUpdate = libraryActions.onDismissUpdate,
    onToggleSkipSilence = playbackActions.onToggleSkipSilence,
    onToggleAutoPreAmp = equalizerActions.onToggleAutoPreAmp,
    onRescanLibrary = libraryActions.onRescanLibrary,
    onSortOrderChanged = libraryActions.onSortOrderChanged,
    onStartSelection = libraryActions.onStartSelection,
    onToggleSelection = libraryActions.onToggleSelection,
    onSelectAll = libraryActions.onSelectAll,
    onClearSelection = libraryActions.onClearSelection,
    onBatchDeleteClick = onBatchDeleteClick,
    onToggleHiResFloat = playbackActions.onToggleHiResFloat,
    onAddBatchToPlaylist = libraryActions.onAddBatchToPlaylist,
    onAddBatchToQueue = libraryActions.onAddBatchToQueue,
    onPlayBatchNext = libraryActions.onPlayBatchNext,
    onSetMinDurationSeconds = libraryActions.onSetMinDurationSeconds,
    onToggleLoudnessNormalization = playbackActions.onToggleLoudnessNormalization,
    onExportBackup = libraryActions.onExportBackup,
    onImportBackup = libraryActions.onImportBackup,
    onRemoveSongFromPlaylist = libraryActions.onRemoveSongFromPlaylist,
    onSetCrossfadeSeconds = playbackActions.onSetCrossfadeSeconds,
    onToggleResumeOnHeadsetConnect = playbackActions.onToggleResumeOnHeadsetConnect,
    onToggleCrossfeed = equalizerActions.onToggleCrossfeed,
    onToggleDarkMode = libraryActions.onToggleDarkMode,
    modifier = modifier
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MusicUiState,
    positionFlow: StateFlow<Long>,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    onClearSelectedPlaylist: () -> Unit,
    onFolderSelected: (AudioFolder) -> Unit = {},
    onClearSelectedFolder: () -> Unit = {},
    onArtistSelected: (AudioArtist) -> Unit = {},
    onClearSelectedArtist: () -> Unit = {},
    onAlbumSelected: (AudioAlbum) -> Unit = {},
    onClearSelectedAlbum: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onOpenNowPlaying: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onEditTagClick: (Song) -> Unit = {},
    onSongLongPress: (Song) -> Unit = {},
    onSaveMetadata: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onDismissTagEditor: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onToggleSkipSilence: (Boolean) -> Unit = {},
    onToggleHiResFloat: (Boolean) -> Unit = {},
    onToggleAutoPreAmp: (Boolean) -> Unit = {},
    onRescanLibrary: () -> Unit = {},
    onSortOrderChanged: (SortOrder) -> Unit = {},
    onStartSelection: (Song?) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onSelectAll: (List<Song>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBatchDeleteClick: (List<Song>) -> Unit = {},
    onAddBatchToPlaylist: (Long, Collection<Long>) -> Unit = { _, _ -> },
    onAddBatchToQueue: (List<Song>) -> Unit = {},
    onPlayBatchNext: (List<Song>) -> Unit = {},
    onSetMinDurationSeconds: (Int) -> Unit = {},
    onToggleLoudnessNormalization: (Boolean) -> Unit = {},
    onExportBackup: ((String?) -> Unit) -> Unit = {},
    onImportBackup: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onRemoveSongFromPlaylist: (Long, Long) -> Unit = { _, _ -> },
    onSetCrossfadeSeconds: (Int) -> Unit = {},
    onToggleResumeOnHeadsetConnect: (Boolean) -> Unit = {},
    onToggleCrossfeed: (Boolean) -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showBatchPlaylistPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    // Active displayed track list based on selected filter and user sort order.
    val rawSongs = when {
        uiState.isSearching -> uiState.searchResults
        uiState.selectedPlaylist != null -> uiState.selectedPlaylistSongs
        uiState.selectedFolder != null -> uiState.selectedFolder.songs
        uiState.selectedArtist != null -> uiState.selectedArtist.songs
        uiState.selectedAlbum != null -> uiState.selectedAlbum.songs
        uiState.selectedTab == LibraryTab.FAVORITES -> uiState.favorites
        uiState.selectedTab == LibraryTab.WEEKLY_ROTATION -> uiState.weeklyRotation
        uiState.selectedTab == LibraryTab.FORGOTTEN_GEMS -> uiState.forgottenGems
        uiState.selectedTab == LibraryTab.RECENTLY_ADDED -> uiState.recentlyAdded
        uiState.selectedTab == LibraryTab.MOST_PLAYED -> uiState.mostPlayed
        uiState.selectedTab == LibraryTab.HISTORY -> uiState.history
        else -> uiState.songs
    }

    val displayedSongs = remember(rawSongs, uiState.sortOrder, uiState.isSearching, uiState.selectedTab) {
        if (uiState.isSearching || uiState.selectedTab == LibraryTab.RECENTLY_ADDED || uiState.selectedTab == LibraryTab.MOST_PLAYED || uiState.selectedTab == LibraryTab.HISTORY || uiState.selectedTab == LibraryTab.WEEKLY_ROTATION || uiState.selectedTab == LibraryTab.FORGOTTEN_GEMS) {
            rawSongs
        } else {
            uiState.sortOrder.sort(rawSongs)
        }
    }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AmbientBackdrop)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (uiState.isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedSongIds.size,
                        totalCount = displayedSongs.size,
                        onClearSelection = onClearSelection,
                        onSelectAll = { onSelectAll(displayedSongs) },
                        onAddToPlaylist = { showBatchPlaylistPicker = true },
                        onAddToQueue = {
                            val selected = displayedSongs.filter { it.id in uiState.selectedSongIds }
                            if (selected.isNotEmpty()) {
                                onAddBatchToQueue(selected)
                            }
                        },
                        onPlayNext = {
                            val selected = displayedSongs.filter { it.id in uiState.selectedSongIds }
                            if (selected.isNotEmpty()) {
                                onPlayBatchNext(selected)
                            }
                        },
                        onShareSelected = {
                            val selected = displayedSongs.filter { it.id in uiState.selectedSongIds }
                            if (selected.isNotEmpty()) {
                                val uris = ArrayList(selected.map { it.contentUri })
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "audio/*"
                                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة المقاطع الصوتية"))
                            }
                        },
                        onDeleteSelected = {
                            val toDelete = displayedSongs.filter { it.id in uiState.selectedSongIds }
                            if (toDelete.isNotEmpty()) {
                                onBatchDeleteClick(toDelete)
                            }
                        }
                    )
                } else {
                    HomeTopBar(
                        songCount = uiState.songs.size,
                        isEqualizerEnabled = uiState.equalizerState.isEnabled,
                        availableUpdate = uiState.availableUpdate,
                        isDarkMode = uiState.isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onOpenEqualizer = onOpenEqualizer,
                        onOpenSettings = { showSettings = true },
                        onOpenSort = { showSortSheet = true },
                        onStartSelection = { onStartSelection(null) }
                    )

                    HomeSearchBar(
                        query = uiState.searchQuery,
                        songCount = uiState.songs.size,
                        onQueryChanged = onSearchQueryChanged,
                        focusManager = focusManager
                    )
                }

                if (!uiState.isSelectionMode && !uiState.isSearching && uiState.selectedPlaylist == null && uiState.selectedFolder == null && uiState.selectedArtist == null && uiState.selectedAlbum == null) {
                    HomeFilterPills(
                        selectedTab = uiState.selectedTab,
                        counts = listOf(
                            uiState.songs.size,
                            uiState.artists.size,
                            uiState.albums.size,
                            uiState.favorites.size,
                            uiState.weeklyRotation.size,
                            uiState.forgottenGems.size,
                            uiState.folders.size,
                            uiState.recentlyAdded.size,
                            uiState.playlists.size,
                            uiState.mostPlayed.size,
                            uiState.history.size
                        ),
                        onTabSelected = onTabSelected,
                        haptic = haptic
                    )
                }

                // ── Breadcrumb headers for Folders, Playlists, Artists or Albums ──
                if (uiState.selectedArtist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelectedArtist) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedArtist.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.selectedArtist.songCount} مقطع",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (uiState.selectedArtist.songs.isNotEmpty()) {
                                    onSongSelected(uiState.selectedArtist.songs.first(), uiState.selectedArtist.songs)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, color = DarkSurface, fontSize = 12.sp)
                        }
                    }
                }

                if (uiState.selectedAlbum != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelectedAlbum) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedAlbum.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.selectedAlbum.artist}  •  ${uiState.selectedAlbum.songCount} مقطع",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (uiState.selectedAlbum.songs.isNotEmpty()) {
                                    onSongSelected(uiState.selectedAlbum.songs.first(), uiState.selectedAlbum.songs)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, color = DarkSurface, fontSize = 12.sp)
                        }
                    }
                }

                if (uiState.selectedPlaylist != null) {
                    val playlistSongs = uiState.selectedPlaylistSongs
                    val totalDurationMs = remember(playlistSongs) { playlistSongs.sumOf { it.durationMs } }
                    val totalMinutes = totalDurationMs / 60_000
                    val durationText = if (totalMinutes >= 60) {
                        "${totalMinutes / 60} س ${totalMinutes % 60} د"
                    } else {
                        "$totalMinutes دقيقة"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSheetCard)
                            .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onClearSelectedPlaylist) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedPlaylist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlistSongs.size} مقطع  •  $durationText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (playlistSongs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onSongSelected(playlistSongs.first(), playlistSongs) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = DarkSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تشغيل الكل", fontWeight = FontWeight.Bold, color = DarkSurface, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        val shuffled = playlistSongs.shuffled()
                                        onSongSelected(shuffled.first(), shuffled)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainer),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        tint = AccentTitaniumWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("عشوائي", fontWeight = FontWeight.SemiBold, color = AccentTitaniumWhite, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                if (uiState.selectedFolder != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelectedFolder) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedFolder.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.selectedFolder.songCount} مقطع",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (uiState.selectedFolder.songs.isNotEmpty()) {
                                    onSongSelected(uiState.selectedFolder.songs.first(), uiState.selectedFolder.songs)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, color = DarkSurface, fontSize = 12.sp)
                        }
                    }
                }

                // ── Unified 120 FPS Master Track List ───────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.selectedTab == LibraryTab.ARTISTS && uiState.selectedArtist == null && !uiState.isSearching) {
                        ArtistListView(
                            artists = uiState.artists,
                            onArtistClick = onArtistSelected
                        )
                    } else if (uiState.selectedTab == LibraryTab.ALBUMS && uiState.selectedAlbum == null && !uiState.isSearching) {
                        AlbumListView(
                            albums = uiState.albums,
                            onAlbumClick = onAlbumSelected
                        )
                    } else if (uiState.selectedTab == LibraryTab.FOLDERS && uiState.selectedFolder == null && !uiState.isSearching) {
                        FolderListView(
                            folders = uiState.folders,
                            onFolderClick = onFolderSelected
                        )
                    } else if (uiState.selectedTab == LibraryTab.PLAYLISTS && uiState.selectedPlaylist == null && !uiState.isSearching) {
                        PlaylistsView(
                            playlists = uiState.playlists,
                            weeklyRotationCount = uiState.weeklyRotation.size,
                            forgottenGemsCount = uiState.forgottenGems.size,
                            onOpenSmartPlaylist = onTabSelected,
                            onPlaylistClick = onPlaylistSelected,
                            onDeletePlaylist = onDeletePlaylist,
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                        )
                    } else {
                        // High-performance single master song list with immediate 0ms click response
                        var lastSongClickTime by remember { mutableLongStateOf(0L) }
                        var lastSongClickId by remember { mutableLongStateOf(-1L) }

                        val handleSongClick = remember(displayedSongs, uiState.playbackState, onSongSelected, onOpenNowPlaying) {
                            { song: Song ->
                                val now = android.os.SystemClock.uptimeMillis()
                                val isThisSongPlaying = uiState.playbackState.isPlaying && uiState.playbackState.currentSong?.id == song.id
                                if (isThisSongPlaying) {
                                    // إذا الأغنية مشتغلة: ضغطة وحدة تفتحها كنافذة كبيرة فوراً
                                    onOpenNowPlaying()
                                } else {
                                    val isQuickDoubleClick = (song.id == lastSongClickId) && (now - lastSongClickTime < 450L)
                                    if (isQuickDoubleClick) {
                                        // ضغطتين متتاليتين على أغنية غير مشتغلة: تشغيلها وفتح نافذة كبيرة فوراً
                                        onSongSelected(song, displayedSongs)
                                        onOpenNowPlaying()
                                        lastSongClickTime = 0L
                                        lastSongClickId = -1L
                                    } else {
                                        // نقرة واحدة: تشغيل فوري بدون أي تأخير إطلاقاً (0ms latency!)
                                        onSongSelected(song, displayedSongs)
                                        lastSongClickTime = now
                                        lastSongClickId = song.id
                                    }
                                }
                            }
                        }

                        MasterSongListView(
                            songs = displayedSongs,
                            currentSong = uiState.playbackState.currentSong,
                            isPlaying = uiState.playbackState.isPlaying,
                            onSongClick = handleSongClick,
                            onSongDoubleClick = null,
                            onFavoriteClick = onFavoriteToggle,
                            onEditTagClick = onEditTagClick,
                            onSongLongPress = {
                                if (uiState.isSelectionMode) {
                                    onToggleSelection(it.id)
                                } else {
                                    onSongLongPress(it)
                                }
                            },
                            searchQuery = uiState.searchQuery,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedSongIds = uiState.selectedSongIds,
                            onToggleSelect = { onToggleSelection(it.id) },
                            showAlphabetScroller = !uiState.isSelectionMode &&
                                !uiState.isSearching &&
                                uiState.selectedFolder == null &&
                                uiState.selectedPlaylist == null &&
                                uiState.selectedArtist == null &&
                                uiState.selectedAlbum == null &&
                                (uiState.selectedTab == LibraryTab.TRACKS || uiState.selectedTab == LibraryTab.FAVORITES) &&
                                (uiState.sortOrder == SortOrder.TITLE_ASC || uiState.sortOrder == SortOrder.ARTIST_ASC)
                        )
                    }
                }

                // ── Poweramp Full-Width Bottom Console MiniPlayer with Fluid Entrance ───────────
                val currentSong = uiState.playbackState.currentSong
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) + fadeIn(tween(180)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    ) + fadeOut(tween(140))
                ) {
                    if (currentSong != null) {
                        MiniPlayer(
                            currentSong = currentSong,
                            isPlaying = uiState.playbackState.isPlaying,
                            positionFlow = positionFlow,
                            durationMs = uiState.playbackState.durationMs,
                            onPlayPauseClick = onPlayPause,
                            onSkipNextClick = onSkipNext,
                            onSkipPreviousClick = onSkipPrevious,
                            onPlayerClick = onOpenNowPlaying,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }

            // ── Dialogs & Sheets ──────────────────────────────────────────
            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onCreate = { name ->
                        onCreatePlaylist(name)
                        showCreatePlaylistDialog = false
                    }
                )
            }

            uiState.songToEdit?.let { targetSong ->
                TagEditorSheet(
                    song = targetSong,
                    onSave = { title, artist, album ->
                        onSaveMetadata(targetSong.id, title, artist, album)
                    },
                    onDismiss = onDismissTagEditor
                )
            }

            if (showBatchPlaylistPicker) {
                PlaylistPickerBottomSheet(
                    playlists = uiState.playlists,
                    songTitle = "${uiState.selectedSongIds.size} مقاطع محددة",
                    onSelectPlaylist = { playlist ->
                        onAddBatchToPlaylist(playlist.playlistId, uiState.selectedSongIds)
                        showBatchPlaylistPicker = false
                    },
                    onCreateNewPlaylistClick = {
                        showBatchPlaylistPicker = false
                        showCreatePlaylistDialog = true
                    },
                    onDismiss = { showBatchPlaylistPicker = false }
                )
            }

            if (showSettings) {
                SettingsSheet(
                    trackCount = uiState.songs.size,
                    availableUpdate = uiState.availableUpdate,
                    isSkipSilenceEnabled = uiState.playbackState.isSkipSilenceEnabled,
                    onToggleSkipSilence = onToggleSkipSilence,
                    isHiResFloatEnabled = uiState.playbackState.isHiResFloatEnabled,
                    onToggleHiResFloat = onToggleHiResFloat,
                    isAutoPreAmpEnabled = uiState.equalizerState.isAutoPreAmpEnabled,
                    onToggleAutoPreAmp = onToggleAutoPreAmp,
                    isLoudnessNormalizationEnabled = uiState.isLoudnessNormalizationEnabled,
                    onToggleLoudnessNormalization = onToggleLoudnessNormalization,
                    isCrossfeedEnabled = uiState.equalizerState.isCrossfeedEnabled,
                    onToggleCrossfeed = onToggleCrossfeed,
                    crossfadeSeconds = uiState.playbackState.crossfadeSeconds,
                    onSetCrossfadeSeconds = onSetCrossfadeSeconds,
                    isResumeOnHeadsetConnect = uiState.playbackState.isResumeOnHeadsetConnect,
                    onToggleResumeOnHeadsetConnect = onToggleResumeOnHeadsetConnect,
                    minDurationSeconds = uiState.minDurationSeconds,
                    onSetMinDurationSeconds = onSetMinDurationSeconds,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    isScanning = uiState.isScanning,
                    onRescanLibrary = onRescanLibrary,
                    onOpenEqualizer = onOpenEqualizer,
                    onDismiss = { showSettings = false }
                )
            }

            if (showSortSheet) {
                SortBottomSheet(
                    currentSortOrder = uiState.sortOrder,
                    onSelectSortOrder = onSortOrderChanged,
                    onDismiss = { showSortSheet = false }
                )
            }
        }

}

// ── Ultra-Fast 120 FPS LazyColumn with Recycling ─────────────────────────────
@Composable
private fun MasterSongListView(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean = false,
    onSongClick: (Song) -> Unit,
    onSongDoubleClick: ((Song) -> Unit)? = null,
    onFavoriteClick: (Song) -> Unit,
    onEditTagClick: ((Song) -> Unit)? = null,
    onSongLongPress: (Song) -> Unit,
    searchQuery: String = "",
    isSelectionMode: Boolean = false,
    selectedSongIds: Set<Long> = emptySet(),
    onToggleSelect: ((Song) -> Unit)? = null,
    showAlphabetScroller: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "لم يتم العثور على أي مقاطع صوتية",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hasScroller = showAlphabetScroller && songs.size >= 25

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (hasScroller) 14.dp else 0.dp)
        ) {
            items(
                items = songs,
                key = { song -> song.id },
                contentType = { "audiophile_song_row" }
            ) { song ->
                SongListItem(
                    modifier = Modifier.animateItem(),
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    onSongClick = onSongClick,
                    onDoubleClick = onSongDoubleClick,
                    onFavoriteClick = onFavoriteClick,
                    onEditTagClick = onEditTagClick,
                    onLongClick = onSongLongPress,
                    searchQuery = searchQuery,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedSongIds.contains(song.id),
                    onToggleSelect = onToggleSelect
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        if (hasScroller) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
            ) {
                FastAlphabetScroller(
                    songs = songs,
                    onIndexSelected = { idx ->
                        scope.launch { listState.scrollToItem(idx) }
                    }
                )
            }
        }
    }
}

// ── Artists View ─────────────────────────────────────────────────────────────
@Composable
private fun ArtistListView(
    artists: List<AudioArtist>,
    onArtistClick: (AudioArtist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد فنانون", color = TextSecondary)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(artists, key = { it.name }, contentType = { "artist_row" }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular artist avatar with subtle ring border
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceContainer)
                        .border(1.dp, DarkCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = artist.name.trim().take(1).uppercase()
                    Text(
                        text = if (initial.isNotBlank()) initial else "♪",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${artist.songCount} مقطع صوتي",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { onArtistClick(artist) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Artist",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
        }
    }
}

// ── Albums View (Studio 2-Column Gallery Grid) ──────────────────────────────
@Composable
private fun AlbumListView(
    albums: List<AudioAlbum>,
    onAlbumClick: (AudioAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (albums.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد ألبومات", color = TextSecondary)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { "${it.name}_${it.artist}" }) { album ->
            val thumbReq = if (album.albumArtUri != android.net.Uri.EMPTY) {
                remember(album.albumArtUri) {
                    ImageRequest.Builder(context)
                        .data(album.albumArtUri)
                        .size(320, 320)
                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
            } else null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAlbumClick(album) }
            ) {
                // Large square album artwork card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceContainer)
                        .border(0.75.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    if (thumbReq != null) {
                        AsyncImage(
                            model = thumbReq,
                            contentDescription = album.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // Track count pill in bottom-end
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD9000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${album.songCount} مقطع",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Folders View ─────────────────────────────────────────────────────────────
@Composable
private fun FolderListView(
    folders: List<AudioFolder>,
    onFolderClick: (AudioFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد مجلدات صوتية", color = TextSecondary)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(folders, key = { it.path }, contentType = { "folder_row" }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceContainer)
                        .border(0.75.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AccentTitaniumWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.songCount} مقطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { onFolderClick(folder) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open Folder",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
        }
    }
}

// ── Playlists View ───────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsView(
    playlists: List<PlaylistEntity>,
    weeklyRotationCount: Int,
    forgottenGemsCount: Int,
    onOpenSmartPlaylist: (LibraryTab) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = DarkSurfaceContainer,
            title = { Text("حذف قائمة التشغيل", color = TextPrimary) },
            text = {
                Text("سيتم حذف قائمة «${target.name}». ستبقى المقاطع الصوتية في مكتبتك كما هي.", color = TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(target.playlistId)
                    pendingDelete = null
                }) { Text("حذف", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء", color = TextSecondary) }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Smart Dynamic Playlists Section ─────────────────────
            item(key = "smart_header") {
                Text(
                    text = "القوائم الذكية",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentGold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
                )
            }

            // 1. Weekly Heavy Rotation
            item(key = "smart_weekly") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSmartPlaylist(LibraryTab.WEEKLY_ROTATION) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceContainer)
                            .border(0.75.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "التدوير الأسبوعي",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$weeklyRotationCount مقطع  •  الأكثر استماعاً خلال الأسبوع",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
            }

            // 2. Forgotten Gems
            item(key = "smart_gems") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSmartPlaylist(LibraryTab.FORGOTTEN_GEMS) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceContainer)
                            .border(0.75.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "جواهر منسية",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$forgottenGemsCount مقطع  •  مفضلتك التي لم تسمعها مؤخراً",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
            }

            // ── User Manual Playlists Section ────────────────────────
            item(key = "user_playlists_header") {
                Text(
                    text = "قوائمي (${playlists.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                )
            }

            if (playlists.isEmpty()) {
                item(key = "empty_playlists") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد قوائم مخصصة حتى الآن", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                items(playlists, key = { it.playlistId }, contentType = { "playlist_row" }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onPlaylistClick(playlist) },
                                onLongClick = { pendingDelete = playlist }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceContainer)
                                .border(0.75.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = AccentTitaniumWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { pendingDelete = playlist },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Button(
                onClick = onCreatePlaylistClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("قائمة جديدة", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Create Playlist Dialog ───────────────────────────────────────────────────
@Composable
internal fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceContainer,
        title = { Text("قائمة تشغيل جديدة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("اسم القائمة", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate(name.trim()) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface)
            ) {
                Text("إنشاء", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        }
    )
}
