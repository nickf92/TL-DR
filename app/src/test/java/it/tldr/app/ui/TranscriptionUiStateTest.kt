package it.tldr.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionUiStateTest {

    sealed interface UiState {
        object Idle : UiState
        data class Progress(val percent: Int, val textSnippet: String) : UiState
        data class Success(val fullText: String) : UiState
        data class Error(val message: String) : UiState
    }

    @Test
    fun testProgressStateFormatting() {
        val state = UiState.Progress(75, "Trascrizione parziale...")
        
        assertTrue(state is UiState.Progress)
        assertEquals(75, state.percent)
        assertEquals("Trascrizione parziale...", state.textSnippet)
    }
}
