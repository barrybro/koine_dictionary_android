package com.whoisbarry.pocketgreekdictionary.data.models

data class DictionaryEntry(
    val id: Int,
    val difficulty: Int,
    val frequency: Int,
    val word: String,
    val fullWord: String,
    val gloss: String,
    val keyLetter: String,
    val sourceName: String,
    val type: String,
    val verbStem: String,
) {
    /**
     * The lemma on its own. [word] also carries principal parts for some entries (e.g.
     * "τιμάω, τιμήσω, ἐτίμησα, …"), which are too long for constrained surfaces such as the
     * home screen widget.
     */
    val headword: String
        get() {
            val candidate = word.substringBefore(',').trim().ifEmpty { word.trim() }
            return if (candidate.length <= HEADWORD_MAX_LENGTH) {
                candidate
            } else {
                candidate.substringBefore(' ').ifEmpty { candidate }
            }
        }

    /**
     * The first sense of [gloss], for surfaces too small to show the whole definition. Glosses
     * list senses separated by semicolons; anything dropped is marked with an ellipsis.
     */
    val primaryGloss: String
        get() {
            if (gloss.length <= SHORT_GLOSS_LENGTH) return gloss
            val firstSense = gloss.substringBefore(';').trim()
            val shortened = if (firstSense.length <= PRIMARY_GLOSS_MAX_LENGTH) {
                firstSense
            } else {
                firstSense.take(PRIMARY_GLOSS_MAX_LENGTH).substringBeforeLast(' ').trimEnd(',')
            }
            return if (shortened.length < gloss.length) "$shortened…" else gloss
        }

    fun shareText(): String {
        return "$word - $gloss"
    }

    private companion object {
        const val HEADWORD_MAX_LENGTH = 18
        const val SHORT_GLOSS_LENGTH = 60
        const val PRIMARY_GLOSS_MAX_LENGTH = 60
    }
}
