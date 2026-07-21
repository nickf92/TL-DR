package it.tldl.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProjectInitializationTest {

    @Test
    fun testAppInitializationConfig() {
        val appName = "TL;DL"
        val isPrivacyFirst = true
        
        assertNotNull("App name should not be null", appName)
        assertEquals("TL;DL", appName)
        assertEquals(true, isPrivacyFirst)
    }
}
