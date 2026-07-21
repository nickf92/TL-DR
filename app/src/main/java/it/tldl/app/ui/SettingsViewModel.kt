package it.tldl.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import it.tldl.app.core.stt.ModelDownloadWorker
import it.tldl.app.core.stt.ModelInfo
import it.tldl.app.core.stt.ModelManager
import it.tldl.app.core.stt.RamCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModelItemState(
    val info: ModelInfo,
    val isDownloaded: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    private val workManager = WorkManager.getInstance(application)

    private val _models = MutableStateFlow<List<ModelItemState>>(emptyList())
    val models = _models.asStateFlow()

    private val _availableRam = MutableStateFlow(0L)
    val availableRam = _availableRam.asStateFlow()

    init {
        refreshModels()
        _availableRam.value = RamCalculator.getAvailableRamMb(application)
    }

    fun refreshModels() {
        viewModelScope.launch {
            val available = modelManager.getAvailableModels()
            _models.value = available.map { info ->
                ModelItemState(
                    info = info,
                    isDownloaded = modelManager.isModelDownloaded(info.id)
                )
            }
        }
    }

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
            ExistingWorkPolicy.REPLACE, // Permette di riavviare se bloccato
            requests[0]
        )
        
        for (i in 1 until requests.size) {
            continuation = continuation.then(requests[i])
        }
        
        continuation.enqueue()
        
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download_${model.id}").collect { workInfos ->
                if (workInfos.isNotEmpty()) {
                    val finishedCount = workInfos.count { it.state == WorkInfo.State.SUCCEEDED }
                    val currentRunning = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    val currentProgress = currentRunning?.progress?.getInt("progress", 0) ?: 0
                    
                    val totalProgress = ((finishedCount * 100 + currentProgress) / requests.size)
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    val hasFailed = workInfos.any { it.state == WorkInfo.State.FAILED }

                    val errorMsg = if (hasFailed) "Errore durante il download. Riprova." else null
                    
                    updateModelState(model.id, isDownloading = isRunning, progress = totalProgress, error = errorMsg)
                    
                    if (workInfos.all { it.state == WorkInfo.State.SUCCEEDED }) {
                        refreshModels()
                    }
                }
            }
        }
    }

    private fun updateModelState(modelId: String, isDownloading: Boolean, progress: Int, error: String?) {
        _models.value = _models.value.map { 
            if (it.info.id == modelId) it.copy(
                isDownloading = isDownloading, 
                downloadProgress = progress,
                error = error
            )
            else it
        }
    }
}
