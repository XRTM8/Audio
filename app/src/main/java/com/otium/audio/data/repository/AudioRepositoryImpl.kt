package com.otium.audio.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.provider.MediaStore
import android.content.ContentUris
import com.otium.audio.data.local.dao.AudioDao
import com.otium.audio.data.local.entity.FavoriteEntity
import com.otium.audio.data.local.entity.HistoryEntity
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.local.entity.PlaylistSongCrossRef
import com.otium.audio.data.model.AudioFolder
import com.otium.audio.data.model.Song
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(FlowPreview::class)
class AudioRepositoryImpl(
    private val context: Context,
    private val audioDao: AudioDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : AudioRepository {

    private val resolver = context.contentResolver
    private val collection: Uri = audioCollection()

    // One background thread owns every MediaStore read, so a scan can never compete with
    // the decoder for CPU or disk. Everything below shares a single observer and a single
    // query loop; the previous version re-subscribed (and re-scanned) once per derived flow.
    private val observerThread =
        HandlerThread("otium-observer", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }

    private val scanExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "otium-scan").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + scanExecutor.asCoroutineDispatcher())

    private val songs = MutableStateFlow<List<Song>>(emptyList())
    private val favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val stale = MutableStateFlow(false)

    private val observer = object : ContentObserver(Handler(observerThread.looper)) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            stale.value = true
        }
    }

    init {
        resolver.registerContentObserver(collection, true, observer)

        scope.launch {
            songs.value = readLibrary(songs.value)
            stale.filter { it }
                .debounce(RESCAN_WINDOW_MS)
                .collect {
                    stale.value = false
                    songs.value = readLibrary(songs.value)
                }
        }

        scope.launch {
            audioDao.getAllFavoriteIdsFlow().collect { favoriteIds.value = it.toSet() }
        }
    }

    override fun getAllSongsFlow(): Flow<List<Song>> =
        combine(songs, favoriteIds) { list, favorites ->
            if (favorites.isEmpty()) list else list.map { it.withFavorite(favorites) }
        }

    override fun getFavoritesFlow(): Flow<List<Song>> =
        combine(songs, favoriteIds) { list, favorites ->
            if (favorites.isEmpty()) emptyList<Song>()
            else list.filter { favorites.contains(it.id) }.map { it.copy(isFavorite = true) }
        }

    override fun getFoldersFlow(): Flow<List<AudioFolder>> =
        songs.map { AudioFolder.extractFoldersFromSongs(it) }

    override fun getRecentlyAddedFlow(): Flow<List<Song>> =
        combine(songs, favoriteIds) { list, favorites ->
            list.asSequence()
                .sortedByDescending { it.dateAdded }
                .take(RECENT_LIMIT)
                .map { it.withFavorite(favorites) }
                .toList()
        }

    override fun searchSongs(query: String): Flow<List<Song>> =
        getAllSongsFlow().map { list ->
            if (query.isBlank()) list
            else list.filter {
                it.title.contains(query, true) ||
                    it.artist.contains(query, true) ||
                    it.album.contains(query, true)
            }
        }

    override suspend fun toggleFavorite(songId: Long) = withContext(io) {
        if (audioDao.isFavorite(songId)) audioDao.removeFavorite(songId)
        else audioDao.insertFavorite(FavoriteEntity(songId))
    }

    override suspend fun isFavorite(songId: Long): Boolean =
        withContext(io) { audioDao.isFavorite(songId) }

    override fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> = audioDao.getAllPlaylistsFlow()

    override suspend fun createPlaylist(name: String): Long =
        withContext(io) { audioDao.insertPlaylist(PlaylistEntity(name = name)) }

    override suspend fun deletePlaylist(playlistId: Long) = withContext(io) {
        audioDao.deletePlaylist(playlistId)
        audioDao.clearPlaylistSongs(playlistId)
    }

    override fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>> =
        combine(getAllSongsFlow(), audioDao.getSongIdsForPlaylistFlow(playlistId)) { list, ids ->
            val byId = list.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(io) {
        // orderIndex used to stay 0, so "ORDER BY orderIndex" was a no-op and playlists
        // came back in arbitrary order. Append explicitly instead.
        val next = audioDao.getNextOrderIndex(playlistId)
        audioDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, next))
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        withContext(io) { audioDao.removeSongFromPlaylist(playlistId, songId) }

    override suspend fun recordHistory(songId: Long) = withContext(io) {
        audioDao.recordHistory(HistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
    }

    override fun getRecentlyPlayedFlow(): Flow<List<Song>> =
        combine(getAllSongsFlow(), audioDao.getRecentHistorySongIdsFlow()) { list, ids ->
            val byId = list.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    override fun getMostPlayedFlow(limit: Int): Flow<List<Song>> =
        combine(getAllSongsFlow(), audioDao.getMostPlayedSongIdsFlow(limit)) { list, ids ->
            val byId = list.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    override suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String
    ): Boolean = withContext(io) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.ALBUM, album)
        }
        runCatching {
            resolver.update(ContentUris.withAppendedId(collection, songId), values, null, null) > 0
        }.getOrDefault(false)
    }

    // Keeps identity for unchanged rows. Compose can then skip those items entirely
    // instead of re-diffing a fully reallocated list on every library change.
    private fun readLibrary(previous: List<Song>): List<Song> {
        val byId = previous.associateBy { it.id }
        val result = ArrayList<Song>(byId.size + 32)

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
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(MIN_DURATION_MS.toString())

        runCatching {
            resolver.query(collection, projection, selection, args, SORT_ORDER)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: UNKNOWN_TITLE
                    val artist = c.getString(artistCol)
                        ?.takeUnless { it == UNKNOWN_ARTIST_TAG } ?: UNKNOWN_ARTIST
                    val album = c.getString(albumCol) ?: UNKNOWN_ALBUM
                    val duration = c.getLong(durationCol)
                    val added = c.getLong(addedCol)

                    val old = byId[id]
                    val same = old != null &&
                        old.title == title &&
                        old.artist == artist &&
                        old.album == album &&
                        old.durationMs == duration &&
                        old.dateAdded == added

                    result += (old?.takeIf { same } ?: Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        contentUri = ContentUris.withAppendedId(collection, id),
                        albumArtUri = ContentUris.withAppendedId(
                            ALBUM_ART_URI, c.getLong(albumIdCol)
                        ),
                        size = c.getLong(sizeCol),
                        relativePath = c.getString(pathCol) ?: "",
                        dateAdded = added,
                        isFavorite = old?.isFavorite ?: false
                    ))
                }
            }
        }

        return result
    }

    private fun Song.withFavorite(favorites: Set<Long>): Song {
        val favorite = favorites.contains(id)
        return if (isFavorite == favorite) this else copy(isFavorite = favorite)
    }

    private fun audioCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private companion object {
        const val RESCAN_WINDOW_MS = 1_200L
        const val MIN_DURATION_MS = 30_000L
        const val RECENT_LIMIT = 50
        const val UNKNOWN_TITLE = "Unknown Title"
        const val UNKNOWN_ARTIST = "Unknown Artist"
        const val UNKNOWN_ALBUM = "Unknown Album"
        const val UNKNOWN_ARTIST_TAG = "<unknown>"
        val SORT_ORDER = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
