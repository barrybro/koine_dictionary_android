package com.whoisbarry.koinedictionary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AzureBlue,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0) // Light grey for variant
)

private val LightColorScheme = lightColorScheme(
    primary = AzureBlue,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF49454F) // Dark grey for variant
)

@Composable
fun KoineDictionaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}