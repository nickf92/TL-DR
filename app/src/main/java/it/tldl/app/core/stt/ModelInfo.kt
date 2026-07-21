package it.tldl.app.core.stt

enum class ModelType {
    ZIPFORMER,
    WHISPER
}

data class ModelInfo(
    val id: String,
    val name: String,
    val ramRequiredMb: Long,
    val sizeMb: Long = 100,
    val engine: String = "sherpa-onnx",
    val type: ModelType = ModelType.ZIPFORMER,
    val downloadUrl: String = "",
    val isIdealCap: Boolean = false,
    val requiredFiles: List<String> = listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
)
