package com.otium.audio.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class AudioAlbum(
    val name: String,
    val artist: String,
    val albumArtUri: Uri,
    val songCount: Int,
    val songs: List<Song>
) {
    companion object {
        fun extractAlbumsFromSongs(songs: List<Song>): List<AudioAlbum> {
            val map = LinkedHashMap<String, MutableList<Song>>()
            for (song in songs) {
                val album = if (song.album.isBlank() || song.album.equals("<unknown>", true) || song.album.equals("unknown", true)) "ألبوم غير معروف" else song.album
                map.getOrPut(album) { ArrayList() }.add(song)
            }
            return map.map { (album, list) ->
                val firstArt = list.firstOrNull { it.albumArtUri != Uri.EMPTY }?.albumArtUri ?: Uri.EMPTY
                val artist = list.firstOrNull { it.artist.isNotBlank() && !it.artist.equals("<unknown>", true) }?.artist ?: "فنان غير معروف"
                AudioAlbum(
                    name = album,
                    artist = artist,
                    albumArtUri = firstArt,
                    songCount = list.size,
                    songs = list
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }
}
