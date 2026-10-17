package com.securevault.next.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope

/*
 * Empty states are composed, never blank: an icon in a quiet fill
 * well, a title, a description that says how to fill the emptiness,
 * and the action that does it.
 */

/**
 * Full empty state for a screen or section with no content yet.
 *
 * @param icon Decorative iconography for the state (never emoji).
 * @param title What is empty, in the interface's voice.
 * @param description One line on how to fill it — an empty state that
 * does not say what to do next is unfinished.
 * @param actionLabel Label for the primary action (e.g. "Add login").
 * @param onAction Invoked when the action is tapped.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.extraLarge, vertical = spacing.extraExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(colors.fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: title + description carry the meaning.
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colors.secondaryLabel,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.label,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center,
        )
        VaultButton(
            text = actionLabel,
            onClick = onAction,
            modifier = Modifier.padding(top = spacing.small),
        )
    }
}
