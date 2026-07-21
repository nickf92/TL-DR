package it.tldl.app.core.stt

enum class ModelType {
    ZIPFORMER,
    WHISPER,
    PUNCTUATION_ONNX,
    SMOLLM_ONNX
}

enum class ModelCategory {
    TRANSCRIPTION,
    TEXT_CLEANING
}

data class ModelInfo(
    val id: String,
    val name: String,
    val ramRequiredMb: Long,
    val sizeMb: Long = 100,
    val engine: String = "sherpa-onnx",
    val type: ModelType = ModelType.WHISPER,
    val category: ModelCategory = ModelCategory.TRANSCRIPTION,
    val downloadUrl: String = "",
    val isIdealCap: Boolean = false,
    val requiredFiles: List<String> = listOf("encoder.onnx", "decoder.onnx", "tokens.txt")
)
