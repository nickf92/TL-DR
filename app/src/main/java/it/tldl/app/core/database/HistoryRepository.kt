package it.tldl.app.core.database

class HistoryRepository(
    private val context: android.content.Context? = null,
    private val dao: TranscriptionDao? = null,
    isOptInEnabled: Boolean = true
) {
    // RAM Session Store (Default behavior)
    private val sessionMemoryStore = java.util.Collections.synchronizedList(mutableListOf<TranscriptionEntity>())

    private var _isOptInEnabledState: Boolean = isOptInEnabled

    var isOptInEnabled: Boolean
        get() {
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("tldl_prefs", android.content.Context.MODE_PRIVATE)
                return prefs.getBoolean("enable_history_opt_in", true)
            }
            return _isOptInEnabledState
        }
        set(value) {
            _isOptInEnabledState = value
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("tldl_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("enable_history_opt_in", value).apply()
            }
        }

    suspend fun saveTranscription(fileName: String, text: String, durationSeconds: Int = 0) {
        val entity = TranscriptionEntity(
            fileName = fileName,
            transcribedText = text,
            durationSeconds = durationSeconds
        )

        // Salva sempre in RAM per la sessione corrente
        synchronized(sessionMemoryStore) {
            sessionMemoryStore.add(0, entity)
        }

        // Salva nel database cifrato solo se l'utente ha attivato l'opt-in nelle impostazioni
        if (isOptInEnabled && dao != null) {
            dao.insert(entity)
        }
    }

    suspend fun getRecentTranscriptions(): List<TranscriptionEntity> {
        return if (isOptInEnabled && dao != null) {
            dao.getAll()
        } else {
            synchronized(sessionMemoryStore) {
                sessionMemoryStore.toList()
            }
        }
    }

    suspend fun deleteTranscription(id: Long) {
        synchronized(sessionMemoryStore) {
            sessionMemoryStore.removeAll { it.id == id }
        }
        if (isOptInEnabled && dao != null) {
            dao.deleteById(id)
        }
    }

    suspend fun clearAllTranscriptions() {
        synchronized(sessionMemoryStore) {
            sessionMemoryStore.clear()
        }
        if (isOptInEnabled && dao != null) {
            dao.clearAll()
        }
    }

    fun clearSessionMemory() {
        synchronized(sessionMemoryStore) {
            sessionMemoryStore.clear()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HistoryRepository? = null

        fun getInstance(context: android.content.Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val prefs = appContext.getSharedPreferences("tldl_prefs", android.content.Context.MODE_PRIVATE)
                val isOptIn = prefs.getBoolean("enable_history_opt_in", true)
                val db = DatabaseProvider.getDatabase(appContext)
                val instance = HistoryRepository(context = appContext, dao = db.transcriptionDao(), isOptInEnabled = isOptIn)
                INSTANCE = instance
                instance
            }
        }
    }
}
