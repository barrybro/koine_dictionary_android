package com.whoisbarry.koinedictionary.data.models

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
)
