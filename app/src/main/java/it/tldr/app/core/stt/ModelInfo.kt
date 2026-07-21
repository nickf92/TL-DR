package it.tldr.app.core.stt

data class ModelInfo(
    val id: String,
    val name: String,
    val ramRequiredMb: Long,
    val sizeMb: Long = 100,
    val engine: String = "sherpa-onnx",
    val downloadUrl: String = "",
    val isIdealCap: Boolean = false
)
