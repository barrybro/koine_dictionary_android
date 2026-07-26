package com.whoisbarry.pocketgreekdictionary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.features.widget.DictionaryWidget
import com.whoisbarry.pocketgreekdictionary.features.widget.LARGE_SIZE
import com.whoisbarry.pocketgreekdictionary.features.widget.WIDGET_PADDING_DP
import com.whoisbarry.pocketgreekdictionary.features.widget.entryRemoteViews
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the widget layout at the sizes a user can resize the widget to and checks that no text
 * is truncated. Each render is also written to the app's external files dir for eyeballing.
 */
@RunWith(AndroidJUnit4::class)
class DictionaryWidgetRenderTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val outputDir = File(context.getExternalFilesDir(null), "widget-render").apply {
        mkdirs()
    }

    /** Widget sizes in dp, before the surrounding padding is subtracted. */
    private val sizes = listOf(
        "2x1" to DpSize(110.dp, 70.dp),
        "2x2" to DpSize(110.dp, 110.dp),
        "3x2" to DpSize(180.dp, 110.dp),
        "4x2" to DpSize(250.dp, 110.dp),
        "4x4" to DpSize(250.dp, 250.dp)
    )

    private fun entry(word: String, gloss: String) = DictionaryEntry(
        id = 1, difficulty = 1, frequency = 1, word = word, fullWord = word, gloss = gloss,
        keyLetter = word.take(1), sourceName = "Test", type = "noun", verbStem = ""
    )

    @Test
    fun everySizeRendersWithoutTruncatingText() {
        // Shortest, typical and longest content in the dictionary, plus a principal-parts entry,
        // which is the worst case for the word line.
        val samples = mapOf(
            "short" to entry("λόγος", "word"),
            "typical" to entry("ἀββά", "Abba, father"),
            "longgloss" to entry(
                "κατά",
                "down, down (from or along), throughout, according to; κατὰ γῆν by land; " +
                    "κατὰ φύσιν in accordance with nature; κατ' ἔθνη by nations; καθ᾿ἕνα one by one."
            ),
            "principalparts" to entry(
                "τελευτάω, τελευτήσω, ἐτελεύτησα, τετελεύτηκα, τετελεύτημαι, ἐτελευτήθην",
                "die, end, finish"
            )
        )

        // A 2x1 strip is only ~46dp of content height; the longest gloss in the dictionary
        // cannot fit there even trimmed to its first sense.
        val allowedToEllipsize = setOf("longgloss-2x1")
        val failures = mutableListOf<String>()

        samples.forEach { (name, sample) ->
            sizes.forEach { (sizeName, size) ->
                val root = inflateEntryLayout(sample, size)
                saveScreenshot(root, "$name-$sizeName.png")

                val word = root.requireView<TextView>(R.id.widget_word)
                val gloss = root.requireView<TextView>(R.id.widget_gloss)
                val case = "$name-$sizeName"

                if (word.isTruncated()) failures += "$case: word truncated (${word.describe()})"
                if (gloss.isTruncated() && case !in allowedToEllipsize) {
                    failures += "$case: gloss truncated (${gloss.describe()})"
                }
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    /** The Glance composition must hand the layout the full widget area to size against. */
    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun composedWidgetFillsAvailableSpace() {
        val remoteViews = runBlocking {
            DictionaryWidget().compose(context = context, size = DpSize(250.dp, 110.dp))
        }

        val root = FrameLayout(context).also { parent ->
            parent.addView(remoteViews.apply(context, parent))
        }
        root.measureAndLayout(250f.toPx(), 110f.toPx())
        saveScreenshot(root, "composed-4x2.png")

        val word = root.requireView<TextView>(R.id.widget_word)
        val gloss = root.requireView<TextView>(R.id.widget_gloss)

        assertTrue("word view was not laid out", word.width > 0 && word.height > 0)
        assertTrue("gloss view was not laid out", gloss.width > 0 && gloss.height > 0)
        // Padding is 12dp a side; anything much narrower means the layout was not stretched.
        val expectedContentWidth = (250 - 2 * WIDGET_PADDING_DP).toFloat().toPx()
        assertTrue(
            "layout did not fill the widget (word width ${word.width}px, " +
                "expected about ${expectedContentWidth}px)",
            word.width >= expectedContentWidth * 0.9f
        )
        assertTrue("word text missing", word.text.isNotEmpty())
    }

    private fun inflateEntryLayout(entry: DictionaryEntry, size: DpSize): View {
        val parent = FrameLayout(context)
        val isSmall = size.width < LARGE_SIZE.width
        val view = entryRemoteViews(context, entry, isSmall).apply(context, parent)
        parent.addView(view)
        parent.measureAndLayout(
            (size.width.value - 2 * WIDGET_PADDING_DP).toPx(),
            (size.height.value - 2 * WIDGET_PADDING_DP).toPx()
        )
        return parent
    }

    private fun View.measureAndLayout(widthPx: Int, heightPx: Int) {
        measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, widthPx, heightPx)
    }

    private fun Float.toPx(): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
    ).toInt()

    private fun saveScreenshot(view: View, fileName: String) {
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(Color.WHITE)
        view.draw(Canvas(bitmap))
        FileOutputStream(File(outputDir, fileName)).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun TextView.describe(): String {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return "${textSize / scaledDensity}sp, $lineCount lines, ${height}px tall"
    }
}

/** True when the layout ellipsized the text or ran past the bottom of the view. */
private fun TextView.isTruncated(): Boolean {
    val layout = layout ?: return true
    val ellipsized = (0 until layout.lineCount).any { layout.getEllipsisCount(it) > 0 }
    return ellipsized || layout.getLineBottom(layout.lineCount - 1) > height
}

@Suppress("UNCHECKED_CAST")
private fun <T : View> View.requireView(id: Int): T =
    requireNotNull(findViewByIdRecursive(id)) { "view $id not found" } as T

private fun View.findViewByIdRecursive(id: Int): View? {
    findViewById<View>(id)?.let { return it }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).findViewByIdRecursive(id)?.let { return it }
        }
    }
    return null
}
