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
        
        var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else 16000

        var channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val sampleChunks = mutableListOf<ShortArray>()
        var totalShortCount = 0
        val bufferInfo = MediaCodec.BufferInfo()
        var isExtractorDone = false
        var isCodecDone = false

        try {
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
                    outputBuffer.position(bufferInfo.offset)
                    val chunkShortCount = bufferInfo.size / 2
                    val data = ShortArray(chunkShortCount)
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data)
                    sampleChunks.add(data)
                    totalShortCount += chunkShortCount
                    
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isCodecDone = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }

        val rawPcm = ShortArray(totalShortCount)
        var pos = 0
        for (chunk in sampleChunks) {
            System.arraycopy(chunk, 0, rawPcm, pos, chunk.size)
            pos += chunk.size
        }

        val monoPcm = if (channelCount > 1) {
            val numFrames = rawPcm.size / channelCount
            ShortArray(numFrames) { i ->
                var sum = 0L
                for (c in 0 until channelCount) {
                    sum += rawPcm[i * channelCount + c]
                }
                (sum / channelCount).toShort()
            }
        } else {
            rawPcm
        }

        val targetSampleRate = 16000
        return if (sampleRate != targetSampleRate && sampleRate > 0) {
            resampleLinear(monoPcm, sampleRate, targetSampleRate)
        } else {
            monoPcm
        }
    }

    private fun resampleLinear(inputMono: ShortArray, srcSampleRate: Int, targetSampleRate: Int): ShortArray {
        if (srcSampleRate == targetSampleRate || inputMono.isEmpty()) return inputMono
        val ratio = srcSampleRate.toDouble() / targetSampleRate.toDouble()
        val outputLength = (inputMono.size / ratio).toInt()
        val output = ShortArray(outputLength)
        for (i in 0 until outputLength) {
            val srcPos = i * ratio
            val index = srcPos.toInt()
            val frac = srcPos - index
            if (index + 1 < inputMono.size) {
                val s1 = inputMono[index].toDouble()
                val s2 = inputMono[index + 1].toDouble()
                val interpolated = s1 + frac * (s2 - s1)
                output[i] = interpolated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else if (index < inputMono.size) {
                output[i] = inputMono[index]
            }
        }
        return output
    }
}
