package com.otium.audio.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.util.Locale
import java.util.concurrent.TimeUnit

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val size: Long,
    val relativePath: String,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val formattedDuration: String = formatDurationFast(durationMs),
    val audioFormatBadge: String = determineAudioBadgeFast(relativePath, contentUri),
    val displayTitle: String = cleanDisplayTitleFast(title),
    val subtitle: String = "$artist  ·  $formattedDuration"
) {
    companion object {
        fun cleanDisplayTitleFast(raw: String): String {
            var s = raw.trim()
            if (s.startsWith("(") && s.endsWith(")") && s.length > 2) {
                s = s.substring(1, s.length - 1).trim()
            } else if (s.startsWith("[") && s.endsWith("]") && s.length > 2) {
                s = s.substring(1, s.length - 1).trim()
            }
            return s
        }

        fun formatDurationFast(durationMs: Long): String {
            val totalSec = (durationMs / 1000).coerceAtLeast(0)
            val min = totalSec / 60
            val sec = totalSec % 60
            val minStr = if (min < 10) "0$min" else min.toString()
            val secStr = if (sec < 10) "0$sec" else sec.toString()
            return "$minStr:$secStr"
        }

        fun determineAudioBadgeFast(path: String, uri: Uri? = null): String {
            val target = if (path.isNotEmpty()) path else (uri?.path ?: "")
            val dot = target.lastIndexOf('.')
            if (dot == -1 || dot >= target.length - 1) return "HI-RES"
            val ext = target.substring(dot + 1)
            return when {
                ext.equals("flac", ignoreCase = true) -> "FLAC"
                ext.equals("wav", ignoreCase = true) -> "WAV"
                ext.equals("opus", ignoreCase = true) -> "OPUS"
                ext.equals("ogg", ignoreCase = true) -> "OGG"
                ext.equals("m4a", ignoreCase = true) -> "ALAC"
                ext.equals("aac", ignoreCase = true) -> "AAC"
                ext.equals("mp3", ignoreCase = true) -> "MP3"
                else -> "HI-RES"
            }
        }
    }

    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .setIsPlayable(true)
            .build()

        val requestMetadata = MediaItem.RequestMetadata.Builder()
            .setMediaUri(contentUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setRequestMetadata(requestMetadata)
            .setMediaMetadata(metadata)
            .build()
    }
}
