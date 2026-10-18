package com.securevault.next.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.footnote

/*
 * Errors are inline and specific: they say what went wrong and how to
 * fix it, in the interface's voice, without apologizing. Never an
 * alert for something the interface can say in place.
 */

/**
 * Inline error: an icon plus a specific message, tinted with the
 * danger token. For field-level errors prefer the [VaultTextField]
 * error slot; use this for form-level or section-level failures
 * (e.g. "Could not reach the sync server. Check your connection.").
 */
@Composable
fun InlineError(message: String, modifier: Modifier = Modifier) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colors.danger
        )
        Text(
            text = message,
            style = MaterialTheme.typography.footnote,
            color = colors.danger,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Full error state for a screen or section that failed to load.
 * The message names the problem and the retry button names the fix.
 */
@Composable
fun ErrorState(
    title: String,
    description: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Try again"
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.extraLarge, vertical = spacing.extraExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Decorative: title + description carry the meaning.
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.danger
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.label
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondaryLabel
        )
        VaultSecondaryButton(
            text = retryLabel,
            onClick = onRetry,
            modifier = Modifier.padding(top = spacing.small)
        )
    }
}
