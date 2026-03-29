package com.whoisbarry.koinedictionary.features.alphabet.ui

import androidx.compose.foundation.clickable
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
                letter = "$letterUppercase $letterLowercase, $letterName",
                onClick = {
                    TextToSpeechService.speakText(letterName)
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun AlphabetItem(letter: String, onClick: () -> Unit) {
    Text(
        text = letter,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    )
}
