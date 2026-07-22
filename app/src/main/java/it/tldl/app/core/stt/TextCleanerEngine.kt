package it.tldl.app.core.stt

interface TextCleanerEngine {
    fun cleanText(rawText: String): String
}

typealias LocalTextCleaner = TextCleaner
typealias RuleBasedTextCleaner = TextCleaner
typealias OnnxPunctuationTextCleaner = TextCleaner
typealias SmolLmTextCleaner = TextCleaner
