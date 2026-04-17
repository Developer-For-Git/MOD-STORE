package com.sorwe.store.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sorwe.store.data.preferences.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonRedDark.copy(alpha = 0.2f),
    onPrimaryContainer = CrimsonRedLight,
    secondary = SecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = SecondaryAccent.copy(alpha = 0.15f),
    onSecondaryContainer = SecondaryAccent,
    tertiary = GlassTeal,
    onTertiary = Color.White,
    tertiaryContainer = GlassTeal.copy(alpha = 0.15f),
    onTertiaryContainer = GlassTeal,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ErrorRed,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonRedLight.copy(alpha = 0.4f),
    onPrimaryContainer = CrimsonRedDark,
    secondary = GlassAqua,
    onSecondary = Color.White,
    secondaryContainer = GlassAquaSoft.copy(alpha = 0.2f),
    onSecondaryContainer = GlassAqua,
    tertiary = GlassTeal,
    onTertiary = Color.White,
    tertiaryContainer = GlassTeal.copy(alpha = 0.15f),
    onTertiaryContainer = GlassTeal,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorRed,
    onError = Color.White,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemeEntryPoint {
    fun userPreferences(): UserPreferences
}

@Composable
fun SorweStoreTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ThemeEntryPoint::class.java
    )
    val userPreferences = entryPoint.userPreferences()
    val isDarkMode = true
    val customAccentHex by userPreferences.customAccentColor.collectAsState(initial = "#FF3B5C")
    
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(customAccentHex))
    } catch (e: Exception) {
        CrimsonRed
    }

    val baseColorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
    val colorScheme = baseColorScheme.copy(primary = primaryColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkMode
                isAppearanceLightNavigationBars = !isDarkMode
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SorweTypography,
        content = content
    )
}
