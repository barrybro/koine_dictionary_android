package com.whoisbarry.pocketgreekdictionary.features.settings.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.glance.appwidget.updateAll
import com.whoisbarry.pocketgreekdictionary.features.notification.DailyEntryNotification
import com.whoisbarry.pocketgreekdictionary.features.notification.DailyEntryNotificationWorker
import com.whoisbarry.pocketgreekdictionary.features.widget.DictionaryWidget
import com.whoisbarry.pocketgreekdictionary.features.widget.DictionaryWidgetWorker
import com.whoisbarry.pocketgreekdictionary.singletons.AccentColorService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val PREFS_NAME = "koine_dictionary_prefs"
    private val WIDGET_INTERVAL_KEY = "widget_update_interval"
    private val NOTIFICATION_INTERVAL_KEY = "notification_interval"

    private val _updateInterval = MutableStateFlow(24) // Default 24 hours
    val updateInterval: StateFlow<Int> = _updateInterval

    private val _notificationInterval = MutableStateFlow(DailyEntryNotification.INTERVAL_OFF)
    val notificationInterval: StateFlow<Int> = _notificationInterval

    /**
     * The accents live in [AccentColorService] rather than in this ViewModel: the theme reads them
     * at the root of the app, above and outside the Settings screen that changes them.
     */
    val lightAccentColor: StateFlow<Color> = AccentColorService.lightAccentColor
    val darkAccentColor: StateFlow<Color> = AccentColorService.darkAccentColor
    val defaultLightAccentColor: Color = AccentColorService.defaultLightColor
    val defaultDarkAccentColor: Color = AccentColorService.defaultDarkColor

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _updateInterval.value = prefs.getInt(WIDGET_INTERVAL_KEY, 24)

        _notificationInterval.value =
            prefs.getInt(NOTIFICATION_INTERVAL_KEY, DailyEntryNotification.INTERVAL_OFF)

        // Notification permission can be revoked from system settings while the schedule is still
        // stored, so a setting the user can no longer act on is reset rather than shown as on.
        if (!DailyEntryNotification.areNotificationsAllowed(context)) {
            setNotificationInterval(context, DailyEntryNotification.INTERVAL_OFF)
        }
    }

    fun setLightAccentColor(context: Context, color: Color) {
        AccentColorService.setLightAccentColor(context, color)
    }

    fun setDarkAccentColor(context: Context, color: Color) {
        AccentColorService.setDarkAccentColor(context, color)
    }

    fun resetAccentColors(context: Context) {
        AccentColorService.resetAccentColors(context)
    }

    fun setUpdateInterval(context: Context, hours: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(WIDGET_INTERVAL_KEY, hours).apply()
        _updateInterval.value = hours

        // Update the WorkManager job with the new interval
        DictionaryWidgetWorker.enqueue(context, hours)

        // Trigger an immediate widget refresh
        MainScope().launch {
            DictionaryWidget.refreshAll(context)
        }
    }

    /**
     * Sets how often a random entry is delivered as a notification, or turns them off with
     * [DailyEntryNotification.INTERVAL_OFF]. Callers are responsible for having a usable
     * notification permission first — see [DailyEntryNotification.areNotificationsAllowed].
     */
    fun setNotificationInterval(context: Context, hours: Int) {
        if (_notificationInterval.value == hours) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(NOTIFICATION_INTERVAL_KEY, hours).apply()
        _notificationInterval.value = hours

        if (hours == DailyEntryNotification.INTERVAL_OFF) {
            DailyEntryNotificationWorker.cancel(context)
        } else {
            DailyEntryNotificationWorker.enqueue(context, hours)
        }
    }
}
