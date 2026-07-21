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
                id = "zipformer-it-small",
                name = "Italiano (Veloce)",
                ramRequiredMb = 400,
                sizeMb = 120,
                isIdealCap = true,
                type = ModelType.ZIPFORMER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-asr-zipformer-it-2023-06-26/resolve/main/",
                requiredFiles = listOf("encoder-epoch-99-avg-1.int8.onnx", "decoder-epoch-99-avg-1.int8.onnx", "joiner-epoch-99-avg-1.int8.onnx", "tokens.txt")
            ),
            ModelInfo(
                id = "zipformer-it-large",
                name = "Italiano (Preciso)",
                ramRequiredMb = 1200,
                sizeMb = 450,
                isIdealCap = false,
                type = ModelType.ZIPFORMER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-asr-zipformer-it-2023-06-26/resolve/main/",
                requiredFiles = listOf("encoder-epoch-99-avg-1.onnx", "decoder-epoch-99-avg-1.onnx", "joiner-epoch-99-avg-1.onnx", "tokens.txt")
            ),
            ModelInfo(
                id = "whisper-tiny-en",
                name = "OpenAI Whisper Tiny (Inglese)",
                ramRequiredMb = 300,
                sizeMb = 40,
                isIdealCap = false,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny.en/resolve/main/",
                requiredFiles = listOf("tiny-encoder.int8.onnx", "tiny-decoder.int8.onnx", "tiny-tokens.txt")
            ),
            ModelInfo(
                id = "whisper-base-it",
                name = "OpenAI Whisper Base (Multilingua)",
                ramRequiredMb = 600,
                sizeMb = 150,
                isIdealCap = false,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main/",
                requiredFiles = listOf("base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt")
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
    
    fun getSmartDefaultModel(): ModelInfo {
        val freeRam = RamCalculator.getAvailableRamMb(context)
        val models = getAvailableModels().sortedByDescending { it.ramRequiredMb }
        
        // Trova il modello migliore che sta nella RAM, ma non superare l'IdealCap se possibile
        return models.firstOrNull { it.ramRequiredMb < freeRam * 0.7 } ?: models.last()
    }
}
