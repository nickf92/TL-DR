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

import it.tldl.app.core.stt.TranscriptionPipeline

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTranscription(audioFile: File) {
        _state.value = TranscriptionState.Decoding
        currentTranscriptionJob = serviceScope.launch {
            try {
                val historyRepo = try {
                    it.tldl.app.core.database.HistoryRepository.getInstance(applicationContext)
                } catch (t: Throwable) { null }

                val pipeline = TranscriptionPipeline(
                    audioProcessor = audioProcessor,
                    sttEngine = sttEngine,
                    modelManager = modelManager,
                    textCleaner = textCleaner,
                    historyRepository = historyRepo
                )

                pipeline.execute(audioFile) { state ->
                    _state.value = state
                    when (state) {
                        is TranscriptionState.Decoding -> updateNotification(0, "Decodifica audio...")
                        is TranscriptionState.Transcribing -> updateNotification(state.progress, if (state.progress <= 5) state.partialText else "Trascrizione: ${state.progress}%")
                        is TranscriptionState.Success -> updateNotification(100, "Trascrizione completata! Tocca per aprire.", isFinished = true)
                        is TranscriptionState.Error -> updateNotification(0, "Errore: ${state.message}", isFinished = true)
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _state.value = TranscriptionState.Error(e.message ?: "Errore sconosciuto")
                    updateNotification(0, "Errore: ${e.message}", isFinished = true)
                }
            }
        }
    }

    private fun updateNotification(progress: Int, text: String, isFinished: Boolean = false) {
        try {
            val notification = buildNotification(progress, text, isFinished)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            // Ignore notification error if service unattached in tests
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Trascrizione Audio TL;DL",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mostra l'avanzamento della trascrizione vocale in background"
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            } catch (t: Throwable) {
                // Ignore channel creation error if unattached in tests
            }
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
            val cancelIntent = Intent(this, TranscriptionService::class.java).apply {
                action = ACTION_CANCEL
            }
            val cancelPendingIntent = android.app.PendingIntent.getService(
                this,
                1,
                cancelIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val isIndeterminate = progress <= 0
            builder.setProgress(100, progress, isIndeterminate)
            builder.setSubText(if (!isIndeterminate) "$progress%" else "In lavorazione")
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Annulla",
                cancelPendingIntent
            )
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
