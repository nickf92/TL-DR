package it.tldl.app.core.stt

import org.json.JSONObject
import java.io.File

class BpeTokenizer(private val modelDir: File) {

    private val vocabMap = mutableMapOf<String, Long>()
    private val idToVocabMap = mutableMapOf<Long, String>()
    var isInitialized = false
        private set

    init {
        loadVocabulary()
    }

    private fun loadVocabulary() {
        try {
            val tokenizerFile = File(modelDir, "tokenizer.json")
            if (tokenizerFile.exists()) {
                val jsonContent = tokenizerFile.readText()
                val jsonObject = JSONObject(jsonContent)
                val modelObj = jsonObject.optJSONObject("model")
                val vocabObj = modelObj?.optJSONObject("vocab") ?: jsonObject.optJSONObject("vocab")
                
                vocabObj?.keys()?.forEach { key ->
                    val id = vocabObj.getLong(key)
                    vocabMap[key] = id
                    idToVocabMap[id] = key
                }
                isInitialized = vocabMap.isNotEmpty()
            }
        } catch (e: Exception) {
            isInitialized = false
        }
    }

    fun encode(text: String): LongArray {
        if (!isInitialized || text.isBlank()) return longArrayOf()

        val tokens = mutableListOf<Long>()
        // Split by whitespace and common delimiters
        val words = text.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,?!])|(?=[.,?!])"))

        for (word in words) {
            if (word.isEmpty()) continue
            val directId = vocabMap[word] ?: vocabMap[word.lowercase()]
            if (directId != null) {
                tokens.add(directId)
            } else {
                // Character-level BPE fallback
                for (ch in word) {
                    val charStr = ch.toString()
                    val charId = vocabMap[charStr] ?: 0L
                    tokens.add(charId)
                }
            }
        }
        return tokens.toLongArray()
    }

    fun decode(tokens: LongArray): String {
        if (!isInitialized || tokens.isEmpty()) return ""
        val sb = StringBuilder()
        for (tokenId in tokens) {
            val tokenStr = idToVocabMap[tokenId] ?: continue
            if (tokenStr.startsWith("<|") && tokenStr.endsWith("|>")) continue // Skip special ChatML control tokens
            val cleanToken = tokenStr.replace("Ġ", " ").replace(" ", " ")
            sb.append(cleanToken)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
