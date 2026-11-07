# Media3 proguard rules
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# Keep Native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Immutable Data Models
-keep class com.otium.audio.data.model.** { *; }
-keep class com.otium.audio.ui.viewmodel.** { *; }
