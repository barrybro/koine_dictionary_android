package com.whoisbarry.pocketgreekdictionary

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.util.renderEntryImage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class EntryImageRendererTest {

    private fun entry(word: String, gloss: String, source: String) = DictionaryEntry(
        id = 1,
        difficulty = 1,
        frequency = 1,
        word = word,
        fullWord = word,
        gloss = gloss,
        keyLetter = word.take(1),
        sourceName = source,
        type = "noun",
        verbStem = ""
    )

    @Test
    fun rendersEntriesOfVaryingLength() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDir = File(context.getExternalFilesDir(null), "render-test").apply { mkdirs() }

        val samples = mapOf(
            "short" to entry("λόγος", "word, statement, message", "Strong's"),
            "medium" to entry(
                "δικαιοσύνη",
                "righteousness, justice; the quality or state of being in right relationship",
                "Louw-Nida Greek-English Lexicon"
            ),
            "long" to entry(
                "ἀντιπαρῆλθεν",
                "he passed by on the opposite side; a compound aorist form used to describe " +
                    "deliberately avoiding someone by crossing to the other side of the road, " +
                    "carrying the sense of both physical distance and moral indifference toward " +
                    "the person in need, and appearing in narrative contexts where the avoidance " +
                    "itself is the point of the account being told to the listener",
                "A Greek-English Lexicon of the New Testament and Other Early Christian Literature"
            ),
            "longword" to entry(
                "ἀλληλουϊαπροσευχησυναγωγή",
                "an artificially long compound used only to check word wrapping behaviour",
                "Test"
            )
        )

        samples.forEach { (name, sample) ->
            val bitmap = renderEntryImage(context, sample)
            assertTrue(bitmap.width == 1080)
            assertTrue(bitmap.height >= 1080)
            FileOutputStream(File(outputDir, "$name.png")).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
