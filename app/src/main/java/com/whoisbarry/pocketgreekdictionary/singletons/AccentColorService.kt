package com.whoisbarry.pocketgreekdictionary.singletons

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The colour the app is tinted with — everything the theme draws in `primary`: headings, entry
 * words, the alphabet index and the tab bar.
 *
 * Light and dark mode each keep their own accent, because a colour that reads well on a white
 * background is often unreadable on a black one and vice versa. The theme picks between them.
 *
 * The values are held in [StateFlow]s rather than read from preferences where they are needed, so
 * that picking a new colour recomposes the theme immediately. Until [init] has run the defaults
 * are used, which is also what keeps `@Preview` composables working without any setup.
 */
object AccentColorService {
    /** What light mode starts on, and what a reset restores it to. */
    val defaultLightColor = Color.Black

    /** What dark mode starts on, and what a reset restores it to. */
    val defaultDarkColor = Color.White

    private const val PREFS_NAME = "koine_dictionary_prefs"
    private const val LIGHT_ACCENT_COLOR_KEY = "accent_color_light"
    private const val DARK_ACCENT_COLOR_KEY = "accent_color_dark"

    /** The single accent used before light and dark were configured separately. */
    private const val LEGACY_ACCENT_COLOR_KEY = "accent_color"

    private val _lightAccentColor = MutableStateFlow(defaultLightColor)
    val lightAccentColor: StateFlow<Color> = _lightAccentColor.asStateFlow()

    private val _darkAccentColor = MutableStateFlow(defaultDarkColor)
    val darkAccentColor: StateFlow<Color> = _darkAccentColor.asStateFlow()

    /** Loads the stored accents. Call before the first composition so the theme starts on them. */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // An accent picked before this was split in two is kept for both modes, so that upgrading
        // doesn't silently throw away the colour the user chose.
        val legacy = prefs.getInt(LEGACY_ACCENT_COLOR_KEY, 0).takeIf { it != 0 }?.let { Color(it) }

        _lightAccentColor.value = prefs.readColor(
            LIGHT_ACCENT_COLOR_KEY,
            legacy ?: defaultLightColor
        )
        _darkAccentColor.value = prefs.readColor(
            DARK_ACCENT_COLOR_KEY,
            legacy ?: defaultDarkColor
        )
    }

    fun setLightAccentColor(context: Context, color: Color) {
        context.storeColor(LIGHT_ACCENT_COLOR_KEY, color)
        _lightAccentColor.value = color
    }

    fun setDarkAccentColor(context: Context, color: Color) {
        context.storeColor(DARK_ACCENT_COLOR_KEY, color)
        _darkAccentColor.value = color
    }

    /** Puts both modes back on the colours the app shipped with. */
    fun resetAccentColors(context: Context) {
        setLightAccentColor(context, defaultLightColor)
        setDarkAccentColor(context, defaultDarkColor)
    }

    private fun android.content.SharedPreferences.readColor(key: String, default: Color) =
        Color(getInt(key, default.toArgb()))

    private fun Context.storeColor(key: String, color: Color) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, color.toArgb())
            .apply()
    }
}
