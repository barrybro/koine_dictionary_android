package com.whoisbarry.pocketgreekdictionary.features.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.glance.appwidget.updateAll
import com.whoisbarry.pocketgreekdictionary.features.widget.DictionaryWidget
import com.whoisbarry.pocketgreekdictionary.features.widget.DictionaryWidgetWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val PREFS_NAME = "koine_dictionary_prefs"
    private val WIDGET_INTERVAL_KEY = "widget_update_interval"

    private val _updateInterval = MutableStateFlow(24) // Default 24 hours
    val updateInterval: StateFlow<Int> = _updateInterval

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _updateInterval.value = prefs.getInt(WIDGET_INTERVAL_KEY, 24)
    }

    fun setUpdateInterval(context: Context, hours: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(WIDGET_INTERVAL_KEY, hours).apply()
        _updateInterval.value = hours

        // Update the WorkManager job with the new interval
        DictionaryWidgetWorker.enqueue(context, hours)

        // Trigger an immediate widget update
        MainScope().launch {
            DictionaryWidget().updateAll(context)
        }
    }
}
