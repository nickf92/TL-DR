package it.tldl.app.core.database

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRepositoryTest {

    @Test
    fun testHistoryRepositorySavesInSessionMemoryByDefault() = runTest {
        val repo = HistoryRepository(dao = null, isOptInEnabled = false)
        repo.saveTranscription("audio_1.m4a", "Testo trascritto di prova")

        val recent = repo.getRecentTranscriptions()
        assertEquals(1, recent.size)
        assertEquals("audio_1.m4a", recent[0].fileName)
        assertEquals("Testo trascritto di prova", recent[0].transcribedText)
    }

    @Test
    fun testHistoryRepositoryClearsSessionMemory() = runTest {
        val repo = HistoryRepository(dao = null, isOptInEnabled = false)
        repo.saveTranscription("audio_2.mp3", "Trascrizione 2")
        repo.clearSessionMemory()

        val recent = repo.getRecentTranscriptions()
        assertTrue(recent.isEmpty())
    }
}
