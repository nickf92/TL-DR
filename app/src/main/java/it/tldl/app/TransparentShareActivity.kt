package it.tldl.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import it.tldl.app.core.service.TranscriptionService
import it.tldl.app.core.service.TranscriptionState
import it.tldl.app.ui.TranscriptionBottomSheet
import java.io.File
import java.io.FileOutputStream

class TransparentShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioUri: Uri? = if (intent?.action == Intent.ACTION_SEND) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
            }
        } else null

        if (audioUri != null) {
            startTranscriptionService(audioUri)
        }

        setContent {
            val state by TranscriptionService.state.collectAsState()

            val progressPercent = when (state) {
                is TranscriptionState.Transcribing -> (state as TranscriptionState.Transcribing).progress
                is TranscriptionState.Decoding -> 0
                is TranscriptionState.Success -> 100
                else -> 0
            }

            val transcribedText = when (state) {
                is TranscriptionState.Transcribing -> (state as TranscriptionState.Transcribing).partialText
                is TranscriptionState.Success -> (state as TranscriptionState.Success).text
                is TranscriptionState.Error -> "Errore: ${(state as TranscriptionState.Error).message}"
                else -> ""
            }

            val isFinished = state is TranscriptionState.Success || state is TranscriptionState.Error

            TranscriptionBottomSheet(
                progressPercent = progressPercent,
                transcribedText = transcribedText,
                isFinished = isFinished,
                onCopyClick = { textToCopy ->
                    copyToClipboard(textToCopy)
                },
                onShareClick = { textToShare ->
                    shareText(textToShare)
                },
                onDismiss = {
                    finish()
                },
                onGoToSettings = {
                    val intent = Intent(this@TransparentShareActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                },
                onCancelClick = {
                    val cancelIntent = Intent(this@TransparentShareActivity, TranscriptionService::class.java).apply {
                        action = TranscriptionService.ACTION_CANCEL
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(cancelIntent)
                    } else {
                        startService(cancelIntent)
                    }
                    finish()
                }
            )
        }
    }

    private fun startTranscriptionService(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "audio"
        val cacheFile = File(cacheDir, "input_audio_${System.currentTimeMillis()}.$extension")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val intent = Intent(this, TranscriptionService::class.java).apply {
                action = TranscriptionService.ACTION_START
                putExtra(TranscriptionService.EXTRA_AUDIO_PATH, cacheFile.absolutePath)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Impossibile leggere il file audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Trascrizione Vocale", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Testo copiato negli appunti!", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Condividi testo trascritto"))
    }
}
