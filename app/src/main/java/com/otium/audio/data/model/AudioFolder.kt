package com.otium.audio.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AudioFolder(
    val name: String,
    val path: String,
    val songCount: Int,
    val songs: List<Song>
) {
    companion object {
        fun extractFoldersFromSongs(songs: List<Song>): List<AudioFolder> {
            val folderMap = LinkedHashMap<String, MutableList<Song>>()

            for (song in songs) {
                val rawPath = song.relativePath
                val lastSep = rawPath.lastIndexOfAny(SEPARATORS)
                val folderPath = if (lastSep > 0) rawPath.substring(0, lastSep) else "Root"
                folderMap.getOrPut(folderPath) { ArrayList() }.add(song)
            }

            return folderMap.map { (path, songList) ->
                val folderName = if (path == "Root") "Music" else {
                    val lastSep = path.lastIndexOfAny(SEPARATORS)
                    if (lastSep >= 0 && lastSep < path.length - 1) path.substring(lastSep + 1) else path
                }
                AudioFolder(
                    name = folderName,
                    path = path,
                    songCount = songList.size,
                    songs = songList
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

        private val SEPARATORS = charArrayOf('/', '\\')
    }
}
