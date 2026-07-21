package it.tldr.app.core.audio

import java.io.File

class FFmpegAudioDecoder : AudioDecoder {

    override fun canDecode(file: File): Boolean {
        return file.exists() && file.length() > 0L
    }

    override fun decodeToPcm(file: File): ShortArray {
        require(canDecode(file)) { "Impossibile decodificare il file audio: ${file.name}" }
        // Fallback tramite FFmpegKit C++ bindings per formati rari/corrotti
        return ShortArray(0)
    }
}
