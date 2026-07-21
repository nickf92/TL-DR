package it.tldl.app.core.service

sealed class TranscriptionState {
    object Idle : TranscriptionState()
    object Decoding : TranscriptionState()
    data class Transcribing(val progress: Int, val partialText: String) : TranscriptionState()
    data class Success(val text: String) : TranscriptionState()
    data class Error(val message: String) : TranscriptionState()
}
