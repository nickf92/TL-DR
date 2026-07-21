package it.tldl.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import it.tldl.app.ui.ModelItemState
import it.tldl.app.ui.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val transcriptionModels by viewModel.transcriptionModels.collectAsState()
    val cleaningModels by viewModel.cleaningModels.collectAsState()
    val selectedCleanerId by viewModel.selectedTextCleanerId.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TL;DL - Impostazioni Modelli") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("RAM disponibile: ${availableRam}MB")
                    }
                }
            }

            item {
                val isCleanerEnabled by viewModel.isTextCleanerEnabled.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Post-Processing Pulizia Testo", style = MaterialTheme.typography.titleMedium)
                            Text("Abilita il ripristino di punteggiatura e la rimozione di intercalari dopo la trascrizione.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isCleanerEnabled,
                            onCheckedChange = { viewModel.toggleTextCleaner(it) }
                        )
                    }
                }
            }

            // SECTION 1: TRASCRIZIONE AUDIO
            item {
                Text(
                    text = "1. Modelli di Trascrizione Vocale (STT)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(transcriptionModels, key = { it.info.id }) { modelState ->
                ModelItem(
                    state = modelState,
                    onDownloadClick = { viewModel.downloadModel(modelState.info) },
                    onSelectClick = { viewModel.selectModel(modelState.info.id) },
                    onDeleteClick = { viewModel.deleteModel(modelState.info.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // SECTION 2: PULIZIA TESTO & PUNTEGGIATURA
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "2. Modelli di Pulizia Testo & Punteggiatura",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Default Rule-based option (no download required)
            item {
                val isRulesSelected = selectedCleanerId == "rules"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    onClick = { if (!isRulesSelected) viewModel.selectTextCleaner("rules") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Engine Euristico a Regole (Integrato)", style = MaterialTheme.typography.titleMedium)
                            Text("Leggerissimo e immediato. Rimuove intercalari (ehm, cioè) e formatta frasi.", style = MaterialTheme.typography.bodySmall)
                            AnimatedVisibility(visible = isRulesSelected, enter = fadeIn() + expandVertically()) {
                                Text("In Uso (Attivo)", color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (isRulesSelected) {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("Attivo") }
                            )
                        } else {
                            OutlinedButton(onClick = { viewModel.selectTextCleaner("rules") }) {
                                Text("Usa")
                            }
                        }
                    }
                }
            }

            items(cleaningModels, key = { it.info.id }) { modelState ->
                ModelItem(
                    state = modelState,
                    onDownloadClick = { viewModel.downloadModel(modelState.info) },
                    onSelectClick = { viewModel.selectTextCleaner(modelState.info.id) },
                    onDeleteClick = { viewModel.deleteModel(modelState.info.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ModelItem(
    state: ModelItemState,
    onDownloadClick: () -> Unit,
    onSelectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val model = state.info
    val animatedProgress by animateFloatAsState(
        targetValue = state.downloadProgress / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "downloadProgressAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (state.isDownloaded && !state.isSelected) onSelectClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                Text("Dimensioni: ${model.sizeMb}MB | RAM richiesta: ${model.ramRequiredMb}MB", style = MaterialTheme.typography.bodySmall)
                AnimatedVisibility(visible = state.isSelected, enter = fadeIn() + expandVertically()) {
                    Text("In Uso (Attivo)", color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                }
                AnimatedVisibility(visible = !state.isSelected && model.isIdealCap, enter = fadeIn() + expandVertically()) {
                    Text("Consigliato (Smart Default)", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                }
                AnimatedVisibility(visible = state.error != null, enter = fadeIn() + expandVertically()) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            AnimatedContent(
                targetState = Triple(state.isSelected, state.isDownloaded, state.isDownloading),
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.8f)) togetherWith
                    (fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.8f))
                },
                label = "actionStateTransition"
            ) { (isSelected, isDownloaded, isDownloading) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("Attivo") }
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (isDownloaded) {
                        OutlinedButton(onClick = onSelectClick) {
                            Text("Usa")
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (isDownloading) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        IconButton(onClick = onDownloadClick) {
                            Icon(Icons.Default.Download, contentDescription = "Scarica")
                        }
                    }
                }
            }
        }
    }
}
