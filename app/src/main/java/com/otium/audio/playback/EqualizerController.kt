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

private val DEFAULT_PRESET_BANDS = listOf(
    EqualizerBand(0, 60, 0f, -15f, 15f),
    EqualizerBand(1, 230, 0f, -15f, 15f),
    EqualizerBand(2, 910, 0f, -15f, 15f),
    EqualizerBand(3, 3600, 0f, -15f, 15f),
    EqualizerBand(4, 14000, 0f, -15f, 15f)
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = DEFAULT_PRESET_BANDS,
    val bassBoostStrength: Int = 0, // 0 to 1000
    val virtualizerStrength: Int = 0, // 0 to 1000
    val currentPreset: String = "Flat",
    val presets: List<String> = listOf(
        "Flat",
        "Harman Target (Audiophile)",
        "Sennheiser HD-Series",
        "Sony WH-1000XM Series",
        "AirPods Pro Reference",
        "Bass Boost Extreme",
        "Acoustic & Vocals",
        "Rock",
        "Electronic Club",
        "Pop",
        "Jazz",
        "Custom"
    )
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

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
            val bandCount = eq.numberOfBands.toInt()

            val currentBands = _state.value.bands
            val updatedBands = mutableListOf<EqualizerBand>()

            for (i in 0 until bandCount) {
                val band = i.toShort()
                val freq = eq.getCenterFreq(band) / 1000 // mHz to Hz
                val existingLevel = currentBands.getOrNull(i)?.levelDb ?: 0f

                try {
                    eq.setBandLevel(band, (existingLevel * 100).toInt().toShort())
                } catch (_: Exception) {}

                updatedBands.add(
                    EqualizerBand(
                        bandIndex = band,
                        centerFreqHz = freq,
                        levelDb = existingLevel,
                        minDb = minLevel,
                        maxDb = maxLevel
                    )
                )
            }

            _state.update {
                it.copy(bands = if (updatedBands.isNotEmpty()) updatedBands else it.bands)
            }
        } catch (_: Exception) {
            // AudioFx gracefully handles devices without native DSP
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
        _state.update { it.copy(isEnabled = enabled) }
    }

    fun setBandLevel(bandIndex: Short, levelDb: Float) {
        try {
            equalizer?.setBandLevel(bandIndex, (levelDb * 100).toInt().toShort())
        } catch (_: Exception) {}

        _state.update { currentState ->
            val updatedBands = currentState.bands.map { band ->
                if (band.bandIndex == bandIndex) band.copy(levelDb = levelDb) else band
            }
            currentState.copy(bands = updatedBands, currentPreset = "Custom")
        }
    }

    fun setBassBoost(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(safeStrength.toShort())
                }
            }
        } catch (_: Exception) {}
        _state.update { it.copy(bassBoostStrength = safeStrength) }
    }

    fun setVirtualizer(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(safeStrength.toShort())
                }
            }
        } catch (_: Exception) {}
        _state.update { it.copy(virtualizerStrength = safeStrength) }
    }

    fun applyPreset(presetName: String) {
        val bands = _state.value.bands

        val levels = when (presetName) {
            "Flat" -> listOf(0f, 0f, 0f, 0f, 0f)
            "Harman Target (Audiophile)" -> listOf(5.5f, 2.0f, -0.5f, 3.0f, 2.5f)
            "Sennheiser HD-Series" -> listOf(2.5f, 1.0f, 0.0f, 1.5f, 2.0f)
            "Sony WH-1000XM Series" -> listOf(-1.5f, -2.5f, 2.0f, 3.5f, 4.0f)
            "AirPods Pro Reference" -> listOf(2.5f, 0.5f, 0.0f, 1.0f, 1.5f)
            "Bass Boost Extreme" -> listOf(7.5f, 5.0f, 1.0f, 0.0f, 0.0f)
            "Acoustic & Vocals" -> listOf(-1.0f, 1.5f, 4.0f, 3.5f, 2.5f)
            "Rock" -> listOf(5f, 3f, -1f, 2f, 5f)
            "Electronic Club" -> listOf(6f, 4f, 0f, 3f, 6f)
            "Pop" -> listOf(-1f, 2f, 5f, 3f, -1f)
            "Jazz" -> listOf(4f, 2f, -1f, 2f, 4f)
            else -> return
        }

        val updatedBands = bands.mapIndexed { index, band ->
            val level = levels.getOrElse(index) { 0f }
            try {
                equalizer?.setBandLevel(band.bandIndex, (level * 100).toInt().toShort())
            } catch (_: Exception) {}
            band.copy(levelDb = level)
        }

        _state.update { it.copy(bands = updatedBands, currentPreset = presetName) }
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
