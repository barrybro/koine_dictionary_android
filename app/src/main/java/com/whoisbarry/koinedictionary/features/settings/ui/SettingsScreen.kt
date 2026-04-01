package com.whoisbarry.koinedictionary.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val updateInterval by viewModel.updateInterval.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Widget Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = "Update Interval",
            style = MaterialTheme.typography.titleMedium
        )

        val options = listOf(
            1 to "Every hour",
            12 to "Every 12 hours",
            24 to "Once a day"
        )

        options.forEach { (hours, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setUpdateInterval(context, hours) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = updateInterval == hours,
                    onClick = { viewModel.setUpdateInterval(context, hours) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "About", style = MaterialTheme.typography.titleLarge)
        Text("Pocket Greek Dictionary 1.0")
        Text("By me")
    }
}
