package com.otium.audio.data.repository

import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun getAllSongsFlow(): Flow<List<Song>>
    fun getFavoritesFlow(): Flow<List<Song>>
    fun getFoldersFlow(): Flow<List<AudioFolder>>
    suspend fun toggleFavorite(songId: Long)
    suspend fun isFavorite(songId: Long): Boolean

    fun getPlaylistsFlow(): Flow<List<PlaylistEntity>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>>
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    suspend fun recordHistory(songId: Long)
    suspend fun pruneHistory(maxAgeDays: Int)
    fun getRecentlyPlayedFlow(): Flow<List<Song>>
    fun getMostPlayedFlow(limit: Int = 50): Flow<List<Song>>
    fun getRecentlyAddedFlow(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun updateSongMetadata(songId: Long, title: String, artist: String, album: String): Boolean

    /** Force an immediate MediaStore re-scan without waiting for the observer debounce. */
    fun refresh()
}
