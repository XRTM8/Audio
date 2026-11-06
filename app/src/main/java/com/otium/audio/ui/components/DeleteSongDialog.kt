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
 * Confirms, then deletes through the platform.
 *
 * On Android 11 and newer the system owns the decision: `createDeleteRequest` shows its own
 * consent sheet and does the deletion, so the app needs no broad storage permission. On
 * older versions the row is removed directly. Either way the MediaStore observer picks the
 * change up and the library refreshes itself.
 */
@Composable
fun DeleteSongDialog(
    song: Song?,
    onDismiss: () -> Unit
) {
    if (song == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { /* the platform already deleted it; the observer refreshes the list */ }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceContainer,
        title = { Text("حذف المقطع الصوتي", color = TextPrimary) },
        text = {
            Text(
                "«${song.title}» ستُحذف من الجهاز نهائياً. لا يمكن التراجع.",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                        val pending: IntentSender =
                            MediaStore.createDeleteRequest(context.contentResolver, listOf(song.contentUri)).intentSender
                        launcher.launch(IntentSenderRequest.Builder(pending).build())
                    }
                } else {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { context.contentResolver.delete(song.contentUri, null, null) }
                        }
                    }
                }
                onDismiss()
            }) {
                Text("حذف", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        }
    )
}
