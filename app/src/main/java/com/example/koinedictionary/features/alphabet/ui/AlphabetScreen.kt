package com.example.koinedictionary.features.alphabet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlphabetScreen(viewModel: AlphabetViewModel, modifier: Modifier = Modifier) {
    val greekAlphabetList = viewModel.greekAlphabet.toList()

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(greekAlphabetList) { letter ->
            AlphabetItem(letter = letter.toString())
            HorizontalDivider()
        }
    }
}

@Composable
fun AlphabetItem(letter: String) {
    Text(
        text = letter,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
