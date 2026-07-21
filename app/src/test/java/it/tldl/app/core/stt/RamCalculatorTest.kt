package it.tldl.app.core.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RamCalculatorTest {

    @Test
    fun testModelSafetyBadgesForLowRam() {
        val freeRamMb = 1024L // 1 GB
        val calculator = RamCalculator

        val smallModel = ModelInfo("whisper-base", "Whisper Base", ramRequiredMb = 500)
        val largeModel = ModelInfo("whisper-large", "Whisper Large", ramRequiredMb = 2048)

        assertTrue(calculator.isSafeForDevice(smallModel, freeRamMb))
        assertFalse(calculator.isSafeForDevice(largeModel, freeRamMb))
    }

    @Test
    fun testSmartDefaultSelectionCapsAtIdealModel() {
        val highRamMb = 8192L // 8 GB
        val calculator = RamCalculator

        val baseModel = ModelInfo("whisper-base", "Whisper Base", ramRequiredMb = 500)
        val idealModel = ModelInfo("sensevoice-small", "SenseVoice Small (Ideal Cap)", ramRequiredMb = 1200, isIdealCap = true)
        val heavyModel = ModelInfo("whisper-medium", "Whisper Medium", ramRequiredMb = 3072)

        val models = listOf(baseModel, idealModel, heavyModel)
        val selected = calculator.selectSmartDefaultModel(models, highRamMb)

        assertEquals("sensevoice-small", selected?.id)
    }
}
