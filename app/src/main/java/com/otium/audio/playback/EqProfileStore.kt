package com.otium.audio.playback

import android.content.Context

/**
 * Remembers the equalizer between runs. Slider drags are throttled, so a full sweep of the
 * band controls writes a handful of times rather than a hundred.
 */
class EqProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private var lastWrite = 0L

    fun save(state: EqualizerState, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWrite < WRITE_INTERVAL_MS) return
        lastWrite = now

        prefs.edit()
            .putString(KEY_BANDS, state.bands.joinToString(",") { it.levelDb.toString() })
            .putInt(KEY_BASS, state.bassBoostStrength)
            .putInt(KEY_VIRTUALIZER, state.virtualizerStrength)
            .putFloat(KEY_PREAMP, state.preAmpDb)
            .putBoolean(KEY_ENABLED, state.isEnabled)
            .putString(KEY_PRESET, state.currentPreset)
            .apply()
    }

    fun load(): Saved? {
        val bands = prefs.getString(KEY_BANDS, null) ?: return null
        val levels = bands.split(',').mapNotNull { it.toFloatOrNull() }
        if (levels.isEmpty()) return null
        return Saved(
            bandLevels = levels,
            bassBoost = prefs.getInt(KEY_BASS, 0),
            virtualizer = prefs.getInt(KEY_VIRTUALIZER, 0),
            preAmpDb = prefs.getFloat(KEY_PREAMP, 0f),
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            preset = prefs.getString(KEY_PRESET, null)
        )
    }

    data class Saved(
        val bandLevels: List<Float>,
        val bassBoost: Int,
        val virtualizer: Int,
        val preAmpDb: Float,
        val enabled: Boolean,
        val preset: String?
    )

    private companion object {
        const val FILE = "eq_profile"
        const val KEY_BANDS = "bands"
        const val KEY_BASS = "bass"
        const val KEY_VIRTUALIZER = "virtualizer"
        const val KEY_PREAMP = "preamp"
        const val KEY_ENABLED = "enabled"
        const val KEY_PRESET = "preset"
        const val WRITE_INTERVAL_MS = 600L
    }
}
