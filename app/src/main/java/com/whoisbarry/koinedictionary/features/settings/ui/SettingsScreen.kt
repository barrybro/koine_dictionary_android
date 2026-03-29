package com.whoisbarry.koinedictionary.features.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        Text(text = "About")
        Text("Pocket Greek Dictionary 1.0")
        Text("By me")
        Text("Visit this website for more information.")
    }

}