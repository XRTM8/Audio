package com.otium.audio.playback

import android.content.Context

/**
 * Remembers the queue and the playback position across process death.
 *
 * The queue is written only when it changes; the position is written every few seconds.
 * That keeps a ten thousand track queue from being serialised on every tick.
 */
class PlaybackPersistence(private val context: Context) {

    private val prefs by lazy { context.getSharedPreferences(FILE, Context.MODE_PRIVATE) }

    fun saveQueue(songIds: List<Long>, index: Int) {
        prefs.edit()
            .putString(KEY_IDS, songIds.joinToString(","))
            .putInt(KEY_INDEX, index)
            .apply()
    }

    fun saveIndex(index: Int, sync: Boolean = false) {
        val editor = prefs.edit().putInt(KEY_INDEX, index)
        if (sync) editor.commit() else editor.apply()
    }

    fun savePosition(positionMs: Long, sync: Boolean = false) {
        val editor = prefs.edit().putLong(KEY_POSITION, positionMs)
        if (sync) editor.commit() else editor.apply()
    }

    fun saveSkipSilence(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_SILENCE, enabled).apply()
    }

    fun isSkipSilenceEnabled(): Boolean = prefs.getBoolean(KEY_SKIP_SILENCE, false)

    fun saveHiResFloatOutput(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HI_RES_FLOAT, enabled).apply()
    }

    fun isHiResFloatOutputEnabled(): Boolean = prefs.getBoolean(KEY_HI_RES_FLOAT, false)

    fun saveLoudnessNormalization(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOUDNESS_NORMALIZATION, enabled).apply()
    }

    fun isLoudnessNormalizationEnabled(): Boolean = prefs.getBoolean(KEY_LOUDNESS_NORMALIZATION, false)

    fun saveMinDurationSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_MIN_DURATION, seconds).apply()
    }

    fun getMinDurationSeconds(): Int = prefs.getInt(KEY_MIN_DURATION, 0) // 0s (default, filters only <3s)

    fun saveBlacklistedFolders(folders: Set<String>) {
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, folders).apply()
    }

    fun getBlacklistedFolders(): Set<String> = prefs.getStringSet(KEY_BLACKLISTED_FOLDERS, emptySet()) ?: emptySet()

    fun saveSortOrder(order: com.otium.audio.data.model.SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
    }

    fun getSortOrder(): com.otium.audio.data.model.SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, null) ?: return com.otium.audio.data.model.SortOrder.DATE_ADDED_DESC
        return runCatching { com.otium.audio.data.model.SortOrder.valueOf(name) }
            .getOrDefault(com.otium.audio.data.model.SortOrder.DATE_ADDED_DESC)
    }

    fun saveCrossfadeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE_SECONDS, seconds).apply()
    }

    fun getCrossfadeSeconds(): Int = prefs.getInt(KEY_CROSSFADE_SECONDS, 0) // 0s = disabled

    fun saveResumeOnHeadsetConnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RESUME_HEADSET, enabled).apply()
    }

    fun isResumeOnHeadsetConnectEnabled(): Boolean = prefs.getBoolean(KEY_RESUME_HEADSET, false)

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, true)

    fun clearQueue() {
        prefs.edit().remove(KEY_IDS).remove(KEY_INDEX).remove(KEY_POSITION).apply()
    }

    fun hasSession(): Boolean = !prefs.getString(KEY_IDS, null).isNullOrEmpty()

    fun load(): Session? {
        val raw = prefs.getString(KEY_IDS, null) ?: return null
        val ids = raw.split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return Session(ids, prefs.getInt(KEY_INDEX, 0), prefs.getLong(KEY_POSITION, 0L))
    }

    data class Session(val songIds: List<Long>, val index: Int, val positionMs: Long)

    private companion object {
        const val FILE = "playback_session"
        const val KEY_IDS = "queue_ids"
        const val KEY_INDEX = "queue_index"
        const val KEY_POSITION = "queue_position"
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_HI_RES_FLOAT = "hi_res_float_output"
        const val KEY_LOUDNESS_NORMALIZATION = "loudness_normalization"
        const val KEY_MIN_DURATION = "min_duration_seconds"
        const val KEY_BLACKLISTED_FOLDERS = "blacklisted_folders"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        const val KEY_RESUME_HEADSET = "resume_on_headset"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
