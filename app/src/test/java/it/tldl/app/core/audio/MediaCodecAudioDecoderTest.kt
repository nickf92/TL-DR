package it.tldl.app.core.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MediaCodecAudioDecoderTest {

    private val decoder = MediaCodecAudioDecoder()

    @Test
    fun `canDecode should return true for valid extensions`() {
        val file = File.createTempFile("test", ".opus")
        file.writeText("audio data")
        assertTrue(decoder.canDecode(file))
        file.delete()
    }

    @Test
    fun `canDecode should return false for invalid extensions`() {
        val file = File.createTempFile("test", ".txt")
        file.writeText("text data")
        assertFalse(decoder.canDecode(file))
        file.delete()
    }
}
