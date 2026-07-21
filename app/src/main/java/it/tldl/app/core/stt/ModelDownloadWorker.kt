package it.tldl.app.core.stt

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
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

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    if (totalBytes > 0) {
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        setProgress(workDataOf("progress" to progress))
                    }
                }

                output.flush()
                output.close()
                input.close()

                // Atomic rename to final filename
                if (tempFile.renameTo(targetFile)) {
                    Log.d("ModelDownloadWorker", "Download completed and renamed: $fileName")
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
}
