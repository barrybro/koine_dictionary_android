package com.whoisbarry.pocketgreekdictionary.features.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.*
import com.whoisbarry.pocketgreekdictionary.MainActivity
import com.whoisbarry.pocketgreekdictionary.R
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import java.util.concurrent.TimeUnit

class DictionaryWidget : GlanceAppWidget() {

    // Fitting the text is the layout's job: the TextViews autosize themselves (see
    // res/layout/widget_dictionary_entry.xml). Size is only consulted to decide how much of a
    // long gloss to offer, so two buckets are enough — no per-pixel recomposition needed.
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, LARGE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = currentEntry(context, id)

        provideContent {
            GlanceTheme {
                DictionaryWidgetContent(entry)
            }
        }
    }

    @Composable
    private fun DictionaryWidgetContent(entry: DictionaryEntry?) {
        val context = LocalContext.current
        val isSmall = LocalSize.current.width < LARGE_SIZE.width
        val backgroundColor = ColorProvider(
            day = Color(0xCCFFFFFF),
            night = Color(0xCC000000)
        )

        // Tapping the widget opens the word it is showing; with nothing to show, the app itself.
        val openAction = if (entry != null) {
            actionStartActivity(MainActivity.entryIntent(context, entry.id))
        } else {
            actionStartActivity<MainActivity>()
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(WIDGET_PADDING_DP.dp)
                .clickable(openAction),
            contentAlignment = Alignment.Center
        ) {
            if (entry != null) {
                AndroidRemoteViews(
                    remoteViews = entryRemoteViews(context, entry, isSmall),
                    modifier = GlanceModifier.fillMaxSize()
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

    companion object {
        private val ENTRY_ID_KEY = intPreferencesKey("widget_entry_id")

        /**
         * Draws a new random entry on every placed widget. Redraws triggered by anything else
         * (resize, reboot, host refresh) keep showing the stored entry, so the word only changes
         * when a refresh is actually intended.
         */
        suspend fun refreshAll(context: Context) {
            val widget = DictionaryWidget()
            GlanceAppWidgetManager(context)
                .getGlanceIds(DictionaryWidget::class.java)
                .forEach { glanceId ->
                    val entry = DictionaryService.getRandomEntry(context)
                    if (entry != null) {
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[ENTRY_ID_KEY] = entry.id
                        }
                    }
                    widget.update(context, glanceId)
                }
        }

        private suspend fun currentEntry(context: Context, id: GlanceId): DictionaryEntry? {
            val storedId = runCatching {
                getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[ENTRY_ID_KEY]
            }.getOrNull()

            storedId?.let { DictionaryService.getEntryById(context, it) }?.let { return it }

            val entry = DictionaryService.getRandomEntry(context) ?: return null
            runCatching {
                updateAppWidgetState(context, id) { prefs -> prefs[ENTRY_ID_KEY] = entry.id }
            }
            return entry
        }
    }
}

internal const val WIDGET_PADDING_DP = 12

/** Smallest placement the widget allows, matching dictionary_widget_info.xml. */
internal val SMALL_SIZE = DpSize(110.dp, 70.dp)

/** Anything at least this wide gets the full gloss rather than just its first sense. */
internal val LARGE_SIZE = DpSize(180.dp, 110.dp)

internal fun entryRemoteViews(
    context: Context,
    entry: DictionaryEntry,
    isSmall: Boolean
): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_dictionary_entry).apply {
        setTextViewText(R.id.widget_word, entry.headword)
        setTextViewText(R.id.widget_gloss, if (isSmall) entry.primaryGloss else entry.gloss)
    }

class DictionaryWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        DictionaryWidget.refreshAll(context)
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
                ExistingPeriodicWorkPolicy.UPDATE,
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
