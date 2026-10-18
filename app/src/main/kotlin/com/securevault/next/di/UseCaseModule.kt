package com.securevault.next.di

import com.securevault.next.core.crypto.PasswordGeneratorImpl
import com.securevault.next.core.domain.usecase.ChangeMasterPasswordUseCase
import com.securevault.next.core.domain.usecase.CloseVaultUseCase
import com.securevault.next.core.domain.usecase.CreateBackupUseCase
import com.securevault.next.core.domain.usecase.CreateItemUseCase
import com.securevault.next.core.domain.usecase.CreateVaultUseCase
import com.securevault.next.core.domain.usecase.DeleteItemUseCase
import com.securevault.next.core.domain.usecase.ExportVaultUseCase
import com.securevault.next.core.domain.usecase.GeneratePasswordUseCase
import com.securevault.next.core.domain.usecase.ImportVaultUseCase
import com.securevault.next.core.domain.usecase.LockVaultUseCase
import com.securevault.next.core.domain.usecase.OpenVaultUseCase
import com.securevault.next.core.domain.usecase.RestoreBackupUseCase
import com.securevault.next.core.domain.usecase.RestoreItemUseCase
import com.securevault.next.core.domain.usecase.RunSecurityAuditUseCase
import com.securevault.next.core.domain.usecase.SearchItemsUseCase
import com.securevault.next.core.domain.usecase.UnlockVaultUseCase
import com.securevault.next.core.domain.usecase.UpdateItemUseCase
import com.securevault.next.core.domain.usecase.ValidatePasswordUseCase
import com.securevault.next.core.security.usecase.ValidatePasswordUseCaseImpl
import com.securevault.next.core.vault.usecase.AuditUseCaseImpl
import com.securevault.next.core.vault.usecase.ChangeMasterPasswordUseCaseImpl
import com.securevault.next.core.vault.usecase.CloseVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.CreateBackupUseCaseImpl
import com.securevault.next.core.vault.usecase.CreateItemUseCaseImpl
import com.securevault.next.core.vault.usecase.CreateVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.DeleteItemUseCaseImpl
import com.securevault.next.core.vault.usecase.ExportVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.ImportVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.LockVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.OpenVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.RestoreBackupUseCaseImpl
import com.securevault.next.core.vault.usecase.RestoreItemUseCaseImpl
import com.securevault.next.core.vault.usecase.SearchItemsUseCaseImpl
import com.securevault.next.core.vault.usecase.UnlockVaultUseCaseImpl
import com.securevault.next.core.vault.usecase.UpdateItemUseCaseImpl
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
