package com.whoisbarry.koinedictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whoisbarry.koinedictionary.features.alphabet.ui.AlphabetScreen
import com.whoisbarry.koinedictionary.features.dictionary.ui.DictionaryScreen
import com.whoisbarry.koinedictionary.features.settings.ui.SettingsScreen
import com.whoisbarry.koinedictionary.singletons.DictionaryService
import com.whoisbarry.koinedictionary.singletons.TextToSpeechService
import com.whoisbarry.koinedictionary.ui.theme.KoineDictionaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TextToSpeechService.init(this)
        DictionaryService.setupDictionaryStructure()
        enableEdgeToEdge()
        setContent {
            KoineDictionaryTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TextToSpeechService.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dictionary", "Alphabet", "Settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = tabs[selectedTabIndex]) }
            )
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.navigationBarsPadding()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) },
                        icon = {
                            when (index) {
                                0 -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_book_2_24),
                                        contentDescription = null
                                    )
                                }

                                1 -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_book_6_24),
                                        contentDescription = null
                                    )
                                }

                                2 -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_settings_24),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTabIndex) {
                0 -> DictionaryScreen(
                    viewModel = viewModel(),
                    modifier = Modifier
                )

                1 -> AlphabetScreen(
                    viewModel = viewModel(),
                    modifier = Modifier
                )

                2 -> SettingsScreen(
                    viewModel = viewModel(),
                    modifier = Modifier
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    KoineDictionaryTheme {
        MainScreen()
    }
}
