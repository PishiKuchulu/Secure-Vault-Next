package com.securevault.next.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/*
 * SecureVault Next motion tokens.
 *
 * Motion laws (from the design skills, translated to Compose):
 * - Everything is timing, under 300ms, strong ease-out. Never ease-in
 *   on an entrance.
 * - Press feedback lands on press-IN, 100-150ms: scale 0.97 on buttons
 *   and cards, a background highlight (never scale) on list rows,
 *   opacity on bar buttons.
 * - When in doubt, delete the animation.
 */

/**
 * The single easing curve for the whole app: strong ease-out,
 * cubic-bezier(0.23, 1, 0.32, 1). Entrances, state changes and press
 * feedback all use it.
 */
val SecureVaultEaseOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Press-in feedback duration: lands on press-in, never later. */
const val SECURE_VAULT_PRESS_DURATION_MS = 120

/** Standard state-change duration (progress, color, selection). Always < 300ms. */
const val SECURE_VAULT_STATE_CHANGE_DURATION_MS = 250

/**
 * Scale target for press feedback on buttons and cards (0.97).
 * List rows never scale — they use a background highlight instead.
 */
const val SECURE_VAULT_PRESS_SCALE = 0.97f

/**
 * Applies press-in scale feedback (0.97 over 120ms, ease-out) driven by
 * [interactionSource]. Buttons and cards use this; list rows must not.
 *
 * The caller owns the [InteractionSource] (usually the same one passed
 * to the clickable/button) so the pressed state stays in sync:
 *
 *     val interactions = remember { MutableInteractionSource() }
 *     Button(
 *         onClick = onClick,
 *         interactionSource = interactions,
 *         modifier = Modifier.pressScale(interactions),
 *     ) { ... }
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    targetScale: Float = SECURE_VAULT_PRESS_SCALE,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec =
            tween(
                durationMillis = SECURE_VAULT_PRESS_DURATION_MS,
                easing = SecureVaultEaseOut,
            ),
        label = "secureVaultPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
