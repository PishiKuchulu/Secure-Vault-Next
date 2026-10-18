package com.securevault.next.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SECURE_VAULT_STATE_CHANGE_DURATION_MS
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultEaseOut
import com.securevault.next.core.ui.theme.footnote

/**
 * Animated password-strength meter.
 *
 * Progress and color animate together over 250ms with the app easing —
 * brief, purposeful, ease-out. Strength is never conveyed by color
 * alone: pass [label] (e.g. "Weak", "Strong") so the meaning is also
 * in words, and the bar carries a content description for screen
 * readers.
 *
 * @param score Normalized strength in 0..1 (values are coerced).
 * @param label Optional textual strength, rendered beside the bar in
 * the matching signal color.
 */
@Composable
fun StrengthMeter(
    score: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = SecureVaultColorsScope.colors
    val clamped = score.coerceIn(0f, 1f)

    val animatedScore by animateFloatAsState(
        targetValue = clamped,
        animationSpec =
            tween(
                durationMillis = SECURE_VAULT_STATE_CHANGE_DURATION_MS,
                easing = SecureVaultEaseOut,
            ),
        label = "strengthProgress",
    )
    val meterColor by animateColorAsState(
        targetValue =
            when {
                clamped < 0.34f -> colors.danger
                clamped < 0.67f -> colors.warning
                else -> colors.success
            },
        animationSpec =
            tween(
                durationMillis = SECURE_VAULT_STATE_CHANGE_DURATION_MS,
                easing = SecureVaultEaseOut,
            ),
        label = "strengthColor",
    )

    val description = label ?: "Password strength ${(clamped * 100).toInt()} percent"

    Column(
        modifier = modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = meterColor,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.fill),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(animatedScore)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(meterColor),
            )
        }
    }
}
