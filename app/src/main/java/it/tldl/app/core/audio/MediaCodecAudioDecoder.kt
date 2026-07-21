package it.tldl.app.core.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MediaCodecAudioDecoder : AudioDecoder {

    override fun canDecode(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val ext = file.extension.lowercase()
        return ext in listOf("opus", "ogg", "m4a", "mp3", "wav", "aac", "flac")
    }

    override suspend fun decodeToPcm(file: File): ShortArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)

        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }

        if (trackIndex < 0) {
            extractor.release()
            throw IllegalArgumentException("Nessuna traccia audio trovata in ${file.name}")
        }

        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
        
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val allSamples = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var isExtractorDone = false
        var isCodecDone = false

        while (!isCodecDone) {
            if (!isExtractorDone) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorDone = true
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                val data = ShortArray(bufferInfo.size / 2)
                outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data)
                allSamples.addAll(data.toList())
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isCodecDone = true
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return allSamples.toShortArray()
    }
}
