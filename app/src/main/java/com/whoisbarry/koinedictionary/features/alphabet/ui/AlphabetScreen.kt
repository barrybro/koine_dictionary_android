package com.whoisbarry.koinedictionary.features.alphabet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whoisbarry.koinedictionary.singletons.TextToSpeechService

@Composable
fun AlphabetScreen(viewModel: AlphabetViewModel, modifier: Modifier = Modifier) {
    val greekAlphabetList = viewModel.greekAlphabet.toList()

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(greekAlphabetList) { letter ->
            val letterName = viewModel.greekLetterNames[letter.toString()] ?: ""
            val letterUppercase = letter.toString().uppercase()
            val letterLowercase = letter.toString().lowercase()

            AlphabetItem(
                uppercase = letterUppercase,
                lowercase = letterLowercase,
                name = letterName,
                onClick = {
                    TextToSpeechService.speakText(letterName)
                }
            )
        }
    }
}

@Composable
fun AlphabetItem(uppercase: String, lowercase: String, name: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$uppercase $lowercase",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
