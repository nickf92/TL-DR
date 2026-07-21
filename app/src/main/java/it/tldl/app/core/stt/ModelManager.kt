package it.tldl.app.core.stt

import android.content.Context
import java.io.File

class ModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models")
    private val prefs = context.getSharedPreferences("tldl_prefs", Context.MODE_PRIVATE)

    init {
        if (!modelsDir.exists()) modelsDir.mkdirs()
    }

    fun getAvailableModels(): List<ModelInfo> {
        return listOf(
            // --- TRASCRIZIONE AUDIO ---
            ModelInfo(
                id = "whisper-base",
                name = "Whisper Base (Multilingua - Consigliato)",
                ramRequiredMb = 600,
                sizeMb = 150,
                category = ModelCategory.TRANSCRIPTION,
                type = ModelType.WHISPER,
                isIdealCap = true,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main/",
                requiredFiles = listOf("base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt")
            ),
            ModelInfo(
                id = "whisper-small",
                name = "Whisper Small (Multilingua - Alta Precisione)",
                ramRequiredMb = 1200,
                sizeMb = 375,
                category = ModelCategory.TRANSCRIPTION,
                type = ModelType.WHISPER,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main/",
                requiredFiles = listOf("small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt")
            ),

            // --- PULIZIA TESTO & PUNTEGGIATURA ---
            ModelInfo(
                id = "punct-onnx",
                name = "ONNX Punctuation (Ripristino Punteggiatura IA)",
                ramRequiredMb = 100,
                sizeMb = 16,
                category = ModelCategory.TEXT_CLEANING,
                type = ModelType.PUNCTUATION_ONNX,
                downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12/resolve/main/",
                requiredFiles = listOf("model.onnx")
            ),
            ModelInfo(
                id = "smollm-onnx",
                name = "SmolLM2-135M (Mini LLM Locale Riscrittura Testo)",
                ramRequiredMb = 300,
                sizeMb = 135,
                category = ModelCategory.TEXT_CLEANING,
                type = ModelType.SMOLLM_ONNX,
                downloadUrl = "https://huggingface.co/onnx-community/SmolLM2-135M-Instruct-ONNX/resolve/main/",
                requiredFiles = listOf("onnx/model_quantized.onnx", "tokenizer.json")
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

    fun isTextCleanerEnabled(): Boolean {
        return prefs.getBoolean("enable_text_cleaner", false)
    }

    fun setTextCleanerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enable_text_cleaner", enabled).apply()
    }

    fun getSelectedModelId(): String? {
        return prefs.getString("selected_model_id", null)
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString("selected_model_id", modelId).apply()
    }

    fun getSelectedTextCleanerId(): String {
        return prefs.getString("selected_text_cleaner_id", "rules") ?: "rules"
    }

    fun setSelectedTextCleanerId(cleanerId: String) {
        prefs.edit().putString("selected_text_cleaner_id", cleanerId).apply()
    }

    fun getCustomCleanerPrompt(): String {
        return prefs.getString(
            "custom_cleaner_prompt",
            "Correggi la punteggiatura, gli errori di sintassi e rimuovi gli intercalari dal seguente testo in italiano senza modificarne il significato."
        ) ?: "Correggi la punteggiatura, gli errori di sintassi e rimuovi gli intercalari dal seguente testo in italiano senza modificarne il significato."
    }

    fun setCustomCleanerPrompt(prompt: String) {
        prefs.edit().putString("custom_cleaner_prompt", prompt).apply()
    }

    fun deleteModel(modelId: String): Boolean {
        val modelFolder = File(modelsDir, modelId)
        val success = if (modelFolder.exists()) modelFolder.deleteRecursively() else false
        if (getSelectedModelId() == modelId) {
            prefs.edit().remove("selected_model_id").apply()
        }
        if (getSelectedTextCleanerId() == modelId) {
            prefs.edit().putString("selected_text_cleaner_id", "rules").apply()
        }
        return success
    }

    fun getActiveModel(): ModelInfo? {
        val selectedId = getSelectedModelId()
        if (selectedId != null && isModelDownloaded(selectedId)) {
            getAvailableModels().find { it.id == selectedId }?.let { return it }
        }

        val downloaded = getAvailableModels()
            .filter { it.category == ModelCategory.TRANSCRIPTION }
            .filter { isModelDownloaded(it.id) }
            
        if (downloaded.isEmpty()) return null

        val freeRam = RamCalculator.getAvailableRamMb(context)
        return RamCalculator.selectSmartDefaultModel(downloaded, freeRam) ?: downloaded.first()
    }
}
