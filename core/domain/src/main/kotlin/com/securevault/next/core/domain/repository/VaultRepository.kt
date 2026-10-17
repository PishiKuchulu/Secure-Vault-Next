package com.securevault.next.core.domain.repository

import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Persists and retrieves vaults.
 *
 * This is a contract only — Part 2 (Repository Layer): "Repositories are
 * interfaces. Infrastructure provides implementations." The concrete
 * implementation (backed by `data:persistence` and `core:crypto`) lands
 * in `data:repository` starting Milestone 4.
 *
 * Every method that can fail returns a [DomainResult] rather than
 * throwing; every method that represents an ongoing stream of changes
 * returns a [Flow] rather than requiring the caller to poll.
 */
interface VaultRepository {

    /** Emits the full [Vault] aggregate whenever it changes, or `null` if it does not exist or is locked. */
    fun observeVault(vaultId: VaultId): Flow<Vault?>

    /** Emits the list of [VaultMetadata] for every vault on the device, whether unlocked or not. */
    fun observeAllVaultMetadata(): Flow<List<VaultMetadata>>

    suspend fun getVault(vaultId: VaultId): DomainResult<Vault>

    suspend fun getVaultMetadata(vaultId: VaultId): DomainResult<VaultMetadata>

    suspend fun createVault(
        metadata: VaultMetadata,
        securityPolicy: VaultSecurityPolicy,
        settings: VaultSettings,
        envelopeJson: String?,
    ): DomainResult<Vault>

    /**
     * Loads the serialized encrypted vault envelope for [vaultId], or `null`
     * when the vault predates envelope storage (and is therefore not
     * unlockable). The envelope is intentionally an opaque serialized string
     * here: `core:domain` never depends on the crypto envelope type — the
     * use-case layer performs the encode/decode and cryptographic operations.
     */
    suspend fun getEnvelopeJson(vaultId: VaultId): DomainResult<String?>

    /**
     * Persists a rotated envelope (see `ChangeMasterPasswordUseCase`).
     * Only the envelope column is touched; all other vault state is left
     * alone. Fails with `RepositoryError.NotFound` when the vault id is
     * unknown.
     */
    suspend fun updateEnvelopeJson(vaultId: VaultId, envelopeJson: String): DomainResult<Unit>

    suspend fun updateVaultSettings(vaultId: VaultId, settings: VaultSettings): DomainResult<Unit>

    suspend fun updateVaultSecurityPolicy(vaultId: VaultId, policy: VaultSecurityPolicy): DomainResult<Unit>

    suspend fun deleteVault(vaultId: VaultId): DomainResult<Unit>
}
