package com.whoisbarry.pocketgreekdictionary.util

import java.text.Normalizer
import java.util.Locale

/** Compiled once: building it is the costliest part of a call, and it never changes. */
private val combiningDiacriticalMarks = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private val greekLocale = Locale.forLanguageTag("el-GR")

/**
 * Returns a version of the string with Greek diacritics removed and converted to lowercase.
 */
val String.withoutDiacritics: String
    get() {
        // 1. Normalize the string to separate characters from their accents (NFD)
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)

        // 2. Remove all "Non-Spacing Mark" characters (the accents), then 3. convert to lowercase
        // using the Greek locale
        return combiningDiacriticalMarks.replace(normalized, "").lowercase(greekLocale)
    }
