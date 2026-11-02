package com.otium.audio.data.lyrics

import java.io.File
import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false
) {
    fun findActiveIndex(currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (currentPositionMs >= lines[i].timeMs) {
                return i
            }
        }
        return 0
    }
}

object LrcParser {
    private val LRC_PATTERN = Pattern.compile("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    fun parse(lrcContent: String): TrackLyrics {
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
        return TrackLyrics(
            lines = sortedLines,
            isSynced = sortedLines.isNotEmpty()
        )
    }

    fun findLrcForAudio(audioPath: String): TrackLyrics? {
        try {
            if (audioPath.isBlank()) return null
            val audioFile = File(audioPath)
            if (!audioFile.exists()) return null

            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                return parse(lrcFile.readText())
            }
        } catch (_: Exception) {
            // Ignore I/O issues gracefully
        }
        return null
    }
}
