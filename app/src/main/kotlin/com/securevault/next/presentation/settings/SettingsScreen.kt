package com.securevault.next.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.next.R
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.ui.components.InlineError
import com.securevault.next.core.ui.components.SectionHeader
import com.securevault.next.core.ui.components.SettingsRow
import com.securevault.next.core.ui.components.VaultButton
import com.securevault.next.core.ui.components.VaultPasswordTextField
import com.securevault.next.core.ui.components.VaultSheet
import com.securevault.next.core.ui.components.VaultSheetTitle
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope

private val AutoLockOptions =
    listOf(
        AutoLockPolicy.IMMEDIATELY,
        AutoLockPolicy.MINUTE_1,
        AutoLockPolicy.MINUTES_5,
        AutoLockPolicy.MINUTES_15,
        AutoLockPolicy.MINUTES_30,
        AutoLockPolicy.NEVER
    )

/**
 * The real settings screen, inset-grouped: security controls (auto-lock
 * timeout, biometric unlock, change master password, lock now) and the
 * about section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLockNow: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val promptTitle = stringResource(R.string.unlock_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.unlock_biometric_prompt_subtitle)
    val promptNegative = stringResource(R.string.unlock_biometric_prompt_negative)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.baseBackground,
                    titleContentColor = colors.label,
                    navigationIconContentColor = colors.label
                )
            )
        },
        containerColor = colors.baseBackground
    ) { padding ->
        Column(
            modifier =
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = stringResource(R.string.settings_section_security))

            Surface(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                shape = MaterialTheme.shapes.large,
                color = colors.surface
            ) {
                Column {
                    SettingsRow(
                        title = stringResource(R.string.settings_auto_lock),
                        subtitle = stringResource(uiState.autoLockPolicy.labelResId()),
                        onClick = viewModel::onOpenAutoLockSheet,
                        onClickLabel = stringResource(R.string.settings_auto_lock),
                        showDivider = true
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_biometric),
                        subtitle =
                        if (uiState.biometricAvailable) {
                            stringResource(
                                if (uiState.biometricEnabled) {
                                    R.string.settings_biometric_subtitle_on
                                } else {
                                    R.string.settings_biometric_subtitle_off
                                }
                            )
                        } else {
                            stringResource(R.string.settings_biometric_unavailable)
                        },
                        showChevron = false,
                        showDivider = true,
                        trailing = {
                            Switch(
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { enable ->
                                    if (activity != null) {
                                        viewModel.onBiometricToggle(
                                            activity = activity,
                                            enable = enable,
                                            promptTitle = promptTitle,
                                            promptSubtitle = promptSubtitle,
                                            promptNegative = promptNegative
                                        )
                                    }
                                },
                                enabled =
                                uiState.biometricAvailable &&
                                    !uiState.biometricBusy &&
                                    activity != null
                            )
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_change_password),
                        subtitle = stringResource(R.string.settings_change_password_subtitle),
                        onClick = viewModel::onOpenChangePassword,
                        onClickLabel = stringResource(R.string.settings_change_password),
                        showDivider = true
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_lock_now),
                        subtitle = stringResource(R.string.settings_lock_now_subtitle),
                        onClick = onLockNow,
                        onClickLabel = stringResource(R.string.settings_lock_now)
                    )
                }
            }

            if (uiState.biometricError != null) {
                InlineError(
                    message = uiState.biometricError!!,
                    modifier =
                    Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small
                    )
                )
            }

            SectionHeader(title = stringResource(R.string.settings_section_about))

            Surface(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                shape = MaterialTheme.shapes.large,
                color = colors.surface
            ) {
                SettingsRow(
                    title = stringResource(R.string.settings_version),
                    showChevron = false,
                    trailing = {
                        Text(
                            text = stringResource(R.string.settings_version_value),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.secondaryLabel
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraLarge))
        }
    }

    if (uiState.showAutoLockSheet) {
        AutoLockSheet(
            selected = uiState.autoLockPolicy,
            onSelect = viewModel::onAutoLockSelected,
            onDismiss = viewModel::onDismissAutoLockSheet
        )
    }

    if (uiState.showChangePasswordSheet) {
        ChangePasswordSheet(
            busy = uiState.changePasswordBusy,
            error = uiState.changePasswordError,
            success = uiState.changePasswordSuccess,
            onSubmit = viewModel::onChangePassword,
            onDismiss = viewModel::onDismissChangePassword
        )
    }
}

/**
 * The auto-lock timeout picker: a short interruption listing the
 * policy values, the current one checked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoLockSheet(selected: AutoLockPolicy, onSelect: (AutoLockPolicy) -> Unit, onDismiss: () -> Unit) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    VaultSheet(onDismissRequest = onDismiss) {
        VaultSheetTitle(text = stringResource(R.string.settings_auto_lock))

        Spacer(modifier = Modifier.height(spacing.small))

        AutoLockOptions.forEachIndexed { index, option ->
            SettingsRow(
                title = stringResource(option.labelResId()),
                onClick = { onSelect(option) },
                onClickLabel = stringResource(option.labelResId()),
                showChevron = false,
                showDivider = index < AutoLockOptions.lastIndex,
                trailing = {
                    if (option == selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.accent
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))
    }
}

private fun AutoLockPolicy.labelResId(): Int = when (this) {
    AutoLockPolicy.IMMEDIATELY -> R.string.settings_auto_lock_immediately
    AutoLockPolicy.MINUTE_1 -> R.string.settings_auto_lock_1_min
    AutoLockPolicy.MINUTES_5 -> R.string.settings_auto_lock_5_min
    AutoLockPolicy.MINUTES_15 -> R.string.settings_auto_lock_15_min
    AutoLockPolicy.MINUTES_30 -> R.string.settings_auto_lock_30_min
    AutoLockPolicy.NEVER -> R.string.settings_auto_lock_never
}

/**
 * Change-master-password sheet: current password (verified by the
 * engine — a wrong one surfaces as an inline error), the new password
 * (policy violations surface inline), and confirmation. The fields are
 * cleared from composition state on dismiss; the engine wipes the
 * CharArrays on every path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(
    busy: Boolean,
    error: String?,
    success: Boolean,
    onSubmit: (current: String, new: String, confirm: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val spacing = SecureVaultSpacingScope.spacing

    VaultSheet(onDismissRequest = onDismiss) {
        VaultSheetTitle(text = stringResource(R.string.settings_change_password))

        Spacer(modifier = Modifier.height(spacing.small))

        if (success) {
            Text(
                text = stringResource(R.string.settings_change_password_success_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SecureVaultColorsScope.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = spacing.medium)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            VaultButton(
                text = stringResource(R.string.action_done),
                onClick = onDismiss,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            )
        } else {
            VaultPasswordTextField(
                value = current,
                onValueChange = { current = it },
                label = stringResource(R.string.settings_change_password_current),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            )
            Spacer(modifier = Modifier.height(spacing.small))
            VaultPasswordTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = stringResource(R.string.settings_change_password_new),
                supporting = stringResource(R.string.settings_change_password_requirements),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            )
            Spacer(modifier = Modifier.height(spacing.small))
            VaultPasswordTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = stringResource(R.string.settings_change_password_confirm),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            )

            if (error != null) {
                InlineError(
                    message = error,
                    modifier =
                    Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small
                    )
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))
            VaultButton(
                text = stringResource(R.string.settings_change_password_action),
                onClick = { onSubmit(current, newPassword, confirm) },
                loading = busy,
                enabled =
                current.isNotBlank() &&
                    newPassword.isNotBlank() &&
                    confirm.isNotBlank(),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))
    }
}
