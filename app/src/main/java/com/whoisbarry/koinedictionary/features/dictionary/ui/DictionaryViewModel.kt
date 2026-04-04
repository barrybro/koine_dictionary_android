package com.whoisbarry.koinedictionary.features.dictionary.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry
import com.whoisbarry.koinedictionary.singletons.DictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.whoisbarry.koinedictionary.util.withoutDiacritics

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val _allEntries = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<List<DictionaryEntry>> = combine(_allEntries, _searchQuery) { entries, query ->
        if (query.isBlank()) {
            entries
        } else {
            entries.filter { it.word.withoutDiacritics.contains(query, ignoreCase = true) || it.gloss.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = DictionaryService.getAllEntries(getApplication())
            _allEntries.value = entries.sortedBy { it.word.withoutDiacritics }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query.withoutDiacritics
    }
}
