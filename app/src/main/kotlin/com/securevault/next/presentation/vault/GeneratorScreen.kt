package com.securevault.next.presentation.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.securevault.next.R
import com.securevault.next.core.domain.model.enums.PasswordStrengthLevel
import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.usecase.GeneratePasswordUseCase
import com.securevault.next.core.domain.usecase.GeneratedPassword
import com.securevault.next.core.domain.usecase.PasswordGenerationOptions
import com.securevault.next.core.platform.clipboard.SecureClipboardManager
import com.securevault.next.core.ui.components.SectionHeader
import com.securevault.next.core.ui.components.StrengthMeter
import com.securevault.next.core.ui.components.VaultButton
import com.securevault.next.core.ui.components.VaultSecondaryButton
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.SecureVaultTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Screen copy is centralized in strings.xml (generator_*). The clipboard label and
// the strength labels stay consts: the clipboard label is used in the ViewModel
// and the strength labels in the non-composable strengthLabel() helper.
private const val STR_COPY = "Copy password"
private const val STR_STRENGTH_VERY_WEAK = "Very weak"
private const val STR_STRENGTH_WEAK = "Weak"
private const val STR_STRENGTH_FAIR = "Fair"
private const val STR_STRENGTH_STRONG = "Strong"
private const val STR_STRENGTH_VERY_STRONG = "Very strong"

data class GeneratorUiState(
    val length: Int = PasswordGenerationOptions.DEFAULT_LENGTH,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
    val password: GeneratedPassword? = null,
    val justCopied: Boolean = false,
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generatePassword: GeneratePasswordUseCase,
    private val clipboard: SecureClipboardManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        regenerate()
    }

    fun onLengthChange(length: Int) {
        _uiState.update {
            it.copy(
                length = length.coerceIn(
                    PasswordGenerationOptions.MIN_LENGTH,
                    PasswordGenerationOptions.MAX_LENGTH,
                ),
                justCopied = false,
            )
        }
        regenerate()
    }

    fun onToggleUppercase(enabled: Boolean) = setClass(enabled) { copy(includeUppercase = it) }
    fun onToggleLowercase(enabled: Boolean) = setClass(enabled) { copy(includeLowercase = it) }
    fun onToggleDigits(enabled: Boolean) = setClass(enabled) { copy(includeDigits = it) }
    fun onToggleSymbols(enabled: Boolean) = setClass(enabled) { copy(includeSymbols = it) }

    fun onToggleExcludeAmbiguous(enabled: Boolean) {
        _uiState.update { it.copy(excludeAmbiguous = enabled, justCopied = false) }
        regenerate()
    }

    fun onRegenerate() {
        _uiState.update { it.copy(justCopied = false) }
        regenerate()
    }

    fun onCopy() {
        val value = _uiState.value.password?.value ?: return
        clipboard.copyToClipboard(STR_COPY, value)
        _uiState.update { it.copy(justCopied = true) }
    }

    /**
     * The last enabled character class can't be switched off — the
     * generator contract requires at least one class.
     */
    private fun setClass(
        enabled: Boolean,
        set: GeneratorUiState.(Boolean) -> GeneratorUiState,
    ) {
        val current = _uiState.value
        if (!enabled) {
            val enabledCount = listOf(
                current.includeUppercase,
                current.includeLowercase,
                current.includeDigits,
                current.includeSymbols,
            ).count { it }
            if (enabledCount <= 1) return
        }
        _uiState.update { it.set(enabled).copy(justCopied = false) }
        regenerate()
    }

    private fun regenerate() {
        val state = _uiState.value
        val options = PasswordGenerationOptions(
            length = state.length,
            includeUppercase = state.includeUppercase,
            includeLowercase = state.includeLowercase,
            includeDigits = state.includeDigits,
            includeSymbols = state.includeSymbols,
            excludeAmbiguousCharacters = state.excludeAmbiguous,
        )
        _uiState.update { it.copy(password = generatePassword(options)) }
    }
}

