package it.tldl.app.core.stt

import android.content.Context
import java.io.File

class ModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models")

    init {
        if (!modelsDir.exists()) modelsDir.mkdirs()
    }

    fun getAvailableModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "whisper-tiny",
                name = "Whisper Tiny (Multilingua - Veloce)",
                ramRequiredMb = 300,
                sizeMb = 45,
                isIdealCap = false,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/",
                requiredFiles = listOf("tiny-encoder.int8.onnx", "tiny-decoder.int8.onnx", "tiny-tokens.txt")
            ),
            ModelInfo(
                id = "whisper-base",
                name = "Whisper Base (Multilingua - Consigliato)",
                ramRequiredMb = 600,
                sizeMb = 150,
                isIdealCap = true,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main/",
                requiredFiles = listOf("base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt")
            ),
            ModelInfo(
                id = "whisper-small",
                name = "Whisper Small (Multilingua - Alta Precisione)",
                ramRequiredMb = 1200,
                sizeMb = 375,
                isIdealCap = false,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main/",
                requiredFiles = listOf("small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt")
            )
        )
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val modelFolder = File(modelsDir, modelId)
        val info = getAvailableModels().find { it.id == modelId } ?: return false
        return info.requiredFiles.all { File(modelFolder, it).exists() }
    }

    fun getModelPath(modelId: String): File? {
        val modelFolder = File(modelsDir, modelId)
        return if (isModelDownloaded(modelId)) modelFolder else null
    }
    
    private val prefs = context.getSharedPreferences("tldl_prefs", Context.MODE_PRIVATE)

    fun getSelectedModelId(): String? {
        return prefs.getString("selected_model_id", null)
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString("selected_model_id", modelId).apply()
    }

    fun deleteModel(modelId: String): Boolean {
        val modelFolder = File(modelsDir, modelId)
        val success = if (modelFolder.exists()) modelFolder.deleteRecursively() else false
        if (getSelectedModelId() == modelId) {
            prefs.edit().remove("selected_model_id").apply()
        }
        return success
    }

    fun getActiveModel(): ModelInfo? {
        val selectedId = getSelectedModelId()
        if (selectedId != null && isModelDownloaded(selectedId)) {
            getAvailableModels().find { it.id == selectedId }?.let { return it }
        }

        val downloaded = getAvailableModels().filter { isModelDownloaded(it.id) }
        if (downloaded.isEmpty()) return null

        val freeRam = RamCalculator.getAvailableRamMb(context)
        return RamCalculator.selectSmartDefaultModel(downloaded, freeRam) ?: downloaded.first()
    }
}
