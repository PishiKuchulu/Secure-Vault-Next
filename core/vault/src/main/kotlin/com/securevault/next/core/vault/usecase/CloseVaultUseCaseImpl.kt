package com.securevault.next.core.vault.usecase

import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.CloseVaultUseCase
import com.securevault.next.core.security.session.LockTrigger
import com.securevault.next.core.security.session.SessionKeyStore
import com.securevault.next.core.security.session.VaultSessionManager
import javax.inject.Inject

/**
 * Ends an unlocked session and discards any decrypted material held in
 * memory for it (Milestone 4 — the previously-missing close contract).
 *
 * Delegates entirely to [VaultSessionManager.lock] with
 * [LockTrigger.EXPLICIT]: the manager is the single owner of the
 * session <-> key lifecycle, and its [VaultSessionManager.lock] already
 * guarantees `SessionKeyStore.destroy()` runs *before* the Locked state is
 * published (so no observer ever sees "locked" while the key is still in
 * memory). This use case explicitly does NOT call [SessionKeyStore.destroy]
 * itself — doing so would duplicate the manager's ownership logic and risk
 * a double-destroy.
 *
 * Idempotency is contract-driven, matching the domain's own lifecycle
 * vocabulary: closing an already-closed vault is the [DomainError.VaultError.AlreadyLocked]
 * case, not a silent no-op and not an invented error.
 */
class CloseVaultUseCaseImpl
@Inject
constructor(private val sessionManager: VaultSessionManager) :
    CloseVaultUseCase {
    override suspend fun invoke(vaultId: VaultId): DomainResult<Unit> {
        val closed = sessionManager.lock(reason = LockTrigger.EXPLICIT)
        return if (closed) {
            DomainResult.success(Unit)
        } else {
            DomainResult.failure(DomainError.VaultError.AlreadyLocked(vaultId))
        }
    }
}
