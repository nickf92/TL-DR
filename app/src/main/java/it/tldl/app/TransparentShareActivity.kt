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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.tldl.app.core.service.TranscriptionService
import it.tldl.app.core.service.TranscriptionState
import it.tldl.app.ui.TranscriptionBottomSheet
import java.io.File
import java.io.FileOutputStream

class TransparentShareActivity : ComponentActivity() {

    private var activeMediaPlayer: android.media.MediaPlayer? = null
    private var currentCacheFile: File? = null

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

        // Prevent restarting transcription service on orientation change or config change
        if (savedInstanceState == null && audioUri != null) {
            startTranscriptionService(audioUri)
        }

        setContent {
            it.tldl.app.ui.theme.TLDLTheme {
                val state by TranscriptionService.state.collectAsState()
                var isAudioPlaying by remember { mutableStateOf(false) }

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
                    isPlayingAudio = isAudioPlaying,
                    onCopyClick = { textToCopy ->
                        copyToClipboard(textToCopy)
                    },
                    onShareClick = { textToShare ->
                        shareText(textToShare)
                    },
                    onPlayAudioClick = {
                        toggleAudioPlayback { playing ->
                            isAudioPlaying = playing
                        }
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
    }

    private fun toggleAudioPlayback(onStateChanged: (Boolean) -> Unit) {
        val file = currentCacheFile
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File audio non disponibile", Toast.LENGTH_SHORT).show()
            onStateChanged(false)
            return
        }

        try {
            val player = activeMediaPlayer
            if (player != null) {
                if (player.isPlaying) {
                    player.pause()
                    onStateChanged(false)
                } else {
                    if (player.currentPosition >= player.duration - 100) {
                        player.seekTo(0)
                    }
                    player.start()
                    onStateChanged(true)
                }
            } else {
                val newPlayer = android.media.MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        onStateChanged(false)
                    }
                    start()
                }
                activeMediaPlayer = newPlayer
                onStateChanged(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("TransparentShareActivity", "Error playing audio file", e)
            Toast.makeText(this, "Impossibile riprodurre l'audio", Toast.LENGTH_SHORT).show()
            onStateChanged(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        } catch (e: Exception) {}
        activeMediaPlayer = null
    }

    private fun startTranscriptionService(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "audio"
        val cacheFile = File(cacheDir, "input_audio_${System.currentTimeMillis()}.$extension")
        currentCacheFile = cacheFile
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
