package it.tldl.app.core.stt

import com.k2fsa.sherpa.onnx.*
import java.io.File

class SherpaOnnxEngine : SpeechToTextEngine {

    private var onlineRecognizer: OnlineRecognizer? = null
    private var offlineRecognizer: OfflineRecognizer? = null

    override fun initialize(modelDirectory: File) {
        val files = modelDirectory.listFiles() ?: emptyArray()
        
        val encoder = files.find { it.name.contains("encoder") && it.name.endsWith(".onnx") }
        val decoder = files.find { it.name.contains("decoder") && it.name.endsWith(".onnx") }
        val joiner = files.find { it.name.contains("joiner") && it.name.endsWith(".onnx") }
        val tokens = files.find { it.name.contains("tokens") && it.name.endsWith(".txt") }

        if (joiner != null) {
            // Zipformer (Online)
            require(encoder != null && decoder != null && tokens != null) {
                "File del modello Zipformer incompleti in: ${modelDirectory.absolutePath}"
            }
            val config = OnlineRecognizerConfig(
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoder.absolutePath,
                        decoder = decoder.absolutePath,
                        joiner = joiner.absolutePath,
                    ),
                    tokens = tokens.absolutePath,
                    numThreads = 2,
                    debug = false
                ),
                decodingMethod = "greedy_search",
                maxActivePaths = 4
            )
            onlineRecognizer = OnlineRecognizer(null, config)
        } else {
            // Whisper (Offline)
            require(encoder != null && decoder != null && tokens != null) {
                "File del modello Whisper incompleti in: ${modelDirectory.absolutePath}"
            }
            val config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = encoder.absolutePath,
                        decoder = decoder.absolutePath,
                        language = "it",
                        task = "transcribe"
                    ),
                    tokens = tokens.absolutePath,
                    numThreads = 2,
                    debug = false
                ),
                decodingMethod = "greedy_search"
            )
            offlineRecognizer = OfflineRecognizer(null, config)
        }
    }

    override fun transcribeStream(
        samples: ShortArray,
        onProgress: (progressPercent: Int, partialText: String) -> Unit
    ): String {
        return if (onlineRecognizer != null) {
            transcribeOnline(samples, onProgress)
        } else if (offlineRecognizer != null) {
            transcribeOffline(samples, onProgress)
        } else {
            "Errore: Motore non inizializzato"
        }
    }

    private fun transcribeOnline(
        samples: ShortArray,
        onProgress: (progressPercent: Int, partialText: String) -> Unit
    ): String {
        val engine = onlineRecognizer!!
        val stream = engine.createStream()
        val floatSamples = FloatArray(samples.size) { samples[it] / 32768.0f }
        val chunkSize = 16000
        val totalChunks = (floatSamples.size + chunkSize - 1) / chunkSize
        var fullResult = ""
        
        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, floatSamples.size)
            stream.acceptWaveform(floatSamples.copyOfRange(start, end), 16000)
            while (engine.isReady(stream)) {
                engine.decode(stream)
            }
            val text = engine.getResult(stream).text
            if (text.isNotEmpty()) {
                fullResult = text
                onProgress(((i + 1) * 100 / totalChunks), fullResult)
            }
        }
        stream.release()
        return fullResult
    }

    private fun transcribeOffline(
        samples: ShortArray,
        onProgress: (progressPercent: Int, partialText: String) -> Unit
    ): String {
        val engine = offlineRecognizer!!
        val stream = engine.createStream()
        val floatSamples = FloatArray(samples.size) { samples[it] / 32768.0f }
        
        onProgress(10, "Avvio Whisper...")
        stream.acceptWaveform(floatSamples, 16000)
        onProgress(50, "Elaborazione Whisper...")
        engine.decode(stream)
        
        val result = engine.getResult(stream).text
        onProgress(100, result)
        
        stream.release()
        return result
    }

    override fun release() {
        onlineRecognizer?.release()
        onlineRecognizer = null
        offlineRecognizer?.release()
        offlineRecognizer = null
    }
}
