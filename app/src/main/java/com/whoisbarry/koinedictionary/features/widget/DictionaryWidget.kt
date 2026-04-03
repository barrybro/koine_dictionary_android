package com.whoisbarry.koinedictionary.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.whoisbarry.koinedictionary.MainActivity
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry
import com.whoisbarry.koinedictionary.singletons.DictionaryService
import java.util.concurrent.TimeUnit

class DictionaryWidget : GlanceAppWidget() {

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

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (entry != null) {
                // Dynamic font size for the word based on its length
                val wordFontSize = when {
                    entry.word.length > 20 -> 14.sp
                    entry.word.length > 15 -> 18.sp
                    entry.word.length > 10 -> 21.sp
                    else -> 28.sp
                }

                // Dynamic font size for the gloss based on its length
                val glossFontSize = when {
                    entry.gloss.length > 400 -> 6.sp
                    entry.gloss.length > 300 -> 7.sp
                    entry.gloss.length > 250 -> 8.sp
                    entry.gloss.length > 200 -> 9.sp
                    entry.gloss.length > 150 -> 10.sp
                    entry.gloss.length > 100 -> 11.sp
                    entry.gloss.length > 70 -> 12.sp
                    entry.gloss.length > 50 -> 14.sp
                    else -> 18.sp
                }

                Text(
                    text = entry.word,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = wordFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = entry.gloss,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = glossFontSize
                    ),
                    maxLines = 12
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
