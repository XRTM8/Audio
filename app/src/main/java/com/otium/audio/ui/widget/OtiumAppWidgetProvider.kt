package com.otium.audio.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.otium.audio.R
import com.otium.audio.data.model.Song
import com.otium.audio.playback.PlaybackService
import com.otium.audio.ui.MainActivity

class OtiumAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.otium.audio.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.otium.audio.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.otium.audio.ACTION_SKIP_PREVIOUS"

        private var lastSong: Song? = null
        private var lastIsPlaying: Boolean = false

        fun updateWidget(context: Context, song: Song?, isPlaying: Boolean) {
            lastSong = song
            lastIsPlaying = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, OtiumAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) return

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_otium_player)

                if (song != null) {
                    views.setTextViewText(R.id.widget_title, song.title)
                    views.setTextViewText(R.id.widget_artist, song.artist)
                } else {
                    views.setTextViewText(R.id.widget_title, "Otium Audio")
                    views.setTextViewText(R.id.widget_artist, "Touch to play")
                }

                val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

                // Intent to open Main Activity
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, pendingFlags)
                views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

                // Play / Pause Action
                val playPauseIntent = Intent(context, OtiumAppWidgetProvider::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                }
                val playPausePendingIntent = PendingIntent.getBroadcast(context, 1, playPauseIntent, pendingFlags)
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

                // Skip Next Action
                val nextIntent = Intent(context, OtiumAppWidgetProvider::class.java).apply {
                    action = ACTION_SKIP_NEXT
                }
                val nextPendingIntent = PendingIntent.getBroadcast(context, 2, nextIntent, pendingFlags)
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

                // Skip Previous Action
                val prevIntent = Intent(context, OtiumAppWidgetProvider::class.java).apply {
                    action = ACTION_SKIP_PREVIOUS
                }
                val prevPendingIntent = PendingIntent.getBroadcast(context, 3, prevIntent, pendingFlags)
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidget(context, lastSong, lastIsPlaying)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val serviceIntent = Intent(context, PlaybackService::class.java)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                serviceIntent.action = ACTION_PLAY_PAUSE
                startServiceSafely(context, serviceIntent)
            }
            ACTION_SKIP_NEXT -> {
                serviceIntent.action = ACTION_SKIP_NEXT
                startServiceSafely(context, serviceIntent)
            }
            ACTION_SKIP_PREVIOUS -> {
                serviceIntent.action = ACTION_SKIP_PREVIOUS
                startServiceSafely(context, serviceIntent)
            }
        }
    }

    private fun startServiceSafely(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }
}
