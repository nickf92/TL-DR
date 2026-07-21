package it.tldl.app.core.audio

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

class FFmpegAudioDecoder : AudioDecoder {

    override fun canDecode(file: File): Boolean {
        return file.exists() && file.length() > 0L
    }

    override suspend fun decodeToPcm(file: File): ShortArray {
        require(canDecode(file)) { "Impossibile decodificare il file audio: ${file.name}" }
        
        val outputRaw = File(file.parent, "${file.nameWithoutExtension}_temp.raw")
        if (outputRaw.exists()) outputRaw.delete()

        // Comando FFmpeg per convertire in PCM 16-bit 16kHz Mono RAW
        val command = "-y -i \"${file.absolutePath}\" -f s16le -acodec pcm_s16le -ar 16000 -ac 1 \"${outputRaw.absolutePath}\""
        
        return suspendCancellableCoroutine { continuation ->
            val session = FFmpegKit.executeAsync(command) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val bytes = outputRaw.readBytes()
                    val shorts = ShortArray(bytes.size / 2)
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                    outputRaw.delete()
                    continuation.resume(shorts)
                } else {
                    continuation.resume(ShortArray(0))
                }
            }
            
            continuation.invokeOnCancellation {
                FFmpegKit.cancel(session.sessionId)
                if (outputRaw.exists()) outputRaw.delete()
            }
        }
    }
}
