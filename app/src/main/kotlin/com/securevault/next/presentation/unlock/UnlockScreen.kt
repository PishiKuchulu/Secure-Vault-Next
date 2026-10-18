package com.securevault.next.presentation.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.next.R
import com.securevault.next.core.ui.components.InlineError
import com.securevault.next.core.ui.components.VaultButton
import com.securevault.next.core.ui.components.VaultPasswordTextField
import com.securevault.next.core.ui.components.VaultSecondaryButton
import com.securevault.next.core.ui.components.VaultSheet
import com.securevault.next.core.ui.components.VaultSheetTitle
import com.securevault.next.core.ui.components.VaultTextButton
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.tabularNumerals
import kotlinx.coroutines.flow.collectLatest

/**
 * The vault lock screen: password unlock, biometric quick-unlock when
 * enrolled, and the brute-force cool-down.
 *
 * A one-way door: [onUnlocked] clears this destination from the back
 * stack, so back can never return here from the vault home.
 */
@Composable
fun UnlockScreen(vaultId: String, onUnlocked: () -> Unit, viewModel: UnlockViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(vaultId) {
        viewModel.load(vaultId)
    }
    LaunchedEffect(viewModel) {
        viewModel.homeEvents.collectLatest { onUnlocked() }
    }

    val promptTitle = stringResource(R.string.unlock_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.unlock_biometric_prompt_subtitle)
    val promptNegative = stringResource(R.string.unlock_biometric_prompt_negative)
    val lockedOut = uiState.lockoutRemainingSeconds > 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.baseBackground
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.extraExtraLarge))

            Box(
                modifier =
                Modifier
                    .size(88.dp)
                    .background(colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))

            Text(
                text =
                if (uiState.vaultName.isNotBlank()) {
                    stringResource(R.string.unlock_title, uiState.vaultName)
                } else {
                    stringResource(R.string.unlock_title_fallback)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.label,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.small))

            Text(
                text = stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.extraLarge))

            VaultPasswordTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.unlock_password_label),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            UnlockStatusArea(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.medium))

            VaultButton(
                text = stringResource(R.string.unlock_submit),
                onClick = { viewModel.onUnlockClick(vaultId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !lockedOut,
                loading = uiState.isLoading
            )

            if (uiState.canUseBiometrics && activity != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                VaultSecondaryButton(
                    text = stringResource(R.string.unlock_with_biometrics),
                    onClick = {
                        viewModel.onBiometricClick(
                            activity = activity,
                            vaultId = vaultId,
                            promptTitle = promptTitle,
                            promptSubtitle = promptSubtitle,
                            promptNegative = promptNegative
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !lockedOut,
                    loading = uiState.biometricBusy,
                    icon = Icons.Filled.Fingerprint
                )
            }

            Spacer(modifier = Modifier.height(spacing.large))
        }
    }

    if (uiState.showBiometricOffer && activity != null) {
        BiometricOfferSheet(
            enabling = uiState.enablingBiometrics,
            enableFailure = uiState.biometricEnableFailure,
            onEnable = {
                viewModel.onEnableBiometrics(
                    activity = activity,
                    vaultId = vaultId,
                    promptTitle = promptTitle,
                    promptSubtitle = promptSubtitle,
                    promptNegative = promptNegative
                )
            },
            onDismiss = { viewModel.onDismissBiometricOffer(vaultId) }
        )
    }
}

/**
 * The status area under the password field: exactly one of the
 * cool-down countdown, the wrong-password message with attempts left,
 * or a plain inline error — never stacked, never a toast.
 */
@Composable
private fun UnlockStatusArea(uiState: UnlockUiState, modifier: Modifier = Modifier) {
    val colors = SecureVaultColorsScope.colors

    when {
        uiState.lockoutRemainingSeconds > 0 -> {
            Text(
                text =
                stringResource(
                    R.string.unlock_locked_out,
                    formatCountdown(uiState.lockoutRemainingSeconds)
                ),
                style = MaterialTheme.typography.bodyLarge.tabularNumerals(),
                color = colors.danger,
                textAlign = TextAlign.Center,
                modifier = modifier
            )
        }
        uiState.wrongPasswordAttemptsLeft != null -> {
            InlineError(
                message =
                stringResource(R.string.unlock_incorrect_password) +
                    " " +
                    stringResource(
                        R.string.unlock_attempts_remaining,
                        uiState.wrongPasswordAttemptsLeft
                    ),
                modifier = modifier
            )
        }
        uiState.biometricKeyInvalidated -> {
            InlineError(
                message = stringResource(R.string.unlock_biometric_key_invalid),
                modifier = modifier
            )
        }
        uiState.biometricEnableFailure != null -> {
            InlineError(
                message =
                stringResource(R.string.biometric_enable_failed) +
                    " " + uiState.biometricEnableFailure,
                modifier = modifier
            )
        }
        uiState.inlineError != null -> {
            InlineError(
                message = uiState.inlineError,
                modifier = modifier
            )
        }
    }
}

private fun formatCountdown(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * Offered once, right after a successful password unlock: enroll
 * biometric quick-unlock now, or dismiss and keep password-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiometricOfferSheet(
    enabling: Boolean,
    enableFailure: String?,
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = SecureVaultSpacingScope.spacing
    val colors = SecureVaultColorsScope.colors

    VaultSheet(onDismissRequest = onDismiss) {
        VaultSheetTitle(text = stringResource(R.string.biometric_offer_title))

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = stringResource(R.string.biometric_offer_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.secondaryLabel
        )

        if (enableFailure != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            InlineError(
                message = stringResource(R.string.biometric_enable_failed) + " " + enableFailure
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))

        VaultButton(
            text = stringResource(R.string.biometric_offer_enable),
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth(),
            loading = enabling
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VaultTextButton(
                text = stringResource(R.string.biometric_offer_not_now),
                onClick = onDismiss
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))
    }
}
