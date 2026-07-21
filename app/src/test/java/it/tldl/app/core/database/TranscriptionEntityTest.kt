package it.tldl.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TranscriptionEntityTest {

    @Test
    fun testTranscriptionEntityCreation() {
        val entity = TranscriptionEntity(
            id = 1,
            fileName = "voice_note_123.opus",
            transcribedText = "Ciao, questo è un messaggio vocale di prova.",
            timestampMs = 1784650000000L,
            durationSeconds = 15
        )

        assertNotNull(entity)
        assertEquals("voice_note_123.opus", entity.fileName)
        assertEquals("Ciao, questo è un messaggio vocale di prova.", entity.transcribedText)
        assertEquals(15, entity.durationSeconds)
    }
}
