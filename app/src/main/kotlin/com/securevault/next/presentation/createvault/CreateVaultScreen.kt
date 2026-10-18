package com.securevault.next.presentation.createvault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.next.R
import com.securevault.next.core.ui.components.InlineError
import com.securevault.next.core.ui.components.StrengthMeter
import com.securevault.next.core.ui.components.VaultButton
import com.securevault.next.core.ui.components.VaultPasswordTextField
import com.securevault.next.core.ui.components.VaultSecondaryButton
import com.securevault.next.core.ui.components.VaultSheet
import com.securevault.next.core.ui.components.VaultSheetTitle
import com.securevault.next.core.ui.components.VaultTextButton
import com.securevault.next.core.ui.components.VaultTextField
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope

/**
 * Creates a new vault: name, master password with live strength, and
 * an embedded generator. All validation is inline — a blank name can
 * never reach [com.securevault.next.core.domain.model.valueobject.VaultName]'s
 * throwing constructor (audit H2), and policy violations surface the
 * engine validator's own messages on the password field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVaultScreen(
    onNavigateBack: () -> Unit,
    onVaultCreated: (vaultId: String) -> Unit,
    viewModel: CreateVaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    LaunchedEffect(uiState.createdVaultId) {
        uiState.createdVaultId?.let { vaultId ->
            viewModel.onCreatedConsumed()
            onVaultCreated(vaultId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_vault_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .imePadding()
        ) {
            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.create_vault_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.secondaryLabel
                )

                VaultTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = stringResource(R.string.create_vault_name_label),
                    modifier = Modifier.fillMaxWidth(),
                    error = uiState.nameError?.resolve(),
                    supporting = stringResource(R.string.create_vault_name_supporting)
                )

                VaultPasswordTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.create_vault_password_label),
                    modifier = Modifier.fillMaxWidth(),
                    error = uiState.passwordError?.resolve()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    VaultTextButton(
                        text = stringResource(R.string.create_vault_generate),
                        onClick = viewModel::onOpenGenerator
                    )
                }

                val strength = uiState.strength
                if (strength != null && uiState.password.isNotEmpty()) {
                    StrengthMeter(
                        score = strength.score.value / 100f,
                        label = stringResource(strength.level.labelResId()),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                VaultPasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = stringResource(R.string.create_vault_confirm_label),
                    modifier = Modifier.fillMaxWidth(),
                    error = uiState.confirmError?.resolve()
                )

                if (uiState.formError != null) {
                    InlineError(message = uiState.formError!!)
                }
            }

            VaultButton(
                text = stringResource(R.string.create_vault_submit),
                onClick = viewModel::onCreateClick,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.large),
                loading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(spacing.large))
        }
    }

    if (uiState.showGenerator) {
        GeneratorSheet(
            generatedPassword = uiState.generatedPassword,
            onRegenerate = viewModel::regeneratePassword,
            onUsePassword = viewModel::onUseGeneratedPassword,
            onDismiss = viewModel::onDismissGenerator
        )
    }
}

@Composable
private fun CreateVaultFieldError.resolve(): String = when (this) {
    is CreateVaultFieldError.Resource -> stringResource(resId)
    is CreateVaultFieldError.EngineMessage -> message
}

/**
 * The embedded password generator: a short interruption with one way
 * out. Shows the generated password, lets the user regenerate or take
 * it into the form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorSheet(
    generatedPassword: String,
    onRegenerate: () -> Unit,
    onUsePassword: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = SecureVaultSpacingScope.spacing
    val colors = SecureVaultColorsScope.colors

    VaultSheet(onDismissRequest = onDismiss) {
        VaultSheetTitle(text = stringResource(R.string.generator_title))

        Spacer(modifier = Modifier.height(spacing.medium))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.fill
        ) {
            SelectionContainer {
                Text(
                    text = generatedPassword,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.label,
                    modifier = Modifier.padding(spacing.medium)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultSecondaryButton(
                text = stringResource(R.string.generator_regenerate),
                onClick = onRegenerate
            )
            VaultButton(
                text = stringResource(R.string.generator_use_password),
                onClick = onUsePassword
            )
        }

        Spacer(modifier = Modifier.height(spacing.large))
    }
}
