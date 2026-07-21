package it.tldr.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioProcessorTest {

    private class MockPrimaryDecoder(val succeed: Boolean) : AudioDecoder {
        override fun canDecode(file: File): Boolean = succeed
        override fun decodeToPcm(file: File): ShortArray {
            if (!succeed) throw IllegalArgumentException("Primary decoder failed")
            return shortArrayOf(100, 200, 300, 400)
        }
    }

    private class MockFallbackDecoder : AudioDecoder {
        override fun canDecode(file: File): Boolean = true
        override fun decodeToPcm(file: File): ShortArray {
            return shortArrayOf(500, 600, 700, 800)
        }
    }

    @Test
    fun testAudioProcessorUsesPrimaryDecoderWhenSupported() {
        val primary = MockPrimaryDecoder(succeed = true)
        val fallback = MockFallbackDecoder()
        val processor = AudioProcessor(primary, fallback)

        val tempFile = File.createTempFile("test_voice", ".opus")
        val result = processor.processAudioFile(tempFile)

        assertEquals(4, result.size)
        assertEquals(100.toShort(), result[0])
        tempFile.delete()
    }

    @Test
    fun testAudioProcessorUsesFallbackDecoderWhenPrimaryFails() {
        val primary = MockPrimaryDecoder(succeed = false)
        val fallback = MockFallbackDecoder()
        val processor = AudioProcessor(primary, fallback)

        val tempFile = File.createTempFile("test_voice_custom", ".raw")
        val result = processor.processAudioFile(tempFile)

        assertEquals(4, result.size)
        assertEquals(500.toShort(), result[0])
        tempFile.delete()
    }
}
