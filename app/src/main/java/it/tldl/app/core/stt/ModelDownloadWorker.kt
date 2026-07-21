package it.tldl.app.core.stt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "tldl_download_channel"
        const val NOTIFICATION_ID = 2001
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = inputData.getString("url") ?: return@withContext Result.failure()
        val modelId = inputData.getString("id") ?: return@withContext Result.failure()
        val fileName = inputData.getString("fileName") ?: return@withContext Result.failure()

        Log.d("ModelDownloadWorker", "Starting download: $fileName from $modelUrl")

        createNotificationChannel()

        val notificationId = NOTIFICATION_ID + (modelId.hashCode() and 0x7FFF)
        updateNotification(notificationId, modelId, fileName, 0)

        val modelsDir = File(applicationContext.filesDir, "models/$modelId")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, fileName)
        val tempFile = File(modelsDir, "$fileName.tmp")
        
        // Skip if already successfully downloaded
        if (targetFile.exists() && targetFile.length() > 1024) {
            Log.d("ModelDownloadWorker", "File $fileName already exists, skipping.")
            setProgress(workDataOf("progress" to 100))
            return@withContext Result.success()
        }
        
        val request = Request.Builder()
            .url(modelUrl)
            .header("User-Agent", "TLDL-Android-App/1.0")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ModelDownloadWorker", "Download failed with code: ${response.code} for $fileName")
                    return@withContext Result.failure()
                }

                val body = response.body
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                val input = body.byteStream()
                val output = FileOutputStream(tempFile)
                val buffer = ByteArray(32768) // Larger buffer for ONNX files
                var bytesRead: Int
                var lastProgress = 0

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    if (totalBytes > 0) {
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        if (progress - lastProgress >= 5 || progress == 100) {
                            lastProgress = progress
                            setProgress(workDataOf("progress" to progress))
                            updateNotification(notificationId, modelId, fileName, progress)
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                // Atomic rename to final filename with fallback copy
                val isSuccess = tempFile.renameTo(targetFile) || run {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    targetFile.exists()
                }

                if (isSuccess) {
                    Log.d("ModelDownloadWorker", "Download completed and renamed: $fileName")
                    showCompletionNotification(notificationId, modelId, fileName)
                    Result.success()
                } else {
                    Log.e("ModelDownloadWorker", "Failed to rename temp file for $fileName")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("ModelDownloadWorker", "Exception during download of $fileName: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Modelli TL;DL",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra l'avanzamento del download dei modelli AI per la trascrizione"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(notificationId: Int, modelId: String, fileName: String, progress: Int) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("TL;DL - Download Modello")
            .setContentText("Scaricando $fileName ($progress%)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

        manager.notify(notificationId, notification)

        // Try setting WorkManager foreground if allowed, safely ignoring any OS restrictions
        try {
            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(notificationId, notification)
            }
            setForegroundAsync(foregroundInfo)
        } catch (t: Throwable) {
            // Ignore OS foreground service restrictions if invoked in background
        }
    }

    private fun showCompletionNotification(notificationId: Int, modelId: String, fileName: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("TL;DL - Download Completato")
            .setContentText("File $fileName scaricato con successo.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}
