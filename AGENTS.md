# AGENTS.md - KoineDictionary

## Project Overview
Android Koine Greek dictionary app using Jetpack Compose, Material 3, Glance (widgets), and WorkManager.
- Min SDK: 29, Target SDK: 36
- Kotlin 2.2.10 with Compose BOM 2024.09.00

---

## Build / Lint / Test Commands

```bash
# Build
./gradlew assembleDebug                          # Debug APK
./gradlew assembleRelease                        # Release APK

# Tests
./gradlew test                                   # All unit tests
./gradlew test --tests "com.whoisbarry.koinedictionary.ExampleUnitTest"  # Single test class
./gradlew test --tests "com.whoisbarry.koinedictionary.ExampleUnitTest.addition_isCorrect"  # Single test

./gradlew connectedAndroidTest                   # Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest --tests "com.whoisbarry.koinedictionary.ExampleInstrumentedTest"  # Single class

# Clean / Build
./gradlew clean && ./gradlew assembleDebug

# Gradle dependency insight
./gradlew dependencies                            # All dependencies
./gradlew app:dependencies                        # App module only
```

---

## Architecture

- **MVVM** with Jetpack ViewModel
- Feature-based package structure: `features/<feature>/ui/`
- Screen + ViewModel per feature
- Shared services/utilities in `singletons/`, `data/models/`, `ui/theme/`
- Application class singleton pattern for global services

---

## Package Naming
```
com.whoisbarry.koinedictionary
├── MainActivity.kt
├── features/
│   ├── alphabet/ui/       # AlphabetScreen, AlphabetViewModel
│   ├── dictionary/ui/     # DictionaryScreen, DictionaryViewModel
│   ├── settings/ui/       # SettingsScreen, SettingsViewModel
│   └── widget/            # Glance widget + Worker
├── data/models/           # Data classes (DictionaryEntry)
├── singletons/            # DictionaryService, TextToSpeechService
└── ui/theme/              # Theme, Color, Typography
```

---

## Code Style

### Kotlin Conventions
- 4-space indentation (no tabs)
- Opening brace on same line for functions/classes
- No semicolons
- `val` for immutable, `var` only when mutability is needed
- Trailing commas (matches Android style)
- No wildcard imports

### Imports
- Grouped and ordered: `android.*` → `androidx.*` → `com.whoisbarry.*` → other
- Avoid `android.*` when Compose equivalents exist
- Fully qualified imports for Compose UI (`androidx.compose.foundation.*`)

### Types
- Use `Int`, `String`, `Boolean`, `List<T>`, `Map<K,V>` from stdlib
- Compose: `Modifier`, `StateFlow`, `MutableStateFlow`, `remember`, `mutableIntStateOf`
- `data class` for models with named parameters
- Sealed classes for UI state with multiple variants

### Naming
- **Classes/Types**: PascalCase (`DictionaryViewModel`, `DictionaryEntry`)
- **Functions/Properties**: camelCase (`onSearchQueryChanged`, `uiState`)
- **Constants/Enums**: SCREAMING_SNAKE_CASE or PascalCase for enum values
- **Composable functions**: PascalCase, noun first (`DictionaryScreen`, `AlphabetItem`)
- **ViewModels**: `<Feature>ViewModel` suffix
- **Screens**: `<Feature>Screen` suffix
- **Private members**: prefix with `_` where idiomatic (`_allEntries`, `_searchQuery`)

### State Management
- ViewModels expose `StateFlow` for UI state
- Use `combine()` for derived state from multiple flows
- `collectAsState()` in Composables to observe flows
- `mutableIntStateOf`, `mutableStateOf` for local Composable state
- Prefer unidirectional data flow: UI events → ViewModel → UI state

### Coroutines
- Use `viewModelScope.launch(Dispatchers.IO)` for blocking I/O
- `MainScope().launch` only for one-off background tasks from non-ViewModel context
- Prefer `StateFlow` over `LiveData`

### Error Handling
- No thrown exceptions for expected failures
- Return `null` or empty collections when data unavailable
- Use `?.` and `?:` (Elvis) operators for null safety
- Use `firstOrNull()` instead of index-based access with exceptions

### KDoc / Comments
- No KDoc required for simple classes/methods
- Add KDoc for non-obvious public API methods
- No inline comments explaining obvious code
- Prefer descriptive naming over comments

### Composables
- `@Composable` annotation on all Composable functions
- `@OptIn(ExperimentalMaterial3Api::class)` for Material3 experimental APIs
- `@Preview(showBackground = true)` for preview functions
- Modifier parameter last, with default `Modifier`: `fun FooScreen(viewModel: FooViewModel, modifier: Modifier = Modifier)`
- `Modifier.fillMaxSize()` applied at root of screen composables
- Use Material 3 components: `Scaffold`, `TopAppBar`, `TabRow`, `Text`, `OutlinedTextField`, etc.
- Avoid `Box` for layout; use `Column`/`Row`/`LazyColumn`

### Testing
- Unit tests: JUnit 4 in `src/test/java/`
- Instrumented tests: AndroidJUnit4 in `src/androidTest/java/`
- Use `assertEquals`, `assertNotNull`, etc. from `org.junit.Assert`
- No mocking framework currently; test with real data where possible

---

## Glance Widgets
- Glance 1.1.1 for app widgets
- Widget worker uses WorkManager 2.9.0
- `DictionaryWidget.updateAll(context)` for immediate widget refresh
- `DictionaryWidgetWorker.enqueue(context, intervalHours)` for scheduled updates

## Database
- SQLite database: `pocketGreekEntries.sqlite` in assets
- Copied to app internal storage on first access
- Read-only access via `DictionaryService`
- Table: `greekDictionaryEntry` with columns: `id`, `difficulty`, `frequency`, `word`, `fullWord`, `gloss`, `keyLetter`, `sourceName`, `type`, `verbStem`

---

## Key Dependencies (from libs.versions.toml)
- `androidx.core:core-ktx:1.10.1`
- `androidx.lifecycle:lifecycle-*:2.6.1`
- `androidx.activity:activity-compose:1.8.0`
- `androidx.compose.*` via BOM 2024.09.00
- `androidx.glance:*:1.1.1`
- `androidx.work:work-runtime-ktx:2.9.0`
- JUnit 4.13.2 / androidx.test 1.1.5
