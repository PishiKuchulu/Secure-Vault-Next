package com.securevault.next.presentation.createvault

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.R
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.common.result.fold
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.enums.PasswordStrengthLevel
import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.usecase.CreateVaultUseCase
import com.securevault.next.core.domain.usecase.GeneratePasswordUseCase
import com.securevault.next.core.domain.usecase.PasswordGenerationOptions
import com.securevault.next.core.domain.usecase.ValidatePasswordUseCase
import com.securevault.next.core.domain.validation.MasterPasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Inline validation problems on the create-vault form. Policy messages
 * come from the engine's [MasterPasswordValidator] verbatim (it owns
 * the wording of its own rules); structural problems (blank name,
 * mismatched confirmation) use string resources.
 */
sealed interface CreateVaultFieldError {
    data class Resource(@StringRes val resId: Int) : CreateVaultFieldError
    data class EngineMessage(val message: String) : CreateVaultFieldError
}

data class CreateVaultUiState(
    val name: String = "",
    val nameError: CreateVaultFieldError? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: CreateVaultFieldError? = null,
    val confirmError: CreateVaultFieldError? = null,
    val strength: PasswordStrength? = null,
    val isLoading: Boolean = false,
    val formError: String? = null,
    val createdVaultId: String? = null,
    val showGenerator: Boolean = false,
    val generatedPassword: String = "",
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CreateVaultViewModel @Inject constructor(
    private val createVaultUseCase: CreateVaultUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
) : ViewModel() {

    // Stateless pure-domain validator with the fixed policy defaults
    // (min 12, max 128, four character classes) — instantiated directly,
    // mirroring CreateVaultUseCaseImpl.
    private val masterPasswordValidator = MasterPasswordValidator()

    private val _uiState = MutableStateFlow(CreateVaultUiState())
    val uiState: StateFlow<CreateVaultUiState> = _uiState.asStateFlow()

    init {
        // Live strength, debounced so typing never janks: the scoring
        // itself is cheap, but re-composing the meter on every keystroke
        // is not worth it.
        _uiState
            .map { it.password }
            .distinctUntilChanged()
            .debounce(300)
            .map { password ->
                if (password.isEmpty()) {
                    null
                } else {
                    val chars = password.toCharArray()
                    try {
                        validatePasswordUseCase(chars)
                    } finally {
                        MemoryWiper.wipe(chars)
                    }
                }
            }
            .onEach { strength -> _uiState.update { it.copy(strength = strength) } }
            .launchIn(viewModelScope)
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, confirmError = null) }
    }

    fun onConfirmPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmPassword = confirm, confirmError = null) }
    }

    fun onOpenGenerator() {
        regeneratePassword()
        _uiState.update { it.copy(showGenerator = true) }
    }

    fun onDismissGenerator() {
        _uiState.update { it.copy(showGenerator = false, generatedPassword = "") }
    }

    fun regeneratePassword() {
        val generated = generatePasswordUseCase(PasswordGenerationOptions())
        _uiState.update { it.copy(generatedPassword = generated.value) }
    }

    fun onUseGeneratedPassword() {
        val generated = _uiState.value.generatedPassword
        _uiState.update {
            it.copy(
                password = generated,
                confirmPassword = "",
                passwordError = null,
                confirmError = null,
                showGenerator = false,
                generatedPassword = "",
            )
        }
    }

    fun onCreateClick() {
        val state = _uiState.value
        if (state.isLoading) return

        // Blank-name guard (audit H2): VaultName's constructor throws on
        // blank, so the form must reject it inline before ever building one.
        if (state.name.isBlank()) {
            _uiState.update {
                it.copy(nameError = CreateVaultFieldError.Resource(R.string.create_vault_name_blank_error))
            }
            return
        }

        // Policy check first: the engine rejects weak/over-long passwords
        // at the gate, so surface its messages on the field, not as a
        // form-level surprise after the KDF spins up.
        val passwordChars = state.password.toCharArray()
        try {
            val validation = masterPasswordValidator.validate(passwordChars)
            if (!validation.isValid) {
                _uiState.update {
                    it.copy(
                        passwordError = CreateVaultFieldError.EngineMessage(
                            validation.validationErrors.first().message,
                        ),
                    )
                }
                return
            }
        } finally {
            MemoryWiper.wipe(passwordChars)
        }

        if (state.password != state.confirmPassword) {
            _uiState.update {
                it.copy(confirmError = CreateVaultFieldError.Resource(R.string.create_vault_password_mismatch))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, formError = null) }
            val name = state.name.trim()
            val masterPassword = state.password.toCharArray()
            // Clear the visible text state the moment the secret leaves it:
            // from here on the password exists only as the CharArray below.
            _uiState.update { it.copy(password = "", confirmPassword = "", strength = null) }
            try {
                createVaultUseCase(
                    name = VaultName(name),
                    masterPassword = masterPassword,
                    securityPolicy = VaultSecurityPolicy(),
                    settings = VaultSettings(),
                ).fold(
                    onSuccess = { vault ->
                        _uiState.update {
                            it.copy(isLoading = false, createdVaultId = vault.metadata.id.value)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, formError = error.message) }
                    },
                )
            } finally {
                MemoryWiper.wipe(masterPassword)
            }
        }
    }

    /** Consumed by the screen after navigating away. */
    fun onCreatedConsumed() {
        _uiState.update { it.copy(createdVaultId = null) }
    }
}

/** Maps a [PasswordStrengthLevel] to its localized meter label. */
fun PasswordStrengthLevel.labelResId(): Int = when (this) {
    PasswordStrengthLevel.VERY_WEAK -> R.string.strength_very_weak
    PasswordStrengthLevel.WEAK -> R.string.strength_weak
    PasswordStrengthLevel.FAIR -> R.string.strength_fair
    PasswordStrengthLevel.STRONG -> R.string.strength_strong
    PasswordStrengthLevel.VERY_STRONG -> R.string.strength_very_strong
}
