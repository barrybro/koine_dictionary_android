package com.whoisbarry.koinedictionary.features.dictionary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry
import com.whoisbarry.koinedictionary.singletons.DictionaryService

@Composable
fun DictionaryScreen(viewModel: DictionaryViewModel, modifier: Modifier = Modifier) {
    val completeDictionary = DictionaryService.completeHashMap
    val dictionaryKeys = completeDictionary.keys.sorted()

    LazyColumn(modifier = modifier) {
        for (key in dictionaryKeys) {
            item {
                Text(
                    text = "${key.uppercase()} ${key.lowercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            val entries = completeDictionary[key] ?: emptyList()
            items(entries) { entry ->
                DictionaryEntryRow(entry)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DictionaryEntryRow(entry: DictionaryEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = entry.word,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = entry.gloss,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
