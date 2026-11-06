package com.otium.audio

import android.app.Application
import com.otium.audio.data.local.OtiumDatabase
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.data.repository.AudioRepositoryImpl
import com.otium.audio.playback.EqProfileStore
import com.otium.audio.playback.EqualizerController
import com.otium.audio.playback.MusicController
import com.otium.audio.playback.SleepTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

class OtiumApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
        // Deliberately here and not in init: the controller opens preferences and the
        // repository registers a content observer, and the base context does not exist
        // until attachBaseContext has run. Touching either in the constructor crashes.
        musicController.onTrackEnded = { sleepTimerController.onTrackEnded() }
        applicationScope.launch { repository.pruneHistory(HISTORY_RETENTION_DAYS) }
    }

    private companion object {
        const val HISTORY_RETENTION_DAYS = 90
    }
}