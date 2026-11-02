package com.otium.audio.data.model

import java.io.File

data class AudioFolder(
    val name: String,
    val path: String,
    val songCount: Int,
    val songs: List<Song>
) {
    companion object {
        fun extractFoldersFromSongs(songs: List<Song>): List<AudioFolder> {
            val folderMap = mutableMapOf<String, MutableList<Song>>()

            for (song in songs) {
                val file = File(song.relativePath)
                val parentDir = file.parentFile
                val folderPath = parentDir?.absolutePath ?: "Root"
                folderMap.getOrPut(folderPath) { mutableListOf() }.add(song)
            }

            return folderMap.map { (path, songList) ->
                val folderName = if (path == "Root") "Music" else File(path).name
                AudioFolder(
                    name = folderName,
                    path = path,
                    songCount = songList.size,
                    songs = songList.sortedBy { it.title.lowercase() }
                )
            }.sortedBy { it.name.lowercase() }
        }
    }
}
