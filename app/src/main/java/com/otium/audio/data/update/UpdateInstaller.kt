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

    private fun fetch(initialUrl: String, target: File, onProgress: (Int) -> Unit): File? {
        var currentUrl = initialUrl
        var redirects = 0
        val maxRedirects = 6
        var connection: HttpURLConnection? = null

        try {
            while (redirects < maxRedirects) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "Otium-Audio-App")
                    setRequestProperty("Accept", "application/vnd.android.package-archive")
                }
                connection.connect()

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrEmpty()) return null
                    currentUrl = location
                    redirects++
                    continue
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return null
                }

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
            }
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun signedLikeUs(context: Context, apk: File): Boolean = runCatching {
        if (!apk.exists() || apk.length() < 1024) return false
        val pm = context.packageManager

        // 1. Get signatures of currently installed app
        val currentSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = packageInfo.signingInfo ?: return false
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.map { it.toByteArray() }
            } else {
                signingInfo.signingCertificateHistory.map { it.toByteArray() }
            }
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.toByteArray() } ?: return false
        }

        if (currentSignatures.isEmpty()) return false

        // 2. Get signatures of the downloaded APK archive
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = pm.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        if (archiveInfo.packageName != context.packageName) return false

        val archiveSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = archiveInfo.signingInfo ?: return false
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.map { it.toByteArray() }
            } else {
                signingInfo.signingCertificateHistory.map { it.toByteArray() }
            }
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.signatures?.map { it.toByteArray() } ?: return false
        }

        if (archiveSignatures.isEmpty()) return false

        // 3. Verify that at least one certificate matches
        currentSignatures.any { currentSig ->
            archiveSignatures.any { archiveSig ->
                currentSig.contentEquals(archiveSig)
            }
        }
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
