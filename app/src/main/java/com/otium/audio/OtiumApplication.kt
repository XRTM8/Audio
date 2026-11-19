package com.otium.audio

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.otium.audio.data.local.OtiumDatabase
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.data.repository.AudioRepositoryImpl
import com.otium.audio.playback.EqualizerController
import com.otium.audio.playback.MusicController
import com.otium.audio.playback.SleepTimerController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

class OtiumApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .allowHardware(true)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }

    private val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("OtiumApplication", "Unhandled coroutine error in applicationScope", throwable)
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + appExceptionHandler)

    val database: OtiumDatabase by lazy {
        OtiumDatabase.getInstance(this)
    }

    val repository: AudioRepository by lazy {
        AudioRepositoryImpl(
            context = applicationContext,
            audioDao = database.audioDao()
        )
    }

    val musicController: MusicController by lazy {
        MusicController(
            context = applicationContext,
            coroutineScope = applicationScope
        )
    }

    val equalizerController: EqualizerController by lazy {
        EqualizerController(this)
    }

    val sleepTimerController: SleepTimerController by lazy {
        SleepTimerController(
            coroutineScope = applicationScope,
            onTimerFinished = {
                if (musicController.playbackState.value.isPlaying) {
                    musicController.togglePlayPause()
                }
            },
            onVolumeChange = { volume ->
                musicController.setVolume(volume)
            }
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to prevent unexpected thread crashes from terminating the app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("OtiumApplication", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        runCatching {
            musicController.onTrackEnded = { sleepTimerController.onTrackEnded() }
        }
        applicationScope.launch(Dispatchers.IO) {
            runCatching { repository.pruneHistory(HISTORY_RETENTION_DAYS) }
        }
    }

    private companion object {
        const val HISTORY_RETENTION_DAYS = 90
    }
}