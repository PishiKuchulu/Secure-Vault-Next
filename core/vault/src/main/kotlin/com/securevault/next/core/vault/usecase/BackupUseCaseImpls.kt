package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.domain.model.BackupMetadata
import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.BackupRepository
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.CreateBackupUseCase
import com.securevault.next.core.domain.usecase.RestoreBackupUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CreateBackupUseCaseImpl
@Inject
constructor(
    private val backupRepository: BackupRepository,
    private val vaultRepository: VaultRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : CreateBackupUseCase {
    override suspend fun invoke(vaultId: VaultId): DomainResult<BackupMetadata> = withContext(ioDispatcher) {
        // Coordinating backup creation
        backupRepository.createBackup(vaultId)
    }
}

class RestoreBackupUseCaseImpl
@Inject
constructor(
    private val backupRepository: BackupRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : RestoreBackupUseCase {
    override suspend fun invoke(backupId: String, masterPassword: CharArray): DomainResult<Vault> =
        withContext(ioDispatcher) {
            backupRepository.restoreBackup(backupId, masterPassword)
        }
}
