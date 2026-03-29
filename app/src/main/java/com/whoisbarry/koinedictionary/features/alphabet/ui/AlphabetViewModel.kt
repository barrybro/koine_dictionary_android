package com.whoisbarry.koinedictionary.features.alphabet.ui

import androidx.lifecycle.ViewModel

class AlphabetViewModel: ViewModel() {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val greekAlphabet = "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"

    val greekLetterNames = mapOf(
        "Α" to "αλφα", "α" to "αλφα",
        "Β" to "βητα", "β" to "βητα",
        "Γ" to "γαμμα", "γ" to "γαμμα",
        "Δ" to "δελτα", "δ" to "δελτα",
        "Ε" to "εψιλον", "ε" to "εψιλον",
        "Ζ" to "ζητα", "ζ" to "ζητα",
        "Η" to "ητα", "η" to "ητα",
        "Θ" to "θητα", "θ" to "θητα",
        "Ι" to "ιωτα", "ι" to "ιωτα",
        "Κ" to "καππα", "κ" to "καππα",
        "Λ" to "λαμδα", "λ" to "λαμδα",
        "Μ" to "μυ", "μ" to "μυ",
        "Ν" to "νυ", "ν" to "νυ",
        "Ξ" to "ξι", "ξ" to "ξι",
        "Ο" to "ομικρον", "ο" to "ομικρον",
        "Π" to "πι", "π" to "πι",
        "Ρ" to "ρω", "ρ" to "ρω",
        "Σ" to "σιγμα", "σ" to "σιγμα", "ς" to "σιγμα",
        "Τ" to "ταυ", "τ" to "ταυ",
        "Υ" to "υψιλον", "υ" to "υψιλον",
        "Φ" to "φι", "φ" to "φι",
        "Χ" to "χι", "χ" to "χι",
        "Ψ" to "ψι", "ψ" to "ψι",
        "Ω" to "ωμεγα", "ω" to "ωμεγα"
    )
}
