package com.otium.audio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.otium.audio.data.local.dao.AudioDao
import com.otium.audio.data.local.entity.FavoriteEntity
import com.otium.audio.data.local.entity.HistoryEntity
import com.otium.audio.data.local.entity.PlaylistEntity
import com.otium.audio.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OtiumDatabase : RoomDatabase() {

    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: OtiumDatabase? = null

        fun getInstance(context: Context): OtiumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OtiumDatabase::class.java,
                    "otium_audio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
