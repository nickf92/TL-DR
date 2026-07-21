package it.tldl.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import it.tldl.app.core.stt.ModelInfo
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
    val isCleanerEnabled by viewModel.isTextCleanerEnabled.collectAsState()

    val onDownload = remember(viewModel) { { info: ModelInfo -> viewModel.downloadModel(info) } }
    val onSelectSTT = remember(viewModel) { { id: String -> viewModel.selectModel(id) } }
    val onSelectCleaner = remember(viewModel) { { id: String -> viewModel.selectTextCleaner(id) } }
    val onDelete = remember(viewModel) { { id: String -> viewModel.deleteModel(id) } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TL;DL - Impostazioni Modelli") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "ram_header", contentType = "ram_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

            item(key = "cleaner_switch", contentType = "cleaner_switch") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
            item(key = "header_stt", contentType = "section_header") {
                Text(
                    text = "1. Modelli di Trascrizione Vocale (STT)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(
                items = transcriptionModels,
                key = { it.info.id },
                contentType = { "model_item" }
            ) { modelState ->
                ModelItem(
                    state = modelState,
                    onDownloadClick = onDownload,
                    onSelectClick = onSelectSTT,
                    onDeleteClick = onDelete
                )
            }

            // SECTION 2: PULIZIA TESTO & PUNTEGGIATURA
            item(key = "header_cleaner", contentType = "section_header") {
                Text(
                    text = "2. Modelli di Pulizia Testo & Punteggiatura",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Default Rule-based option (no download required)
            item(key = "rules_cleaner", contentType = "rules_item") {
                val isRulesSelected = selectedCleanerId == "rules"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Engine Euristico a Regole (Integrato)", style = MaterialTheme.typography.titleMedium)
                            Text("Leggerissimo e immediato. Rimuove intercalari (ehm, cioè) e formatta frasi.", style = MaterialTheme.typography.bodySmall)
                            if (isRulesSelected) {
                                Text("In Uso (Attivo)", color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (isRulesSelected) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Attivo") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        } else {
                            OutlinedButton(onClick = { onSelectCleaner("rules") }) {
                                Text("Usa")
                            }
                        }
                    }
                }
            }

            items(
                items = cleaningModels,
                key = { it.info.id },
                contentType = { "model_item" }
            ) { modelState ->
                ModelItem(
                    state = modelState,
                    onDownloadClick = onDownload,
                    onSelectClick = onSelectCleaner,
                    onDeleteClick = onDelete
                )
            }
        }
    }
}

@Composable
fun ModelItem(
    state: ModelItemState,
    onDownloadClick: (ModelInfo) -> Unit,
    onSelectClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val model = state.info

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                Text("Dimensioni: ${model.sizeMb}MB | RAM richiesta: ${model.ramRequiredMb}MB", style = MaterialTheme.typography.bodySmall)
                
                if (state.isSelected) {
                    Text("In Uso (Attivo)", color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                } else if (model.isIdealCap) {
                    Text("Consigliato (Smart Default)", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                }
                
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isSelected) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Attivo") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { onDeleteClick(model.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                    }
                } else if (state.isDownloaded) {
                    OutlinedButton(onClick = { onSelectClick(model.id) }) {
                        Text("Usa")
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { onDeleteClick(model.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                    }
                } else if (state.isDownloading) {
                    val progressNormalized = (state.downloadProgress / 100f).coerceIn(0f, 1f)
                    CircularProgressIndicator(
                        progress = { progressNormalized },
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    IconButton(onClick = { onDownloadClick(model) }) {
                        Icon(Icons.Default.Download, contentDescription = "Scarica")
                    }
                }
            }
        }
    }
}
