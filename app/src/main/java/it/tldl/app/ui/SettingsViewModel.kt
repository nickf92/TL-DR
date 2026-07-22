package it.tldl.app.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import it.tldl.app.core.stt.ModelCategory
import it.tldl.app.core.stt.ModelDownloadWorker
import it.tldl.app.core.stt.ModelInfo
import it.tldl.app.core.stt.ModelManager
import it.tldl.app.core.stt.RamCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
data class ModelItemState(
    val info: ModelInfo,
    val isDownloaded: Boolean,
    val isSelected: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isSafe: Boolean = true,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    private val workManager = WorkManager.getInstance(application)

    private val _transcriptionModels = MutableStateFlow<List<ModelItemState>>(emptyList())
    val transcriptionModels = _transcriptionModels.asStateFlow()

    private val _cleaningModels = MutableStateFlow<List<ModelItemState>>(emptyList())
    val cleaningModels = _cleaningModels.asStateFlow()

    private val _selectedTextCleanerId = MutableStateFlow("rules")
    val selectedTextCleanerId = _selectedTextCleanerId.asStateFlow()

    private val _availableRam = MutableStateFlow(0L)
    val availableRam = _availableRam.asStateFlow()

    private val _isTextCleanerEnabled = MutableStateFlow(false)
    val isTextCleanerEnabled = _isTextCleanerEnabled.asStateFlow()

    private val _isHistoryOptInEnabled = MutableStateFlow(false)
    val isHistoryOptInEnabled = _isHistoryOptInEnabled.asStateFlow()

    private val _historyItems = MutableStateFlow<List<it.tldl.app.core.database.TranscriptionEntity>>(emptyList())
    val historyItems = _historyItems.asStateFlow()

    private val prefs = application.getSharedPreferences("tldl_prefs", android.content.Context.MODE_PRIVATE)

    init {
        _availableRam.value = RamCalculator.getAvailableRamMb(application)
        _isTextCleanerEnabled.value = modelManager.isTextCleanerEnabled()
        _isHistoryOptInEnabled.value = prefs.getBoolean("enable_history_opt_in", false)
        refreshModels()
        loadHistory()
    }

    fun toggleTextCleaner(enabled: Boolean) {
        modelManager.setTextCleanerEnabled(enabled)
        _isTextCleanerEnabled.value = enabled
    }

    fun toggleHistoryOptIn(enabled: Boolean) {
        prefs.edit().putBoolean("enable_history_opt_in", enabled).apply()
        _isHistoryOptInEnabled.value = enabled
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val repo = it.tldl.app.core.database.HistoryRepository.getInstance(getApplication())
                repo.isOptInEnabled = _isHistoryOptInEnabled.value
                _historyItems.value = repo.getRecentTranscriptions()
            } catch (t: Throwable) {
                // Silently ignore DB load failure if uninitialized
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            val available = modelManager.getAvailableModels()
            val activeSTT = modelManager.getActiveModel()
            val activeCleanerId = modelManager.getSelectedTextCleanerId()
            val currentRam = _availableRam.value

            _selectedTextCleanerId.value = activeCleanerId

            val allStates = available.map { info ->
                val downloaded = modelManager.isModelDownloaded(info.id)
                val isSelected = if (info.category == ModelCategory.TRANSCRIPTION) {
                    downloaded && activeSTT?.id == info.id
                } else {
                    downloaded && activeCleanerId == info.id
                }
                ModelItemState(
                    info = info,
                    isDownloaded = downloaded,
                    isSelected = isSelected,
                    isSafe = RamCalculator.isSafeForDevice(info, currentRam)
                )
            }

            _transcriptionModels.value = allStates.filter { it.info.category == ModelCategory.TRANSCRIPTION }
            _cleaningModels.value = allStates.filter { it.info.category == ModelCategory.TEXT_CLEANING }
        }
    }

    fun selectModel(modelId: String) {
        if (modelManager.isModelDownloaded(modelId)) {
            modelManager.setSelectedModel(modelId)
            refreshModels()
        }
    }

    fun selectTextCleaner(cleanerId: String) {
        if (cleanerId == "rules" || modelManager.isModelDownloaded(cleanerId)) {
            modelManager.setSelectedTextCleanerId(cleanerId)
            refreshModels()
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId)
            refreshModels()
        }
    }

    private val downloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    fun downloadModel(model: ModelInfo) {
        if (modelManager.isModelDownloaded(model.id)) return
        
        Log.d("SettingsViewModel", "Enqueuing download for model: ${model.id}")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requests = model.requiredFiles.map { fileName ->
            OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    "url" to model.downloadUrl + fileName,
                    "id" to model.id,
                    "fileName" to fileName
                ))
                .addTag("download_${model.id}")
                .build()
        }

        if (requests.isEmpty()) return

        var continuation = workManager.beginUniqueWork(
            "download_${model.id}",
            ExistingWorkPolicy.REPLACE,
            requests[0]
        )
        
        for (i in 1 until requests.size) {
            continuation = continuation.then(requests[i])
        }
        
        continuation.enqueue()
        
        downloadJobs[model.id]?.cancel()
        downloadJobs[model.id] = viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download_${model.id}").collect { workInfos ->
                if (workInfos.isNotEmpty()) {
                    val finishedCount = workInfos.count { it.state == WorkInfo.State.SUCCEEDED }
                    val currentRunning = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    val currentProgress = currentRunning?.progress?.getInt("progress", 0) ?: 0
                    
                    val totalProgress = ((finishedCount * 100 + currentProgress) / requests.size)
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    val hasFailed = workInfos.any { it.state == WorkInfo.State.FAILED }

                    val errorMsg = if (hasFailed) "Errore durante il download. Riprova." else null
                    
                    val currentItem = _transcriptionModels.value.find { it.info.id == model.id }
                        ?: _cleaningModels.value.find { it.info.id == model.id }

                    val shouldUpdate = currentItem == null || 
                        currentItem.isDownloading != isRunning || 
                        abs(currentItem.downloadProgress - totalProgress) >= 2 || 
                        (hasFailed && currentItem.error == null)

                    if (shouldUpdate) {
                        updateModelState(model.id, isDownloading = isRunning, progress = totalProgress, error = errorMsg)
                    }
                    
                    if (workInfos.all { it.state == WorkInfo.State.SUCCEEDED }) {
                        refreshModels()
                    }
                }
            }
        }
    }

    private fun updateModelState(modelId: String, isDownloading: Boolean, progress: Int, error: String?) {
        _transcriptionModels.value = _transcriptionModels.value.map { 
            if (it.info.id == modelId) it.copy(isDownloading = isDownloading, downloadProgress = progress, error = error)
            else it
        }
        _cleaningModels.value = _cleaningModels.value.map { 
            if (it.info.id == modelId) it.copy(isDownloading = isDownloading, downloadProgress = progress, error = error)
            else it
        }
    }
}
