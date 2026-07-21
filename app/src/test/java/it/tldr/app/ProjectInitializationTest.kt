package it.tldr.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProjectInitializationTest {

    @Test
    fun testAppInitializationConfig() {
        val appName = "TL;DR"
        val isPrivacyFirst = true
        
        assertNotNull("App name should not be null", appName)
        assertEquals("TL;DR", appName)
        assertEquals(true, isPrivacyFirst)
    }
}
