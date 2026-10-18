package com.securevault.next.core.vault.usecase

import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.LockVaultUseCase
import com.securevault.next.core.security.session.LockTrigger
import com.securevault.next.core.security.session.SessionKeyStore
import com.securevault.next.core.security.session.VaultSessionManager
import javax.inject.Inject

class LockVaultUseCaseImpl @Inject constructor(
    private val sessionKeyStore: SessionKeyStore,
    private val sessionManager: VaultSessionManager,
) : LockVaultUseCase {

    override suspend fun invoke(vaultId: VaultId): DomainResult<Unit> {
        // destroy() wipes the session key immediately; the manager then
        // publishes the locked state and notifies listeners. Idempotent.
        val locked = sessionManager.lock(reason = LockTrigger.EXPLICIT)
        // sessionKeyStore.destroy() is invoked inside sessionManager.lock().
        return if (locked) DomainResult.success(Unit)
        else DomainResult.failure(
            com.securevault.next.core.domain.error.DomainError.AuthenticationError.SessionExpired,
        )
    }
}
