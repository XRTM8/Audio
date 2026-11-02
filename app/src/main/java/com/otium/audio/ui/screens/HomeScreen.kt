package com.otium.audio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.otium.audio.R
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import com.otium.audio.data.update.UpdateChecker
import com.otium.audio.ui.components.FastAlphabetScroller
import com.otium.audio.ui.components.MiniPlayer
import com.otium.audio.ui.components.SongListItem
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Poweramp Luxury Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "OTIUM AUDIO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = AccentGold
                        )
                        Text(
                            text = "Hi-Res Offline Engine • ${uiState.songs.size} Tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Update Available Badge Button
                    if (uiState.availableUpdate != null) {
                        IconButton(onClick = {
                            UpdateChecker.openDownload(context, uiState.availableUpdate.downloadUrl)
                        }) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Update Available",
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Equalizer Button
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = if (uiState.equalizerState.isEnabled) AccentGold else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // In-App Auto Update Floating Alert Banner
            if (uiState.availableUpdate != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentGold.copy(alpha = 0.6f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🚀 New Update Available: ${uiState.availableUpdate.versionName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentGold
                            )
                            Text(
                                text = "Tap to update seamlessly with zero package conflict.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                UpdateChecker.openDownload(context, uiState.availableUpdate.downloadUrl)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Poweramp Floating Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        text = "Search tracks, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentGold
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
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceContainer,
                    unfocusedContainerColor = DarkSurfaceContainer,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkCardBorder,
                    cursorColor = AccentGold,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            // Poweramp Horizontal Filter Chips (Smooth scrollable pills, zero broken text wrapping!)
            if (!uiState.isSearching && uiState.selectedPlaylist == null && uiState.selectedFolder == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chips = listOf(
                        Triple(LibraryTab.TRACKS, "All Songs", uiState.songs.size),
                        Triple(LibraryTab.FAVORITES, "Favorites", uiState.favorites.size),
                        Triple(LibraryTab.RECENTLY_ADDED, "Recently Added", uiState.recentlyAdded.size),
                        Triple(LibraryTab.MOST_PLAYED, "Most Played", uiState.mostPlayed.size),
                        Triple(LibraryTab.FOLDERS, "Folders", uiState.folders.size),
                        Triple(LibraryTab.PLAYLISTS, "Playlists", uiState.playlists.size),
                        Triple(LibraryTab.HISTORY, "History", uiState.history.size)
                    )

                    chips.forEach { (tab, title, count) ->
                        val isSelected = uiState.selectedTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            },
                            label = {
                                Text(
                                    text = if (count > 0) "$title ($count)" else title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold,
                                selectedLabelColor = DarkSurface,
                                containerColor = DarkSurfaceContainer,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) AccentGold else DarkCardBorder
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // Playlist Navigation Header if inside a playlist
            if (uiState.selectedPlaylist != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            color = TextPrimary
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
                                onSongSelected(uiState.selectedFolder.songs.first(), uiState.selectedFolder.songs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Main Content Area with 120fps smooth transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = when {
                        uiState.isSearching -> "SEARCH"
                        uiState.selectedPlaylist != null -> "PLAYLIST_DETAIL"
                        uiState.selectedFolder != null -> "FOLDER_DETAIL"
                        else -> uiState.selectedTab.name
                    },
                    transitionSpec = { fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) },
                    label = "tab_content_anim"
                ) { target ->
                    when (target) {
                        "SEARCH" -> {
                            SongListView(
                                songs = uiState.searchResults,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.searchResults) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        "PLAYLIST_DETAIL" -> {
                            SongListView(
                                songs = uiState.selectedPlaylistSongs,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.selectedPlaylistSongs) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        "FOLDER_DETAIL" -> {
                            SongListView(
                                songs = uiState.selectedFolder?.songs ?: emptyList(),
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.selectedFolder?.songs ?: emptyList()) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        LibraryTab.TRACKS.name -> {
                            // Unified Master List of All Songs (Poweramp Style)
                            SongListView(
                                songs = uiState.songs,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.songs) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick,
                                showAlphabetScroller = true
                            )
                        }

                        LibraryTab.FAVORITES.name -> {
                            SongListView(
                                songs = uiState.favorites,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.favorites) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        LibraryTab.RECENTLY_ADDED.name -> {
                            SongListView(
                                songs = uiState.recentlyAdded,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.recentlyAdded) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        LibraryTab.MOST_PLAYED.name -> {
                            SongListView(
                                songs = uiState.mostPlayed,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.mostPlayed) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }

                        LibraryTab.FOLDERS.name -> {
                            FolderListView(
                                folders = uiState.folders,
                                onFolderClick = onFolderSelected
                            )
                        }

                        LibraryTab.PLAYLISTS.name -> {
                            PlaylistsView(
                                playlists = uiState.playlists,
                                onPlaylistClick = onPlaylistSelected,
                                onDeletePlaylist = onDeletePlaylist,
                                onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                            )
                        }

                        LibraryTab.HISTORY.name -> {
                            SongListView(
                                songs = uiState.history,
                                currentSong = uiState.playbackState.currentSong,
                                onSongClick = { song -> onSongSelected(song, uiState.history) },
                                onFavoriteClick = onFavoriteToggle,
                                onEditTagClick = onEditTagClick
                            )
                        }
                    }
                }
            }

            // Floating Poweramp MiniPlayer at bottom
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

        // ID3 Tag Editor Bottom Sheet
        if (uiState.songToEdit != null) {
            TagEditorSheet(
                song = uiState.songToEdit,
                onSave = { title, artist, album ->
                    onSaveMetadata(uiState.songToEdit.id, title, artist, album)
                },
                onDismiss = onDismissTagEditor
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
    onEditTagClick: ((Song) -> Unit)? = null,
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
                        },
                        onEditTagClick = onEditTagClick?.let { { it(song) } }
                    )
                }
                item { Spacer(modifier = Modifier.height(90.dp)) }
            }

            if (showAlphabetScroller && songs.size >= 12) {
                FastAlphabetScroller(
                    songs = songs,
                    onIndexSelected = { targetIndex ->
                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
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
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No folders found",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onFolderClick(folder) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            item { Spacer(modifier = Modifier.height(90.dp)) }
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
                    text = "No playlists created yet. Tap '+' to create one.",
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onPlaylistClick(playlist) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                item { Spacer(modifier = Modifier.height(90.dp)) }
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
        title = { Text(stringResource(R.string.create_playlist), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name), color = TextSecondary) },
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
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkSurface)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary)
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = DarkSurfaceContainer
    )
}
