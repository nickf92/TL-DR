package it.tldl.app.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.tldl.app.core.stt.LocalTextCleaner
import it.tldl.app.core.stt.ModelManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionBottomSheet(
    progressPercent: Int,
    transcribedText: String,
    isFinished: Boolean,
    onCopyClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit,
    onGoToSettings: () -> Unit = {},
    onPlayAudioClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    var isCleaned by remember { mutableStateOf(false) }
    val textCleaner = remember { LocalTextCleaner(modelManager) }

    val displayText = remember(transcribedText, isCleaned) {
        if (isCleaned && transcribedText.isNotEmpty()) {
            textCleaner.cleanText(transcribedText)
        } else {
            transcribedText
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progressAnimation"
    )

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

            AnimatedContent(
                targetState = Pair(isFinished, transcribedText.startsWith("Errore: Modello")),
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                     slideInVertically(initialOffsetY = { 40 }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))) togetherWith
                    (fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { -40 }))
                },
                label = "contentStateTransition"
            ) { (finished, isModelError) ->
                if (isModelError && finished) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    }
                } else if (!finished) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Elaborazione in corso ($progressPercent%)...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onCancelClick) {
                            Text("Annulla", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 400.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                AnimatedContent(
                                    targetState = displayText,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                                    },
                                    label = "textCleanTransition"
                                ) { textToDisplay ->
                                    Text(
                                        text = textToDisplay.ifEmpty { "Nessun testo trascritto." },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
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
                                onClick = {
                                    val nextCleaned = !isCleaned
                                    isCleaned = nextCleaned
                                    if (nextCleaned) {
                                        val cleaned = textCleaner.cleanText(transcribedText)
                                        if (cleaned == transcribedText) {
                                            Toast.makeText(context, "Nessuna modifica necessaria", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Pulizia applicata (${modelManager.getSelectedTextCleanerId()})", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                label = { Text(if (isCleaned) "Pulito" else "Pulisci Testo") }
                            )
                             IconButton(onClick = onPlayAudioClick) {
                                 Icon(Icons.Default.PlayArrow, contentDescription = "Ascolta Audio")
                             }
                             Button(onClick = { onCopyClick(displayText) }) {
                                 Text("Copia")
                             }
                             Button(onClick = { onShareClick(displayText) }) {
                                 Text("Condividi")
                             }
                        }
                    }
                }
            }
        }
    }
}
