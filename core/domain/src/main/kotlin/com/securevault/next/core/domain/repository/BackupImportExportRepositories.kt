package com.securevault.next.core.domain.repository

import com.securevault.next.core.domain.model.BackupMetadata
import com.securevault.next.core.domain.model.ExportMetadata
import com.securevault.next.core.domain.model.ImportMetadata
import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/** Creates, lists and restores encrypted vault backups. */
interface BackupRepository {
    fun observeBackups(vaultId: VaultId): Flow<List<BackupMetadata>>

    suspend fun createBackup(vaultId: VaultId): DomainResult<BackupMetadata>

    /** [masterPassword] is consumed to derive the key that decrypts the backup; the repository never stores it. */
    suspend fun restoreBackup(backupId: String, masterPassword: CharArray): DomainResult<Vault>

    suspend fun deleteBackup(backupId: String): DomainResult<Unit>
}

/** Imports vault items from an external source and format. */
interface ImportRepository {
    suspend fun importFrom(vaultId: VaultId, source: String, format: String): DomainResult<ImportMetadata>
}

/** Exports vault items to an external destination and format. */
interface ExportRepository {
    suspend fun exportTo(vaultId: VaultId, destination: String, format: String): DomainResult<ExportMetadata>
}
