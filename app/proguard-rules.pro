# Media3 proguard rules
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
