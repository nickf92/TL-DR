package it.tldl.app.core.stt

import java.io.File

interface TextCleanerEngine {
    fun cleanText(rawText: String): String
}

class RuleBasedTextCleaner : TextCleanerEngine {
    override fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return rawText

        var cleaned = rawText

        // 1. Remove common Italian spoken filler words (ehm, ehmm, ah, um, cioè, mm, eh, praticamente, diciamo, insomma, guardi, senta, tipo)
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
}

class OnnxPunctuationTextCleaner(
    private val modelManager: ModelManager
) : TextCleanerEngine {
    private val ruleBasedFallback = RuleBasedTextCleaner()

    override fun cleanText(rawText: String): String {
        val modelPath = modelManager.getModelPath("punct-onnx")
        if (modelPath == null) {
            return ruleBasedFallback.cleanText(rawText)
        }
        return try {
            val modelFile = java.io.File(modelPath, "model.onnx")
            if (!modelFile.exists()) return ruleBasedFallback.cleanText(rawText)

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
            if (result.isNotBlank()) result else ruleBasedFallback.cleanText(rawText)
        } catch (e: Throwable) {
            ruleBasedFallback.cleanText(rawText)
        }
    }
}

class SmolLmTextCleaner(
    private val modelManager: ModelManager
) : TextCleanerEngine {
    private val ruleBasedFallback = RuleBasedTextCleaner()

    companion object {
        private var isOrtAvailable: Boolean? = null

        fun ensureNativeLibsLoaded(): Boolean {
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
        val modelPath = modelManager.getModelPath("smollm-onnx")
        if (modelPath == null || !ensureNativeLibsLoaded()) {
            return ruleBasedFallback.cleanText(rawText)
        }

        val systemPrompt = modelManager.getCustomCleanerPrompt()
        // Format prompt using ChatML format for SmolLM2:
        val formattedChatPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$rawText<|im_end|>\n<|im_start|>assistant\n"

        return try {
            val modelDir: File = modelManager.getModelPath("smollm-onnx") ?: return ruleBasedFallback.cleanText(rawText)
            val nestedFile = File(modelDir, "onnx/model_quantized.onnx")
            val rootFile = File(modelDir, "model_quantized.onnx")
            val modelFile = if (nestedFile.exists()) nestedFile else rootFile

            if (!modelFile.exists()) return ruleBasedFallback.cleanText(rawText)

            val tokenizer = BpeTokenizer(modelDir)
            if (!tokenizer.isInitialized) return ruleBasedFallback.cleanText(rawText)

            val inputTokens = tokenizer.encode(formattedChatPrompt)
            if (inputTokens.isEmpty()) return ruleBasedFallback.cleanText(rawText)

            val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
            val sessionOptions = ai.onnxruntime.OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            val session = env.createSession(modelFile.absolutePath, sessionOptions)

            val inputShape = longArrayOf(1, inputTokens.size.toLong())
            val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(inputTokens), inputShape)
            val attentionMask = LongArray(inputTokens.size) { 1L }
            val maskTensor = ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(attentionMask), inputShape)

            val inputs = mapOf("input_ids" to inputTensor, "attention_mask" to maskTensor)
            val results = session.run(inputs)

            var generatedTokenText = ""
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
                    val decoded = tokenizer.decode(longArrayOf(maxIdx.toLong()))
                    if (decoded.isNotBlank()) {
                        generatedTokenText = decoded
                    }
                }
            }

            inputTensor.close()
            maskTensor.close()
            results.close()
            session.close()
            sessionOptions.close()

            val baseResult = ruleBasedFallback.cleanText(rawText)
            if (generatedTokenText.isNotBlank()) {
                "$baseResult ($generatedTokenText)".trim()
            } else {
                baseResult
            }
        } catch (e: Throwable) {
            isOrtAvailable = false
            ruleBasedFallback.cleanText(rawText)
        }
    }
}

class LocalTextCleaner(
    private val modelManager: ModelManager? = null
) : TextCleanerEngine {

    private val ruleBasedCleaner = RuleBasedTextCleaner()
    private val punctCleaner = modelManager?.let { OnnxPunctuationTextCleaner(it) }
    private val smolLmCleaner = modelManager?.let { SmolLmTextCleaner(it) }

    override fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return rawText
        if (modelManager == null) return ruleBasedCleaner.cleanText(rawText)

        return when (modelManager.getSelectedTextCleanerId()) {
            "punct-onnx" -> punctCleaner?.cleanText(rawText) ?: ruleBasedCleaner.cleanText(rawText)
            "smollm-onnx" -> smolLmCleaner?.cleanText(rawText) ?: ruleBasedCleaner.cleanText(rawText)
            else -> ruleBasedCleaner.cleanText(rawText)
        }
    }
}
