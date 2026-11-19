package com.otium.audio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.otium.audio.data.local.entity.FavoriteEntity
import com.otium.audio.data.local.entity.HistoryEntity
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    // --- Favorites ---------------------------------------------------------

    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoriteIdsFlow(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    // --- Playlists ---------------------------------------------------------

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongIdsForPlaylistFlow(playlistId: Long): Flow<List<Long>>

    // orderIndex was never written, so every row kept the default 0 and the ORDER BY
    // above sorted nothing. New rows now get the next free index.
    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getNextOrderIndex(playlistId: Long): Int

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    // --- Backup & Restore --------------------------------------------------

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAllPlaylistSongs(): List<PlaylistSongCrossRef>

    @Query("SELECT songId FROM favorites")
    suspend fun getAllFavoriteIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongs(songs: List<PlaylistSongCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)

    // --- Playback history --------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordHistory(history: HistoryEntity)

    // GROUP BY keeps the newest timestamp per song, so a song played five times does not
    // occupy five of the fifty "recently played" slots.
    @Query(
        """
        SELECT songId FROM playback_history
        GROUP BY songId
        ORDER BY MAX(playedAt) DESC
        LIMIT :limit
        """
    )
    fun getRecentHistorySongIdsFlow(limit: Int = 50): Flow<List<Long>>

    @Query(
        """
        SELECT songId FROM playback_history
        GROUP BY songId
        ORDER BY COUNT(historyId) DESC
        LIMIT :limit
        """
    )
    fun getMostPlayedSongIdsFlow(limit: Int = 50): Flow<List<Long>>

    // --- Smart Dynamic Playlists ---

    @Query(
        """
        SELECT songId FROM playback_history
        WHERE playedAt >= :sinceTimestamp
        GROUP BY songId
        ORDER BY COUNT(historyId) DESC, MAX(playedAt) DESC
        LIMIT :limit
        """
    )
    fun getWeeklyRotationSongIdsFlow(sinceTimestamp: Long, limit: Int = 30): Flow<List<Long>>

    @Query(
        """
        SELECT f.songId FROM favorites f
        LEFT JOIN (
            SELECT songId, MAX(playedAt) as lastPlayed
            FROM playback_history
            GROUP BY songId
        ) h ON f.songId = h.songId
        WHERE h.lastPlayed IS NULL OR h.lastPlayed < :cutoffTimestamp
        ORDER BY f.addedAt DESC
        LIMIT :limit
        """
    )
    fun getForgottenGemsSongIdsFlow(cutoffTimestamp: Long, limit: Int = 30): Flow<List<Long>>

    @Query("DELETE FROM playback_history WHERE playedAt < :cutoff")
    suspend fun pruneHistory(cutoff: Long)
}
