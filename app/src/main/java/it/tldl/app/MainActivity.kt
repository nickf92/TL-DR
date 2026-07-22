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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SettingsViewModel = viewModel()) {
    var isSettingsOpen by remember { mutableStateOf(false) }
    val transcriptionModels by viewModel.transcriptionModels.collectAsState()
    val activeModelName = remember(transcriptionModels) {
        transcriptionModels.find { it.isSelected }?.info?.name
    }

    BackHandler(enabled = isSettingsOpen) {
        isSettingsOpen = false
    }

    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { isSettingsOpen = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "TL;DL - Cronologia",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeModelName != null) "Modello attivo: $activeModelName" else "Nessun modello attivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeModelName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSettingsOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Impostazioni"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { isSettingsOpen = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val transcriptionModels by viewModel.transcriptionModels.collectAsState()
    val cleaningModels by viewModel.cleaningModels.collectAsState()
    val selectedCleanerId by viewModel.selectedTextCleanerId.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()
    val isCleanerEnabled by viewModel.isTextCleanerEnabled.collectAsState()
    val isHistoryOptIn by viewModel.isHistoryOptInEnabled.collectAsState()
    val context = LocalContext.current

    val onDownload = remember(viewModel) { { info: ModelInfo -> viewModel.downloadModel(info) } }
    val onSelectSTT = remember(viewModel) { { id: String -> viewModel.selectModel(id) } }
    val onSelectCleaner = remember(viewModel) { { id: String -> viewModel.selectTextCleaner(id) } }
    val onDelete = remember(viewModel) { { id: String -> viewModel.deleteModel(id) } }

    var modelToDelete by remember { mutableStateOf<ModelInfo?>(null) }

    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Eliminare il Modello ${model.name}?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Il file del modello verrà rimosso dal dispositivo. Per utilizzarlo di nuovo in futuro dovrai scaricarlo nuovamente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(model.id)
                        modelToDelete = null
                        Toast.makeText(context, "Modello eliminato", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Elimina Modello")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Impostazioni & Modelli IA",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna alla Cronologia"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // RAM METRIC BANNER
            item(key = "ram_header") {
                val ramPercentage = (availableRam.toFloat() / 4096f).coerceIn(0.1f, 1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = "Memoria RAM Disponibile nel Dispositivo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${availableRam} MB liberi",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { ramPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
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
            item(key = "history_opt_in_switch") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Salvataggio Cronologia Trascrizioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Mantiene uno storico cifrato in locale delle trascrizioni eseguite.",
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

            // SECTION 1: TRASCRIZIONE STT
            item(key = "header_stt") {
                Text(
                    text = "1. Modelli Trascrizione Vocale (STT)",
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
                    onDeleteClick = { id ->
                        val info = transcriptionModels.find { it.info.id == id }?.info
                        if (info != null) {
                            modelToDelete = info
                        } else {
                            onDelete(id)
                        }
                    }
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
fun HistoryScreen(
    viewModel: SettingsViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val isHistoryOptIn by viewModel.isHistoryOptInEnabled.collectAsState()
    val hasActiveSTTModel by viewModel.hasActiveSTTModel.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDetailItem by remember { mutableStateOf<TranscriptionEntity?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<TranscriptionEntity?>(null) }

    val context = LocalContext.current

    val filteredItems = remember(historyItems, searchQuery) {
        if (searchQuery.isBlank()) historyItems
        else historyItems.filter { it.transcribedText.contains(searchQuery, ignoreCase = true) || it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
        viewModel.refreshModels()
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Svuotare la Cronologia?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Sei sicuro di voler eliminare tutte le trascrizioni salvate? Questa azione non può essere annullata.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Cronologia svuotata", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Elimina Tutto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Eliminare Trascrizione?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Sei sicuro di voler eliminare questa trascrizione in modo permanente?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistoryItem(item.id)
                        if (selectedDetailItem?.id == item.id) {
                            selectedDetailItem = null
                        }
                        itemToDelete = null
                        Toast.makeText(context, "Trascrizione eliminata", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    selectedDetailItem?.let { item ->
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
        val formattedDate = remember(item.timestampMs) { dateFormat.format(Date(item.timestampMs)) }
        val wordCount = remember(item.transcribedText) {
            if (item.transcribedText.isBlank()) 0
            else item.transcribedText.trim().split("\\s+".toRegex()).size
        }
        val charCount = remember(item.transcribedText) { item.transcribedText.length }

        AlertDialog(
            onDismissRequest = { selectedDetailItem = null },
            icon = {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Trascrizione Vocale",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$wordCount parole") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$charCount caratteri") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
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
                            SelectionContainer {
                                Text(
                                    text = item.transcribedText.ifEmpty { "Nessun testo presente." },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ACTION ROW 1: Copia e Condividi (Equal Weight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, item.transcribedText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Condividi trascrizione"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Condividi")
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Trascrizione Vocale", item.transcribedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Testo copiato negli appunti!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copia")
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ACTION ROW 2: Elimina (Destructive) e Chiudi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteHistoryItem(item.id)
                                selectedDetailItem = null
                                Toast.makeText(context, "Trascrizione eliminata", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Elimina")
                        }

                        TextButton(onClick = { selectedDetailItem = null }) {
                            Text("Chiudi")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // BANNER MODELLO MANCANTE
        if (!hasActiveSTTModel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Configurazione Modello Richiesta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nessun modello IA di trascrizione vocale è attualmente attivo sul tuo dispositivo. Scarica e seleziona un modello nelle impostazioni per iniziare a trascrivere file audio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer,
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Configura Modelli Ora")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

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
                        "Per persistere le trascrizioni in modo permanente e sicuro sul tuo dispositivo (cifrato con SQLCipher), abilita l'opzione nelle impostazioni.",
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
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancella ricerca")
                    }
                }
            } else null,
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
                if (searchQuery.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Nessun risultato trovato per '$searchQuery'",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Nessuna trascrizione in cronologia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Condividi un file audio o una nota vocale da WhatsApp, Telegram o dal Registratore Vocale verso l'app TL;DL per trascriverlo automaticamente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
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
                TextButton(onClick = { showClearHistoryDialog = true }) {
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
                        onItemClick = { selectedDetailItem = it },
                        onCopyClick = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Trascrizione", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Testo copiato!", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = { id ->
                            val entity = filteredItems.find { it.id == id }
                            if (entity != null) itemToDelete = entity
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
    onItemClick: (TranscriptionEntity) -> Unit,
    onCopyClick: (String) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestampMs) { dateFormat.format(Date(item.timestampMs)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(item) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.durationSeconds > 0) {
                        Spacer(Modifier.width(8.dp))
                        val minutes = item.durationSeconds / 60
                        val seconds = item.durationSeconds % 60
                        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "⏱ $durationStr",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                maxLines = 4
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onCopyClick(item.transcribedText) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copia")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onItemClick(item) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apri")
                }
            }
        }
    }
}
