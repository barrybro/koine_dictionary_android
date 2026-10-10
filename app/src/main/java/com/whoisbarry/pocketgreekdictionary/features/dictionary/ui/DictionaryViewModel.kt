package com.whoisbarry.pocketgreekdictionary.features.dictionary.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.whoisbarry.pocketgreekdictionary.util.withoutDiacritics

/** One letter's run of entries in the list, under its heading. */
data class DictionarySection(val keyLetter: String, val entries: List<DictionaryEntry>)

/**
 * What the list shows: the matching entries in their letter sections, and where each section's
 * heading sits in the list, for the alphabet index to jump to.
 */
data class DictionaryListState(
    val sections: List<DictionarySection> = emptyList(),
    val headingIndexes: Map<String, Int> = emptyMap()
)

/**
 * An entry with its search keys worked out once, when the dictionary loads. Stripping accents is
 * far too slow to repeat for every entry on every keystroke.
 */
private class SearchableEntry(val entry: DictionaryEntry) {
    val wordKey = entry.word.withoutDiacritics
    val glossKey = entry.gloss.withoutDiacritics
}

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    /** Every entry, sorted alphabetically. */
    private val _allEntries = MutableStateFlow<List<SearchableEntry>>(emptyList())

    /** The search field's text, as typed — accents and all. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** The entry whose detail view is open, or null while the list is showing. */
    private val _selectedEntry = MutableStateFlow<DictionaryEntry?>(null)
    val selectedEntry: StateFlow<DictionaryEntry?> = _selectedEntry.asStateFlow()

    /**
     * Filtered and grouped off the main thread, so typing never waits on it. Queries are compared
     * without accents, so a change to just an accent doesn't search again.
     */
    val uiState: StateFlow<DictionaryListState> = combine(
        _allEntries,
        _searchQuery.map { it.withoutDiacritics }.distinctUntilChanged()
    ) { entries, query ->
        val matches = if (query.isBlank()) {
            entries
        } else {
            entries.filter { query in it.wordKey || query in it.glossKey }
        }
        buildListState(matches)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DictionaryListState())

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = DictionaryService.getAllEntries(getApplication())
            _allEntries.value = entries.map(::SearchableEntry).sortedBy { it.wordKey }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectEntry(entry: DictionaryEntry?) {
        _selectedEntry.value = entry
    }

    /**
     * Opens the entry with [id] — the entry a widget or notification was tapped on. Reads it
     * straight from the database rather than [uiState], which is still loading right after a
     * cold start and is narrowed by any active search.
     */
    fun selectEntryById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedEntry.value = DictionaryService.getEntryById(getApplication(), id)
        }
    }
}

/** Sections in key-letter order; within each, [matches] keeps its alphabetical order. */
private fun buildListState(matches: List<SearchableEntry>): DictionaryListState {
    val sections = matches
        .groupBy({ it.entry.keyLetter }, { it.entry })
        .toSortedMap()
        .map { (keyLetter, entries) -> DictionarySection(keyLetter, entries) }

    // Each section is its heading followed by its entries, which is how the list lays them out.
    val headingIndexes = HashMap<String, Int>(sections.size)
    var index = 0
    for (section in sections) {
        headingIndexes[section.keyLetter] = index
        index += 1 + section.entries.size
    }

    return DictionaryListState(sections, headingIndexes)
}
