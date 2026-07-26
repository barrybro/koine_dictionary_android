package com.whoisbarry.pocketgreekdictionary.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.*
import com.whoisbarry.pocketgreekdictionary.MainActivity
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class DictionaryWidget : GlanceAppWidget() {

    // Required to read the widget's real current size via LocalSize, since this widget is
    // user-resizable and a single fixed layout (the default SizeMode.Single) would always
    // report the minWidth/minHeight from dictionary_widget_info.xml instead.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = DictionaryService.getRandomEntry(context)

        provideContent {
            GlanceTheme {
                DictionaryWidgetContent(entry)
            }
        }
    }

    @Composable
    private fun DictionaryWidgetContent(entry: DictionaryEntry?) {
        val backgroundColor = ColorProvider(
            day = Color(0xCCFFFFFF),
            night = Color(0xCC000000)
        )
        val padding = 16.dp
        val spacerHeight = 8.dp
        val size = LocalSize.current
        val contentWidth = (size.width - padding * 2).coerceAtLeast(40.dp)
        val contentHeight = (size.height - padding * 2).coerceAtLeast(24.dp)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (entry != null) {
                val wordFontSizeSp = fitFontSizeSp(
                    text = entry.word,
                    availableWidth = contentWidth,
                    availableHeight = contentHeight,
                    minSp = MIN_WORD_FONT_SP,
                    maxSp = MAX_WORD_FONT_SP,
                    charWidthFactor = WORD_CHAR_WIDTH_FACTOR,
                    maxLines = 1
                )
                val wordHeight = (wordFontSizeSp * LINE_HEIGHT_FACTOR).dp
                val glossAvailableHeight =
                    (contentHeight - wordHeight - spacerHeight).coerceAtLeast(16.dp)
                val glossFontSizeSp = fitFontSizeSp(
                    text = entry.gloss,
                    availableWidth = contentWidth,
                    availableHeight = glossAvailableHeight,
                    minSp = MIN_GLOSS_FONT_SP,
                    maxSp = MAX_GLOSS_FONT_SP,
                    charWidthFactor = GLOSS_CHAR_WIDTH_FACTOR
                )
                val glossMaxLines = linesNeededFor(
                    text = entry.gloss,
                    availableWidth = contentWidth,
                    fontSizeSp = glossFontSizeSp,
                    charWidthFactor = GLOSS_CHAR_WIDTH_FACTOR
                )

                Text(
                    text = entry.word,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = wordFontSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(spacerHeight))
                Text(
                    text = entry.gloss,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = glossFontSizeSp.sp
                    ),
                    maxLines = glossMaxLines
                )
            } else {
                Text(
                    text = "No entry found",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

private const val MIN_WORD_FONT_SP = 10f
private const val MAX_WORD_FONT_SP = 26f
private const val MIN_GLOSS_FONT_SP = 8f
private const val MAX_GLOSS_FONT_SP = 18f

// RemoteViews can't measure text directly, so wrapping is estimated from character count using
// an average glyph width as a fraction of font size. Bold (word) glyphs are wider than regular
// (gloss) ones, hence the separate factors.
private const val WORD_CHAR_WIDTH_FACTOR = 0.62f
private const val GLOSS_CHAR_WIDTH_FACTOR = 0.52f
private const val LINE_HEIGHT_FACTOR = 1.3f

/**
 * Largest font size in [minSp, maxSp] for which [text] is estimated to wrap into at most
 * [maxLines] lines within [availableWidth] x [availableHeight].
 */
private fun fitFontSizeSp(
    text: String,
    availableWidth: Dp,
    availableHeight: Dp,
    minSp: Float,
    maxSp: Float,
    charWidthFactor: Float,
    maxLines: Int = Int.MAX_VALUE
): Float {
    if (text.isEmpty()) return maxSp

    var fontSize = maxSp
    while (fontSize > minSp) {
        val lines = linesNeededFor(text, availableWidth, fontSize, charWidthFactor)
        val neededHeight = lines * fontSize * LINE_HEIGHT_FACTOR
        if (lines <= maxLines && neededHeight <= availableHeight.value) {
            return fontSize
        }
        fontSize -= 1f
    }
    return minSp
}

private fun linesNeededFor(
    text: String,
    availableWidth: Dp,
    fontSizeSp: Float,
    charWidthFactor: Float
): Int {
    val charsPerLine = (availableWidth.value / (fontSizeSp * charWidthFactor))
        .toInt()
        .coerceAtLeast(1)
    return ceil(text.length / charsPerLine.toFloat()).toInt().coerceAtLeast(1)
}

class DictionaryWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        DictionaryWidget().updateAll(context)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "DictionaryWidgetUpdateWorker"

        fun enqueue(context: Context, intervalHours: Int) {
            val request = PeriodicWorkRequestBuilder<DictionaryWidgetWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

class DictionaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DictionaryWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val prefs = context.getSharedPreferences("koine_dictionary_prefs", Context.MODE_PRIVATE)
        val interval = prefs.getInt("widget_update_interval", 24)
        DictionaryWidgetWorker.enqueue(context, interval)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        DictionaryWidgetWorker.cancel(context)
    }
}
