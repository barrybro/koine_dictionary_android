package com.whoisbarry.pocketgreekdictionary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whoisbarry.pocketgreekdictionary.features.alphabet.ui.AlphabetScreen
import com.whoisbarry.pocketgreekdictionary.features.dictionary.ui.DictionaryScreen
import com.whoisbarry.pocketgreekdictionary.features.dictionary.ui.DictionaryViewModel
import com.whoisbarry.pocketgreekdictionary.features.settings.ui.SettingsScreen
import com.whoisbarry.pocketgreekdictionary.singletons.AccentColorService
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import com.whoisbarry.pocketgreekdictionary.singletons.GreekFontService
import com.whoisbarry.pocketgreekdictionary.singletons.TextToSpeechService
import com.whoisbarry.pocketgreekdictionary.ui.theme.KoineDictionaryTheme

class MainActivity : ComponentActivity() {

    /** Entry the app was opened on from a widget or notification, until the UI has shown it. */
    private var deepLinkEntryId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TextToSpeechService.init(this)
        DictionaryService.setupDictionaryStructure()
        AccentColorService.init(this)
        GreekFontService.init(this)
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

/** From this width up the tabs move from the bottom of the screen to a rail down its side. */
private val NavigationRailMinWidth = 600.dp

private enum class MainTab(val label: String, val title: String, @DrawableRes val icon: Int) {
    Dictionary("Dictionary", "Ancient Greek Dictionary", R.drawable.outline_book_2_24),
    Alphabet("Alphabet", "Greek Alphabet", R.drawable.outline_book_6_24),
    Settings("Settings", "Settings", R.drawable.baseline_settings_24)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(entryId: Int? = null, onEntryIdHandled: () -> Unit = {}) {
    // Saveable so that rotating a tablet, or resizing the window, doesn't drop back to the first tab.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = MainTab.entries[selectedTabIndex]

    val dictionaryViewModel: DictionaryViewModel = viewModel()

    LaunchedEffect(entryId) {
        if (entryId != null) {
            selectedTabIndex = MainTab.Dictionary.ordinal
            dictionaryViewModel.selectEntryById(entryId)
            onEntryIdHandled()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= NavigationRailMinWidth

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail {
                    MainTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTabIndex = tab.ordinal },
                            icon = { Icon(painter = painterResource(id = tab.icon), contentDescription = null) },
                            label = { Text(text = tab.label) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // The rail already pads itself clear of a display cutout or side navigation
                    // bar; without this the top bar would leave the same gap again beside it.
                    .then(
                        if (useNavigationRail) {
                            Modifier.consumeWindowInsets(
                                NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                            )
                        } else {
                            Modifier
                        }
                    ),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = selectedTab.title) }
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            MainTab.entries.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTabIndex = tab.ordinal },
                                    text = { Text(text = tab.label) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = tab.icon),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        // Screens with their own Scaffold, like an entry's detail view, would
                        // otherwise pad for the status bar a second time under this top bar.
                        .consumeWindowInsets(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedTab) {
                        MainTab.Dictionary -> DictionaryScreen(
                            viewModel = dictionaryViewModel,
                            modifier = Modifier
                        )

                        MainTab.Alphabet -> AlphabetScreen(
                            viewModel = viewModel(),
                            modifier = Modifier
                        )

                        MainTab.Settings -> SettingsScreen(
                            viewModel = viewModel(),
                            modifier = Modifier
                        )
                    }
                }
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

@Preview(showBackground = true, device = Devices.TABLET)
@Composable
fun MainScreenTabletPreview() {
    KoineDictionaryTheme {
        MainScreen()
    }
}
