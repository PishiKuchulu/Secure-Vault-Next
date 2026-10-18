package com.securevault.next.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * SecureVault Next — semantic color tokens.
 *
 * Two hard rules govern every screen:
 *
 *  1. ONE LOCKED ACCENT. The accent is #0B6EDE (light) / #4DA3FF (dark)
 *     and it is THE accent everywhere: primary actions, active states,
 *     progress, selection. No other hue may act as an accent. Neutrals
 *     carry the interface; the accent is spent where it matters.
 *
 *  2. ONE GREY FAMILY. Every neutral below is a cool neutral grey
 *     (barely blue-shifted, iOS-like). Warm greys never appear.
 *
 * There are no gradients, no decorative hues, and no per-screen accent
 * improvisation. Every hex literal in the design system lives in this
 * file; components read tokens only, never literals.
 */

/** The single locked accent color (light theme). White text on it is 4.9:1. */
val AccentLight = Color(0xFF0B6EDE)

/** The single locked accent color (dark theme). */
val AccentDark = Color(0xFF4DA3FF)

/** Content drawn on top of the accent (light theme). */
val OnAccentLight = Color(0xFFFFFFFF)

/** Content drawn on top of the accent (dark theme). */
val OnAccentDark = Color(0xFF002952)

/** Soft tinted container for accent highlights (light theme). */
val AccentSoftLight = Color(0xFFE2EFFD)

/** Soft tinted container for accent highlights (dark theme). */
val AccentSoftDark = Color(0xFF14283F)

/*
 * Cool neutral grey family (single family, light theme).
 * Used structurally, never as decoration.
 */
private val GreyLightBase = Color(0xFFF2F2F7) // screen base (inset-grouped)
private val GreyLightSurface = Color(0xFFFFFFFF) // cards, sheets
private val GreyLightLabel = Color(0xFF111214) // primary text
private val GreyLightSecondaryLabel = Color(0xFF6C6C70) // secondary text, 5.2:1 on surface
private val GreyLightSeparator = Color(0xFFD1D1D6) // hairlines
private val GreyLightFill = Color(0xFFE9E9EE) // subtle fills (chips, pressed states)

/*
 * Cool neutral grey family (single family, dark theme).
 */
private val GreyDarkBase = Color(0xFF0A0A0C) // screen base
private val GreyDarkSurface = Color(0xFF1C1C1E) // cards, sheets
private val GreyDarkLabel = Color(0xFFFFFFFF) // primary text
private val GreyDarkSecondaryLabel = Color(0xFF9B9BA1) // secondary text
private val GreyDarkSeparator = Color(0xFF38383C) // hairlines
private val GreyDarkFill = Color(0xFF2C2C30) // subtle fills

// Semantic signal colors: one meaning per color, both themes.

val SuccessLight = Color(0xFF237A3B)
val SuccessDark = Color(0xFF3FB950)

val WarningLight = Color(0xFF8A5A00)
val WarningDark = Color(0xFFFFB340)

val DangerLight = Color(0xFFD92D20)
val DangerDark = Color(0xFFFF6B61)

/** Content drawn on top of danger-filled surfaces (both themes). */
val OnDangerLight = Color(0xFFFFFFFF)
val OnDangerDark = Color(0xFF3D0A06)

/**
 * The complete semantic token set resolved for one theme. Components
 * read these tokens; they never reach for a hex literal or a
 * Material3 slot directly.
 */
data class SecureVaultColors(
    /** Screen base background (inset-grouped look). */
    val baseBackground: Color,
    /** Cards, sheets, menus — surfaces that sit on [baseBackground]. */
    val surface: Color,
    /** Primary text and iconography. */
    val label: Color,
    /** Secondary text, captions, placeholders. */
    val secondaryLabel: Color,
    /** Hairline separators and unfocused input borders. */
    val separator: Color,
    /** Subtle fills: chips, icon wells, pressed states. */
    val fill: Color,
    /** The one locked accent. */
    val accent: Color,
    /** Content on top of the accent. */
    val onAccent: Color,
    /** Soft tinted container for accent highlights. */
    val accentSoft: Color,
    /** Success signal. */
    val success: Color,
    /** Warning signal. */
    val warning: Color,
    /** Danger / destructive signal. */
    val danger: Color,
    /** Content on top of danger-filled surfaces. */
    val onDanger: Color
)

val LightSecureVaultColors =
    SecureVaultColors(
        baseBackground = GreyLightBase,
        surface = GreyLightSurface,
        label = GreyLightLabel,
        secondaryLabel = GreyLightSecondaryLabel,
        separator = GreyLightSeparator,
        fill = GreyLightFill,
        accent = AccentLight,
        onAccent = OnAccentLight,
        accentSoft = AccentSoftLight,
        success = SuccessLight,
        warning = WarningLight,
        danger = DangerLight,
        onDanger = OnDangerLight
    )

val DarkSecureVaultColors =
    SecureVaultColors(
        baseBackground = GreyDarkBase,
        surface = GreyDarkSurface,
        label = GreyDarkLabel,
        secondaryLabel = GreyDarkSecondaryLabel,
        separator = GreyDarkSeparator,
        fill = GreyDarkFill,
        accent = AccentDark,
        onAccent = OnAccentDark,
        accentSoft = AccentSoftDark,
        success = SuccessDark,
        warning = WarningDark,
        danger = DangerDark,
        onDanger = OnDangerDark
    )

internal val LocalSecureVaultColors = staticCompositionLocalOf { LightSecureVaultColors }

/**
 * Namespace object mirroring [SecureVaultSpacingScope], so colors read
 * naturally next to the rest of the theme:
 * `SecureVaultColorsScope.colors.accent`.
 */
object SecureVaultColorsScope {
    val colors: SecureVaultColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSecureVaultColors.current
}
