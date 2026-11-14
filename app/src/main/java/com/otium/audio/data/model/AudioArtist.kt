package com.otium.audio.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AudioArtist(
    val name: String,
    val songCount: Int,
    val songs: List<Song>
) {
    companion object {
        fun extractArtistsFromSongs(songs: List<Song>): List<AudioArtist> {
            val map = LinkedHashMap<String, MutableList<Song>>()
            for (song in songs) {
                val artist = if (song.artist.isBlank() || song.artist.equals("<unknown>", true) || song.artist.equals("unknown", true)) "فنان غير معروف" else song.artist
                map.getOrPut(artist) { ArrayList() }.add(song)
            }
            return map.map { (artist, list) ->
                AudioArtist(
                    name = artist,
                    songCount = list.size,
                    songs = list
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }
}
