package com.whoisbarry.pocketgreekdictionary.features.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whoisbarry.pocketgreekdictionary.MainActivity
import com.whoisbarry.pocketgreekdictionary.R
import com.whoisbarry.pocketgreekdictionary.singletons.DictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * A recurring notification showing a random dictionary entry. Independent of the home screen
 * widget: it has its own schedule, its own setting, and runs whether or not a widget is placed.
 */
object DailyEntryNotification {

    /** Interval meaning "don't notify at all"; any other value is a number of hours. */
    const val INTERVAL_OFF = 0

    private const val CHANNEL_ID = "daily_entry"

    /** Fixed, so a new entry replaces the previous one rather than stacking up unread words. */
    private const val NOTIFICATION_ID = 1001

    /**
     * Posts a random entry. Silently does nothing when the user has notifications turned off for
     * the app — the schedule can outlive the permission, since it is revocable from system
     * settings at any time.
     */
    // Lint can't follow the POST_NOTIFICATIONS check into areNotificationsAllowed, which the
    // settings screen needs to call on its own anyway.
    @SuppressLint("MissingPermission")
    fun showRandomEntry(context: Context) {
        if (!areNotificationsAllowed(context)) return
        val entry = DictionaryService.getRandomEntry(context) ?: return

        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            MainActivity.entryIntent(context, entry.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_book_2_24)
            .setContentTitle(entry.headword)
            .setContentText(entry.primaryGloss)
            .setStyle(NotificationCompat.BigTextStyle().bigText(entry.gloss))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Whether a posted notification would actually reach the user: the runtime permission on
     * Android 13+, plus the app-wide toggle in system settings on every version.
     */
    fun areNotificationsAllowed(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}

class DailyEntryNotificationWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        DailyEntryNotification.showRandomEntry(context)
        Result.success()
    }

    companion object {
        private const val WORK_NAME = "DailyEntryNotificationWorker"

        /**
         * Schedules the notification every [intervalHours]. The first one waits out a full
         * interval: a notification firing the instant the setting is touched reads as a bug, and
         * re-enqueueing restarts the clock so the chosen spacing always holds from here on.
         */
        fun enqueue(context: Context, intervalHours: Int) {
            val request = PeriodicWorkRequestBuilder<DailyEntryNotificationWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            )
                .setInitialDelay(intervalHours.toLong(), TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
