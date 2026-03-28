package com.example.koinedictionary.features.dictionary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.koinedictionary.data.models.DictionaryEntry

@Composable
fun DictionaryScreen(viewModel: DictionaryViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.uiState.collectAsState()

    LazyColumn(modifier = modifier) {
        items(entries) { entry ->
            DictionaryEntryRow(entry)
            HorizontalDivider()
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
