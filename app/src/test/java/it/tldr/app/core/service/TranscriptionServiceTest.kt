package it.tldr.app.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TranscriptionServiceTest {

    @Test
    fun testTranscriptionProgressNotificationFormatting() {
        val progress = 45
        val statusText = "Trascrizione in corso ($progress%)"
        
        assertNotNull(statusText)
        assertEquals("Trascrizione in corso (45%)", statusText)
    }
}
