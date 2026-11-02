package com.otium.audio

import android.app.Application
import com.otium.audio.data.local.OtiumDatabase
import com.otium.audio.data.repository.AudioRepository
import com.otium.audio.data.repository.AudioRepositoryImpl
import com.otium.audio.playback.MusicController
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

    override fun onTerminate() {
        super.onTerminate()
        musicController.release()
    }
}
