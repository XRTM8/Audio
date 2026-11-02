package com.otium.audio.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelDb: Float, // in dB (-15dB to +15dB)
    val minDb: Float = -15f,
    val maxDb: Float = 15f
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0 to 1000
    val virtualizerStrength: Int = 0, // 0 to 1000
    val currentPreset: String = "Custom",
    val presets: List<String> = listOf("Flat", "Bass Boost", "Rock", "Pop", "Jazz", "Electronic", "Acoustic", "Custom")
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        release()

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            equalizer = eq

            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb

            val virt = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt

            val minLevel = eq.bandLevelRange[0] / 100f
            val maxLevel = eq.bandLevelRange[1] / 100f
            val bandCount = eq.numberOfBands

            val bandList = mutableListOf<EqualizerBand>()
            for (i in 0 until bandCount) {
                val band = i.toShort()
                val freq = eq.getCenterFreq(band) / 1000 // Convert mHz to Hz
                val currentLevel = eq.getBandLevel(band) / 100f
                bandList.add(
                    EqualizerBand(
                        bandIndex = band,
                        centerFreqHz = freq,
                        levelDb = currentLevel,
                        minDb = minLevel,
                        maxDb = maxLevel
                    )
                )
            }

            _state.update {
                it.copy(bands = bandList)
            }
        } catch (_: Exception) {
            // AudioFx may not be available on all emulators/devices
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _state.update { it.copy(isEnabled = enabled) }
        } catch (_: Exception) {}
    }

    fun setBandLevel(bandIndex: Short, levelDb: Float) {
        try {
            equalizer?.setBandLevel(bandIndex, (levelDb * 100).toInt().toShort())
            _state.update { currentState ->
                val updatedBands = currentState.bands.map { band ->
                    if (band.bandIndex == bandIndex) band.copy(levelDb = levelDb) else band
                }
                currentState.copy(bands = updatedBands, currentPreset = "Custom")
            }
        } catch (_: Exception) {}
    }

    fun setBassBoost(strength: Int) {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
            _state.update { it.copy(bassBoostStrength = strength) }
        } catch (_: Exception) {}
    }

    fun setVirtualizer(strength: Int) {
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
            _state.update { it.copy(virtualizerStrength = strength) }
        } catch (_: Exception) {}
    }

    fun applyPreset(presetName: String) {
        val bands = _state.value.bands
        if (bands.isEmpty()) return

        val levels = when (presetName) {
            "Flat" -> List(bands.size) { 0f }
            "Bass Boost" -> listOf(6f, 4f, 1f, 0f, 0f)
            "Rock" -> listOf(5f, 3f, -1f, 2f, 5f)
            "Pop" -> listOf(-1f, 2f, 5f, 3f, -1f)
            "Jazz" -> listOf(4f, 2f, -1f, 2f, 4f)
            "Electronic" -> listOf(5f, 4f, 0f, 3f, 5f)
            "Acoustic" -> listOf(4f, 2f, 1f, 3f, 2f)
            else -> return
        }

        bands.forEachIndexed { index, band ->
            val level = levels.getOrElse(index) { 0f }
            setBandLevel(band.bandIndex, level)
        }

        _state.update { it.copy(currentPreset = presetName) }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
