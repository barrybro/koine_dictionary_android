package com.whoisbarry.koinedictionary.util

import java.text.Normalizer
import java.util.Locale

/**
 * Returns a version of the string with Greek diacritics removed and converted to lowercase.
 */
val String.withoutDiacritics: String
    get() {
        // 1. Normalize the string to separate characters from their accents (NFD)
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)

        // 2. Use a regex to remove all "Non-Spacing Mark" characters (the accents)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()

        // 3. Convert to lowercase using Greek locale and remove the marks
        return regex.replace(normalized, "").lowercase(Locale.forLanguageTag("el-GR"))
    }
