package com.otium.audio.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.C
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelDb: Float, // in dB (-15dB to +15dB)
    val minDb: Float = -15f,
    val maxDb: Float = 15f
)

// 10-Band Audiophile reference frequencies (Poweramp & studio hardware standard)
private val DEFAULT_10_BANDS = listOf(
    EqualizerBand(0, 31, 0f, -15f, 15f),
    EqualizerBand(1, 62, 0f, -15f, 15f),
    EqualizerBand(2, 125, 0f, -15f, 15f),
    EqualizerBand(3, 250, 0f, -15f, 15f),
    EqualizerBand(4, 500, 0f, -15f, 15f),
    EqualizerBand(5, 1000, 0f, -15f, 15f),
    EqualizerBand(6, 2000, 0f, -15f, 15f),
    EqualizerBand(7, 4000, 0f, -15f, 15f),
    EqualizerBand(8, 8000, 0f, -15f, 15f),
    EqualizerBand(9, 16000, 0f, -15f, 15f)
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val isBound: Boolean = false,
    val deviceLabel: String = "",
    val preAmpDb: Float = 0f, // -10dB to +10dB Pre-Amp gain
    val bands: List<EqualizerBand> = DEFAULT_10_BANDS,
    val bassBoostStrength: Int = 0, // 0 to 1000
    val virtualizerStrength: Int = 0, // 0 to 1000
    val reverbStrength: Int = 0, // 0 to 100
    val reverbPreset: String = "Studio", // "Studio", "Room", "Hall", "Plate"
    val chorusStrength: Int = 0, // 0 to 100
    val clarityStrength: Int = 0, // 0 to 100
    val currentPreset: String = "Flat",
    val presets: List<String> = listOf(
        "Flat",
        "Warm",
        "Bass",
        "Vocal",
        "Bright",
        "Loudness",
        "Custom"
    )
)

