package it.tldl.app.core.service

import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TranscriptionServiceTest {

    @Test
    fun `service should start in Idle state`() = runTest {
        val controller = Robolectric.buildService(TranscriptionService::class.java)
        val service = controller.create().get()
        
        val currentState = TranscriptionService.state.value
        assertTrue(currentState is TranscriptionState.Idle)
    }

    @Test
    fun `service should transition to Decoding when ACTION_START is received`() = runTest {
        val controller = Robolectric.buildService(TranscriptionService::class.java)
        val service = controller.create().get()
        
        val intent = Intent(service, TranscriptionService::class.java).apply {
            action = TranscriptionService.ACTION_START
            putExtra(TranscriptionService.EXTRA_AUDIO_PATH, "/dummy/path.opus")
        }
        
        service.onStartCommand(intent, 0, 1)
        
        // This should trigger the state change. 
        // Note: Since it's in a coroutine, we might need to wait or use a more synchronous approach for the test.
        assertTrue(TranscriptionService.state.value is TranscriptionState.Decoding)
    }

    @Test
    fun `service should transition to Idle when ACTION_CANCEL is received`() = runTest {
        val controller = Robolectric.buildService(TranscriptionService::class.java)
        val service = controller.create().get()

        val cancelIntent = Intent(service, TranscriptionService::class.java).apply {
            action = TranscriptionService.ACTION_CANCEL
        }

        service.onStartCommand(cancelIntent, 0, 2)

        assertTrue(TranscriptionService.state.value is TranscriptionState.Idle)
    }
}
