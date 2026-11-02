package com.otium.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.otium.audio.R
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.playback.BatteryOptimizationHelper
import com.otium.audio.ui.components.FastAlphabetScroller
import kotlinx.coroutines.launch
import com.otium.audio.ui.components.MiniPlayer
import com.otium.audio.ui.components.SongListItem
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.viewmodel.LibraryTab
import com.otium.audio.ui.viewmodel.MusicUiState

@Composable
fun HomeScreen(
    uiState: MusicUiState,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    onClearSelectedPlaylist: () -> Unit,
    onFolderSelected: (AudioFolder) -> Unit = {},
    onClearSelectedFolder: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showBatteryBanner by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Otium Audio",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.songs.size} tracks offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = if (uiState.equalizerState.isEnabled) AccentGold else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Battery Optimization Alert Banner (For aggressive OEM battery managers)
            if (showBatteryBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unrestricted Playback",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Keep music playing uninterrupted when screen is off.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        TextButton(onClick = {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                            showBatteryBanner = false
                        }) {
                            Text("Allow", color = AccentGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Row (Hidden while actively searching, viewing a playlist, or viewing a folder)
            if (!uiState.isSearching && uiState.selectedPlaylist == null && uiState.selectedFolder == null) {
                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = DarkSurface,
                    contentColor = AccentGold,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(uiState.selectedTab.ordinal),
                            color = AccentGold,
                            height = 3.dp
                        )
                    }
                ) {
                    LibraryTab.entries.forEach { tab ->
                        val title = when (tab) {
                            LibraryTab.TRACKS -> stringResource(R.string.all_songs)
                            LibraryTab.FOLDERS -> "Folders"
                            LibraryTab.FAVORITES -> stringResource(R.string.favorites)
                            LibraryTab.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryTab.HISTORY -> "History"
                        }
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (uiState.selectedTab == tab) AccentGold else TextSecondary
                                )
                            }
                        )
                    }
                }
            }

            // Playlist Navigation Header if inside a playlist
            if (uiState.selectedPlaylist != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearSelectedPlaylist) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = uiState.selectedPlaylist.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            // Folder Navigation Header if inside a folder
            if (uiState.selectedFolder != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearSelectedFolder) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedFolder.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.selectedFolder.songCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Content List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isSearching -> {
                        SongListView(
                            songs = uiState.searchResults,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.searchResults) },
                            onFavoriteClick = onFavoriteToggle
                        )
                    }

                    uiState.selectedPlaylist != null -> {
                        SongListView(
                            songs = uiState.selectedPlaylistSongs,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.selectedPlaylistSongs) },
                            onFavoriteClick = onFavoriteToggle
                        )
                    }

                    uiState.selectedFolder != null -> {
                        SongListView(
                            songs = uiState.selectedFolder.songs,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.selectedFolder.songs) },
                            onFavoriteClick = onFavoriteToggle
                        )
                    }

                    uiState.selectedTab == LibraryTab.TRACKS -> {
                        SongListView(
                            songs = uiState.songs,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.songs) },
                            onFavoriteClick = onFavoriteToggle,
                            showAlphabetScroller = true
                        )
                    }

                    uiState.selectedTab == LibraryTab.FOLDERS -> {
                        FolderListView(
                            folders = uiState.folders,
                            onFolderClick = onFolderSelected
                        )
                    }

                    uiState.selectedTab == LibraryTab.FAVORITES -> {
                        SongListView(
                            songs = uiState.favorites,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.favorites) },
                            onFavoriteClick = onFavoriteToggle
                        )
                    }

                    uiState.selectedTab == LibraryTab.HISTORY -> {
                        SongListView(
                            songs = uiState.history,
                            currentSong = uiState.playbackState.currentSong,
                            onSongClick = { song -> onSongSelected(song, uiState.history) },
                            onFavoriteClick = onFavoriteToggle
                        )
                    }

                    uiState.selectedTab == LibraryTab.PLAYLISTS -> {
                        PlaylistsView(
                            playlists = uiState.playlists,
                            onPlaylistClick = onPlaylistSelected,
                            onDeletePlaylist = onDeletePlaylist,
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                        )
                    }
                }
            }

            // Floating MiniPlayer at bottom
            val currentSong = uiState.playbackState.currentSong
            if (currentSong != null) {
                MiniPlayer(
                    currentSong = currentSong,
                    isPlaying = uiState.playbackState.isPlaying,
                    currentPositionMs = uiState.playbackState.currentPositionMs,
                    durationMs = uiState.playbackState.durationMs,
                    onPlayPauseClick = onPlayPause,
                    onSkipNextClick = onSkipNext,
                    onPlayerClick = onOpenNowPlaying,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name ->
                    onCreatePlaylist(name)
                    showCreatePlaylistDialog = false
                }
            )
        }
    }
}

@Composable
private fun SongListView(
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    showAlphabetScroller: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_songs_found),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        onSongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSongClick(song)
                        },
                        onFavoriteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteClick(song)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            if (showAlphabetScroller && songs.size >= 15) {
                FastAlphabetScroller(
                    songs = songs,
                    onIndexSelected = { targetIndex ->
                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 16.dp, bottom = 120.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderListView(
    folders: List<AudioFolder>,
    onFolderClick: (AudioFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No audio folders found", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(folders, key = { it.path }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onFolderClick(folder) }
                        .padding(16.dp),
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
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${folder.songCount} tracks • ${folder.path}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PlaylistsView(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No playlists created yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(playlists, key = { it.playlistId }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onPlaylistClick(playlist) }
                            .padding(16.dp),
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
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { onDeletePlaylist(playlist.playlistId) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = TextSecondary
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onCreatePlaylistClick,
            containerColor = AccentGold,
            contentColor = DarkSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_playlist),
                color = TextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface)
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceContainer
    )
}
