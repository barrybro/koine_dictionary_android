package com.whoisbarry.pocketgreekdictionary.features.alphabet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whoisbarry.pocketgreekdictionary.singletons.TextToSpeechService
import com.whoisbarry.pocketgreekdictionary.ui.theme.LocalGreekFontFamily

/**
 * Narrowest a letter's card gets before the grid drops a column: a phone in portrait keeps the
 * single list it always had, while a tablet or a landscape phone spreads the letters across.
 */
private val MinLetterCardWidth = 280.dp

@Composable
fun AlphabetScreen(viewModel: AlphabetViewModel, modifier: Modifier = Modifier) {
    val greekAlphabetList = viewModel.greekAlphabet.toList()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MinLetterCardWidth),
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
                fontFamily = LocalGreekFontFamily.current,
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
