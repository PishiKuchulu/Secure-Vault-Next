package com.securevault.next.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.securevault.next.core.ui.theme.SecureVaultColorsScope

/*
 * Shape lock: inputs are 12dp. Error is never conveyed by border color
 * alone: the label, the supporting text and the trailing icon all take
 * the danger tint, and the message says what went wrong.
 *
 * Public signatures are stable: screens reference VaultTextField and
 * VaultPasswordTextField, so those keep their names and parameters
 * (supporting text is additive).
 */

@Composable
private fun vaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SecureVaultColorsScope.colors.label,
    unfocusedTextColor = SecureVaultColorsScope.colors.label,
    disabledTextColor = SecureVaultColorsScope.colors.secondaryLabel,
    errorTextColor = SecureVaultColorsScope.colors.label,
    focusedContainerColor = SecureVaultColorsScope.colors.surface,
    unfocusedContainerColor = SecureVaultColorsScope.colors.surface,
    disabledContainerColor = SecureVaultColorsScope.colors.surface,
    errorContainerColor = SecureVaultColorsScope.colors.surface,
    cursorColor = SecureVaultColorsScope.colors.accent,
    errorCursorColor = SecureVaultColorsScope.colors.danger,
    focusedBorderColor = SecureVaultColorsScope.colors.accent,
    unfocusedBorderColor = SecureVaultColorsScope.colors.separator,
    disabledBorderColor = SecureVaultColorsScope.colors.separator.copy(alpha = 0.5f),
    errorBorderColor = SecureVaultColorsScope.colors.danger,
    focusedLabelColor = SecureVaultColorsScope.colors.accent,
    unfocusedLabelColor = SecureVaultColorsScope.colors.secondaryLabel,
    disabledLabelColor = SecureVaultColorsScope.colors.secondaryLabel.copy(alpha = 0.6f),
    errorLabelColor = SecureVaultColorsScope.colors.danger,
    focusedSupportingTextColor = SecureVaultColorsScope.colors.secondaryLabel,
    unfocusedSupportingTextColor = SecureVaultColorsScope.colors.secondaryLabel,
    errorSupportingTextColor = SecureVaultColorsScope.colors.danger,
    focusedTrailingIconColor = SecureVaultColorsScope.colors.secondaryLabel,
    unfocusedTrailingIconColor = SecureVaultColorsScope.colors.secondaryLabel,
    errorTrailingIconColor = SecureVaultColorsScope.colors.danger,
)

@Composable
private fun fieldSupportingText(error: String?, supporting: String?) {
    val message = error ?: supporting ?: return
    Text(
        text = message,
        style = MaterialTheme.typography.footnote,
    )
}

/**
 * Standard text field for SecureVault Next.
 *
 * @param error When non-null, the field enters the error state: danger
 * tint on border, label, trailing icon AND the message below — never
 * the border alone.
 * @param supporting Optional helper text shown when there is no error
 * (e.g. "Used to encrypt your vault").
 */
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = error != null,
        supportingText = { fieldSupportingText(error, supporting) },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        colors = vaultTextFieldColors(),
    )
}

/**
 * Password field with a show/hide toggle.
 *
 * The toggle is an icon-only [IconButton] and always carries a
 * contentDescription ("Show password" / "Hide password").
 */
@Composable
fun VaultPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = error != null,
        supportingText = { fieldSupportingText(error, supporting) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = description)
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = vaultTextFieldColors(),
    )
}
