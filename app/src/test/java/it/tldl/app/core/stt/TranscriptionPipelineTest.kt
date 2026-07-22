package it.tldl.app.core.stt

import it.tldl.app.core.audio.AudioProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

class FakeAudioProcessor : AudioProcessor() {
    var processAudioFileCalled = false
    override suspend fun processAudioFile(file: File): ShortArray {
        processAudioFileCalled = true
        return ShortArray(16000) { 0 }
    }
}

class FakeSpeechToTextEngine : SpeechToTextEngine {
    var initializedWithDir: File? = null
    var isReleased = false

    override fun initialize(modelDirectory: File) {
        initializedWithDir = modelDirectory
    }

    override fun transcribeStream(
        samples: ShortArray,
        onProgress: (progressPercent: Int, partialText: String) -> Unit
    ): String {
        onProgress(50, "Ciao mondo")
        return "Ciao mondo"
    }

    override fun release() {
        isReleased = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TranscriptionPipelineTest {

    private lateinit var modelManager: ModelManager
    private lateinit var testAudioFile: File

    @Before
    fun setUp() {
        modelManager = ModelManager(RuntimeEnvironment.getApplication())
        testAudioFile = File.createTempFile("test_audio", ".wav")
    }

    @Test
    fun `execute returns error when no active model is downloaded`() = runTest {
        val fakeAudioProcessor = FakeAudioProcessor()
        val fakeSttEngine = FakeSpeechToTextEngine()

        val pipeline = TranscriptionPipeline(
            audioProcessor = fakeAudioProcessor,
            sttEngine = fakeSttEngine,
            modelManager = modelManager
        )

        var caughtException: Exception? = null
        try {
            pipeline.execute(testAudioFile) {}
        } catch (e: Exception) {
            caughtException = e
        }

        assertTrue(caughtException is IllegalStateException)
        assertTrue(fakeSttEngine.isReleased)
    }
}
