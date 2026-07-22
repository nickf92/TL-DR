package it.tldl.app.core.stt

import it.tldl.app.core.audio.AudioProcessor
import it.tldl.app.core.database.HistoryRepository
import it.tldl.app.core.service.TranscriptionState
import java.io.File

class TranscriptionPipeline(
    private val audioProcessor: AudioProcessor = AudioProcessor(),
    private val sttEngine: SpeechToTextEngine = SherpaOnnxEngine(),
    private val modelManager: ModelManager,
    private val textCleaner: TextCleanerEngine = LocalTextCleaner(modelManager),
    private val historyRepository: HistoryRepository? = null
) {

    suspend fun execute(
        audioFile: File,
        onProgress: (TranscriptionState) -> Unit
    ): String {
        try {
            onProgress(TranscriptionState.Decoding)

            val pcmData = audioProcessor.processAudioFile(audioFile)

            onProgress(TranscriptionState.Transcribing(5, "Caricamento modello in RAM..."))

            val model = modelManager.getActiveModel()
                ?: throw IllegalStateException("Nessun modello scaricato. Apri l'app per scaricare un modello.")

            val modelPath = modelManager.getModelPath(model.id)
                ?: throw IllegalStateException("Modello ${model.name} non scaricato. Scaricalo nelle impostazioni.")

            onProgress(TranscriptionState.Transcribing(10, "Caricamento ${model.name} in RAM..."))
            sttEngine.initialize(modelPath)
            onProgress(TranscriptionState.Transcribing(20, "Modello pronto in RAM. Avvio trascrizione..."))

            val rawResult = sttEngine.transcribeStream(pcmData) { progress, partial ->
                onProgress(TranscriptionState.Transcribing(progress, partial))
            }

            val finalResult = if (modelManager.isTextCleanerEnabled()) {
                onProgress(TranscriptionState.Transcribing(98, "Pulizia testo con Post-Processor..."))
                textCleaner.cleanText(rawResult)
            } else {
                rawResult
            }

            historyRepository?.let { repo ->
                try {
                    repo.saveTranscription(audioFile.name, finalResult)
                } catch (t: Throwable) {
                    // Ignore optional history saving errors
                }
            }

            onProgress(TranscriptionState.Success(finalResult))
            return finalResult
        } finally {
            sttEngine.release()
            if (audioFile.exists()) {
                audioFile.delete()
            }
        }
    }
}
