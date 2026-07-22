package it.tldl.app.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.tldl.app.core.stt.LocalTextCleaner
import it.tldl.app.core.stt.ModelManager
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
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
                                    .heightIn(min = 120.dp, max = 360.dp)
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // UTILITY ROW: Text Cleaner & Audio Playback
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = isCleaned,
                                    onClick = {
                                        val nextCleaned = !isCleaned
                                        isCleaned = nextCleaned
                                        val cleaned = textCleaner.cleanText(transcribedText)
                                        val msg = if (nextCleaned) {
                                            if (cleaned == transcribedText) "Nessuna modifica necessaria"
                                            else "Pulizia applicata"
                                        } else "Testo originale ripristinato"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    label = { Text(if (isCleaned) "Testo Pulito" else "Pulisci Testo") }
                                )

                                FilledTonalIconButton(onClick = onPlayAudioClick) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Ascolta Audio")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // PRIMARY ACTION ROW: Condividi & Copia
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onShareClick(displayText) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Condividi")
                                }

                                Button(
                                    onClick = {
                                        onCopyClick(displayText)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Testo copiato negli appunti!")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copia")
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}
