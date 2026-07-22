package it.tldl.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import it.tldl.app.core.database.TranscriptionEntity
import it.tldl.app.core.stt.ModelInfo
import it.tldl.app.ui.ModelItemState
import it.tldl.app.ui.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        if (!isGranted) {
            Toast.makeText(this, "Permesso notifiche negato", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            it.tldl.app.ui.theme.TLDLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

enum class AppTab(val title: String) {
    MODELS("Modelli IA"),
    HISTORY("Cronologia")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SettingsViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(AppTab.MODELS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedTab == AppTab.MODELS) "TL;DL - Gestione Modelli" else "TL;DL - Cronologia Trascrizioni",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.MODELS,
                    onClick = { selectedTab = AppTab.MODELS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(AppTab.MODELS.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.HISTORY,
                    onClick = { selectedTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(AppTab.HISTORY.title) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.MODELS -> SettingsScreen(viewModel)
                AppTab.HISTORY -> HistoryScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val transcriptionModels by viewModel.transcriptionModels.collectAsState()
    val cleaningModels by viewModel.cleaningModels.collectAsState()
    val selectedCleanerId by viewModel.selectedTextCleanerId.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()
    val isCleanerEnabled by viewModel.isTextCleanerEnabled.collectAsState()
    val isHistoryOptIn by viewModel.isHistoryOptInEnabled.collectAsState()

    val onDownload = remember(viewModel) { { info: ModelInfo -> viewModel.downloadModel(info) } }
    val onSelectSTT = remember(viewModel) { { id: String -> viewModel.selectModel(id) } }
    val onSelectCleaner = remember(viewModel) { { id: String -> viewModel.selectTextCleaner(id) } }
    val onDelete = remember(viewModel) { { id: String -> viewModel.deleteModel(id) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RAM METRIC BANNER
        item(key = "ram_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RAM Disponibile nel Dispositivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${availableRam} MB",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // POST-PROCESSING SWITCH
        item(key = "cleaner_switch") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Post-Processing Pulizia Testo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Ripristina punteggiatura e rimuove intercalari automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isCleanerEnabled,
                        onCheckedChange = { viewModel.toggleTextCleaner(it) }
                    )
                }
            }
        }

        // HISTORY OPT-IN SWITCH
        item(key = "history_switch") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Salva Cronologia Locale Cifrata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Persiste le trascrizioni in un database Room locale cifrato con SQLCipher ed Android KeyStore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isHistoryOptIn,
                        onCheckedChange = { viewModel.toggleHistoryOptIn(it) }
                    )
                }
            }
        }

        // SECTION 1: TRASCRIZIONE AUDIO
        item(key = "header_stt") {
            Text(
                text = "1. Modelli di Trascrizione Vocale (STT)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
        item(key = "header_cleaner") {
            Text(
                text = "2. Modelli di Pulizia Testo & Punteggiatura",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Default Rule-based option
        item(key = "rules_cleaner") {
            val isRulesSelected = selectedCleanerId == "rules"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Engine Euristico a Regole (Integrato)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Leggerissimo e immediato. Rimuove intercalari (ehm, cioè) e formatta frasi.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        if (isRulesSelected) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Attivo") },
                                icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }

                    if (!isRulesSelected) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Dimensioni: ${model.sizeMb} MB | RAM richiesta: ${model.ramRequiredMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isSelected) {
                        AssistChip(
                            onClick = { },
                            label = { Text("In Uso") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        IconButton(onClick = { onDownloadClick(model) }) {
                            Icon(Icons.Default.Download, contentDescription = "Scarica", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // STATUS CHIPS ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.isIdealCap && !state.isSelected) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Consigliato") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                if (state.isSafe) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("RAM OK") },
                        icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("RAM Elevata") },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            if (state.isDownloading) {
                Spacer(Modifier.height(8.dp))
                val progressNormalized = (state.downloadProgress / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressNormalized },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(4.dp))
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: SettingsViewModel) {
    val historyItems by viewModel.historyItems.collectAsState()
    val isHistoryOptIn by viewModel.isHistoryOptInEnabled.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredItems = remember(historyItems, searchQuery) {
        if (searchQuery.isBlank()) historyItems
        else historyItems.filter { it.transcribedText.contains(searchQuery, ignoreCase = true) || it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isHistoryOptIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Salvataggio Cronologia Disattivato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Per persistere le trascrizioni in modo permanente e sicuro sul tuo dispositivo (cifrato con SQLCipher), abilita l'opzione 'Salva Cronologia Locale Cifrata' nella scheda Modelli.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca nelle trascrizioni...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(Modifier.height(12.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Nessun risultato trovato per '$searchQuery'" else "Nessuna trascrizione in cronologia",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredItems.size} trascrizioni salvate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Cancella Tutto", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = filteredItems,
                    key = { it.id }
                ) { item ->
                    HistoryItemCard(
                        item = item,
                        onCopyClick = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Trascrizione", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Testo copiato!", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = { id ->
                            viewModel.deleteHistoryItem(id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: TranscriptionEntity,
    onCopyClick: (String) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestampMs) { dateFormat.format(Date(item.timestampMs)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { onDeleteClick(item.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina trascrizione",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.transcribedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onCopyClick(item.transcribedText) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copia")
                }
            }
        }
    }
}
