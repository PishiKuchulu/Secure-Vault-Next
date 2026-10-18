package com.securevault.next.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The SecureVault Next spacing scale.
 *
 * Part 6 (Spacing System): "Use an 8dp spacing grid. Consistent padding.
 * Consistent margins. Consistent alignment. Avoid random spacing
 * values." Every screen and component should reach for
 * [SecureVaultSpacingScope.spacing] instead of writing a raw `.dp`
 * literal, so the 8dp grid is enforced structurally rather than by
 * convention.
 */
data class SecureVaultSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp
)

internal val LocalSecureVaultSpacing = staticCompositionLocalOf { SecureVaultSpacing() }

/**
 * Namespace object mirroring `MaterialTheme.colorScheme` /
 * `MaterialTheme.typography`, so spacing reads naturally alongside the
 * rest of the theme: `SecureVaultSpacingScope.spacing.medium`.
 *
 * Named distinctly from the `SecureVaultTheme` composable function in
 * Theme.kt — Kotlin does not allow a top-level object and a top-level
 * function to share a name in the same file set.
 */
object SecureVaultSpacingScope {
    val spacing: SecureVaultSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSecureVaultSpacing.current
}
