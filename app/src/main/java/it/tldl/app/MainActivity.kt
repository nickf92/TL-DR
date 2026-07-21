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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val models by viewModel.models.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TL;DL - Impostazioni") })
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
                            Text("Pulizia Testo Sperimentale (LLM/Post-Processing)", style = MaterialTheme.typography.titleMedium)
                            Text("Rimuove intercalari (ehm, cioè), aggiunge punteggiatura e corregge le maiuscole.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isCleanerEnabled,
                            onCheckedChange = { viewModel.toggleTextCleaner(it) }
                        )
                    }
                }
            }

            items(models) { modelState ->
                ModelItem(
                    state = modelState,
                    onDownloadClick = { viewModel.downloadModel(modelState.info) },
                    onSelectClick = { viewModel.selectModel(modelState.info.id) },
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
                Text("RAM richiesta: ${model.ramRequiredMb}MB", style = MaterialTheme.typography.bodySmall)
                if (state.isSelected) {
                    Text("In Uso (Attivo)", color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                } else if (model.isIdealCap) {
                    Text("Consigliato (Smart Default)", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                }
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (state.isSelected) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("Attivo") }
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            } else if (state.isDownloaded) {
                OutlinedButton(onClick = onSelectClick) {
                    Text("Usa")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            } else if (state.isDownloading) {
                CircularProgressIndicator(
                    progress = { state.downloadProgress / 100f },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onDownloadClick) {
                    Icon(Icons.Default.Download, contentDescription = "Scarica")
                }
            }
        }
    }
}
