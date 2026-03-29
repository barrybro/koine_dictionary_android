package com.whoisbarry.koinedictionary.features.dictionary.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry
import com.whoisbarry.koinedictionary.singletons.DictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val uiState: StateFlow<List<DictionaryEntry>> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = DictionaryService.getAllEntries(getApplication())
            _uiState.value = entries
        }
    }
}
