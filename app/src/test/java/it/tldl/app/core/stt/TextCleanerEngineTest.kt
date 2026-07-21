package it.tldl.app.core.stt

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerEngineTest {

    private val cleaner = RuleBasedTextCleaner()

    @Test
    fun testRemovesCommonItalianFillers() {
        val input = "ciao ehm come stai cioè tutto bene"
        val expected = "Ciao come stai tutto bene"
        val actual = cleaner.cleanText(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testRemovesMultipleSpokenIntercalari() {
        val input = "ehm allora praticamente volevo diciamo dire che insomma va bene"
        val expected = "Allora volevo dire che va bene"
        val actual = cleaner.cleanText(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testFixesPunctuationSpacingAndCapitalization() {
        val input = "ciao , come stai ? tutto bene . a domani !"
        val expected = "Ciao, come stai? Tutto bene. A domani!"
        val actual = cleaner.cleanText(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testHandlesBlankAndEmptyStrings() {
        assertEquals("", cleaner.cleanText(""))
        assertEquals("   ", cleaner.cleanText("   "))
    }

    @Test
    fun testLocalTextCleanerFallbackWithNullManager() {
        val localCleaner = LocalTextCleaner(null)
        val input = "ehm ciao cioè"
        val expected = "Ciao"
        val actual = localCleaner.cleanText(input)
        assertEquals(expected, actual)
    }
}
