package com.otium.audio.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.otium.audio.data.local.dao.AudioDao
import com.otium.audio.data.local.entity.FavoriteEntity
import com.otium.audio.data.local.entity.HistoryEntity
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.local.entity.PlaylistSongCrossRef
import com.otium.audio.data.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AudioRepositoryImpl(
    private val context: Context,
    private val audioDao: AudioDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AudioRepository {

    companion object {
        private const val MIN_TRACK_DURATION_MS = 30_000L // 30 seconds
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }

    override fun getAllSongsFlow(): Flow<List<Song>> {
        val rawSongsFlow = callbackFlow {
            // Initial scan
            trySend(querySongsFromMediaStore())

            var debounceJob: kotlinx.coroutines.Job? = null
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    debounceJob?.cancel()
                    debounceJob = launch(ioDispatcher) {
                        kotlinx.coroutines.delay(500L) // Debounce batch file additions / album downloads
                        trySend(querySongsFromMediaStore())
                    }
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.registerContentObserver(collection, true, observer)

            awaitClose {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }

        return combine(rawSongsFlow, audioDao.getAllFavoriteIdsFlow()) { songs, favoriteIds ->
            val favSet = favoriteIds.toSet()
            songs.map { song ->
                song.copy(isFavorite = favSet.contains(song.id))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getFavoritesFlow(): Flow<List<Song>> {
        return combine(getAllSongsFlow(), audioDao.getAllFavoriteIdsFlow()) { songs, favoriteIds ->
            val favSet = favoriteIds.toSet()
            songs.filter { favSet.contains(it.id) }
        }.flowOn(ioDispatcher)
    }

    override suspend fun toggleFavorite(songId: Long) = withContext(ioDispatcher) {
        if (audioDao.isFavorite(songId)) {
            audioDao.removeFavorite(songId)
        } else {
            audioDao.insertFavorite(FavoriteEntity(songId = songId))
        }
    }

    override suspend fun isFavorite(songId: Long): Boolean = withContext(ioDispatcher) {
        audioDao.isFavorite(songId)
    }

    override fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> {
        return audioDao.getAllPlaylistsFlow().flowOn(ioDispatcher)
    }

    override suspend fun createPlaylist(name: String): Long = withContext(ioDispatcher) {
        audioDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) = withContext(ioDispatcher) {
        audioDao.deletePlaylist(playlistId)
        audioDao.clearPlaylistSongs(playlistId)
    }

    override fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>> {
        return combine(getAllSongsFlow(), audioDao.getSongIdsForPlaylistFlow(playlistId)) { songs, songIds ->
            val songMap = songs.associateBy { it.id }
            songIds.mapNotNull { songMap[it] }
        }.flowOn(ioDispatcher)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(ioDispatcher) {
        audioDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(ioDispatcher) {
        audioDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun recordHistory(songId: Long) = withContext(ioDispatcher) {
        audioDao.recordHistory(HistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
    }

    override fun getRecentlyPlayedFlow(): Flow<List<Song>> {
        return combine(getAllSongsFlow(), audioDao.getRecentHistorySongIdsFlow()) { songs, historyIds ->
            val songMap = songs.associateBy { it.id }
            historyIds.mapNotNull { songMap[it] }
        }.flowOn(ioDispatcher)
    }

    private val cachedSongs = java.util.concurrent.CopyOnWriteArrayList<Song>()

    override fun searchSongs(query: String): Flow<List<Song>> {
        return flow {
            val list = if (cachedSongs.isNotEmpty()) cachedSongs else querySongsFromMediaStore()
            val filtered = if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
            }
            emit(filtered)
        }.flowOn(ioDispatcher)
    }

    private fun querySongsFromMediaStore(): List<Song> {
        val songs = mutableListOf<Song>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_TRACK_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Title"
                val artist = cursor.getString(artistCol).takeIf { it != "<unknown>" } ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdCol)
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val path = cursor.getString(pathCol) ?: ""
                val dateAdded = cursor.getLong(dateAddedCol)

                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    size = size,
                    relativePath = path,
                    dateAdded = dateAdded
                )
                songs.add(song)
            }
        }
        cachedSongs.clear()
        cachedSongs.addAll(songs)
        return songs
    }
}
