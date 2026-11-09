package com.otium.audio.data.lyrics

import com.otium.audio.audio.dsp.NativeAudioDsp
import java.io.File
import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val timestamps: LongArray = LongArray(0)
) {
    fun findActiveIndex(currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (NativeAudioDsp.isAvailable && timestamps.isNotEmpty()) {
            val idx = NativeAudioDsp.findActiveLrcLine(timestamps, currentPositionMs)
            if (idx >= 0 && idx < lines.size) return idx
        }
        for (i in lines.indices.reversed()) {
            if (currentPositionMs >= lines[i].timeMs) {
                return i
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TrackLyrics
        return lines == other.lines && isSynced == other.isSynced
    }

    override fun hashCode(): Int {
        var result = lines.hashCode()
        result = 31 * result + isSynced.hashCode()
        return result
    }
}

object LrcParser {
    private val LRC_PATTERN = Pattern.compile("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    fun parse(lrcContent: String): TrackLyrics {
        if (NativeAudioDsp.isAvailable) {
            try {
                val nativeResult = NativeAudioDsp.parseLrc(lrcContent)
                if (!nativeResult.isNullOrBlank()) {
                    val parsedLines = ArrayList<LyricLine>()
                    val rawEntries = nativeResult.split('\n')
                    for (entry in rawEntries) {
                        val tabIdx = entry.indexOf('\t')
                        if (tabIdx > 0) {
                            val ts = entry.substring(0, tabIdx).toLongOrNull() ?: continue
                            val txt = entry.substring(tabIdx + 1)
                            if (txt.isNotBlank()) {
                                parsedLines.add(LyricLine(timeMs = ts, text = txt))
                            }
                        }
                    }
                    if (parsedLines.isNotEmpty()) {
                        val tsArray = LongArray(parsedLines.size) { parsedLines[it].timeMs }
                        return TrackLyrics(
                            lines = parsedLines,
                            isSynced = true,
                            timestamps = tsArray
                        )
                    }
                }
            } catch (_: Throwable) {
                // Graceful fallback to pure Kotlin parser
            }
        }

        val lines = mutableListOf<LyricLine>()
        val rawLines = lrcContent.lines()

        for (rawLine in rawLines) {
            val matcher = LRC_PATTERN.matcher(rawLine.trim())
            if (matcher.matches()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val msPart = matcher.group(3) ?: "00"
                val millis = if (msPart.length == 2) {
                    (msPart.toLongOrNull() ?: 0L) * 10
                } else {
                    msPart.toLongOrNull() ?: 0L
                }
                val timeMs = (minutes * 60 + seconds) * 1000 + millis
                val text = matcher.group(4)?.trim() ?: ""

                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs = timeMs, text = text))
                }
            }
        }

        val sortedLines = lines.sortedBy { it.timeMs }
        val tsArray = LongArray(sortedLines.size) { sortedLines[it].timeMs }
        return TrackLyrics(
            lines = sortedLines,
            isSynced = sortedLines.isNotEmpty(),
            timestamps = tsArray
        )
    }

    private val lyricsCache = androidx.collection.LruCache<String, TrackLyrics>(64)

    fun findLrcForAudio(audioPath: String): TrackLyrics? {
        try {
            if (audioPath.isBlank()) return null
            val cached = synchronized(lyricsCache) { lyricsCache.get(audioPath) }
            if (cached != null) return if (cached.lines.isEmpty()) null else cached

            val dot = audioPath.lastIndexOf('.')
            if (dot <= 0) {
                synchronized(lyricsCache) { lyricsCache.put(audioPath, TrackLyrics()) }
                return null
            }
            val lrcPath = audioPath.substring(0, dot) + ".lrc"
            val lrcFile = File(lrcPath)
            if (lrcFile.exists() && lrcFile.canRead()) {
                val parsed = parse(lrcFile.readText())
                synchronized(lyricsCache) { lyricsCache.put(audioPath, parsed) }
                return parsed
            }

            // Fallback: extract embedded lyrics from file tags via Rust
            val embedded = NativeAudioDsp.extractLyricsSafe(audioPath)
            if (!embedded.isNullOrBlank()) {
                    val parsed = parse(embedded)
                    val result = if (parsed.lines.isNotEmpty()) {
                        parsed
                    } else {
                        val plainLines = embedded.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }
                        if (plainLines.isNotEmpty()) TrackLyrics(lines = plainLines, isSynced = false) else null
                    }
                    if (result != null) {
                        synchronized(lyricsCache) { lyricsCache.put(audioPath, result) }
                        return result
                    }
                }
            }

            // Negative cache to prevent repeated disk I/O for songs without lyrics
            synchronized(lyricsCache) { lyricsCache.put(audioPath, TrackLyrics()) }
        } catch (_: Exception) {
            // Ignore I/O issues gracefully
        }
        return null
    }
}
