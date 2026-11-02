package com.otium.audio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)
