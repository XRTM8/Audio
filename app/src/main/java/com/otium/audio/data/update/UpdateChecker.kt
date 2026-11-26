package com.otium.audio.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/XRTM8/Audio/releases/latest"

    suspend fun checkLatestRelease(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!com.otium.audio.BuildConfig.ENABLE_SELF_UPDATE) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Otium-Audio-App")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val json = JSONObject(response)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "")

                // Find APK asset
                // Prefer the APK built for this device, then the universal one.
                val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
                var downloadUrl = ""
                var universalUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (!name.endsWith(".apk")) continue
                        val url = asset.optString("browser_download_url", "")
                        when {
                            abi.isNotEmpty() && name.contains(abi) -> { downloadUrl = url; break }
                            name.contains("universal") -> universalUrl = url
                            downloadUrl.isEmpty() -> downloadUrl = url
                        }
                    }
                }
                if (downloadUrl.isEmpty()) downloadUrl = universalUrl

                if (isNewerVersion(tagName, currentVersion.removePrefix("v").trim())) {
                    return@withContext UpdateInfo(
                        versionName = "v$tagName",
                        releaseNotes = body.take(300),
                        downloadUrl = downloadUrl.ifEmpty { htmlUrl },
                        releasePageUrl = htmlUrl
                    )
                }
            } else {
                // Zero-rate-limit fallback via GitHub web redirect
                val redirectConn = (URL("https://github.com/XRTM8/Audio/releases/latest").openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "HEAD"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "Otium-Audio-App")
                }
                try {
                    val location = redirectConn.getHeaderField("Location").orEmpty()
                    val tagName = location.substringAfterLast("/tag/", "").trim()
                    if (tagName.isNotEmpty() && isNewerVersion(tagName, currentVersion)) {
                        val releaseUrl = "https://github.com/XRTM8/Audio/releases/tag/$tagName"
                        return@withContext UpdateInfo(
                            versionName = if (tagName.startsWith("v")) tagName else "v$tagName",
                            releaseNotes = "إصدار جديد متوفر على GitHub",
                            downloadUrl = releaseUrl,
                            releasePageUrl = releaseUrl
                        )
                    }
                } finally {
                    runCatching { redirectConn.disconnect() }
                }
            }
        } catch (_: Exception) {
            // Silently fail if offline or network unavailable
        } finally {
            runCatching { connection?.disconnect() }
        }
        null
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val cleanRemote = remote.removePrefix("v").trim().substringBefore('-')
        val cleanCurrent = current.removePrefix("v").trim().substringBefore('-')
        if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun openDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
