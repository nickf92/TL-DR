package it.tldl.app.core.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class BpeTokenizerTest {

    @Test
    fun `test non existent model dir returns uninitialized tokenizer`() {
        val dummyDir = File("/invalid/dir/path")
        val tokenizer = BpeTokenizer(dummyDir)
        assertFalse(tokenizer.isInitialized)
        assertEquals(0, tokenizer.encode("Ciao").size)
        assertEquals("", tokenizer.decode(longArrayOf(1, 2, 3)))
    }
}
