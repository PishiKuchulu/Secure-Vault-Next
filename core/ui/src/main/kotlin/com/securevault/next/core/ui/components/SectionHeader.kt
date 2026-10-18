package com.securevault.next.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.footnote

/*
 * Settings surfaces follow the inset-grouped pattern: a SectionHeader
 * names the group, SettingsRows carry title + subtitle + one trailing
 * control each. One control per row; a row never holds two actions.
 */

/**
 * Section header for a grouped settings/content section.
 * Sentence case, quiet, carrying the label color — emphasis stays in
 * weight, not in decoration.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.medium,
                    end = spacing.medium,
                    top = spacing.large,
                    bottom = spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.label,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.footnote,
                    color = colors.secondaryLabel,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * A settings row: title, optional subtitle, and one trailing control
 * (switch, value label, or nothing with a chevron for navigation).
 *
 * Rows use a background highlight on press — never scale.
 *
 * @param onClick When non-null the whole row is tappable with a
 * chevron affordance; when null the row is static and the trailing
 * control carries the interaction.
 * @param showDivider Draws a hairline below the row. In a grouped card
 * of rows, pass true for every row except the last.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    showChevron: Boolean = true,
    showDivider: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    val rowModifier =
        if (onClick != null) {
            modifier.clickable(
                onClick = onClick,
                onClickLabel = onClickLabel ?: title,
                role = Role.Button,
            )
        } else {
            modifier
        }

    Column(modifier = rowModifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = spacing.medium, vertical = spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.footnote,
                        color = colors.secondaryLabel,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null && showChevron) {
                // Decorative: the row's onClickLabel already names the action.
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colors.secondaryLabel,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = spacing.medium),
                thickness = 1.dp,
                color = colors.separator,
            )
        }
    }
}
