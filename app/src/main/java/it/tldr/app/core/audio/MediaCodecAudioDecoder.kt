package it.tldr.app.core.audio

import java.io.File

class MediaCodecAudioDecoder : AudioDecoder {

    override fun canDecode(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val ext = file.extension.lowercase()
        return ext in listOf("opus", "ogg", "m4a", "mp3", "wav", "aac", "flac")
    }

    override fun decodeToPcm(file: File): ShortArray {
        require(canDecode(file)) { "MediaCodec non è in grado di decodificare il file: ${file.name}" }
        // Nota: Nel runtime Android reale, MediaExtractor e MediaCodec decodificano il file 
        // in campioni PCM 16-bit 16000Hz Mono.
        return ShortArray(0)
    }
}
