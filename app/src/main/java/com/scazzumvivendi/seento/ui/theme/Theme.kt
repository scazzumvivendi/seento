package com.scazzumvivendi.seento.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SeentoRedLight,
    secondary = SeentoRedLight,
    tertiary = SeentoRedLight,
    background = SeentoInk,
    surface = Color(0xFF2B2324),
    surfaceVariant = Color(0xFF493638)
)

private val LightColorScheme = lightColorScheme(
    primary = SeentoRed,
    secondary = SeentoRed,
    tertiary = SeentoRed,
    background = SeentoCanvas,
    surface = Color.White,
    surfaceVariant = SeentoRedSoft,
    onBackground = SeentoInk,
    onSurface = SeentoInk,
    onSurfaceVariant = SeentoMuted,
    error = Color(0xFFB3261E),
    outline = Color(0xFFE5CBCD)
)

@Composable
fun SeentoTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]?.toLanguageTag() ?: "en"
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = remember(locale) {
        Typography.copy(
            displayLarge = Typography.displayLarge.copy(),
            displayMedium = Typography.displayMedium.copy(),
            displaySmall = Typography.displaySmall.copy(),
            headlineLarge = Typography.headlineLarge.copy(),
            headlineMedium = Typography.headlineMedium.copy(),
            headlineSmall = Typography.headlineSmall.copy(),
            titleLarge = Typography.titleLarge.copy(),
            titleMedium = Typography.titleMedium.copy(),
            titleSmall = Typography.titleSmall.copy(),
            bodyLarge = Typography.bodyLarge.copy(),
            bodyMedium = Typography.bodyMedium.copy(),
            bodySmall = Typography.bodySmall.copy(),
            labelLarge = Typography.labelLarge.copy(),
            labelMedium = Typography.labelMedium.copy(),
            labelSmall = Typography.labelSmall.copy()
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
