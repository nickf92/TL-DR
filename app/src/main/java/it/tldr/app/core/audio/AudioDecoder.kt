package it.tldr.app.core.audio

import java.io.File

interface AudioDecoder {
    fun canDecode(file: File): Boolean
    fun decodeToPcm(file: File): ShortArray
}
