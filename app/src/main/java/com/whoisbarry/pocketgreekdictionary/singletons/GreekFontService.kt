package com.whoisbarry.pocketgreekdictionary.singletons

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.whoisbarry.pocketgreekdictionary.ui.theme.GreekFont

/**
 * The font Greek text is set in. Held in a [StateFlow] for the same reason as
 * [AccentColorService]: the theme reads it at the root of the app, so choosing a font in Settings
 * restyles every screen immediately. Until [init] has run the default is used.
 */
object GreekFontService {
    val defaultFont = GreekFont.GentiumBookPlus

    private const val PREFS_NAME = "koine_dictionary_prefs"
    private const val GREEK_FONT_KEY = "greek_font"

    private val _greekFont = MutableStateFlow(defaultFont)
    val greekFont: StateFlow<GreekFont> = _greekFont.asStateFlow()

    /** Loads the stored font. Call before the first composition so the theme starts on it. */
    fun init(context: Context) {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(GREEK_FONT_KEY, null)
        // A name that no longer matches an option (a font since removed) falls back to the default.
        _greekFont.value = GreekFont.entries.firstOrNull { it.name == stored } ?: defaultFont
    }

    fun setGreekFont(context: Context, font: GreekFont) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(GREEK_FONT_KEY, font.name)
            .apply()
        _greekFont.value = font
    }
}
