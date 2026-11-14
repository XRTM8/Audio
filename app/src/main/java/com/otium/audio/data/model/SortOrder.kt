package com.otium.audio.data.model

enum class SortOrder(val displayName: String) {
    DATE_ADDED_DESC("تاريخ الإضافة (الأحدث أولاً)"),
    DATE_ADDED_ASC("تاريخ الإضافة (الأقدم أولاً)"),
    TITLE_ASC("عنوان المقطع (أ - ي)"),
    TITLE_DESC("عنوان المقطع (ي - أ)"),
    ARTIST_ASC("اسم الفنان (أ - ي)"),
    DURATION_DESC("المدة (الأطول أولاً)"),
    DURATION_ASC("المدة (الأقصر أولاً)");

    fun sort(songs: List<Song>): List<Song> = when (this) {
        DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
        DATE_ADDED_ASC -> songs.sortedBy { it.dateAdded }
        TITLE_ASC -> songs.sortedBy { it.displayTitle.lowercase() }
        TITLE_DESC -> songs.sortedByDescending { it.displayTitle.lowercase() }
        ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
        DURATION_DESC -> songs.sortedByDescending { it.durationMs }
        DURATION_ASC -> songs.sortedBy { it.durationMs }
    }
}
