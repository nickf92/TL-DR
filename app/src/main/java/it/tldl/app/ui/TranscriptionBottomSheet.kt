package it.tldl.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.tldl.app.core.stt.LocalTextCleaner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionBottomSheet(
    progressPercent: Int,
    transcribedText: String,
    isFinished: Boolean,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit = {}
) {
    var isCleaned by remember { mutableStateOf(false) }
    val textCleaner = remember { LocalTextCleaner() }

    val displayText = remember(transcribedText, isCleaned) {
        if (isCleaned && transcribedText.isNotEmpty()) {
            textCleaner.cleanText(transcribedText)
        } else {
            transcribedText
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TL;DL Trascrizione Vocale",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (transcribedText.startsWith("Errore: Modello") && isFinished) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Modello non trovato",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Per trascrivere devi prima scaricare un modello IA nelle impostazioni.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = onGoToSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vai alle Impostazioni")
                }
            } else if (!isFinished) {
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Elaborazione in corso ($progressPercent%)...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = displayText.ifEmpty { "Nessun testo trascritto." },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isCleaned,
                        onClick = { isCleaned = !isCleaned },
                        leadingIcon = {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(if (isCleaned) "Pulito" else "Pulisci LLM") }
                    )
                    Button(onClick = onCopyClick) {
                        Text("Copia")
                    }
                    Button(onClick = onShareClick) {
                        Text("Condividi")
                    }
                }
            }
        }
    }
}
