package it.tldl.app.core.audio

import java.io.File

/**
 * Deep AudioEngine module that consolidates audio decoding, hardware MediaCodec
 * extraction, FFmpeg fallback, mono downmixing, 16kHz resampling, and temp file cleanup.
 */
open class AudioEngine(
    private val primaryDecoder: AudioDecoder = MediaCodecAudioDecoder(),
    private val fallbackDecoder: AudioDecoder = FFmpegAudioDecoder()
) {

    open suspend fun decodeToPcm(file: File): ShortArray {
        require(file.exists()) { "File audio non trovato: ${file.absolutePath}" }

        return try {
            if (primaryDecoder.canDecode(file)) {
                primaryDecoder.decodeToPcm(file)
            } else {
                fallbackDecoder.decodeToPcm(file)
            }
        } catch (e: Exception) {
            try {
                fallbackDecoder.decodeToPcm(file)
            } catch (fallbackEx: Exception) {
                throw Exception("Entrambi i decoder hanno fallito. Primario: ${e.message}, Fallback: ${fallbackEx.message}", fallbackEx)
            }
        }
    }

    open suspend fun processAudioFile(file: File): ShortArray = decodeToPcm(file)
}
