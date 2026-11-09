package com.otium.audio.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class TrackColors(
    val dominant: Color = DarkSurface,
    val vibrant: Color = AccentGold,
    val darkVibrant: Color = DarkSurfaceVariant
)

object PaletteManager {
    private val colorCache = android.util.LruCache<String, TrackColors>(128)

    private fun getImageLoader(context: Context): ImageLoader = context.imageLoader

    fun getCached(key: String): TrackColors? = synchronized(colorCache) { colorCache.get(key) }

    suspend fun extractColors(context: Context, albumArtUri: Uri): TrackColors = withContext(Dispatchers.IO) {
        val key = albumArtUri.toString()
        synchronized(colorCache) { colorCache.get(key) }?.let { return@withContext it }

        try {
            val loader = getImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(albumArtUri)
                .size(64, 64) // Fast 64x64 decode, 75% less RAM and 4x faster CPU processing for Palette
                .allowHardware(false) // Required for Palette Bitmap access
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getDominantColor(0xFF000000.toInt())
                    val rawVibrant = palette.getVibrantColor(0xFFFFFFFF.toInt())
                    val safeVibrant = Color(rawVibrant).asReadableAccent().toArgb()
                    val darkVibrant = palette.getDarkVibrantColor(0xFF14141A.toInt())

                    val trackColors = TrackColors(
                        dominant = Color(dominant),
                        vibrant = Color(safeVibrant),
                        darkVibrant = Color(darkVibrant)
                    )
                    synchronized(colorCache) { colorCache.put(key, trackColors) }
                    return@withContext trackColors
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to default aesthetic colors
        }

        val defaultColors = TrackColors()
        synchronized(colorCache) { colorCache.put(key, defaultColors) }
        defaultColors
    }
}

@Composable
fun rememberTrackColors(context: Context, albumArtUri: Uri): TrackColors {
    val key = albumArtUri.toString()
    val initial = PaletteManager.getCached(key) ?: TrackColors()
    var colors by remember(albumArtUri) { mutableStateOf(initial) }

    LaunchedEffect(albumArtUri) {
        if (PaletteManager.getCached(key) == null) {
            colors = PaletteManager.extractColors(context, albumArtUri)
        }
    }

    return colors
}
