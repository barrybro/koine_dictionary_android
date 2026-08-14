# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, Test, and Lint Commands

```bash
./gradlew assembleDebug                          # Debug APK
./gradlew assembleRelease                        # Release APK

./gradlew test                                   # All unit tests
./gradlew test --tests "com.whoisbarry.koinedictionary.ExampleUnitTest.addition_isCorrect"  # Single test
./gradlew connectedAndroidTest                   # Instrumented tests (requires device/emulator)

./gradlew clean && ./gradlew assembleDebug       # Full rebuild
```

## Architecture

**MVVM + Jetpack Compose**, feature-based package structure under `com.whoisbarry.koinedictionary`:

```
features/
  alphabet/ui/     # AlphabetScreen, AlphabetViewModel
  dictionary/ui/   # DictionaryScreen, DictionaryViewModel, DictionaryEntryDetailScreen
  notification/    # DailyEntryNotification, DailyEntryNotificationWorker
  settings/ui/     # SettingsScreen, SettingsViewModel
  widget/          # DictionaryWidget (Glance), DictionaryWidgetWorker, DictionaryWidgetReceiver
data/models/       # DictionaryEntry data class
singletons/        # DictionaryService, TextToSpeechService, PocketGreekDictionaryApplication
ui/theme/          # Material 3 theme, colors, typography
util/              # StringUtils (diacritic removal for Greek search)
```

**No DI framework** — global services are singletons accessed via the `Application` class. **No Room/ORM** — `DictionaryService` does raw SQLite queries.

### Data Flow

```
assets/pocketGreekEntries.sqlite
  → DictionaryService (copies to internal storage on first use, caches entries in HashMap by keyLetter)
  → ViewModels (expose StateFlow, use combine() for derived/filtered state)
  → Composable screens (collectAsState())
```

### Widget

`DictionaryWidget` (Glance) shows a random dictionary entry. `DictionaryWidgetWorker` (WorkManager `CoroutineWorker`) handles periodic refresh. Call `DictionaryWidget.updateAll(context)` for immediate refresh and `DictionaryWidgetWorker.enqueue(context, intervalHours)` for scheduled updates.

### Entry Notification

Separate from the widget: `DailyEntryNotification.showRandomEntry(context)` posts a random entry, and `DailyEntryNotificationWorker.enqueue(context, intervalHours)` / `.cancel(context)` drive the schedule (SharedPreferences key `notification_interval`, `0` = off, first notification delayed by one full interval). Requires `POST_NOTIFICATIONS`; check `DailyEntryNotification.areNotificationsAllowed(context)` before enabling it.

### Opening an Entry from Outside the App

`MainActivity.entryIntent(context, entryId)` builds the intent used by both the widget tap and the notification tap. `MainActivity` is `launchMode="singleTask"` so a tap while the app is already running is routed to the running instance as `onNewIntent` instead of merely bringing the task to the front — don't drop that without re-testing a widget tap with the app in the background. `MainActivity` reads the id (clearing it so recreation doesn't reopen it), `MainScreen` switches to the Dictionary tab and calls `DictionaryViewModel.selectEntryById(id)`, and `DictionaryScreen` renders `DictionaryEntryDetailScreen` — the same view a list tap opens. Entry selection lives in `DictionaryViewModel.selectedEntry`, not in screen-local state.

### Database Schema

Table `greekDictionaryEntry`: `id`, `difficulty`, `frequency`, `word`, `fullWord`, `gloss`, `keyLetter`, `sourceName`, `type`, `verbStem`

## Code Conventions (from AGENTS.md)

- `val` preferred; private mutable backing fields prefixed with `_` (e.g., `_searchQuery`)
- ViewModels: `StateFlow` + `combine()` for derived state; `viewModelScope.launch(Dispatchers.IO)` for I/O
- Composables: `Modifier` parameter last with default; `Modifier.fillMaxSize()` at screen root; Material 3 components (`Scaffold`, `TopAppBar`, `TabRow`)
- Return `null` or empty collections for missing data — no thrown exceptions for expected failures
- No wildcard imports; grouped: `android.*` → `androidx.*` → `com.whoisbarry.*`

## Key Details

- Min SDK 29, Target SDK 36, Kotlin 2.4.0, Compose BOM 2026.05.01
- `TextToSpeechService` uses locale `el-GR` for Greek pronunciation
- `StringUtils.kt` provides `String.withoutDiacritics` extension for search normalization (removes Greek combining diacritical marks, lowercases with Greek locale)
- Release build has minification disabled (`isMinifyEnabled = false`)
