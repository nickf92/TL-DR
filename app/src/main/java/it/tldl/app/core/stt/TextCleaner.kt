package it.tldl.app.core.stt

import java.io.File

/**
 * Deep TextCleaner module that encapsulates Italian rule-based cleaning,
 * ONNX punctuation models, and SmolLM post-processing with automatic
 * graceful fallback chaining.
 */
class TextCleaner(
    private val modelManager: ModelManager? = null
) : TextCleanerEngine {
    companion object {
        private var isOrtAvailable: Boolean? = null

        private fun ensureNativeLibsLoaded(): Boolean {
            if (isOrtAvailable != null) return isOrtAvailable == true
            return try {
                System.loadLibrary("onnxruntime")
                System.loadLibrary("onnxruntime4j_jni")
                isOrtAvailable = true
                true
            } catch (e: Throwable) {
                isOrtAvailable = false
                false
            }
        }
    }

    override fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return rawText
        if (modelManager == null) return cleanRuleBased(rawText)

        return when (modelManager.getSelectedTextCleanerId()) {
            "punct-onnx" -> cleanPunctuationOnnx(rawText)
            "smollm-onnx" -> cleanSmolLmOnnx(rawText)
            else -> cleanRuleBased(rawText)
        }
    }

    private fun cleanRuleBased(rawText: String): String {
        if (rawText.isBlank()) return rawText

        var cleaned = rawText

        // 1. Remove common Italian spoken filler words
        val fillerRegex = Regex("(?i)(?<=^|[\\s,.?!])(ehm|ehmm|ah|umm?|cioè|mmm?|eh|praticamente|diciamo|insomma|guardi|senta|tipo)(?=$|[\\s,.?!])[\\s,]*")
        cleaned = cleaned.replace(fillerRegex, "")

        // 2. Fix spaces around punctuation (, . ? !)
        cleaned = cleaned.replace(Regex("\\s+([,.?!])"), "$1")
        cleaned = cleaned.replace(Regex("([,.?!])([^\\s0-9])"), "$1 $2")

        // 3. Capitalize first letter of sentences
        cleaned = cleaned.replace(Regex("(^|[.?!]\\s+)([a-zàèéìòù])")) { match ->
            match.groupValues[1] + match.groupValues[2].uppercase()
        }

        // 4. Normalize double spaces and trim
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return cleaned
    }

    private fun cleanPunctuationOnnx(rawText: String): String {
        val manager = modelManager ?: return cleanRuleBased(rawText)
        val modelPath = manager.getModelPath("punct-onnx") ?: return cleanRuleBased(rawText)

        return try {
            val modelFile = File(modelPath, "model.onnx")
            if (!modelFile.exists()) return cleanRuleBased(rawText)

            val config = com.k2fsa.sherpa.onnx.OfflinePunctuationConfig(
                model = com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig(
                    ctTransformer = modelFile.absolutePath,
                    numThreads = 2,
                    debug = false
                )
            )
            val punct = com.k2fsa.sherpa.onnx.OfflinePunctuation(null, config)
            val result = punct.addPunctuation(rawText)
            punct.release()
            if (result.isNotBlank()) result else cleanRuleBased(rawText)
        } catch (e: Throwable) {
            cleanRuleBased(rawText)
        }
    }

    private fun cleanSmolLmOnnx(rawText: String): String {
        val manager = modelManager ?: return cleanRuleBased(rawText)
        val modelPath = manager.getModelPath("smollm-onnx")
        if (modelPath == null || !ensureNativeLibsLoaded()) {
            return cleanRuleBased(rawText)
        }

        val systemPrompt = manager.getCustomCleanerPrompt()
        val formattedChatPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$rawText<|im_end|>\n<|im_start|>assistant\n"

        return try {
            val modelDir = manager.getModelPath("smollm-onnx") ?: return cleanRuleBased(rawText)
            val nestedFile = File(modelDir, "onnx/model_quantized.onnx")
            val rootFile = File(modelDir, "model_quantized.onnx")
            val modelFile = if (nestedFile.exists()) nestedFile else rootFile

            if (!modelFile.exists()) return cleanRuleBased(rawText)

            val tokenizer = BpeTokenizer(modelDir)
            if (!tokenizer.isInitialized) return cleanRuleBased(rawText)

            val inputTokens = tokenizer.encode(formattedChatPrompt)
            if (inputTokens.isEmpty()) return cleanRuleBased(rawText)

            val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
            val sessionOptions = ai.onnxruntime.OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            
            val session = env.createSession(modelFile.absolutePath, sessionOptions)
            val generatedTokens = mutableListOf<Long>()
            val currentTokens = inputTokens.toMutableList()
            val maxNewTokens = 128

            try {
                for (step in 0 until maxNewTokens) {
                    val inputShape = longArrayOf(1, currentTokens.size.toLong())
                    val inputBuffer = java.nio.LongBuffer.wrap(currentTokens.toLongArray())
                    val maskBuffer = java.nio.LongBuffer.wrap(LongArray(currentTokens.size) { 1L })
                    
                    val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(env, inputBuffer, inputShape)
                    val maskTensor = ai.onnxruntime.OnnxTensor.createTensor(env, maskBuffer, inputShape)

                    val inputs = mapOf("input_ids" to inputTensor, "attention_mask" to maskTensor)
                    val results = session.run(inputs)

                    var nextTokenId = -1L
                    val logitsTensor = results.get(0) as? ai.onnxruntime.OnnxTensor
                    if (logitsTensor != null) {
                        val shape = logitsTensor.info.shape
                        if (shape.size >= 3) {
                            val seqLen = shape[1].toInt()
                            val vocabSize = shape[2].toInt()
                            val floatBuffer = logitsTensor.floatBuffer

                            val offset = (seqLen - 1) * vocabSize
                            var maxIdx = 0
                            var maxVal = -Float.MAX_VALUE
                            val limit = minOf(offset + vocabSize, floatBuffer.capacity())
                            for (i in offset until limit) {
                                val valAtI = floatBuffer.get(i)
                                if (valAtI > maxVal) {
                                    maxVal = valAtI
                                    maxIdx = i - offset
                                }
                            }
                            nextTokenId = maxIdx.toLong()
                        }
                    }

                    inputTensor.close()
                    maskTensor.close()
                    results.close()

                    // End of sequence (EOS / <|im_end|>) or invalid token
                    if (nextTokenId <= 0L || nextTokenId == 2L || nextTokenId == 50256L) {
                        break
                    }

                    generatedTokens.add(nextTokenId)
                    currentTokens.add(nextTokenId)
                }
            } finally {
                session.close()
                sessionOptions.close()
            }

            val generatedText = tokenizer.decode(generatedTokens.toLongArray())
            if (generatedText.isNotBlank()) {
                generatedText.trim()
            } else {
                cleanRuleBased(rawText)
            }
        } catch (e: Throwable) {
            isOrtAvailable = false
            cleanRuleBased(rawText)
        }
    }
}
