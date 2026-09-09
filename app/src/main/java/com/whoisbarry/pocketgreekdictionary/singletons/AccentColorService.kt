package com.whoisbarry.pocketgreekdictionary.singletons

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.whoisbarry.pocketgreekdictionary.ui.theme.AzureBlue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The colour the app is tinted with — everything the theme draws in `primary`: headings, entry
 * words, the alphabet index and the tab bar.
 *
 * The value is held in a [StateFlow] rather than read from preferences where it is needed, so that
 * picking a new colour recomposes the theme immediately. Until [init] has run the default is used,
 * which is also what keeps `@Preview` composables working without any setup.
 */
object AccentColorService {
    /** The blue the app shipped with, and what a reset restores. */
    val defaultColor = AzureBlue

    private const val PREFS_NAME = "koine_dictionary_prefs"
    private const val ACCENT_COLOR_KEY = "accent_color"

    private val _accentColor = MutableStateFlow(defaultColor)
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    /** Loads the stored accent. Call before the first composition so the theme starts on it. */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _accentColor.value = Color(prefs.getInt(ACCENT_COLOR_KEY, defaultColor.toArgb()))
    }

    fun setAccentColor(context: Context, color: Color) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(ACCENT_COLOR_KEY, color.toArgb())
            .apply()

        _accentColor.value = color
    }

    fun resetAccentColor(context: Context) = setAccentColor(context, defaultColor)
}
