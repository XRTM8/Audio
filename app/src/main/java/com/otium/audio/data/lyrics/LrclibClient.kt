package com.otium.audio.data.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LrclibClient {

    private const val API_BASE = "https://lrclib.net/api/get"
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 5_000

    private val negativeCache = androidx.collection.LruCache<Long, Boolean>(128)

    suspend fun fetchLyrics(
        context: Context,
        songId: Long,
        title: String,
        artist: String,
        album: String,
        durationMs: Long
    ): TrackLyrics? = withContext(Dispatchers.IO) {
        if (songId <= 0L || title.isBlank()) return@withContext null
        if (negativeCache.get(songId) == true) return@withContext null

        val cacheDir = File(context.cacheDir, "lyrics").apply { if (!exists()) mkdirs() }
        val cacheFile = File(cacheDir, "$songId.lrc")

        // 1. Check local cached .lrc
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val cachedText = cacheFile.readText()
                val parsed = LrcParser.parse(cachedText)
                if (parsed.lines.isNotEmpty()) return@withContext parsed
            } catch (_: Exception) {}
        }

        // 2. Fetch from LRCLIB
        try {
            val cleanTitle = cleanTrackName(title)
            val cleanArtist = cleanArtistName(artist)
            val durationSec = (durationMs / 1000L).coerceAtLeast(1L)

            val urlBuilder = StringBuilder(API_BASE)
                .append("?track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
                urlBuilder.append("&artist_name=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
            }
            if (durationSec > 10) {
                urlBuilder.append("&duration=").append(durationSec)
            }

            val connection = (URL(urlBuilder.toString()).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "OtiumAudio/5.2 (Android; Audiophile Player)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val synced = json.optString("syncedLyrics", "").trim()
                val plain = json.optString("plainLyrics", "").trim()

                if (synced.isNotEmpty()) {
                    cacheFile.writeText(synced)
                    val parsed = LrcParser.parse(synced)
                    if (parsed.lines.isNotEmpty()) return@withContext parsed
                } else if (plain.isNotEmpty()) {
                    cacheFile.writeText(plain)
                    val lines = plain.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }
                    if (lines.isNotEmpty()) return@withContext TrackLyrics(lines = lines, isSynced = false)
                }
            } else {
                connection.disconnect()
            }
        } catch (_: Exception) {
            // Graceful handling of offline/network issues
        }

        negativeCache.put(songId, true)
        null
    }

    private fun cleanTrackName(raw: String): String {
        return raw
            .replace(Regex("(?i)\\[(official|video|audio|lyrics?|hd|4k|hq|remastered?)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\((official|video|audio|lyrics?|hd|4k|hq|remastered?)[^)]*\\)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanArtistName(raw: String): String {
        return raw
            .replace(Regex("(?i)\\s+(feat\\.?|ft\\.?|featuring).*"), "")
            .trim()
    }
}
