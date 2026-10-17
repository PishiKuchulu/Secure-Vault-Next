package com.securevault.next.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.usecase.LockVaultUseCase
import com.securevault.next.core.security.session.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs the explicit lock action from navigation callbacks (the home
 * screen's lock action, "Lock now" in settings).
 *
 * Resolves the currently unlocked vault from the session manager, runs
 * [LockVaultUseCase], and reports the locked vault's id back so the
 * caller can navigate to its unlock screen. Locking is idempotent in
 * the engine, so a stale tap while already locked simply re-reports the
 * last known vault id (or nothing when no session ever existed).
 */
@HiltViewModel
class LockActionViewModel @Inject constructor(
    private val lockVaultUseCase: LockVaultUseCase,
    private val sessionManager: VaultSessionManager,
) : ViewModel() {

    fun lockNow(onLocked: (vaultId: String) -> Unit) {
        viewModelScope.launch {
            val vaultId: VaultId = sessionManager.currentSession()?.vaultId ?: return@launch
            lockVaultUseCase(vaultId)
            onLocked(vaultId.value)
        }
    }
}
