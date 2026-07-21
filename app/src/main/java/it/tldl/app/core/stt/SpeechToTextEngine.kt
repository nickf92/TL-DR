package it.tldl.app.core.stt

import java.io.File

interface SpeechToTextEngine {
    fun initialize(modelDirectory: File)
    fun transcribeStream(samples: ShortArray, onProgress: (progressPercent: Int, partialText: String) -> Unit): String
    fun release()
}
