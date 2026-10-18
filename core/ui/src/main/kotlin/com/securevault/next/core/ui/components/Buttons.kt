package com.securevault.next.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.pressScale

/*
 * Shape lock: every button is a pill (fully rounded ends), minimum
 * 48dp tall. Press feedback is scale 0.97 over 120ms on press-in;
 * list rows use a background highlight instead and never scale.
 *
 * Public signatures are stable: screens reference VaultButton and
 * VaultTextButton, so those keep their names and parameters.
 */

private val ButtonMinHeight = 48.dp

@Composable
private fun VaultButtonInnerContent(
    text: String,
    loading: Boolean,
    icon: ImageVector?,
    loadingColor: androidx.compose.ui.graphics.Color
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = loadingColor,
            strokeWidth = 2.dp
        )
    } else {
        if (icon != null) {
            // Decorative: the button label already names the action.
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(SecureVaultSpacingScope.spacing.small))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Primary action. Filled with the locked accent; use once per screen
 * for the single most important action.
 */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = SecureVaultColorsScope.colors
    val interactions = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier =
        modifier
            .heightIn(min = ButtonMinHeight)
            .pressScale(interactions),
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = CircleShape,
        colors =
        ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.accent.copy(alpha = 0.30f),
            disabledContentColor = colors.onAccent.copy(alpha = 0.55f)
        ),
        contentPadding =
        PaddingValues(
            start = 24.dp,
            end = 24.dp
        )
    ) {
        VaultButtonInnerContent(text, loading, icon, colors.onAccent)
    }
}

/**
 * Secondary action. Soft accent tint; pairs with [VaultButton] when a
 * screen has two actions of unequal weight.
 */
@Composable
fun VaultSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = SecureVaultColorsScope.colors
    val interactions = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier =
        modifier
            .heightIn(min = ButtonMinHeight)
            .pressScale(interactions),
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = CircleShape,
        colors =
        ButtonDefaults.buttonColors(
            containerColor = colors.accentSoft,
            contentColor = colors.accent,
            disabledContainerColor = colors.fill.copy(alpha = 0.60f),
            disabledContentColor = colors.secondaryLabel.copy(alpha = 0.55f)
        ),
        contentPadding =
        PaddingValues(
            start = 24.dp,
            end = 24.dp
        )
    ) {
        VaultButtonInnerContent(text, loading, icon, colors.accent)
    }
}

/**
 * Tertiary action. Text only; for low-emphasis actions such as
 * "Cancel" placed next to a primary or secondary button.
 */
@Composable
fun VaultTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = SecureVaultColorsScope.colors
    val interactions = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier =
        modifier
            .heightIn(min = ButtonMinHeight)
            .pressScale(interactions),
        enabled = enabled,
        interactionSource = interactions,
        shape = CircleShape,
        colors =
        ButtonDefaults.textButtonColors(
            contentColor = colors.accent,
            disabledContentColor = colors.secondaryLabel.copy(alpha = 0.55f)
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Destructive action. Filled with the danger token; reserved for
 * irreversible actions (delete, wipe). Destructive confirms always
 * carry a Cancel alongside.
 */
@Composable
fun VaultDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = SecureVaultColorsScope.colors
    val interactions = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier =
        modifier
            .heightIn(min = ButtonMinHeight)
            .pressScale(interactions),
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = CircleShape,
        colors =
        ButtonDefaults.buttonColors(
            containerColor = colors.danger,
            contentColor = colors.onDanger,
            disabledContainerColor = colors.danger.copy(alpha = 0.30f),
            disabledContentColor = colors.onDanger.copy(alpha = 0.55f)
        ),
        contentPadding =
        PaddingValues(
            start = 24.dp,
            end = 24.dp
        )
    ) {
        VaultButtonInnerContent(text, loading, icon, colors.onDanger)
    }
}
