package com.securevault.next.di

import com.securevault.next.core.domain.repository.*
import com.securevault.next.data.persistence.SecureVaultDatabase
import com.securevault.next.data.persistence.TransactionRunner
import com.securevault.next.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * M6: exposes the database transaction runner to the repository
     * implementations (which cannot reference Room types directly) so
     * multi-statement writes run atomically. The SecureVaultDatabase
     * instance is provided by DatabaseModule.
     */
    @Binds
    @Singleton
    abstract fun bindTransactionRunner(db: SecureVaultDatabase): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    @Singleton
    abstract fun bindPasswordRepository(impl: PasswordRepositoryImpl): PasswordRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardRepository(impl: CreditCardRepositoryImpl): CreditCardRepository

    @Binds
    @Singleton
    abstract fun bindBankAccountRepository(impl: BankAccountRepositoryImpl): BankAccountRepository

    @Binds
    @Singleton
    abstract fun bindIdentityRepository(impl: IdentityRepositoryImpl): IdentityRepository

    @Binds
    @Singleton
    abstract fun bindSoftwareLicenseRepository(impl: SoftwareLicenseRepositoryImpl): SoftwareLicenseRepository

    @Binds
    @Singleton
    abstract fun bindApiKeyRepository(impl: ApiKeyRepositoryImpl): ApiKeyRepository

    @Binds
    @Singleton
    abstract fun bindSshKeyRepository(impl: SshKeyRepositoryImpl): SshKeyRepository

    @Binds
    @Singleton
    abstract fun bindRecoveryCodeRepository(impl: RecoveryCodeRepositoryImpl): RecoveryCodeRepository

    @Binds
    @Singleton
    abstract fun bindTotpRepository(impl: TotpRepositoryImpl): TotpRepository
}
