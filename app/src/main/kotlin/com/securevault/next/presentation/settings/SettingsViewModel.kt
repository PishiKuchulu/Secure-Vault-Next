package com.securevault.next.presentation.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.biometric.BiometricKeyManager
import com.securevault.next.biometric.EnableResult
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.usecase.ChangeMasterPasswordUseCase
import com.securevault.next.core.security.session.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoLockPolicy: AutoLockPolicy = AutoLockPolicy.MINUTES_5,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricBusy: Boolean = false,
    val biometricError: String? = null,
    val showAutoLockSheet: Boolean = false,
    val showChangePasswordSheet: Boolean = false,
    val changePasswordBusy: Boolean = false,
    val changePasswordError: String? = null,
    val changePasswordSuccess: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val sessionManager: VaultSessionManager,
    private val biometricKeyManager: BiometricKeyManager,
    private val changeMasterPasswordUseCase: ChangeMasterPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun activeVaultId(): VaultId? = sessionManager.currentSession()?.vaultId

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(biometricAvailable = biometricKeyManager.canAuthenticate())
            }
            val vaultId = activeVaultId() ?: return@launch
            vaultRepository.observeVault(vaultId).collect { vault ->
                vault?.let {
                    _uiState.update { state ->
                        state.copy(
                            autoLockPolicy = it.securityPolicy.autoLockPolicy,
                            biometricEnabled =
                                AuthenticationMethod.BIOMETRIC in it.securityPolicy.authenticationMethods,
                        )
                    }
                }
            }
        }
    }

    fun onOpenAutoLockSheet() {
        _uiState.update { it.copy(showAutoLockSheet = true) }
    }

    fun onDismissAutoLockSheet() {
        _uiState.update { it.copy(showAutoLockSheet = false) }
    }

    fun onAutoLockSelected(policy: AutoLockPolicy) {
        val vaultId = activeVaultId() ?: return
        viewModelScope.launch {
            val current = vaultRepository.getVault(vaultId).getOrNull()?.securityPolicy
                ?: VaultSecurityPolicy()
            vaultRepository.updateVaultSecurityPolicy(
                vaultId,
                current.copy(autoLockPolicy = policy),
            )
            _uiState.update { it.copy(showAutoLockSheet = false) }
        }
    }

    fun onBiometricToggle(
        activity: FragmentActivity,
        enable: Boolean,
        promptTitle: String,
        promptSubtitle: String,
        promptNegative: String,
    ) {
        val vaultId = activeVaultId() ?: return
        if (_uiState.value.biometricBusy) return

        if (!enable) {
            viewModelScope.launch {
                _uiState.update { it.copy(biometricBusy = true, biometricError = null) }
                biometricKeyManager.disable(vaultId)
                updatePolicyBiometric(vaultId, enabled = false)
                _uiState.update { it.copy(biometricBusy = false) }
            }
            return
        }

        // Enrollment wraps the live session key, and the biometric-bound
        // key needs a recent authentication for that encrypt — prompt
        // first, enroll inside the success callback.
        biometricKeyManager.authenticate(
            activity = activity,
            title = promptTitle,
            subtitle = promptSubtitle,
            negativeButtonText = promptNegative,
            onSuccess = {
                viewModelScope.launch {
                    _uiState.update { it.copy(biometricBusy = true, biometricError = null) }
                    when (val result = biometricKeyManager.enable(vaultId)) {
                        EnableResult.Success -> updatePolicyBiometric(vaultId, enabled = true)
                        EnableResult.NoActiveSession,
                        is EnableResult.KeystoreError ->
                            _uiState.update { it.copy(biometricError = result.describe()) }
                    }
                    _uiState.update { it.copy(biometricBusy = false) }
                }
            },
            onError = { message ->
                _uiState.update { it.copy(biometricError = message) }
            },
        )
    }

    private suspend fun updatePolicyBiometric(vaultId: VaultId, enabled: Boolean) {
        val current = vaultRepository.getVault(vaultId).getOrNull()?.securityPolicy
            ?: VaultSecurityPolicy()
        val methods = if (enabled) {
            current.authenticationMethods + AuthenticationMethod.BIOMETRIC
        } else {
            // MASTER_PASSWORD always stays: the policy invariant requires it.
            current.authenticationMethods - AuthenticationMethod.BIOMETRIC
        }
        vaultRepository.updateVaultSecurityPolicy(
            vaultId,
            current.copy(authenticationMethods = methods),
        )
    }

    private fun EnableResult.describe(): String = when (this) {
        EnableResult.Success -> ""
        EnableResult.NoActiveSession -> "no active vault session"
        is EnableResult.KeystoreError -> message
    }

    fun onOpenChangePassword() {
        _uiState.update {
            it.copy(
                showChangePasswordSheet = true,
                changePasswordError = null,
                changePasswordSuccess = false,
            )
        }
    }

    fun onDismissChangePassword() {
        _uiState.update { it.copy(showChangePasswordSheet = false) }
    }

    fun onChangePassword(current: String, new: String, confirm: String) {
        val vaultId = activeVaultId() ?: return
        if (new != confirm) {
            _uiState.update { it.copy(changePasswordError = "The new passwords do not match.") }
            return
        }
        if (_uiState.value.changePasswordBusy) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    changePasswordBusy = true,
                    changePasswordError = null,
                    changePasswordSuccess = false,
                )
            }
            val currentChars = current.toCharArray()
            val newChars = new.toCharArray()
            try {
                when (
                    val result = changeMasterPasswordUseCase(
                        vaultId = vaultId,
                        currentPassword = currentChars,
                        newPassword = newChars,
                    )
                ) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(changePasswordBusy = false, changePasswordSuccess = true)
                        }
                    is Result.Failure ->
                        _uiState.update {
                            it.copy(
                                changePasswordBusy = false,
                                changePasswordError = result.error.toUiMessage(),
                            )
                        }
                }
            } finally {
                // The use case wipes both arrays on every path via the
                // crypto adapter; wipe defensively here as well (the
                // validation-early-return path never reaches the adapter).
                MemoryWiper.wipe(currentChars)
                MemoryWiper.wipe(newChars)
            }
        }
    }

    private fun DomainError.toUiMessage(): String = when (this) {
        is DomainError.AuthenticationError.InvalidCredentials ->
            "The current password is incorrect."
        // ValidationError messages are already user-readable.
        else -> message
    }
}
