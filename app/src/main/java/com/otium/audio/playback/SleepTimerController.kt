package com.otium.audio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val isEndOfTrack: Boolean = false
) {
    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
}

class SleepTimerController(
    private val coroutineScope: CoroutineScope,
    private val onTimerFinished: suspend () -> Unit,
    private val onVolumeChange: (Float) -> Unit
) {
    private var timerJob: Job? = null
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return
        val totalSeconds = minutes * 60L
        _state.value = SleepTimerState(isActive = true, remainingSeconds = totalSeconds, isEndOfTrack = false)

        timerJob = coroutineScope.launch(Dispatchers.Main) {
            var currentSec = totalSeconds
            while (isActive && currentSec > 0) {
                delay(1000L)
                currentSec--

                // Smooth 30-second acoustic fade-out before stopping
                if (currentSec <= 30) {
                    val fadeRatio = (currentSec.toFloat() / 30f).coerceIn(0f, 1f)
                    onVolumeChange(fadeRatio)
                }

                _state.update { it.copy(remainingSeconds = currentSec) }
            }

            if (isActive) {
                onTimerFinished()
                onVolumeChange(1.0f) // Reset volume for next session
                _state.value = SleepTimerState()
            }
        }
    }

    fun startEndOfTrackTimer() {
        cancelTimer()
        _state.value = SleepTimerState(isActive = true, remainingSeconds = 0L, isEndOfTrack = true)
    }

    fun onTrackEnded() {
        if (_state.value.isEndOfTrack) {
            coroutineScope.launch(Dispatchers.Main) {
                onTimerFinished()
                cancelTimer()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        onVolumeChange(1.0f)
        _state.value = SleepTimerState()
    }
}
