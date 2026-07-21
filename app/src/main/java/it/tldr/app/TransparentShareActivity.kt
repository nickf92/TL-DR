package it.tldr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.tldr.app.ui.TranscriptionBottomSheet

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
        } else null

        setContent {
            var progress by remember { mutableStateOf(0) }
            var transcribedText by remember { mutableStateOf("") }
            var isFinished by remember { mutableStateOf(false) }

            TranscriptionBottomSheet(
                progressPercent = progress,
                transcribedText = transcribedText,
                isFinished = isFinished,
                onCopyClick = {
                    copyToClipboard(transcribedText)
                },
                onShareClick = {
                    shareText(transcribedText)
                },
                onDismiss = {
                    finish()
                }
            )
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
