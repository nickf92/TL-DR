package it.tldr.app.core.stt

import java.io.File

class SherpaOnnxEngine : SpeechToTextEngine {

    private var isInitialized = false

    override fun initialize(modelDirectory: File) {
        require(modelDirectory.exists()) { "Directory modello non trovata: ${modelDirectory.absolutePath}" }
        // Inizializzazione della sessione JNI C++ per sherpa-onnx
        isInitialized = true
    }

    override fun transcribeStream(
        samples: ShortArray,
        onProgress: (progressPercent: Int, partialText: String) -> Unit
    ): String {
        check(isInitialized) { "Motore sherpa-onnx non inizializzato" }
        
        // Simula la trascrizione a blocchi (chunk-based streaming)
        val chunkSize = 16000 // 1 secondo a 16kHz
        val totalChunks = (samples.size + chunkSize - 1) / chunkSize
        
        val sb = StringBuilder()
        for (i in 0 until totalChunks) {
            val percent = (((i + 1).toDouble() / totalChunks) * 100).toInt()
            onProgress(percent, sb.toString())
        }
        
        return sb.toString()
    }

    override fun release() {
        // Libera la memoria allocata sul lato nativo C++
        isInitialized = false
    }
}
