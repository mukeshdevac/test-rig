package com.loftitan.eoltestfixture.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BambuDarkColorScheme = darkColorScheme(
    primary = BambuAccentGreen,
    onPrimary = BambuSlateBackground,
    secondary = BambuCyan,
    onSecondary = BambuSlateBackground,
    background = BambuSlateBackground,
    onBackground = BambuTextPrimary,
    surface = BambuCardSurface,
    onSurface = BambuTextPrimary,
    error = BambuErrorRed,
    onError = BambuTextPrimary
)

@Composable
fun LOFTitanEOLTestFixtureTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BambuSlateBackground.toArgb()
                window.navigationBarColor = BambuSlateBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = BambuDarkColorScheme,
        typography = Typography,
        content = content
    )
}
