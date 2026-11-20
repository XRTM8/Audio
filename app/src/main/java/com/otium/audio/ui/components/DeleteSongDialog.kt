package com.otium.audio.ui.components

import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Confirms, then deletes through the platform (single or batch).
 *
 * On Android 11 and newer the system owns the decision: `createDeleteRequest` shows its own
 * consent sheet and does the deletion, so the app needs no broad storage permission. On
 * older versions the row is removed directly. Either way the MediaStore observer picks the
 * change up and the library refreshes itself.
 */
@Composable
fun DeleteSongsDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSongsDeleted: ((List<Song>) -> Unit)? = null
) {
    if (songs.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onSongsDeleted?.invoke(songs)
        }
        onDismiss()
    }

    val isSingle = songs.size == 1
    val titleText = if (isSingle) "حذف المقطع الصوتي" else "حذف المقاطع المحددة"
    val messageText = if (isSingle) {
        "«${songs.first().title}» ستُحذف من الجهاز نهائياً. لا يمكن التراجع."
    } else {
        "سيتم حذف ${songs.size} مقاطع صوتية من الجهاز نهائياً. لا يمكن التراجع."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceContainer,
        title = { Text(titleText, color = TextPrimary) },
        text = {
            Text(messageText, color = TextSecondary)
        },
        confirmButton = {
            TextButton(onClick = {
                val uris = songs.map { it.contentUri }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pending: IntentSender? = runCatching {
                        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                    }.getOrNull()
                    if (pending != null) {
                        launcher.launch(IntentSenderRequest.Builder(pending).build())
                    } else {
                        onDismiss()
                    }
                } else {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            uris.forEach { uri ->
                                runCatching { context.contentResolver.delete(uri, null, null) }
                            }
                        }
                    }
                    onSongsDeleted?.invoke(songs)
                    onDismiss()
                }
            }) {
                Text("حذف", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        }
    )
}

@Composable
fun DeleteSongDialog(
    song: Song?,
    onDismiss: () -> Unit,
    onSongDeleted: ((Song) -> Unit)? = null
) {
    if (song == null) return
    DeleteSongsDialog(
        songs = listOf(song),
        onDismiss = onDismiss,
        onSongsDeleted = { if (it.isNotEmpty()) onSongDeleted?.invoke(it.first()) }
    )
}
