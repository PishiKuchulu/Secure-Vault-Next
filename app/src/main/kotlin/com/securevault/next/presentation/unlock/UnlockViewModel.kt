package com.securevault.next.presentation.unlock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.biometric.BiometricKeyManager
import com.securevault.next.biometric.BiometricUnlockResult
import com.securevault.next.biometric.EnableResult
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.common.result.fold
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.usecase.OpenVaultUseCase
import com.securevault.next.core.domain.usecase.UnlockVaultUseCase
import com.securevault.next.core.security.session.FailedAttemptTracker
import com.securevault.next.core.security.session.LockoutState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class UnlockUiState(
    val vaultName: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val biometricBusy: Boolean = false,
    /** Engine or prompt error message, shown inline. */
    val inlineError: String? = null,
    /** Set after a wrong password: how many attempts remain before lockout. */
    val wrongPasswordAttemptsLeft: Int? = null,
    /** The biometric enrollment died (e.g. new biometric enrolled): fall back to password. */
    val biometricKeyInvalidated: Boolean = false,
    /** Detail message when biometric enrollment failed. */
    val biometricEnableFailure: String? = null,
    /** Seconds left on the brute-force cool-down; 0 when attempts are allowed. */
    val lockoutRemainingSeconds: Long = 0L,
    val canUseBiometrics: Boolean = false,
    val showBiometricOffer: Boolean = false,
    val enablingBiometrics: Boolean = false,
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val openVaultUseCase: OpenVaultUseCase,
    private val unlockVaultUseCase: UnlockVaultUseCase,
    private val failedAttemptTracker: FailedAttemptTracker,
    private val biometricKeyManager: BiometricKeyManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private val homeChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val homeEvents: Flow<Unit> = homeChannel.receiveAsFlow()

    private var lockoutTicker: Job? = null

    fun load(vaultId: String) {
        viewModelScope.launch {
            openVaultUseCase(VaultId(vaultId)).fold(
                onSuccess = { metadata ->
                    _uiState.update { it.copy(vaultName = metadata.name.value) }
                },
                onFailure = { /* name stays blank; unlock still works */ },
            )
            refreshBiometricAvailability(vaultId)
            // A lockout survives screen rotations (the tracker is
            // process-scoped): resume its countdown if one is active.
            when (val state = failedAttemptTracker.currentState(VaultId(vaultId))) {
                is LockoutState.LockedOut -> startLockoutCountdown(state.retryAt)
                is LockoutState.Allowed -> Unit
            }
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                inlineError = null,
                wrongPasswordAttemptsLeft = null,
                biometricKeyInvalidated = false,
                biometricEnableFailure = null,
            )
        }
    }

    fun onUnlockClick(vaultId: String) {
        val state = _uiState.value
        if (state.isLoading || state.lockoutRemainingSeconds > 0) return
        val password = state.password
        if (password.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    inlineError = null,
                    wrongPasswordAttemptsLeft = null,
                    // Clear the visible text the moment the secret leaves
                    // it; only the CharArray below carries it now.
                    password = "",
                )
            }
            val passwordChars = password.toCharArray()
            try {
                unlockVaultUseCase(
                    VaultId(vaultId),
                    passwordChars,
                    AuthenticationMethod.MASTER_PASSWORD,
                ).fold(
                    onSuccess = { onPasswordUnlockSucceeded(vaultId) },
                    onFailure = { error -> handleUnlockError(error) },
                )
            } finally {
                MemoryWiper.wipe(passwordChars)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onBiometricClick(
        activity: FragmentActivity,
        vaultId: String,
        promptTitle: String,
        promptSubtitle: String,
        promptNegative: String,
    ) {
        if (_uiState.value.biometricBusy || _uiState.value.lockoutRemainingSeconds > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(biometricBusy = true, inlineError = null) }
            biometricKeyManager.authenticate(
                activity = activity,
                title = promptTitle,
                subtitle = promptSubtitle,
                negativeButtonText = promptNegative,
                onSuccess = {
                    viewModelScope.launch {
                        when (val result = biometricKeyManager.unlockWithBiometrics(VaultId(vaultId))) {
                            is BiometricUnlockResult.Success -> goHome()
                            is BiometricUnlockResult.AlreadyUnlocked -> goHome()
                            BiometricUnlockResult.KeyInvalidated -> {
                                refreshBiometricAvailability(vaultId)
                                _uiState.update {
                                    it.copy(
                                        biometricBusy = false,
                                        biometricKeyInvalidated = true,
                                    )
                                }
                            }
                            is BiometricUnlockResult.Failed -> {
                                _uiState.update {
                                    it.copy(biometricBusy = false, inlineError = result.message)
                                }
                            }
                        }
                    }
                },
                onError = { message ->
                    _uiState.update { it.copy(biometricBusy = false, inlineError = message) }
                },
            )
        }
    }

    fun onEnableBiometrics(
        activity: FragmentActivity,
        vaultId: String,
        promptTitle: String,
        promptSubtitle: String,
        promptNegative: String,
    ) {
        if (_uiState.value.enablingBiometrics) return
        // The biometric-bound key needs a recent authentication for the
        // wrapping encrypt, so the prompt comes first and enrollment
        // runs inside its success callback.
        biometricKeyManager.authenticate(
            activity = activity,
            title = promptTitle,
            subtitle = promptSubtitle,
            negativeButtonText = promptNegative,
            onSuccess = {
                viewModelScope.launch {
                    _uiState.update { it.copy(enablingBiometrics = true) }
                    when (val result = biometricKeyManager.enable(VaultId(vaultId))) {
                        EnableResult.Success -> {
                            biometricKeyManager.markOfferDismissed(VaultId(vaultId))
                            _uiState.update {
                                it.copy(
                                    showBiometricOffer = false,
                                    enablingBiometrics = false,
                                    canUseBiometrics = true,
                                )
                            }
                            goHome()
                        }
                        EnableResult.NoActiveSession,
                        is EnableResult.KeystoreError -> {
                            _uiState.update {
                                it.copy(
                                    enablingBiometrics = false,
                                    biometricEnableFailure = result.describe(),
                                )
                            }
                        }
                    }
                }
            },
            onError = { message ->
                _uiState.update { it.copy(inlineError = message) }
            },
        )
    }

    fun onDismissBiometricOffer(vaultId: String) {
        biometricKeyManager.markOfferDismissed(VaultId(vaultId))
        _uiState.update { it.copy(showBiometricOffer = false) }
        goHome()
    }

    fun clearInlineError() {
        _uiState.update {
            it.copy(
                inlineError = null,
                biometricKeyInvalidated = false,
                biometricEnableFailure = null,
            )
        }
    }

    private fun onPasswordUnlockSucceeded(vaultId: String) {
        val id = VaultId(vaultId)
        val offerable = biometricKeyManager.canAuthenticate() &&
            !biometricKeyManager.isEnabled(id) &&
            !biometricKeyManager.isOfferDismissed(id)
        if (offerable) {
            _uiState.update { it.copy(showBiometricOffer = true) }
        } else {
            goHome()
        }
    }

    private fun handleUnlockError(error: DomainError) {
        when (error) {
            is DomainError.AuthenticationError.TooManyAttempts ->
                startLockoutCountdown(
                    Instant.now().plusSeconds(error.retryAfterSeconds),
                )
            is DomainError.AuthenticationError.InvalidCredentials -> {
                val remaining = when (val state = failedAttemptTracker.currentState(VaultId(vaultId))) {
                    is LockoutState.Allowed -> state.attemptsRemaining
                    is LockoutState.LockedOut -> {
                        startLockoutCountdown(state.retryAt)
                        null
                    }
                }
                _uiState.update {
                    it.copy(wrongPasswordAttemptsLeft = remaining)
                }
            }
            else -> _uiState.update { it.copy(inlineError = error.message) }
        }
    }

    private fun startLockoutCountdown(retryAt: Instant) {
        lockoutTicker?.cancel()
        lockoutTicker = viewModelScope.launch {
            while (true) {
                val remaining = Duration.between(Instant.now(), retryAt).seconds.coerceAtLeast(0)
                _uiState.update { it.copy(lockoutRemainingSeconds = remaining) }
                if (remaining <= 0) break
                delay(1_000)
            }
        }
    }

    private suspend fun refreshBiometricAvailability(vaultId: String) {
        val id = VaultId(vaultId)
        val available = biometricKeyManager.canAuthenticate() && biometricKeyManager.isEnabled(id)
        _uiState.update { it.copy(canUseBiometrics = available) }
    }

    private fun goHome() {
        viewModelScope.launch { homeChannel.send(Unit) }
    }

    private fun EnableResult.describe(): String = when (this) {
        EnableResult.Success -> ""
        EnableResult.NoActiveSession -> "no active vault session"
        is EnableResult.KeystoreError -> message
    }
}
