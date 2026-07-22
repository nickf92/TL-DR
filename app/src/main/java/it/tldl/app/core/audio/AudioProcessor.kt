package it.tldl.app.core.audio

import java.io.File

open class AudioProcessor(
    private val primaryDecoder: AudioDecoder = MediaCodecAudioDecoder(),
    private val fallbackDecoder: AudioDecoder = FFmpegAudioDecoder()
) {

    open suspend fun processAudioFile(file: File): ShortArray {
        require(file.exists()) { "File audio non trovato: ${file.absolutePath}" }
        
        return try {
            if (primaryDecoder.canDecode(file)) {
                primaryDecoder.decodeToPcm(file)
            } else {
                fallbackDecoder.decodeToPcm(file)
            }
        } catch (e: Exception) {
            // Fallback automatico su FFmpeg se il decoder primario fallisce
            try {
                fallbackDecoder.decodeToPcm(file)
            } catch (fallbackEx: Exception) {
                throw Exception("Entrambi i decoder hanno fallito. Primario: ${e.message}, Fallback: ${fallbackEx.message}", fallbackEx)
            }
        }
    }
}
