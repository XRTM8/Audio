package com.otium.audio.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
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
    val isEnabled: Boolean = true, // Default enabled for immediate feedback
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
    private var lastAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            Log.w("EqualizerController", "Invalid audioSessionId: $audioSessionId")
            return
        }

        lastAudioSessionId = audioSessionId
        release()

        // 1. Equalizer — Priority 1000 ensures our app takes full control of the session
        try {
            val eq = Equalizer(1000, audioSessionId)
            equalizer = eq

            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minDb = if (range != null && range.size >= 2) range[0] / 100f else -15f
            val maxDb = if (range != null && range.size >= 2) range[1] / 100f else 15f

            val currentBands = _state.value.bands
            val updatedBands = mutableListOf<EqualizerBand>()

            for (i in 0 until numBands) {
                val band = i.toShort()
                val freq = try {
                    eq.getCenterFreq(band) / 1000
                } catch (_: Exception) {
                    DEFAULT_PRESET_BANDS.getOrNull(i)?.centerFreqHz ?: ((i + 1) * 1000)
                }

                val existingLevel = currentBands.getOrNull(i)?.levelDb ?: 0f
                val safeMb = (existingLevel * 100).toInt().coerceIn(
                    (minDb * 100).toInt(),
                    (maxDb * 100).toInt()
                ).toShort()

                try {
                    eq.setBandLevel(band, safeMb)
                } catch (e: Exception) {
                    Log.w("EqualizerController", "Error setting band $band: ${e.message}")
                }

                updatedBands.add(
                    EqualizerBand(
                        bandIndex = band,
                        centerFreqHz = freq,
                        levelDb = existingLevel,
                        minDb = minDb,
                        maxDb = maxDb
                    )
                )
            }

            eq.enabled = _state.value.isEnabled

            if (updatedBands.isNotEmpty()) {
                _state.update { it.copy(bands = updatedBands) }
            }
            Log.i("EqualizerController", "Equalizer bound successfully to session $audioSessionId with $numBands bands")
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error initializing Equalizer on session $audioSessionId", e)
        }

        // 2. BassBoost — isolated so speaker / device limitations do not block Equalizer
        try {
            val bb = BassBoost(1000, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb
            Log.i("EqualizerController", "BassBoost attached")
        } catch (e: Exception) {
            Log.w("EqualizerController", "BassBoost not supported on session $audioSessionId: ${e.message}")
            bassBoost = null
        }

        // 3. Virtualizer — isolated so speaker limitations do not block Equalizer
        try {
            val virt = Virtualizer(1000, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt
            Log.i("EqualizerController", "Virtualizer attached")
        } catch (e: Exception) {
            Log.w("EqualizerController", "Virtualizer not supported on session $audioSessionId: ${e.message}")
            virtualizer = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (equalizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        try {
            equalizer?.enabled = enabled
            if (enabled) {
                // Re-apply all bands upon enabling
                _state.value.bands.forEach { band ->
                    try {
                        val mb = (band.levelDb * 100).toInt().toShort()
                        equalizer?.setBandLevel(band.bandIndex, mb)
                    } catch (_: Exception) {}
                }
                if (bassBoost?.strengthSupported == true) {
                    try { bassBoost?.setStrength(_state.value.bassBoostStrength.toShort()) } catch (_: Exception) {}
                }
                if (virtualizer?.strengthSupported == true) {
                    try { virtualizer?.setStrength(_state.value.virtualizerStrength.toShort()) } catch (_: Exception) {}
                }
            }
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error toggling enabled=$enabled", e)
        }

        _state.update { it.copy(isEnabled = enabled) }
    }

    fun setBandLevel(bandIndex: Short, levelDb: Float) {
        if (equalizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        // Auto-enable if user interacts with a band slider
        if (!_state.value.isEnabled) {
            setEnabled(true)
        }

        try {
            val mb = (levelDb * 100).toInt().toShort()
            equalizer?.setBandLevel(bandIndex, mb)
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error setting band $bandIndex: ${e.message}")
        }

        _state.update { currentState ->
            val updatedBands = currentState.bands.map { band ->
                if (band.bandIndex == bandIndex) band.copy(levelDb = levelDb) else band
            }
            currentState.copy(bands = updatedBands, currentPreset = "Custom")
        }
    }

    fun setBassBoost(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)

        if (equalizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        if (safeStrength > 0 && !_state.value.isEnabled) {
            setEnabled(true)
        }

        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(safeStrength.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error setting bass boost: ${e.message}")
        }

        _state.update { it.copy(bassBoostStrength = safeStrength) }
    }

    fun setVirtualizer(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)

        if (equalizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        if (safeStrength > 0 && !_state.value.isEnabled) {
            setEnabled(true)
        }

        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(safeStrength.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error setting virtualizer: ${e.message}")
        }

        _state.update { it.copy(virtualizerStrength = safeStrength) }
    }

    fun applyPreset(presetName: String) {
        if (equalizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        // Auto-enable when applying any preset for instant auditory feedback
        if (!_state.value.isEnabled) {
            setEnabled(true)
        }

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
            val safeMb = (level * 100).toInt().coerceIn(
                (band.minDb * 100).toInt(),
                (band.maxDb * 100).toInt()
            ).toShort()
            try {
                equalizer?.setBandLevel(band.bandIndex, safeMb)
            } catch (e: Exception) {
                Log.w("EqualizerController", "Error setting preset band ${band.bandIndex}: ${e.message}")
            }
            band.copy(levelDb = level)
        }

        _state.update { it.copy(bands = updatedBands, currentPreset = presetName) }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        try {
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
