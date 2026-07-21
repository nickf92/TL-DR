package it.tldr.app.core.audio

import java.io.File

class AudioProcessor(
    private val primaryDecoder: AudioDecoder = MediaCodecAudioDecoder(),
    private val fallbackDecoder: AudioDecoder = FFmpegAudioDecoder()
) {

    fun processAudioFile(file: File): ShortArray {
        require(file.exists()) { "File audio non trovato: ${file.absolutePath}" }
        
        return try {
            if (primaryDecoder.canDecode(file)) {
                primaryDecoder.decodeToPcm(file)
            } else {
                fallbackDecoder.decodeToPcm(file)
            }
        } catch (e: Exception) {
            // Fallback automatico su FFmpeg se il decoder primario fallisce
            fallbackDecoder.decodeToPcm(file)
        }
    }
}
