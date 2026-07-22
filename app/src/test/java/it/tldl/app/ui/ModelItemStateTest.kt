package it.tldl.app.ui

import it.tldl.app.core.stt.ModelInfo
import it.tldl.app.core.stt.RamCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelItemStateTest {

    @Test
    fun testModelItemStateCalculatesIsSafeCorrectly() {
        val freeRam = 1000L
        val safeModel = ModelInfo("safe-m", "Safe Model", ramRequiredMb = 500)
        val unsafeModel = ModelInfo("unsafe-m", "Unsafe Model", ramRequiredMb = 1500)

        val isSafeModelSafe = RamCalculator.isSafeForDevice(safeModel, freeRam)
        val isUnsafeModelSafe = RamCalculator.isSafeForDevice(unsafeModel, freeRam)

        val safeState = ModelItemState(info = safeModel, isDownloaded = false, isSafe = isSafeModelSafe)
        val unsafeState = ModelItemState(info = unsafeModel, isDownloaded = false, isSafe = isUnsafeModelSafe)

        assertTrue(safeState.isSafe)
        assertFalse(unsafeState.isSafe)
    }
}
