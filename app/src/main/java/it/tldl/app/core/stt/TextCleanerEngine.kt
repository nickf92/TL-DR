package it.tldl.app.core.stt

interface TextCleanerEngine {
    fun cleanText(rawText: String): String
}

class LocalTextCleaner : TextCleanerEngine {
    override fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return rawText

        var cleaned = rawText

        // 1. Remove common filler words (intercalari: ehm, ehmm, ah, um, cioè, mm, eh)
        val fillerRegex = Regex("(?i)(?<=^|[\\s,.?!])(ehm|ehmm|ah|umm?|cioè|mmm?|eh)(?=$|[\\s,.?!])[\\s,]*")
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
