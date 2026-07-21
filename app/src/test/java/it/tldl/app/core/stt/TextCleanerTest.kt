package it.tldl.app.core.stt

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {

    private val cleaner = LocalTextCleaner()

    @Test
    fun testRemovesFillerWordsAndFixesCapitalization() {
        val raw = "ehm ciao , cioè questo è un test . ehm spero funzioni ."
        val expected = "Ciao, questo è un test. Spero funzioni."
        val actual = cleaner.cleanText(raw)
        assertEquals("Expected clean text, got actual: '$actual'", expected, actual)
    }

    @Test
    fun testHandlesEmptyString() {
        assertEquals("", cleaner.cleanText(""))
    }
}
