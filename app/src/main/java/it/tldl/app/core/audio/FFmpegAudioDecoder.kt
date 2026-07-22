package it.tldl.app.core.audio

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FFmpegAudioDecoder : AudioDecoder {

    override fun canDecode(file: File): Boolean {
        return file.exists() && file.length() > 0L
    }

    override suspend fun decodeToPcm(file: File): ShortArray {
        require(canDecode(file)) { "Impossibile decodificare il file audio: ${file.name}" }
        
        val tempDir = file.parentFile ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val outputRaw = try {
            File.createTempFile("ffmpeg_pcm_", ".raw", tempDir)
        } catch (e: Exception) {
            File.createTempFile("ffmpeg_pcm_", ".raw")
        }

        val escapedInput = file.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedOutput = outputRaw.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")

        // Comando FFmpeg per convertire in PCM 16-bit 16kHz Mono RAW
        val command = "-y -i \"$escapedInput\" -f s16le -acodec pcm_s16le -ar 16000 -ac 1 \"$escapedOutput\""
        
        return suspendCancellableCoroutine { continuation ->
            val session = FFmpegKit.executeAsync(command) { session ->
                try {
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        val fileLength = outputRaw.length()
                        val numShorts = (fileLength / 2).toInt()
                        val shorts = ShortArray(numShorts)
                        
                        java.io.FileInputStream(outputRaw).use { fis ->
                            val byteBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
                            val byteArray = byteBuffer.array()
                            var bytesRead: Int
                            var shortsReadTotal = 0
                            while (fis.read(byteArray).also { bytesRead = it } != -1) {
                                byteBuffer.rewind()
                                byteBuffer.limit(bytesRead)
                                val shortBuf = byteBuffer.asShortBuffer()
                                val count = shortBuf.remaining()
                                if (shortsReadTotal + count <= shorts.size) {
                                    shortBuf.get(shorts, shortsReadTotal, count)
                                    shortsReadTotal += count
                                }
                            }
                        }
                        if (outputRaw.exists()) outputRaw.delete()
                        continuation.resume(shorts)
                    } else {
                        if (outputRaw.exists()) outputRaw.delete()
                        continuation.resumeWithException(IllegalStateException("Decodifica FFmpeg fallita con return code: ${session.returnCode}"))
                    }
                } catch (t: Throwable) {
                    if (outputRaw.exists()) outputRaw.delete()
                    continuation.resumeWithException(t)
                }
            }
            
            continuation.invokeOnCancellation {
                FFmpegKit.cancel(session.sessionId)
                if (outputRaw.exists()) outputRaw.delete()
            }
        }
    }
}
