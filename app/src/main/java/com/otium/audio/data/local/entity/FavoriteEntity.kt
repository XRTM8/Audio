package com.otium.audio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["addedAt"])]
)
data class FavoriteEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