private fun strengthLabel(level: PasswordStrengthLevel): String = when (level) {
    PasswordStrengthLevel.VERY_WEAK -> STR_STRENGTH_VERY_WEAK
    PasswordStrengthLevel.WEAK -> STR_STRENGTH_WEAK
    PasswordStrengthLevel.FAIR -> STR_STRENGTH_FAIR
    PasswordStrengthLevel.STRONG -> STR_STRENGTH_STRONG
    PasswordStrengthLevel.VERY_STRONG -> STR_STRENGTH_VERY_STRONG
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateLifecycleAware()

    GeneratorContent(
        state = state,
        onBack = onBack,
        onLengthChange = viewModel::onLengthChange,
        onToggleUppercase = viewModel::onToggleUppercase,
        onToggleLowercase = viewModel::onToggleLowercase,
        onToggleDigits = viewModel::onToggleDigits,
        onToggleSymbols = viewModel::onToggleSymbols,
        onToggleExcludeAmbiguous = viewModel::onToggleExcludeAmbiguous,
        onRegenerate = viewModel::onRegenerate,
        onCopy = viewModel::onCopy,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorContent(
    state: GeneratorUiState,
    onBack: () -> Unit,
    onLengthChange: (Int) -> Unit,
    onToggleUppercase: (Boolean) -> Unit,
    onToggleLowercase: (Boolean) -> Unit,
    onToggleDigits: (Boolean) -> Unit,
    onToggleSymbols: (Boolean) -> Unit,
    onToggleExcludeAmbiguous: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Scaffold(
        containerColor = colors.baseBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generator_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.generator_back_desc),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.baseBackground,
                    titleContentColor = colors.label,
                    navigationIconContentColor = colors.label,
                    actionIconContentColor = colors.accent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            // The generated password: selectable, tabular, with strength.
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    SelectionContainer {
                        Text(
                            text = state.password?.value.orEmpty(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.label,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.password?.let { generated ->
                        StrengthMeter(
                            score = generated.strength.score.value / 100f,
                            label = strengthLabel(generated.strength.level),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        generated.strength.feedback.take(3).forEach { tip ->
                            Text(
                                text = "• $tip",
                                style = MaterialTheme.typography.footnote,
                                color = colors.secondaryLabel,
                            )
                        }
                    }
                    if (state.justCopied) {
                        Text(
                            text = stringResource(R.string.generator_copied_hint),
                            style = MaterialTheme.typography.footnote,
                            color = colors.success,
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.generator_section_length))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.generator_length_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.label,
                        )
                        Text(
                            text = state.length.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.label,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        IconButton(
                            onClick = { onLengthChange(state.length - 1) },
                            enabled = state.length > PasswordGenerationOptions.MIN_LENGTH,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = stringResource(R.string.generator_decrease_desc),
                                tint = colors.accent,
                            )
                        }
                        Slider(
                            value = state.length.toFloat(),
                            onValueChange = { onLengthChange(it.toInt()) },
                            valueRange = PasswordGenerationOptions.MIN_LENGTH.toFloat()..
                                PasswordGenerationOptions.MAX_LENGTH.toFloat(),
                            steps = PasswordGenerationOptions.MAX_LENGTH -
                                PasswordGenerationOptions.MIN_LENGTH - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.fill,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onLengthChange(state.length + 1) },
                            enabled = state.length < PasswordGenerationOptions.MAX_LENGTH,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.generator_increase_desc),
                                tint = colors.accent,
                            )
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.generator_section_characters))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    GeneratorSwitchRow(
                        title = stringResource(R.string.generator_uppercase),
                        subtitle = stringResource(R.string.generator_uppercase_sub),
                        checked = state.includeUppercase,
                        onCheckedChange = onToggleUppercase,
                        showDivider = true,
                    )
                    GeneratorSwitchRow(
                        title = stringResource(R.string.generator_lowercase),
                        subtitle = stringResource(R.string.generator_lowercase_sub),
                        checked = state.includeLowercase,
                        onCheckedChange = onToggleLowercase,
                        showDivider = true,
                    )
                    GeneratorSwitchRow(
                        title = stringResource(R.string.generator_digits),
                        subtitle = stringResource(R.string.generator_digits_sub),
                        checked = state.includeDigits,
                        onCheckedChange = onToggleDigits,
                        showDivider = true,
                    )
                    GeneratorSwitchRow(
                        title = stringResource(R.string.generator_symbols),
                        subtitle = stringResource(R.string.generator_symbols_sub),
                        checked = state.includeSymbols,
                        onCheckedChange = onToggleSymbols,
                        showDivider = true,
                    )
                    GeneratorSwitchRow(
                        title = stringResource(R.string.generator_exclude_ambiguous),
                        subtitle = stringResource(R.string.generator_exclude_ambiguous_sub),
                        checked = state.excludeAmbiguous,
                        onCheckedChange = onToggleExcludeAmbiguous,
                        showDivider = false,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = spacing.medium,
                        bottom = spacing.extraLarge,
                    ),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                VaultSecondaryButton(
                    text = stringResource(R.string.generator_screen_regenerate),
                    onClick = onRegenerate,
                    icon = Icons.Filled.Refresh,
                    modifier = Modifier.weight(1f),
                )
                VaultButton(
                    text = STR_COPY,
                    onClick = onCopy,
                    icon = Icons.Filled.ContentCopy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One native switch row: title, quiet subtitle, single trailing control. */
@Composable
private fun GeneratorSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.footnote,
                    color = colors.secondaryLabel,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = colors.secondaryLabel,
                    uncheckedTrackColor = colors.fill,
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
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

// --- Preview ---

@Preview(showBackground = true)
@Composable
private fun GeneratorContentPreview() {
    SecureVaultTheme {
        GeneratorContent(
            state = GeneratorUiState(
                password = GeneratedPassword(
                    value = "xK9#mQ2\$vL8nP4@wR7tY",
                    strength = PasswordStrength(
                        score = SecurityScore(92),
                        level = PasswordStrengthLevel.VERY_STRONG,
                        feedback = listOf("This password looks strong."),
                    ),
                ),
            ),
            onBack = {},
            onLengthChange = {},
            onToggleUppercase = {},
            onToggleLowercase = {},
            onToggleDigits = {},
            onToggleSymbols = {},
            onToggleExcludeAmbiguous = {},
            onRegenerate = {},
            onCopy = {},
        )
    }
}
