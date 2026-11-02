package com.otium.audio

import android.app.Application
import com.otium.audio.data.local.OtiumDatabase
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.data.repository.AudioRepositoryImpl
import com.otium.audio.playback.EqualizerController
import com.otium.audio.playback.MusicController
import com.otium.audio.playback.SleepTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        EqualizerController()
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

    override fun onTerminate() {
        super.onTerminate()
        sleepTimerController.cancelTimer()
        musicController.release()
        equalizerController.release()
    }
}
