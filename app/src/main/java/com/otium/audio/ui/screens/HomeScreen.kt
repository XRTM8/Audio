package com.otium.audio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.update.UpdateChecker
import com.otium.audio.ui.components.FastAlphabetScroller
import com.otium.audio.ui.components.MiniPlayer
import com.otium.audio.ui.components.SettingsSheet
import com.otium.audio.ui.components.SongListItem
import com.otium.audio.ui.components.TagEditorSheet
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldMuted
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.viewmodel.LibraryTab
import com.otium.audio.ui.viewmodel.MusicUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    onEditTagClick: (Song) -> Unit = {},
    onSaveMetadata: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onDismissTagEditor: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkSurface)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App name + track count
                    Column {
                        Text(
                            text = "OTIUM AUDIO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                fontSize = 17.sp
                            ),
                            color = AccentGold
                        )
                        Text(
                            text = "${uiState.songs.size} Tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Right icon cluster
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.availableUpdate != null) {
                            IconButton(onClick = {
                                UpdateChecker.openDownload(context, uiState.availableUpdate.downloadUrl)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Update Available",
                                    tint = AccentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = if (uiState.equalizerState.isEnabled) AccentGold else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ── Search bar ───────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    placeholder = {
                        Text(
                            text = "Search tracks, artists, albums...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceContainer,
                        unfocusedContainerColor = DarkSurfaceContainer,
                        focusedBorderColor = AccentGold.copy(alpha = 0.6f),
                        unfocusedBorderColor = DarkCardBorder,
                        cursorColor = AccentGold,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // ── Tab pills ─────────────────────────────────────────────
                if (!uiState.isSearching && uiState.selectedPlaylist == null && uiState.selectedFolder == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(LibraryTab.TRACKS,       "All Songs",     uiState.songs.size),
                            Triple(LibraryTab.FAVORITES,    "Favorites",     uiState.favorites.size),
                            Triple(LibraryTab.RECENTLY_ADDED,"Recent",       uiState.recentlyAdded.size),
                            Triple(LibraryTab.MOST_PLAYED,  "Most Played",   uiState.mostPlayed.size),
                            Triple(LibraryTab.FOLDERS,      "Folders",       uiState.folders.size),
                            Triple(LibraryTab.PLAYLISTS,    "Playlists",     uiState.playlists.size),
                            Triple(LibraryTab.HISTORY,      "History",       uiState.history.size)
                        )
                        tabs.forEach { (tab, label, count) ->
                            val isSelected = uiState.selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) AccentGold else DarkSurfaceContainer
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (count > 0) "$label ($count)" else label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) DarkSurface else TextSecondary
                                )
                            }
                        }
                    }
                }

                // ── Playlist header ───────────────────────────────────────
                if (uiState.selectedPlaylist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                // ── Folder header ─────────────────────────────────────────
                if (uiState.selectedFolder != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.selectedFolder.songCount} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (uiState.selectedFolder.songs.isNotEmpty()) {
                                    onSongSelected(
                                        uiState.selectedFolder.songs.first(),
                                        uiState.selectedFolder.songs
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Play All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // ── Content area ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = when {
                            uiState.isSearching -> "SEARCH"
                            uiState.selectedPlaylist != null -> "PLAYLIST"
                            uiState.selectedFolder != null -> "FOLDER"
                            else -> uiState.selectedTab.name
                        },
                        transitionSpec = {
                            fadeIn(tween(160)) togetherWith fadeOut(tween(160))
                        },
                        label = "tab_anim"
                    ) { target ->
                        when (target) {
                            "SEARCH" -> SongListView(
                                songs = uiState.searchResults,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.searchResults) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            "PLAYLIST" -> SongListView(
                                songs = uiState.selectedPlaylistSongs,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.selectedPlaylistSongs) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            "FOLDER" -> SongListView(
                                songs = uiState.selectedFolder?.songs ?: emptyList(),
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s ->
                                    onSongSelected(s, uiState.selectedFolder?.songs ?: emptyList())
                                },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            LibraryTab.TRACKS.name -> SongListView(
                                songs = uiState.songs,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.songs) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick,
                                showAlphabetScroller = true
                            )

                            LibraryTab.FAVORITES.name -> SongListView(
                                songs = uiState.favorites,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.favorites) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            LibraryTab.RECENTLY_ADDED.name -> SongListView(
                                songs = uiState.recentlyAdded,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.recentlyAdded) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            LibraryTab.MOST_PLAYED.name -> SongListView(
                                songs = uiState.mostPlayed,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.mostPlayed) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )

                            LibraryTab.FOLDERS.name -> FolderListView(
                                folders = uiState.folders,
                                onFolderClick = onFolderSelected
                            )

                            LibraryTab.PLAYLISTS.name -> PlaylistsView(
                                playlists = uiState.playlists,
                                onPlaylistClick = onPlaylistSelected,
                                onDeletePlaylist = onDeletePlaylist,
                                onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                            )

                            LibraryTab.HISTORY.name -> SongListView(
                                songs = uiState.history,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { s -> onSongSelected(s, uiState.history) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }
                    }
                }

                // ── Mini Player (Poweramp bottom bar) ─────────────────────
                val currentSong = uiState.playbackState.currentSong
                if (currentSong != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder)
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
                } else {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }

            // ── Dialogs & sheets ──────────────────────────────────────────
            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onCreate = { name ->
                        onCreatePlaylist(name)
                        showCreatePlaylistDialog = false
                    }
                )
            }

            if (uiState.songToEdit != null) {
                TagEditorSheet(
                    song = uiState.songToEdit,
                    onSave = { title, artist, album ->
                        onSaveMetadata(uiState.songToEdit.id, title, artist, album)
                    },
                    onDismiss = onDismissTagEditor
                )
            }

            if (showSettings) {
                SettingsSheet(
                    trackCount = uiState.songs.size,
                    availableUpdate = uiState.availableUpdate,
                    onOpenEqualizer = onOpenEqualizer,
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

// ── SongListView ─────────────────────────────────────────────────────────────
@Composable
private fun SongListView(
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onEditTagClick: ((Song) -> Unit)? = null,
    showAlphabetScroller: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No tracks found",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hasScroller = showAlphabetScroller && songs.size >= 12

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (hasScroller) 28.dp else 0.dp)
        ) {
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
                    },
                    onEditTagClick = onEditTagClick?.let { { it(song) } }
                )
            }
            // Space so last item isn't hidden behind bottom bar
            item { Spacer(modifier = Modifier.height(8.dp)) }
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

// ── FolderListView ────────────────────────────────────────────────────────────
@Composable
private fun FolderListView(
    folders: List<AudioFolder>,
    onFolderClick: (AudioFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found", color = TextSecondary)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(folders, key = { it.path }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .clickable { onFolderClick(folder) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.songCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.3f))
        }
    }
}

// ── PlaylistsView ─────────────────────────────────────────────────────────────
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No playlists yet",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCreatePlaylistClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.playlistId }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant)
                            .clickable { onPlaylistClick(playlist) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.3f))
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Floating create button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Button(
                    onClick = onCreatePlaylistClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── CreatePlaylistDialog ──────────────────────────────────────────────────────
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceContainer,
        title = {
            Text("New Playlist", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = TextSecondary) },
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
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
