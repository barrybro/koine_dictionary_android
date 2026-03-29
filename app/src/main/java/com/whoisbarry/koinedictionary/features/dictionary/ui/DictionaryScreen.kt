package com.whoisbarry.koinedictionary.features.dictionary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry

@Composable
fun DictionaryScreen(viewModel: DictionaryViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Group the filtered entries by their keyLetter to maintain the alphabetical structure
    val groupedEntries = uiState.groupBy { it.keyLetter }
    val dictionaryKeys = groupedEntries.keys.sorted()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search Greek or English...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            for (key in dictionaryKeys) {
                item(key = key) {
                    Text(
                        text = "${key.uppercase()} ${key.lowercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                val entries = groupedEntries[key] ?: emptyList()
                items(entries, key = { it.id }) { entry ->
                    DictionaryEntryRow(entry)
                    HorizontalDivider()
                }
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
