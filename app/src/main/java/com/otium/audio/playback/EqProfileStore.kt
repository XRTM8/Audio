package com.otium.audio.playback

import android.content.Context

/**
 * Remembers the equalizer between runs, once per kind of output. A phone speaker and a pair
 * of headphones want different curves, so each keeps its own.
 *
 * Slider drags are throttled: a full sweep of the ten bands writes a handful of times
 * rather than a hundred.
 */
class EqProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private var lastWrite = 0L

    fun save(device: String, state: EqualizerState, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWrite < WRITE_INTERVAL_MS) return
        lastWrite = now

        prefs.edit()
            .putString(key(device, BANDS), state.bands.joinToString(",") { it.levelDb.toString() })
            .putInt(key(device, BASS), state.bassBoostStrength)
            .putInt(key(device, VIRTUALIZER), state.virtualizerStrength)
            .putFloat(key(device, PREAMP), state.preAmpDb)
            .putBoolean(key(device, AUTO_PREAMP), state.isAutoPreAmpEnabled)
            .putBoolean(key(device, ENABLED), state.isEnabled)
            .putString(key(device, PRESET), state.currentPreset)
            .putInt(key(device, REVERB), state.reverbStrength)
            .putString(key(device, REVERB_PRESET), state.reverbPreset)
            .putInt(key(device, CHORUS), state.chorusStrength)
            .putInt(key(device, CLARITY), state.clarityStrength)
            .apply()
    }

    fun load(device: String): Saved? {
        val bands = prefs.getString(key(device, BANDS), null) ?: return null
        val levels = bands.split(',').mapNotNull { it.toFloatOrNull() }
        if (levels.isEmpty()) return null
        return Saved(
            bandLevels = levels,
            bassBoost = prefs.getInt(key(device, BASS), 0),
            virtualizer = prefs.getInt(key(device, VIRTUALIZER), 0),
            preAmpDb = prefs.getFloat(key(device, PREAMP), 0f),
            isAutoPreAmp = prefs.getBoolean(key(device, AUTO_PREAMP), true),
            enabled = prefs.getBoolean(key(device, ENABLED), true),
            preset = prefs.getString(key(device, PRESET), null),
            reverb = prefs.getInt(key(device, REVERB), 0),
            reverbPreset = prefs.getString(key(device, REVERB_PRESET), "Studio") ?: "Studio",
            chorus = prefs.getInt(key(device, CHORUS), 0),
            clarity = prefs.getInt(key(device, CLARITY), 0)
        )
    }

    data class Saved(
        val bandLevels: List<Float>,
        val bassBoost: Int,
        val virtualizer: Int,
        val preAmpDb: Float,
        val isAutoPreAmp: Boolean = true,
        val enabled: Boolean,
        val preset: String?,
        val reverb: Int = 0,
        val reverbPreset: String = "Studio",
        val chorus: Int = 0,
        val clarity: Int = 0
    )

    private fun key(device: String, field: String) = "${device}_$field"

    private companion object {
        const val FILE = "eq_profile"
        const val BANDS = "bands"
        const val BASS = "bass"
        const val VIRTUALIZER = "virtualizer"
        const val PREAMP = "preamp"
        const val AUTO_PREAMP = "auto_preamp"
        const val ENABLED = "enabled"
        const val PRESET = "preset"
        const val REVERB = "reverb"
        const val REVERB_PRESET = "reverb_preset"
        const val CHORUS = "chorus"
        const val CLARITY = "clarity"
        const val WRITE_INTERVAL_MS = 600L
    }
}
