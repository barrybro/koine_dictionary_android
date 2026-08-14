package com.whoisbarry.pocketgreekdictionary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whoisbarry.pocketgreekdictionary.features.alphabet.ui.AlphabetScreen
import com.whoisbarry.pocketgreekdictionary.features.dictionary.ui.DictionaryScreen
import com.whoisbarry.pocketgreekdictionary.features.dictionary.ui.DictionaryViewModel
import com.whoisbarry.pocketgreekdictionary.features.settings.ui.SettingsScreen
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import com.whoisbarry.pocketgreekdictionary.singletons.TextToSpeechService
import com.whoisbarry.pocketgreekdictionary.ui.theme.KoineDictionaryTheme

class MainActivity : ComponentActivity() {

    /** Entry the app was opened on from a widget or notification, until the UI has shown it. */
    private var deepLinkEntryId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TextToSpeechService.init(this)
        DictionaryService.setupDictionaryStructure()
        deepLinkEntryId = consumeEntryId(intent)
        enableEdgeToEdge()
        setContent {
            KoineDictionaryTheme {
                MainScreen(
                    entryId = deepLinkEntryId,
                    onEntryIdHandled = { deepLinkEntryId = null }
                )
            }
        }
    }

    /** Tapping a widget or notification while the app is already open lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkEntryId = consumeEntryId(intent)
    }

    /**
     * Reads the entry to open and clears it from [intent], so that a later recreation of the
     * activity doesn't reopen an entry the user has already navigated away from.
     */
    private fun consumeEntryId(intent: Intent?): Int? {
        val entryId = intent?.getIntExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID)
            ?.takeIf { it != NO_ENTRY_ID }
            ?: return null

        intent.removeExtra(EXTRA_ENTRY_ID)
        return entryId
    }

    override fun onDestroy() {
        super.onDestroy()
        TextToSpeechService.shutdown()
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val NO_ENTRY_ID = -1

        /**
         * An intent that opens the app on [entryId]'s detail view. The entry is also encoded in
         * the intent data: extras are ignored when two intents are compared, so without it every
         * entry would share — and reuse the extras of — a single cached [android.app.PendingIntent].
         */
        fun entryIntent(context: Context, entryId: Int): Intent =
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse("pocketgreekdictionary://entry/$entryId"))
                .putExtra(EXTRA_ENTRY_ID, entryId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(entryId: Int? = null, onEntryIdHandled: () -> Unit = {}) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dictionary", "Alphabet", "Settings")
    val topTitles = listOf("Ancient Greek Dictionary", "Greek Alphabet", "Settings")

    val dictionaryViewModel: DictionaryViewModel = viewModel()

    LaunchedEffect(entryId) {
        if (entryId != null) {
            selectedTabIndex = 0
            dictionaryViewModel.selectEntryById(entryId)
            onEntryIdHandled()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = topTitles[selectedTabIndex]) }
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
                    viewModel = dictionaryViewModel,
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
