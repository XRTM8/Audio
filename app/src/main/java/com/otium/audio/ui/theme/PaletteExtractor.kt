package com.otium.audio.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class TrackColors(
    val dominant: Color = DarkSurface,
    val vibrant: Color = AccentGold,
    val darkVibrant: Color = DarkSurfaceVariant
)

object PaletteManager {
    private val colorCache = ConcurrentHashMap<String, TrackColors>()
    @Volatile private var cachedLoader: ImageLoader? = null

    private fun getImageLoader(context: Context): ImageLoader {
        return cachedLoader ?: synchronized(this) {
            cachedLoader ?: ImageLoader.Builder(context.applicationContext).build().also { cachedLoader = it }
        }
    }

    fun getCached(key: String): TrackColors? = colorCache[key]

    suspend fun extractColors(context: Context, albumArtUri: Uri): TrackColors = withContext(Dispatchers.IO) {
        val key = albumArtUri.toString()
        colorCache[key]?.let { return@withContext it }

        try {
            val loader = getImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(albumArtUri)
                .size(128, 128) // Crucial OOM prevention on 4K/8K embedded covers
                .allowHardware(false) // Required for Palette Bitmap access
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominant = palette.getDominantColor(0xFF0F0F12.toInt())
                    val vibrant = palette.getVibrantColor(0xFFE5A93C.toInt())
                    val darkVibrant = palette.getDarkVibrantColor(0xFF18181E.toInt())

                    val trackColors = TrackColors(
                        dominant = Color(dominant),
                        vibrant = Color(vibrant),
                        darkVibrant = Color(darkVibrant)
                    )
                    colorCache[key] = trackColors
                    return@withContext trackColors
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to default aesthetic colors
        }

        val defaultColors = TrackColors()
        colorCache[key] = defaultColors
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
