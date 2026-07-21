package it.tldl.app.core.database

class HistoryRepository(
    private val dao: TranscriptionDao? = null,
    var isOptInEnabled: Boolean = false
) {
    // RAM Session Store (Default behavior)
    private val sessionMemoryStore = mutableListOf<TranscriptionEntity>()

    suspend fun saveTranscription(fileName: String, text: String, durationSeconds: Int = 0) {
        val entity = TranscriptionEntity(
            fileName = fileName,
            transcribedText = text,
            durationSeconds = durationSeconds
        )

        // Salva sempre in RAM per la sessione corrente
        sessionMemoryStore.add(0, entity)

        // Salva nel database cifrato solo se l'utente ha attivato l'opt-in nelle impostazioni
        if (isOptInEnabled && dao != null) {
            dao.insert(entity)
        }
    }

    suspend fun getRecentTranscriptions(): List<TranscriptionEntity> {
        return if (isOptInEnabled && dao != null) {
            dao.getAll()
        } else {
            sessionMemoryStore.toList()
        }
    }

    fun clearSessionMemory() {
        sessionMemoryStore.clear()
    }
}