class EqualizerController(
    private val context: Context,
    private val store: EqProfileStore = EqProfileStore(context)
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var lastAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _state = MutableStateFlow(
        store.load(OutputDevice.detect(context).key)?.let { saved ->
            EqualizerState(
                isEnabled = saved.enabled,
                preAmpDb = saved.preAmpDb,
                bassBoostStrength = saved.bassBoost,
                virtualizerStrength = saved.virtualizer,
                reverbStrength = saved.reverb,
                reverbPreset = saved.reverbPreset,
                chorusStrength = saved.chorus,
                clarityStrength = saved.clarity,
                currentPreset = saved.preset ?: "Custom",
                bands = DEFAULT_10_BANDS.mapIndexed { index, band ->
                    band.copy(levelDb = saved.bandLevels.getOrElse(index) { band.levelDb })
                }
            )
        } ?: EqualizerState()
    )
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var currentDevice: OutputDevice = OutputDevice.detect(context)

    init {
        // Headphones appearing is a change of curve, not just of volume.
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        manager?.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = reapply()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = reapply()
            },
            null
        )
        _state.update { it.copy(deviceLabel = currentDevice.label) }
    }

    private fun reapply() {
        val detected = OutputDevice.detect(context)
        if (detected == currentDevice) {
            _state.update { it.copy(deviceLabel = detected.label) }
            return
        }
        currentDevice = detected
        val saved = store.load(detected.key)
        _state.update { previous ->
            val bands = if (saved == null) {
                DEFAULT_10_BANDS
            } else {
                DEFAULT_10_BANDS.mapIndexed { index, band ->
                    band.copy(levelDb = saved.bandLevels.getOrElse(index) { band.levelDb })
                }
            }
            previous.copy(
                deviceLabel = detected.label,
                bands = bands,
                preAmpDb = saved?.preAmpDb ?: 0f,
                bassBoostStrength = saved?.bassBoost ?: 0,
                virtualizerStrength = saved?.virtualizer ?: 0,
                reverbStrength = saved?.reverb ?: 0,
                reverbPreset = saved?.reverbPreset ?: "Studio",
                chorusStrength = saved?.chorus ?: 0,
                clarityStrength = saved?.clarity ?: 0,
                currentPreset = saved?.preset ?: "Flat"
            )
        }
    }

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            Log.w("EqualizerController", "Invalid audioSessionId: $audioSessionId")
            return
        }

        lastAudioSessionId = audioSessionId
        release()

        // 1. Hardware BassBoost — attached to the active session for system-level bass drive
        try {
            val bb = BassBoost(1000, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb
            Log.i("EqualizerController", "Hardware BassBoost attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.w("EqualizerController", "Hardware BassBoost unavailable: ${e.message}")
            bassBoost = null
        }

        // 2. Hardware Virtualizer — 3D spatial acoustic widening
        try {
            val virt = Virtualizer(1000, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt
            Log.i("EqualizerController", "Hardware Virtualizer attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.w("EqualizerController", "Hardware Virtualizer unavailable: ${e.message}")
            virtualizer = null
        }

        _state.update { it.copy(isBound = true) }
    }

    fun setEnabled(enabled: Boolean) {
        if (bassBoost == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        try {
            bassBoost?.enabled = enabled
            if (enabled && bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(_state.value.bassBoostStrength.toShort())
            }
            virtualizer?.enabled = enabled
            if (enabled && virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(_state.value.virtualizerStrength.toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error toggling enabled=$enabled", e)
        }

        _state.update { it.copy(isEnabled = enabled) }
        store.save(currentDevice.key, _state.value, force = true)
    }

    fun setPreAmpGain(gainDb: Float) {
        val safeGain = gainDb.coerceIn(-10f, 10f)
        _state.update { it.copy(preAmpDb = safeGain) }
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
        store.save(currentDevice.key, _state.value)
    }

    fun setBassBoost(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)

        if (bassBoost == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
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
        store.save(currentDevice.key, _state.value)
    }

    fun setVirtualizer(strength: Int) {
        val safeStrength = strength.coerceIn(0, 1000)

        if (virtualizer == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
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
        store.save(currentDevice.key, _state.value)
    }

    fun applyPreset(presetName: String) {
        if (bassBoost == null && lastAudioSessionId > 0 && lastAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            bindAudioSession(lastAudioSessionId)
        }

        // Auto-enable when applying any preset for instant auditory feedback
        if (!_state.value.isEnabled) {
            setEnabled(true)
        }

        val bands = _state.value.bands

        // Reference 10-band target curves
        // Deliberately gentle. Anything past four decibels stops being a tone control and
        // starts being distortion, and the "headphone target" curves that used to live here
        // were fiction without a measured impulse response behind them.
        val target10Levels = when (presetName) {
            "Flat" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            "Warm" -> listOf(2.5f, 2.0f, 1.0f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, 1.0f, 1.5f)
            "Bass" -> listOf(3.5f, 3.0f, 1.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f)
            "Vocal" -> listOf(-1.0f, -0.5f, 0.0f, 1.0f, 2.5f, 3.0f, 2.0f, 1.0f, 0.0f, -0.5f)
            "Bright" -> listOf(0.0f, 0.0f, 0.0f, -0.5f, 0.0f, 0.5f, 1.0f, 2.5f, 3.0f, 3.0f)
            "Loudness" -> listOf(3.0f, 2.5f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.5f, 2.5f, 2.5f)
            else -> return
        }

        val updatedBands = bands.mapIndexed { index, band ->
            // Map 10-band target to whatever number of bands the hardware has
            val level = if (bands.size == 10) {
                target10Levels.getOrElse(index) { 0f }
            } else {
                // Interpolate from 10-band curve
                val ratio = if (bands.size > 1) index.toFloat() / (bands.size - 1) else 0f
                val targetIndex = (ratio * (target10Levels.size - 1)).toInt()
                target10Levels.getOrElse(targetIndex) { 0f }
            }

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
        store.save(currentDevice.key, _state.value, force = true)
    }

    fun setReverb(strength: Int, preset: String? = null) {
        val safeStrength = strength.coerceIn(0, 100)
        _state.update {
            it.copy(
                reverbStrength = safeStrength,
                reverbPreset = preset ?: it.reverbPreset
            )
        }
        store.save(currentDevice.key, _state.value)
    }

    fun setReverbPreset(preset: String) {
        _state.update { it.copy(reverbPreset = preset) }
        store.save(currentDevice.key, _state.value)
    }

    fun setChorus(strength: Int) {
        val safeStrength = strength.coerceIn(0, 100)
        _state.update { it.copy(chorusStrength = safeStrength) }
        store.save(currentDevice.key, _state.value)
    }

    fun setClarity(strength: Int) {
        val safeStrength = strength.coerceIn(0, 100)
        _state.update { it.copy(clarityStrength = safeStrength) }
        store.save(currentDevice.key, _state.value)
    }

    fun release() {
        _state.update { it.copy(isBound = false) }
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
