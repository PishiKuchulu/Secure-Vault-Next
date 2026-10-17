package com.securevault.next.di

import com.securevault.next.core.crypto.PasswordGeneratorImpl
import com.securevault.next.core.domain.usecase.*
import com.securevault.next.core.vault.usecase.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindCreateVaultUseCase(impl: CreateVaultUseCaseImpl): CreateVaultUseCase

    @Binds
    @Singleton
    abstract fun bindOpenVaultUseCase(impl: OpenVaultUseCaseImpl): OpenVaultUseCase

    @Binds
    @Singleton
    abstract fun bindUnlockVaultUseCase(impl: UnlockVaultUseCaseImpl): UnlockVaultUseCase

    @Binds
    @Singleton
    abstract fun bindLockVaultUseCase(impl: LockVaultUseCaseImpl): LockVaultUseCase

    @Binds
    @Singleton
    abstract fun bindCloseVaultUseCase(impl: CloseVaultUseCaseImpl): CloseVaultUseCase

    @Binds
    @Singleton
    abstract fun bindCreateItemUseCase(impl: CreateItemUseCaseImpl): CreateItemUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateItemUseCase(impl: UpdateItemUseCaseImpl): UpdateItemUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteItemUseCase(impl: DeleteItemUseCaseImpl): DeleteItemUseCase

    @Binds
    @Singleton
    abstract fun bindValidatePasswordUseCase(impl: ValidatePasswordUseCaseImpl): ValidatePasswordUseCase

    @Binds
    @Singleton
    abstract fun bindRunSecurityAuditUseCase(impl: AuditUseCaseImpl): RunSecurityAuditUseCase

    @Binds
    @Singleton
    abstract fun bindGeneratePasswordUseCase(impl: PasswordGeneratorImpl): GeneratePasswordUseCase

    @Binds
    @Singleton
    abstract fun bindImportVaultUseCase(impl: ImportVaultUseCaseImpl): ImportVaultUseCase

    @Binds
    @Singleton
    abstract fun bindExportVaultUseCase(impl: ExportVaultUseCaseImpl): ExportVaultUseCase

    @Binds
    @Singleton
    abstract fun bindCreateBackupUseCase(impl: CreateBackupUseCaseImpl): CreateBackupUseCase

    @Binds
    @Singleton
    abstract fun bindRestoreBackupUseCase(impl: RestoreBackupUseCaseImpl): RestoreBackupUseCase

    // Finding M10: the two contract-only use cases finally have
    // implementations and bindings.
    @Binds
    @Singleton
    abstract fun bindSearchItemsUseCase(impl: SearchItemsUseCaseImpl): SearchItemsUseCase

    @Binds
    @Singleton
    abstract fun bindRestoreItemUseCase(impl: RestoreItemUseCaseImpl): RestoreItemUseCase

    @Binds
    @Singleton
    abstract fun bindChangeMasterPasswordUseCase(impl: ChangeMasterPasswordUseCaseImpl): ChangeMasterPasswordUseCase
}
