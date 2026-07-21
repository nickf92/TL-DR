package it.tldl.app.core.audio

import java.io.File

interface AudioDecoder {
    fun canDecode(file: File): Boolean
    suspend fun decodeToPcm(file: File): ShortArray
}
