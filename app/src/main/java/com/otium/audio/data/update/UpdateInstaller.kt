package com.otium.audio.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a published APK and hands it to the package installer, so updating never has to
 * go through the browser.
 *
 * The download is accepted only if it is a valid archive that carries the same signing
 * certificate as the installed app — anything else is deleted rather than offered.
 */
object UpdateInstaller {

    private const val AUTHORITY_SUFFIX = ".updates"

    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Blocking download, call from a background dispatcher. [onProgress] receives 0..100 and
     * may be invoked from that background thread.
     */
    suspend fun download(context: Context, url: String, onProgress: (Int) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val target = File(directory, "otium-update.apk")

            val downloaded = runCatching { fetch(url, target, onProgress) }.getOrNull()
            if (downloaded == null || target.length() == 0L || !signedLikeUs(context, target)) {
                target.delete()
                return@withContext null
            }
            target
        }

    private fun fetch(url: String, target: File, onProgress: (Int) -> Unit): File? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "Otium-Audio-App")
            setRequestProperty("Accept", "application/vnd.android.package-archive")
        }

        try {
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val total = connection.contentLengthLong
            var read = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            val percent = ((read * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            return target
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun signedLikeUs(context: Context, apk: File): Boolean = runCatching {
        val manager = context.packageManager
        val flags = PackageManager.GET_SIGNATURES
        val archive = manager.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        if (archive.packageName != context.packageName) return false

        val candidate = archive.signatures?.map { it.toByteArray().contentHashCode() }?.toSet()
        val installed = manager.getPackageInfo(context.packageName, flags)
            .signatures?.map { it.toByteArray().contentHashCode() }?.toSet()

        candidate != null && installed != null && candidate == installed
    }.getOrDefault(false)

    fun install(context: Context, apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }.isSuccess
}
