package com.securevault.next.core.domain.usecase

import com.securevault.next.core.domain.model.AuditResult
import com.securevault.next.core.domain.model.BackupMetadata
import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult

/** Creates an encrypted backup of a vault's current state. */
fun interface CreateBackupUseCase {
    suspend operator fun invoke(vaultId: VaultId): DomainResult<BackupMetadata>
}

/** Restores a vault from a previously created backup, authenticating with the master password used to create it. */
fun interface RestoreBackupUseCase {
    suspend operator fun invoke(backupId: String, masterPassword: CharArray): DomainResult<Vault>
}

/** Runs a security audit over a vault's items, producing an [AuditResult]. */
fun interface RunSecurityAuditUseCase {
    suspend operator fun invoke(vaultId: VaultId): DomainResult<AuditResult>
}
