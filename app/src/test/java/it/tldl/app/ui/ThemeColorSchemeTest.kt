package it.tldl.app.ui

import androidx.compose.ui.graphics.Color
import it.tldl.app.ui.theme.getAmoledDarkColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeColorSchemeTest {

    @Test
    fun testAmoledDarkColorSchemeHasPureBlackBackgrounds() {
        val amoledColors = getAmoledDarkColorScheme()
        assertEquals(Color.Black, amoledColors.background)
        assertEquals(Color.Black, amoledColors.surface)
    }
}
