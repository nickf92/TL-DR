package it.tldl.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackStateTest {

    @Test
    fun testAudioPlaybackStateTogglesCorrectly() {
        var isPlaying = false
        val togglePlayback = {
            isPlaying = !isPlaying
        }

        assertFalse(isPlaying)
        togglePlayback()
        assertTrue(isPlaying)
        togglePlayback()
        assertFalse(isPlaying)
    }
}
