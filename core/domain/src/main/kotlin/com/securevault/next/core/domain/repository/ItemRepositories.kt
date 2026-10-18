package com.securevault.next.core.domain.repository

import com.securevault.next.core.domain.model.PasswordHistory
import com.securevault.next.core.domain.model.item.ApiKey
import com.securevault.next.core.domain.model.item.BankAccount
import com.securevault.next.core.domain.model.item.CreditCard
import com.securevault.next.core.domain.model.item.IdentityDocument
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.RecoveryCode
import com.securevault.next.core.domain.model.item.SSHKey
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.item.SoftwareLicense
import com.securevault.next.core.domain.model.item.TOTPSeed
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Persists and retrieves [PasswordItem]s specifically, including their
 * change history.
 *
 * A dedicated repository per item type (rather than one generic
 * `ItemRepository<T>`) mirrors the project's own module contract list
 * (Part 2) and lets each item type's repository expose operations that
 * only make sense for it — [getPasswordHistory] has no equivalent on
 * [NotesRepository].
 */
interface PasswordRepository {
    fun observePasswordItem(itemId: ItemId): Flow<PasswordItem?>

    fun observePasswordItems(vaultId: VaultId): Flow<List<PasswordItem>>

    suspend fun getPasswordItem(itemId: ItemId): DomainResult<PasswordItem>

    suspend fun createPasswordItem(item: PasswordItem): DomainResult<PasswordItem>

    suspend fun updatePasswordItem(item: PasswordItem): DomainResult<PasswordItem>

    suspend fun deletePasswordItem(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted password out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<PasswordItem>

    suspend fun getPasswordHistory(itemId: ItemId): DomainResult<List<PasswordHistory>>
}

/** Persists and retrieves [SecureNote]s specifically. */
interface NotesRepository {
    fun observeNote(itemId: ItemId): Flow<SecureNote?>

    fun observeNotes(vaultId: VaultId): Flow<List<SecureNote>>

    suspend fun getNote(itemId: ItemId): DomainResult<SecureNote>

    suspend fun createNote(note: SecureNote): DomainResult<SecureNote>

    suspend fun updateNote(note: SecureNote): DomainResult<SecureNote>

    suspend fun deleteNote(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted note out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<SecureNote>
}

/** Persists and retrieves [CreditCard]s specifically. */
interface CreditCardRepository {
    fun observeCreditCard(itemId: ItemId): Flow<CreditCard?>

    fun observeCreditCards(vaultId: VaultId): Flow<List<CreditCard>>

    suspend fun getCreditCard(itemId: ItemId): DomainResult<CreditCard>

    suspend fun createCreditCard(card: CreditCard): DomainResult<CreditCard>

    suspend fun updateCreditCard(card: CreditCard): DomainResult<CreditCard>

    suspend fun deleteCreditCard(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted creditcard out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<CreditCard>
}

/** Persists and retrieves [BankAccount]s specifically. */
interface BankAccountRepository {
    fun observeBankAccount(itemId: ItemId): Flow<BankAccount?>

    fun observeBankAccounts(vaultId: VaultId): Flow<List<BankAccount>>

    suspend fun getBankAccount(itemId: ItemId): DomainResult<BankAccount>

    suspend fun createBankAccount(account: BankAccount): DomainResult<BankAccount>

    suspend fun updateBankAccount(account: BankAccount): DomainResult<BankAccount>

    suspend fun deleteBankAccount(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted bankaccount out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<BankAccount>
}

/** Persists and retrieves [IdentityDocument]s specifically. */
interface IdentityRepository {
    fun observeIdentity(itemId: ItemId): Flow<IdentityDocument?>

    fun observeIdentities(vaultId: VaultId): Flow<List<IdentityDocument>>

    suspend fun getIdentity(itemId: ItemId): DomainResult<IdentityDocument>

    suspend fun createIdentity(identity: IdentityDocument): DomainResult<IdentityDocument>

    suspend fun updateIdentity(identity: IdentityDocument): DomainResult<IdentityDocument>

    suspend fun deleteIdentity(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted identity out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<IdentityDocument>
}

/** Persists and retrieves [SoftwareLicense]s specifically. */
interface SoftwareLicenseRepository {
    fun observeLicense(itemId: ItemId): Flow<SoftwareLicense?>

    fun observeLicenses(vaultId: VaultId): Flow<List<SoftwareLicense>>

    suspend fun getLicense(itemId: ItemId): DomainResult<SoftwareLicense>

    suspend fun createLicense(license: SoftwareLicense): DomainResult<SoftwareLicense>

    suspend fun updateLicense(license: SoftwareLicense): DomainResult<SoftwareLicense>

    suspend fun deleteLicense(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted license out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<SoftwareLicense>
}

/** Persists and retrieves [ApiKey]s specifically. */
interface ApiKeyRepository {
    fun observeApiKey(itemId: ItemId): Flow<ApiKey?>

    fun observeApiKeys(vaultId: VaultId): Flow<List<ApiKey>>

    suspend fun getApiKey(itemId: ItemId): DomainResult<ApiKey>

    suspend fun createApiKey(apiKey: ApiKey): DomainResult<ApiKey>

    suspend fun updateApiKey(apiKey: ApiKey): DomainResult<ApiKey>

    suspend fun deleteApiKey(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted apikey out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<ApiKey>
}

/** Persists and retrieves [SSHKey]s specifically. */
interface SshKeyRepository {
    fun observeSshKey(itemId: ItemId): Flow<SSHKey?>

    fun observeSshKeys(vaultId: VaultId): Flow<List<SSHKey>>

    suspend fun getSshKey(itemId: ItemId): DomainResult<SSHKey>

    suspend fun createSshKey(sshKey: SSHKey): DomainResult<SSHKey>

    suspend fun updateSshKey(sshKey: SSHKey): DomainResult<SSHKey>

    suspend fun deleteSshKey(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted sshkey out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<SSHKey>
}

/** Persists and retrieves [RecoveryCode] sets specifically. */
interface RecoveryCodeRepository {
    fun observeRecoveryCode(itemId: ItemId): Flow<RecoveryCode?>

    fun observeRecoveryCodes(vaultId: VaultId): Flow<List<RecoveryCode>>

    suspend fun getRecoveryCode(itemId: ItemId): DomainResult<RecoveryCode>

    suspend fun createRecoveryCode(recoveryCode: RecoveryCode): DomainResult<RecoveryCode>

    suspend fun updateRecoveryCode(recoveryCode: RecoveryCode): DomainResult<RecoveryCode>

    suspend fun deleteRecoveryCode(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted recoverycode out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<RecoveryCode>
}

/** Persists and retrieves [TOTPSeed]s specifically. */
interface TotpRepository {
    fun observeTotp(itemId: ItemId): Flow<TOTPSeed?>

    fun observeTotps(vaultId: VaultId): Flow<List<TOTPSeed>>

    suspend fun getTotp(itemId: ItemId): DomainResult<TOTPSeed>

    suspend fun createTotp(totp: TOTPSeed): DomainResult<TOTPSeed>

    suspend fun updateTotp(totp: TOTPSeed): DomainResult<TOTPSeed>

    suspend fun deleteTotp(
        itemId: ItemId,
        permanent: Boolean = false,
    ): DomainResult<Unit>

    /**
     * Restores a previously soft-deleted totp out of trash.
     *
     * Added for Finding M10: the [com.securevault.next.core.domain.usecase.RestoreItemUseCase]
     * contract had no repository method to delegate to.
     */
    suspend fun restoreItem(itemId: ItemId): DomainResult<TOTPSeed>
}
