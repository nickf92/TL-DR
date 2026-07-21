package it.tldl.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.tldl.app.core.audio.AudioProcessor
import it.tldl.app.core.stt.LocalTextCleaner
import it.tldl.app.core.stt.ModelManager
import it.tldl.app.core.stt.SherpaOnnxEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class TranscriptionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val audioProcessor = AudioProcessor()
    private val sttEngine = SherpaOnnxEngine()
    private lateinit var modelManager: ModelManager
    private val textCleaner by lazy { LocalTextCleaner(modelManager) }
    private var currentTranscriptionJob: Job? = null

    companion object {
        const val CHANNEL_ID = "tldl_transcription_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH"

        private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
        val state = _state.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        modelManager = ModelManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH)
                val notification = buildNotification(0, "Preparazione trascrizione...")
                startForeground(NOTIFICATION_ID, notification)

                if (audioPath != null) {
                    startTranscription(File(audioPath))
                }
            }
            ACTION_CANCEL -> {
                currentTranscriptionJob?.cancel()
                _state.value = TranscriptionState.Idle
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTranscription(audioFile: File) {
        _state.value = TranscriptionState.Decoding
        currentTranscriptionJob = serviceScope.launch {
            try {
                updateNotification(0, "Decodifica audio...")

                val pcmData = audioProcessor.processAudioFile(audioFile)
                
                _state.value = TranscriptionState.Transcribing(5, "Caricamento modello in RAM...")
                updateNotification(5, "Caricamento modello in memoria RAM...")

                val model = modelManager.getActiveModel()
                if (model == null) {
                    throw IllegalStateException("Nessun modello scaricato. Apri l'app per scaricare un modello.")
                }

                val modelPath = modelManager.getModelPath(model.id)
                if (modelPath == null) {
                    throw IllegalStateException("Modello ${model.name} non scaricato. Scaricalo nelle impostazioni.")
                }

                updateNotification(10, "Caricamento ${model.name} in RAM...")
                sttEngine.initialize(modelPath)
                updateNotification(20, "Modello pronto in RAM. Avvio trascrizione...")

                val rawResult = sttEngine.transcribeStream(pcmData) { progress, partial ->
                    _state.value = TranscriptionState.Transcribing(progress, partial)
                    updateNotification(progress, "Trascrizione: $progress%")
                }

                val finalResult = if (modelManager.isTextCleanerEnabled()) {
                    updateNotification(98, "Pulizia testo con Post-Processor...")
                    textCleaner.cleanText(rawResult)
                } else {
                    rawResult
                }

                _state.value = TranscriptionState.Success(finalResult)
                updateNotification(100, "Trascrizione completata! Tocca per aprire.", isFinished = true)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _state.value = TranscriptionState.Error(e.message ?: "Errore sconosciuto")
                    updateNotification(0, "Errore: ${e.message}", isFinished = true)
                }
            } finally {
                if (audioFile.exists()) {
                    audioFile.delete()
                }
            }
        }
    }

    private fun updateNotification(progress: Int, text: String, isFinished: Boolean = false) {
        val notification = buildNotification(progress, text, isFinished)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trascrizione Audio TL;DL",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra l'avanzamento della trascrizione vocale in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, statusText: String, isFinished: Boolean = false): Notification {
        val openIntent = Intent(this, it.tldl.app.TransparentShareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TL;DL Trascrizione Vocale")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(!isFinished)
            .setAutoCancel(isFinished)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (!isFinished) {
            val isIndeterminate = progress <= 0
            builder.setProgress(100, progress, isIndeterminate)
            builder.setSubText(if (!isIndeterminate) "$progress%" else "In lavorazione")
        } else {
            builder.setProgress(0, 0, false)
            builder.setSubText("Completato")
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "Apri",
                pendingIntent
            )
        }

        return builder.build()
    }

}
