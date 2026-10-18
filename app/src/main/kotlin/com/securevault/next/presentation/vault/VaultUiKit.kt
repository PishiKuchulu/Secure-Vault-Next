package com.securevault.next.presentation.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.footnote
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/*
 * Shared presentation helpers for the vault item flows (home list,
 * detail, editor, generator). Behavior only — every screen owns its
 * copy as private consts at the top of its own file.
 */

/** Masked placeholder for sensitive values shown before reveal. */
internal const val MASKED_VALUE = "••••••••"

/**
 * Lifecycle-aware state collection without the lifecycle-runtime-compose
 * artifact (not on this module's classpath): collects only while the
 * screen is at least STARTED, mirroring what
 * `collectAsStateWithLifecycle` would do.
 */
@Composable
internal fun <T> StateFlow<T>.collectAsStateLifecycleAware(): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state: MutableState<T> = remember { mutableStateOf(value) }
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { state.value = it }
        }
    }
    return state
}

/** The vault the item flows operate on: the first available vault, matching DashboardViewModel's convention. */
internal suspend fun VaultRepository.firstVaultId(): VaultId? = observeAllVaultMetadata().first().firstOrNull()?.id

/** Human-facing error copy for domain failures. Never surfaces raw technical messages. */
internal fun friendlyErrorMessage(error: DomainError): String = when (error) {
    is DomainError.CryptoRequiredError ->
        "The vault is locked. Unlock it and try again."
    is DomainError.RepositoryError.NotFound ->
        "This item couldn't be found. It may have been deleted."
    is DomainError.RepositoryError ->
        "Couldn't reach your vault data. Try again."
    is DomainError.ValidationError ->
        "Some values aren't valid. Check the highlighted fields."
    is DomainError.VaultError ->
        "There's a problem with this vault. Try again."
    is DomainError.AuthenticationError ->
        "Authentication failed. Try again."
    else ->
        "Something went wrong. Try again."
}

/** Sentence-case, user-facing label per item type. */
internal val ItemType.uiLabel: String
    get() =
        when (this) {
            ItemType.PASSWORD -> "Login"
            ItemType.SECURE_NOTE -> "Secure note"
            ItemType.CREDIT_CARD -> "Credit card"
            ItemType.BANK_ACCOUNT -> "Bank account"
            ItemType.IDENTITY_DOCUMENT -> "Identity document"
            ItemType.SOFTWARE_LICENSE -> "Software license"
            ItemType.API_KEY -> "API key"
            ItemType.SSH_KEY -> "SSH key"
            ItemType.RECOVERY_CODE -> "Recovery codes"
            ItemType.TOTP_SEED -> "Authenticator code"
        }

/** One icon family (Material), one icon per item type, used in list leading slots. */
internal val ItemType.uiIcon: ImageVector
    get() =
        when (this) {
            ItemType.PASSWORD -> Icons.Filled.VpnKey
            ItemType.SECURE_NOTE -> Icons.Filled.Description
            ItemType.CREDIT_CARD -> Icons.Filled.CreditCard
            ItemType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
            ItemType.IDENTITY_DOCUMENT -> Icons.Filled.Badge
            ItemType.SOFTWARE_LICENSE -> Icons.Filled.VerifiedUser
            ItemType.API_KEY -> Icons.Filled.Code
            ItemType.SSH_KEY -> Icons.Filled.Terminal
            ItemType.RECOVERY_CODE -> Icons.Filled.Restore
            ItemType.TOTP_SEED -> Icons.Filled.Schedule
        }

/** Localized medium date-time for metadata footers ("Last modified …"). */
internal fun formatInstant(instant: Instant): String {
    val formatter =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

/** Groups TOTP digits the way authenticator apps do ("123 456"). */
internal fun formatTotpCode(code: String): String = code.chunked(3).joinToString(" ")

/**
 * A quiet informational note: accent-soft card, info icon, plain
 * sentence. For honest "not available yet" messaging — never an error.
 */
@Composable
internal fun QuietInfoNote(text: String, modifier: Modifier = Modifier) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.accentSoft
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.footnote,
                color = colors.label,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A read-only detail row: footnote label over the value, with an
 * optional reveal toggle for sensitive values and a copy button.
 * Sensitive values start masked; the copied hint is shown by the
 * caller when this row was the copy source.
 */
@Composable
internal fun DetailFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sensitive: Boolean = false,
    showContentDescription: String? = null,
    copyContentDescription: String? = null,
    onCopy: (() -> Unit)? = null,
    showCopiedHint: Boolean = false,
    copiedHintText: String = ""
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    var revealed by remember(value) { mutableStateOf(!sensitive) }

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(
                horizontal = spacing.medium,
                vertical = spacing.small + spacing.extraSmall
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.footnote,
            color = colors.secondaryLabel
        )
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = if (revealed) value else MASKED_VALUE,
                style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = colors.label,
                modifier = Modifier.weight(1f)
            )
            if (sensitive) {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription =
                        showContentDescription
                            ?: if (revealed) "Hide $label" else "Show $label"
                    )
                }
            }
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = copyContentDescription ?: "Copy $label"
                    )
                }
            }
        }
        if (showCopiedHint) {
            Text(
                text = copiedHintText,
                style = MaterialTheme.typography.footnote,
                color = colors.success,
                modifier = Modifier.padding(top = spacing.extraSmall)
            )
        }
    }
}
