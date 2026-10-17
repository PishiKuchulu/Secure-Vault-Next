package com.securevault.next.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * The Material3 color scheme is derived from the semantic token set in
 * Color.kt — it exists so Material3 components render correctly, but
 * SecureVault components read SecureVaultColorsScope.colors directly.
 * No hex literal appears here or in any component.
 */
private fun SecureVaultColors.toMaterialScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentSoft,
        onSecondaryContainer = accent,
        tertiary = secondaryLabel,
        onTertiary = label,
        error = danger,
        onError = onDanger,
        errorContainer = danger,
        onErrorContainer = onDanger,
        background = baseBackground,
        onBackground = label,
        surface = surface,
        onSurface = label,
        surfaceVariant = fill,
        onSurfaceVariant = secondaryLabel,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = fill,
        surfaceContainerHigh = fill,
        surfaceContainerHighest = fill,
        outline = separator,
        outlineVariant = separator,
        scrim = baseBackground,
        inverseSurface = label,
        inverseOnSurface = baseBackground,
        inversePrimary = accent,
    )
}

/**
 * SecureVault Next theme. Resolves the full semantic token set for the
 * active appearance and provides it to the composition.
 *
 * @param darkTheme Whether to use the dark token set. Defaults to the
 * system setting. There is no in-app appearance switch: both
 * appearances must render correctly from day one.
 * @param dynamicColor Kept for API compatibility; defaults to false and
 * is intentionally NOT honored. Dynamic (wallpaper-derived) colors
 * would break the locked single accent and the single grey family, so
 * the design system always resolves its own tokens.
 */
@Composable
fun SecureVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSecureVaultColors else LightSecureVaultColors
    val colorScheme = colors.toMaterialScheme(darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // System bars follow the screen base token — never the accent.
            window.statusBarColor = colors.baseBackground.toArgb()
            window.navigationBarColor = colors.baseBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSecureVaultColors provides colors,
        LocalSecureVaultSpacing provides SecureVaultSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SecureVaultTypography,
            shapes = SecureVaultShapes,
            content = content
        )
    }
}
