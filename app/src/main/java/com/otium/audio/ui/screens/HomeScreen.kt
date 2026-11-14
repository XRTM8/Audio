package com.otium.audio.ui.screens

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.otium.audio.ui.components.SettingsSheet
import com.otium.audio.ui.components.SongListItem
import com.otium.audio.ui.components.SortBottomSheet
import com.otium.audio.ui.components.TagEditorSheet
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
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
    onToggleAutoPreAmp: (Boolean) -> Unit = {},
    onRescanLibrary: () -> Unit = {},
    onSortOrderChanged: (SortOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
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
        uiState.selectedTab == LibraryTab.RECENTLY_ADDED -> uiState.recentlyAdded
        uiState.selectedTab == LibraryTab.MOST_PLAYED -> uiState.mostPlayed
        uiState.selectedTab == LibraryTab.HISTORY -> uiState.history
        else -> uiState.songs
    }

    val displayedSongs = remember(rawSongs, uiState.sortOrder, uiState.isSearching, uiState.selectedTab) {
        if (uiState.isSearching || uiState.selectedTab == LibraryTab.RECENTLY_ADDED || uiState.selectedTab == LibraryTab.MOST_PLAYED || uiState.selectedTab == LibraryTab.HISTORY) {
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

                HomeTopBar(
                    songCount = uiState.songs.size,
                    isEqualizerEnabled = uiState.equalizerState.isEnabled,
                    availableUpdate = uiState.availableUpdate,
                    onOpenEqualizer = onOpenEqualizer,
                    onOpenSettings = { showSettings = true },
                    onOpenSort = { showSortSheet = true }
                )

                HomeSearchBar(
                    query = uiState.searchQuery,
                    songCount = uiState.songs.size,
                    onQueryChanged = onSearchQueryChanged,
                    focusManager = focusManager
                )

                if (!uiState.isSearching && uiState.selectedPlaylist == null && uiState.selectedFolder == null && uiState.selectedArtist == null && uiState.selectedAlbum == null) {
                    HomeFilterPills(
                        selectedTab = uiState.selectedTab,
                        counts = listOf(
                            uiState.songs.size,
                            uiState.artists.size,
                            uiState.albums.size,
                            uiState.favorites.size,
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (uiState.selectedPlaylist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelectedPlaylist) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Text(
                            text = uiState.selectedPlaylist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("تشغيل الكل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            onPlaylistClick = onPlaylistSelected,
                            onDeletePlaylist = onDeletePlaylist,
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                        )
                    } else {
                        // High-performance single master song list
                        val handleSongClick = remember(displayedSongs, onSongSelected) {
                            { song: Song -> onSongSelected(song, displayedSongs) }
                        }
                        MasterSongListView(
                            songs = displayedSongs,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = handleSongClick,
                            onFavoriteClick = onFavoriteToggle,
                            onEditTagClick = onEditTagClick,
                            onSongLongPress = onSongLongPress,
                            searchQuery = uiState.searchQuery,
                            showAlphabetScroller = !uiState.isSearching && uiState.selectedFolder == null && uiState.selectedPlaylist == null && uiState.selectedArtist == null && uiState.selectedAlbum == null
                        )
                    }
                }

                // ── Poweramp Full-Width Bottom Console MiniPlayer ───────────
                val currentSong = uiState.playbackState.currentSong
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
                } else {
                    Spacer(modifier = Modifier.navigationBarsPadding())
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

            if (showSettings) {
                SettingsSheet(
                    trackCount = uiState.songs.size,
                    availableUpdate = uiState.availableUpdate,
                    isSkipSilenceEnabled = uiState.playbackState.isSkipSilenceEnabled,
                    onToggleSkipSilence = onToggleSkipSilence,
                    isAutoPreAmpEnabled = uiState.equalizerState.isAutoPreAmpEnabled,
                    onToggleAutoPreAmp = onToggleAutoPreAmp,
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
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onEditTagClick: ((Song) -> Unit)? = null,
    onSongLongPress: (Song) -> Unit,
    searchQuery: String = "",
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
                key = { it.id },
                contentType = { "audiophile_song_row" }
            ) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    onSongClick = onSongClick,
                    onFavoriteClick = onFavoriteClick,
                    onEditTagClick = onEditTagClick,
                    onLongClick = onSongLongPress,
                    searchQuery = searchQuery
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
                    .background(DarkSurfaceVariant)
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.songCount} مقطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.25f))
        }
    }
}

// ── Albums View ──────────────────────────────────────────────────────────────
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
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(albums, key = { "${it.name}_${it.artist}" }, contentType = { "album_row" }) { album ->
            val thumbReq = if (album.albumArtUri != android.net.Uri.EMPTY) {
                remember(album.albumArtUri) {
                    ImageRequest.Builder(context)
                        .data(album.albumArtUri)
                        .size(128, 128)
                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
            } else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .clickable { onAlbumClick(album) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    if (thumbReq != null) {
                        AsyncImage(
                            model = thumbReq,
                            contentDescription = album.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist}  •  ${album.songCount} مقطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.25f))
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
                    .background(DarkSurfaceVariant)
                    .clickable { onFolderClick(folder) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.songCount} مقطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.25f))
        }
    }
}

// ── Playlists View ───────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsView(
    playlists: List<PlaylistEntity>,
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
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "لا توجد قوائم تشغيل حتى الآن", color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCreatePlaylistClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قائمة تشغيل جديدة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.playlistId }, contentType = { "playlist_row" }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant)
                            .combinedClickable(
                                onClick = { onPlaylistClick(playlist) },
                                onLongClick = { pendingDelete = playlist }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.25f))
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
