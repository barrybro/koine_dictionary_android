package com.whoisbarry.koinedictionary.features.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.whoisbarry.koinedictionary.MainActivity
import com.whoisbarry.koinedictionary.R
import com.whoisbarry.koinedictionary.data.models.DictionaryEntry
import com.whoisbarry.koinedictionary.singletons.DictionaryService
import androidx.glance.appwidget.updateAll
import androidx.work.*
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
                val context = LocalContext.current
                AndroidRemoteViews(
                    remoteViews = RemoteViews(context.packageName, R.layout.widget_autosize_text).apply {
                        setTextViewText(R.id.widget_word, entry.word)
                        setTextViewText(R.id.widget_gloss, entry.gloss)

                        // Apply colors from GlanceTheme
                        val onSurface = GlanceTheme.colors.onSurface.getColor(context)
                        val onSurfaceVariant = GlanceTheme.colors.onSurfaceVariant.getColor(context)

                        setTextColor(R.id.widget_word, onSurface.toArgb())
                        setTextColor(R.id.widget_gloss, onSurfaceVariant.toArgb())
                    },
                    modifier = GlanceModifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "No entry found",
                    style = TextStyle(color = GlanceTheme.colors.onSurface)
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
