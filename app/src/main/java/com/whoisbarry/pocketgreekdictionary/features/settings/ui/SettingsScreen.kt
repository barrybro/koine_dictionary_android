package com.whoisbarry.pocketgreekdictionary.features.settings.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.whoisbarry.pocketgreekdictionary.BuildConfig
import com.whoisbarry.pocketgreekdictionary.features.notification.DailyEntryNotification

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val updateInterval by viewModel.updateInterval.collectAsState()
    val notificationInterval by viewModel.notificationInterval.collectAsState()
    val lightAccentColor by viewModel.lightAccentColor.collectAsState()
    val darkAccentColor by viewModel.darkAccentColor.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    // Set while the system permission dialog is up, so the chosen interval can be applied once
    // the user answers it.
    var pendingNotificationInterval by remember { mutableStateOf<Int?>(null) }
    var notificationsBlocked by remember { mutableStateOf(false) }
    // Which of the two accents the picker is open for, or null while it is closed.
    var editingAccent by remember { mutableStateOf<AccentMode?>(null) }

    editingAccent?.let { mode ->
        AccentColorPickerDialog(
            title = "${mode.label} Accent Color",
            initialColor = if (mode == AccentMode.Light) lightAccentColor else darkAccentColor,
            onConfirm = { color ->
                when (mode) {
                    AccentMode.Light -> viewModel.setLightAccentColor(context, color)
                    AccentMode.Dark -> viewModel.setDarkAccentColor(context, color)
                }
                editingAccent = null
            },
            onDismiss = { editingAccent = null }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        val requested = pendingNotificationInterval
        pendingNotificationInterval = null

        // The result alone isn't enough: the permission can be granted while notifications are
        // still switched off for the app app-wide, in which case nothing would ever be posted.
        val allowed = DailyEntryNotification.areNotificationsAllowed(context)
        notificationsBlocked = !allowed
        if (allowed && requested != null) {
            viewModel.setNotificationInterval(context, requested)
        }
    }

    val selectNotificationInterval: (Int) -> Unit = { hours ->
        when {
            hours == DailyEntryNotification.INTERVAL_OFF ||
                DailyEntryNotification.areNotificationsAllowed(context) -> {
                notificationsBlocked = false
                viewModel.setNotificationInterval(context, hours)
            }

            // Before Android 13 there is no permission to ask for: notifications are already
            // switched off app-wide, which only system settings can undo.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                pendingNotificationInterval = hours
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> notificationsBlocked = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // About Group
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Pocket Greek Dictionary ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Only of use while testing, so it is kept out of what users see in a release.
                if (BuildConfig.DEBUG) {
                    Text(
                        text = "Build ${BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
//                Text(
//                    text = "By me",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
            }
        }

        // Accent Color Group
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Used for headings, entry words, the alphabet index and the tab bar. " +
                        "Each mode keeps its own colour.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AccentColorRow(
                    mode = AccentMode.Light,
                    color = lightAccentColor,
                    onClick = { editingAccent = AccentMode.Light }
                )

                AccentColorRow(
                    mode = AccentMode.Dark,
                    color = darkAccentColor,
                    onClick = { editingAccent = AccentMode.Dark }
                )

                TextButton(
                    onClick = { viewModel.resetAccentColors(context) },
                    enabled = lightAccentColor != viewModel.defaultLightAccentColor ||
                        darkAccentColor != viewModel.defaultDarkAccentColor,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = "Reset to defaults")
                }
            }
        }

        // Widget Settings Group
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Widget Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Update Interval",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                IntervalOptions(
                    options = INTERVAL_OPTIONS,
                    selected = updateInterval,
                    onSelect = { hours -> viewModel.setUpdateInterval(context, hours) }
                )
            }
        }

        // Notification Settings Group
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Notification Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Send a random entry",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                IntervalOptions(
                    options = INTERVAL_OPTIONS + (DailyEntryNotification.INTERVAL_OFF to "Off"),
                    selected = notificationInterval,
                    onSelect = selectNotificationInterval
                )

                if (notificationsBlocked) {
                    Text(
                        text = "Notifications are turned off for this app. Tap to enable them in " +
                            "system settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { openNotificationSettings(context) }
                    )
                }
            }
        }

        // Data Sources
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Sources",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "New Testament Greek Vocabulary List",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .clickable {
                            uriHandler.openUri("https://www.theology.edu/Remata/Spreadsheet/index.html")
                        }
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = "Dickinson Core Greek",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://dcc.dickinson.edu/greek-core-list")
                    }
                )
            }
        }
    }
}

/** The two accents a user can set, one for each of the themes the app follows. */
private enum class AccentMode(val label: String) {
    Light("Light Mode"),
    Dark("Dark Mode")
}

/** One tappable accent line: the swatch, its hex value, and which mode it applies to. */
@Composable
private fun AccentColorRow(
    mode: AccentMode,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorSwatch(color = color, modifier = Modifier.size(44.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${color.toHexString()} — tap to choose a colour",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Schedules offered for both the widget refresh and the entry notification, in hours. */
private val INTERVAL_OPTIONS = listOf(
    1 to "Every hour",
    12 to "Every 12 hours",
    24 to "Once a day"
)

@Composable
private fun IntervalOptions(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        options.forEach { (hours, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(hours) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == hours,
                    onClick = { onSelect(hours) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(intent) }
}
