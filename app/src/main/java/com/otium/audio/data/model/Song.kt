package com.otium.audio.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

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
    val sampleRate: Int = 0,
    val bitrate: Int = 0
) {
    val formattedDuration: String = formatDurationFast(durationMs)
    val audioFormatBadge: String = determineAudioBadgeFast(relativePath, contentUri, sampleRate, bitrate)
    val displayTitle: String = cleanDisplayTitleFast(title)
    val indexLetter: String = extractIndexLetterFast(title)
    val subtitle: String = "$artist  ·  $formattedDuration"
    val normalizedSearchTarget: String by lazy(LazyThreadSafetyMode.NONE) {
        normalizeForSearchFast("$title $artist $album")
    }
    companion object {
        fun normalizeForSearchFast(input: String): String {
            if (input.isEmpty()) return ""
            val sb = StringBuilder(input.length)
            var lastWasSpace = false
            for (i in 0 until input.length) {
                val c = input[i]
                // 1. Skip Arabic Tashkeel / Harakat (\u064B..\u065F), Dagger Alif (\u0670), Maddah/Hamzas (\u0653..\u0655), Tatweel (\u0640), and Quranic pause marks
                if ((c in '\u064B'..'\u065F') || c == '\u0670' || c == '\u0640' || (c in '\u0653'..'\u0655') ||
                    (c in '\u0610'..'\u061A') || (c in '\u06D6'..'\u06ED')) {
                    continue
                }
                // 2. Normalize Alif variants (آ, أ, إ, ٱ -> ا), Ta Marbuta -> Ha, Alif Maqsura -> Ya, Hamza on Waw/Ya
                val normalized = when (c) {
                    '\u0622', '\u0623', '\u0625', '\u0671' -> 'ا'
                    'ة' -> 'ه'
                    'ى', 'ئ', '\u06CC' -> 'ي'
                    'ؤ' -> 'و'
                    '\u06A9' -> 'ك'
                    else -> c.lowercaseChar()
                }
                // 3. Normalize symbols & whitespace into single space
                if (normalized.isLetterOrDigit()) {
                    sb.append(normalized)
                    lastWasSpace = false
                } else {
                    if (!lastWasSpace && sb.isNotEmpty()) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                }
            }
            if (lastWasSpace && sb.isNotEmpty()) {
                sb.setLength(sb.length - 1)
            }
            return sb.toString()
        }

        fun extractIndexLetterFast(raw: String): String {
            val trimmed = raw.trim()
            var idx = 0
            while (idx < trimmed.length && (trimmed[idx] == '(' || trimmed[idx] == '[' || trimmed[idx] == '{' || trimmed[idx] == '"' || trimmed[idx] == '\'' || trimmed[idx] == ' ')) {
                idx++
            }
            if (idx >= trimmed.length) return "#"
            val ch = trimmed[idx]
            return when {
                ch in 'A'..'Z' || ch in 'a'..'z' -> ch.uppercaseChar().toString()
                ch in '\u0622'..'\u0625' || ch == '\u0627' -> "ا"
                ch in '\u0600'..'\u06FF' -> ch.toString()
                else -> "#"
            }
        }

        fun cleanDisplayTitleFast(raw: String): String {
            var s = raw.trim()
            if (s.startsWith("(") && s.endsWith(")") && s.length > 2) {
                s = s.substring(1, s.length - 1).trim()
            } else if (s.startsWith("[") && s.endsWith("]") && s.length > 2) {
                s = s.substring(1, s.length - 1).trim()
            } else if (s.startsWith("(") && s.contains(")")) {
                val closeIdx = s.indexOf(')')
                if (closeIdx in 2..50) {
                    val part1 = s.substring(1, closeIdx).trim()
                    val part2 = s.substring(closeIdx + 1).trim()
                    s = if (part2.isNotEmpty()) "$part1  ·  $part2" else part1
                }
            } else if (s.startsWith("[") && s.contains("]")) {
                val closeIdx = s.indexOf(']')
                if (closeIdx in 2..50) {
                    val part1 = s.substring(1, closeIdx).trim()
                    val part2 = s.substring(closeIdx + 1).trim()
                    s = if (part2.isNotEmpty()) "$part1  ·  $part2" else part1
                }
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

        fun determineAudioBadgeFast(
            path: String,
            uri: Uri? = null,
            sampleRate: Int = 0,
            bitrate: Int = 0
        ): String {
            if (sampleRate >= 88_200) return "HI-RES"
            val target = if (path.isNotEmpty()) path else (uri?.path ?: "")
            val dot = target.lastIndexOf('.')
            if (dot == -1 || dot >= target.length - 1) {
                return if (sampleRate > 48_000) "HI-RES" else "AUDIO"
            }
            val ext = target.substring(dot + 1).lowercase()
            return when (ext) {
                "dsf", "dff" -> "DSD"
                "flac" -> if (sampleRate > 48_000) "HI-RES" else "FLAC"
                "wav" -> if (sampleRate > 48_000) "HI-RES" else "WAV"
                "aiff", "aif" -> "AIFF"
                "alac" -> "ALAC"
                "opus" -> "OPUS"
                "ogg" -> "OGG"
                "m4a", "aac" -> {
                    if (target.contains("alac", ignoreCase = true) || target.contains("lossless", ignoreCase = true)) {
                        "ALAC"
                    } else {
                        "AAC"
                    }
                }
                "mp3" -> if (bitrate >= 300_000) "320K" else "MP3"
                "wma" -> "WMA"
                else -> if (sampleRate > 48_000) "HI-RES" else "AUDIO"
            }
        }
    }

    @Transient
    private var cachedMediaItem: MediaItem? = null

    fun toMediaItem(): MediaItem {
        val cached = cachedMediaItem
        if (cached != null) return cached

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

        val item = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setRequestMetadata(requestMetadata)
            .setMediaMetadata(metadata)
            .build()
        cachedMediaItem = item
        return item
    }
}
