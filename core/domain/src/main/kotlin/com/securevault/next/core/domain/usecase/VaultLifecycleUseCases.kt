package com.securevault.next.core.domain.usecase

import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.result.DomainResult

/**
 * Vault lifecycle use cases — Part 9 (Application Flow): `Splash →
 * Security Check → Vault Locked → Authentication → Vault Home`.
 *
 * Every use case here is a `fun interface` with a single
 * `suspend operator fun invoke(...)` member, so an implementation reads
 * as `useCase(params)` at the call site rather than
 * `useCase.execute(params)` — Kotlin's single-abstract-method `invoke`
 * convention for a single-responsibility class. These are contracts
 * only in Milestone 2: no class implements any of them yet, since a real
 * implementation needs `core:crypto` (Milestone 3) and `data:repository`
 * (Milestone 4).
 *
 * [masterPassword] parameters are [CharArray], never [String]: a
 * `CharArray` can be zeroed out by the caller immediately after use,
 * while a `String` is immutable and will persist in memory for an
 * unpredictable length of time until garbage collected.
 */
fun interface CreateVaultUseCase {
    suspend operator fun invoke(
        name: VaultName,
        masterPassword: CharArray,
        securityPolicy: VaultSecurityPolicy,
        settings: VaultSettings
    ): DomainResult<Vault>
}

/** Loads a vault's [VaultMetadata] so its lock screen can be shown, without unlocking it. */
fun interface OpenVaultUseCase {
    suspend operator fun invoke(vaultId: VaultId): DomainResult<VaultMetadata>
}

/** Ends an unlocked session and discards any decrypted material held in memory for it. */
fun interface CloseVaultUseCase {
    suspend operator fun invoke(vaultId: VaultId): DomainResult<Unit>
}

/** Locks an already-open vault, requiring re-authentication before its contents are visible again. */
fun interface LockVaultUseCase {
    suspend operator fun invoke(vaultId: VaultId): DomainResult<Unit>
}

/** Authenticates against a vault and, on success, establishes a new [VaultSession]. */
fun interface UnlockVaultUseCase {
    suspend operator fun invoke(
        vaultId: VaultId,
        masterPassword: CharArray,
        method: AuthenticationMethod
    ): DomainResult<VaultSession>
}

/**
 * Changes a vault's master password: verifies [currentPassword], then
 * re-wraps the (unchanged) vault key under [newPassword] and persists the
 * rotated envelope. [newPassword] must satisfy the master-password policy.
 * Both arrays are wiped by the engine on every path; the caller must not
 * reuse them after the call.
 */
fun interface ChangeMasterPasswordUseCase {
    suspend operator fun invoke(
        vaultId: VaultId,
        currentPassword: CharArray,
        newPassword: CharArray
    ): DomainResult<Unit>
}
