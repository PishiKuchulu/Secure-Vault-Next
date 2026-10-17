package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.domain.model.item.*
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.repository.*
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.CreateItemUseCase
import com.securevault.next.core.domain.usecase.UpdateItemUseCase
import com.securevault.next.core.domain.usecase.DeleteItemUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateItemUseCaseImpl @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val notesRepository: NotesRepository,
    private val creditCardRepository: CreditCardRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val identityRepository: IdentityRepository,
    private val softwareLicenseRepository: SoftwareLicenseRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val sshKeyRepository: SshKeyRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val totpRepository: TotpRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : CreateItemUseCase {
    override suspend fun invoke(item: VaultItem): DomainResult<VaultItem> = withContext(ioDispatcher) {
        when (item) {
            is PasswordItem -> passwordRepository.createPasswordItem(item)
            is SecureNote -> notesRepository.createNote(item)
            is CreditCard -> creditCardRepository.createCreditCard(item)
            is BankAccount -> bankAccountRepository.createBankAccount(item)
            is IdentityDocument -> identityRepository.createIdentity(item)
            is SoftwareLicense -> softwareLicenseRepository.createLicense(item)
            is ApiKey -> apiKeyRepository.createApiKey(item)
            is SSHKey -> sshKeyRepository.createSshKey(item)
            is RecoveryCode -> recoveryCodeRepository.createRecoveryCode(item)
            is TOTPSeed -> totpRepository.createTotp(item)
        }
    }
}

class UpdateItemUseCaseImpl @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val notesRepository: NotesRepository,
    private val creditCardRepository: CreditCardRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val identityRepository: IdentityRepository,
    private val softwareLicenseRepository: SoftwareLicenseRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val sshKeyRepository: SshKeyRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val totpRepository: TotpRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : UpdateItemUseCase {
    override suspend fun invoke(item: VaultItem): DomainResult<VaultItem> = withContext(ioDispatcher) {
        when (item) {
            is PasswordItem -> passwordRepository.updatePasswordItem(item)
            is SecureNote -> notesRepository.updateNote(item)
            is CreditCard -> creditCardRepository.updateCreditCard(item)
            is BankAccount -> bankAccountRepository.updateBankAccount(item)
            is IdentityDocument -> identityRepository.updateIdentity(item)
            is SoftwareLicense -> softwareLicenseRepository.updateLicense(item)
            is ApiKey -> apiKeyRepository.updateApiKey(item)
            is SSHKey -> sshKeyRepository.updateSshKey(item)
            is RecoveryCode -> recoveryCodeRepository.updateRecoveryCode(item)
            is TOTPSeed -> totpRepository.updateTotp(item)
        }
    }
}

class DeleteItemUseCaseImpl @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val notesRepository: NotesRepository,
    private val creditCardRepository: CreditCardRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val identityRepository: IdentityRepository,
    private val softwareLicenseRepository: SoftwareLicenseRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val sshKeyRepository: SshKeyRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val totpRepository: TotpRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : DeleteItemUseCase {
    override suspend fun invoke(itemId: ItemId, permanent: Boolean): DomainResult<Unit> = withContext(ioDispatcher) {
        // The per-type repositories all operate on the shared `items` metadata table
        // (ItemDao.getFullItem/deleteItemMetadata are keyed by item id alone, with no
        // type filter; FK cascades clean the type-specific detail rows). Delegating to
        // any one of them therefore deletes an item of any type correctly -- no
        // per-type probing is needed. Delete is idempotent: deleting an id that does
        // not exist (or is already deleted) still returns success.
        passwordRepository.deletePasswordItem(itemId, permanent)
    }
}
