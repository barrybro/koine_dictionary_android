package com.whoisbarry.pocketgreekdictionary.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.whoisbarry.pocketgreekdictionary.R

/**
 * The typefaces offered for Greek text — headwords, the alphabet and the index. English glosses
 * and the rest of the interface stay in the system font.
 *
 * Gentium Book Plus, GFS Didot and GFS Porson ship as their unmodified release files: their
 * licences reserve the font names for unaltered copies, and subsetting would count as altering
 * them. The Noto fonts have no reserved names, so they are subset to Latin, Greek and Greek
 * Extended, which covers every character in the dictionary. A font without a bold cut leaves
 * [boldRes] null, and bold text in it is synthesised.
 */
enum class GreekFont(
    val label: String,
    @FontRes val regularRes: Int?,
    @FontRes val boldRes: Int? = null
) {
    GentiumBookPlus("Gentium Book Plus", R.font.gentium_book_plus_regular, R.font.gentium_book_plus_bold),
    NotoSerif("Noto Serif", R.font.noto_serif_regular, R.font.noto_serif_bold),
    NotoSans("Noto Sans", R.font.noto_sans_regular, R.font.noto_sans_bold),
    GfsDidot("GFS Didot", R.font.gfs_didot_regular),
    GfsPorson("GFS Porson", R.font.gfs_porson_regular),

    /** Whatever the device ships with, which on some phones lacks the polytonic forms. */
    System("System default", null);

    val fontFamily: FontFamily by lazy {
        if (regularRes == null) {
            FontFamily.Default
        } else {
            FontFamily(
                listOfNotNull(
                    Font(regularRes, FontWeight.Normal),
                    boldRes?.let { Font(it, FontWeight.Bold) }
                )
            )
        }
    }

    /** The bold face, for text drawn outside Compose such as the shared entry image. */
    fun boldTypeface(context: Context): Typeface {
        val res = boldRes ?: regularRes ?: return Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val typeface = ResourcesCompat.getFont(context, res) ?: return Typeface.create(Typeface.SERIF, Typeface.BOLD)
        return if (boldRes != null) typeface else Typeface.create(typeface, Typeface.BOLD)
    }
}

/** The family Greek text is set in, provided by [KoineDictionaryTheme]. */
val LocalGreekFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
