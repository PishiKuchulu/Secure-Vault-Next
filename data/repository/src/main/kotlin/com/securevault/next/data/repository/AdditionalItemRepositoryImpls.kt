package com.securevault.next.data.repository

import com.securevault.next.core.common.result.getOrElse
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.item.ApiKey
import com.securevault.next.core.domain.model.item.BankAccount
import com.securevault.next.core.domain.model.item.CreditCard
import com.securevault.next.core.domain.model.item.IdentityDocument
import com.securevault.next.core.domain.model.item.RecoveryCode
import com.securevault.next.core.domain.model.item.SSHKey
import com.securevault.next.core.domain.model.item.SoftwareLicense
import com.securevault.next.core.domain.model.item.TOTPSeed
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.ApiKeyRepository
import com.securevault.next.core.domain.repository.BankAccountRepository
import com.securevault.next.core.domain.repository.CreditCardRepository
import com.securevault.next.core.domain.repository.IdentityRepository
import com.securevault.next.core.domain.repository.RecoveryCodeRepository
import com.securevault.next.core.domain.repository.SoftwareLicenseRepository
import com.securevault.next.core.domain.repository.SshKeyRepository
import com.securevault.next.core.domain.repository.TotpRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.data.persistence.TransactionRunner
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.repository.mapper.ItemMapper
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Conventions shared by the item repositories below:
 *
 * - Every create/update is a multi-statement write (metadata + detail +
 *   tags) and therefore runs inside a single transaction (M6) via
 *   [TransactionRunner].
 * - Updates are load-then-update so the columns the domain model does not
 *   carry (lastAccessedAt, useCount, isPinned, isArchived, isDeleted,
 *   deletedAt, expiresAt) are preserved (H3).
 * - Item notes are encrypted at rest (C5).
 */
@Singleton
class CreditCardRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : CreditCardRepository {
    override fun observeCreditCard(itemId: ItemId): Flow<CreditCard?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? CreditCard
        }
    override fun observeCreditCards(vaultId: VaultId): Flow<List<CreditCard>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.CREDIT_CARD)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? CreditCard }
            }
    override suspend fun getCreditCard(itemId: ItemId): DomainResult<CreditCard> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "CreditCard"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? CreditCard
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a CreditCard"))
        return DomainResult.success(item)
    }
    override suspend fun createCreditCard(card: CreditCard): DomainResult<CreditCard> {
        return writeCreditCard(card, isUpdate = false)
    }
    override suspend fun updateCreditCard(card: CreditCard): DomainResult<CreditCard> {
        return writeCreditCard(card, isUpdate = true)
    }
    private suspend fun writeCreditCard(card: CreditCard, isUpdate: Boolean): DomainResult<CreditCard> {
        val notesEncrypted = ItemMapper.encryptNotesResult(card, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(card.metadata.id)?.item
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.NotFound(card.metadata.id.value, "CreditCard"),
                )
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(card, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(card, notesEncrypted))
            }
            itemDao.insertCreditCard(ItemMapper.toCreditCardEntity(card))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(card.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(card.metadata.tagIds.map { ItemTagCrossRef(card.metadata.id, it) })
        }
        return DomainResult.success(card)
    }
    override suspend fun deleteCreditCard(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<CreditCard> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class BankAccountRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : BankAccountRepository {
    override fun observeBankAccount(itemId: ItemId): Flow<BankAccount?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? BankAccount
        }
    override fun observeBankAccounts(vaultId: VaultId): Flow<List<BankAccount>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.BANK_ACCOUNT)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? BankAccount }
            }
    override suspend fun getBankAccount(itemId: ItemId): DomainResult<BankAccount> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "BankAccount"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? BankAccount
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a BankAccount"))
        return DomainResult.success(item)
    }
    override suspend fun createBankAccount(account: BankAccount): DomainResult<BankAccount> {
        return writeBankAccount(account, isUpdate = false)
    }
    override suspend fun updateBankAccount(account: BankAccount): DomainResult<BankAccount> {
        return writeBankAccount(account, isUpdate = true)
    }
    private suspend fun writeBankAccount(account: BankAccount, isUpdate: Boolean): DomainResult<BankAccount> {
        val notesEncrypted = ItemMapper.encryptNotesResult(account, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(account.metadata.id)?.item
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.NotFound(account.metadata.id.value, "BankAccount"),
                )
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(account, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(account, notesEncrypted))
            }
            itemDao.insertBankAccount(ItemMapper.toBankAccountEntity(account))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(account.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(account.metadata.tagIds.map { ItemTagCrossRef(account.metadata.id, it) })
        }
        return DomainResult.success(account)
    }
    override suspend fun deleteBankAccount(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<BankAccount> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class IdentityRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : IdentityRepository {
    override fun observeIdentity(itemId: ItemId): Flow<IdentityDocument?> =
        itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? IdentityDocument
        }
    override fun observeIdentities(vaultId: VaultId): Flow<List<IdentityDocument>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.IDENTITY_DOCUMENT)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? IdentityDocument }
            }
    override suspend fun getIdentity(itemId: ItemId): DomainResult<IdentityDocument> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "IdentityDocument"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? IdentityDocument
            ?: return DomainResult.failure(
                DomainError.RepositoryError.ReadFailed("Item $itemId is not an IdentityDocument"),
            )
        return DomainResult.success(item)
    }
    override suspend fun createIdentity(identity: IdentityDocument): DomainResult<IdentityDocument> {
        return writeIdentity(identity, isUpdate = false)
    }
    override suspend fun updateIdentity(identity: IdentityDocument): DomainResult<IdentityDocument> {
        return writeIdentity(identity, isUpdate = true)
    }
    private suspend fun writeIdentity(identity: IdentityDocument, isUpdate: Boolean): DomainResult<IdentityDocument> {
        val notesEncrypted = ItemMapper.encryptNotesResult(identity, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(identity.metadata.id)?.item
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.NotFound(identity.metadata.id.value, "IdentityDocument"),
                )
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(identity, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(identity, notesEncrypted))
            }
            itemDao.insertIdentityDocument(ItemMapper.toIdentityDocumentEntity(identity))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(identity.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(identity.metadata.tagIds.map { ItemTagCrossRef(identity.metadata.id, it) })
        }
        return DomainResult.success(identity)
    }
    override suspend fun deleteIdentity(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<IdentityDocument> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class SoftwareLicenseRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : SoftwareLicenseRepository {
    override fun observeLicense(itemId: ItemId): Flow<SoftwareLicense?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SoftwareLicense
        }
    override fun observeLicenses(vaultId: VaultId): Flow<List<SoftwareLicense>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.SOFTWARE_LICENSE)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? SoftwareLicense }
            }
    override suspend fun getLicense(itemId: ItemId): DomainResult<SoftwareLicense> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "SoftwareLicense"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SoftwareLicense
            ?: return DomainResult.failure(
                DomainError.RepositoryError.ReadFailed("Item $itemId is not a SoftwareLicense"),
            )
        return DomainResult.success(item)
    }
    override suspend fun createLicense(license: SoftwareLicense): DomainResult<SoftwareLicense> {
        return writeLicense(license, isUpdate = false)
    }
    override suspend fun updateLicense(license: SoftwareLicense): DomainResult<SoftwareLicense> {
        return writeLicense(license, isUpdate = true)
    }
    private suspend fun writeLicense(license: SoftwareLicense, isUpdate: Boolean): DomainResult<SoftwareLicense> {
        val notesEncrypted = ItemMapper.encryptNotesResult(license, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(license.metadata.id)?.item
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.NotFound(license.metadata.id.value, "SoftwareLicense"),
                )
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(license, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(license, notesEncrypted))
            }
            itemDao.insertSoftwareLicense(ItemMapper.toSoftwareLicenseEntity(license))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(license.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(license.metadata.tagIds.map { ItemTagCrossRef(license.metadata.id, it) })
        }
        return DomainResult.success(license)
    }
    override suspend fun deleteLicense(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<SoftwareLicense> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class ApiKeyRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : ApiKeyRepository {
    override fun observeApiKey(itemId: ItemId): Flow<ApiKey?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? ApiKey
        }
    override fun observeApiKeys(vaultId: VaultId): Flow<List<ApiKey>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.API_KEY)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? ApiKey }
            }
    override suspend fun getApiKey(itemId: ItemId): DomainResult<ApiKey> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "ApiKey"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? ApiKey
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not an ApiKey"))
        return DomainResult.success(item)
    }
    override suspend fun createApiKey(apiKey: ApiKey): DomainResult<ApiKey> {
        return writeApiKey(apiKey, isUpdate = false)
    }
    override suspend fun updateApiKey(apiKey: ApiKey): DomainResult<ApiKey> {
        return writeApiKey(apiKey, isUpdate = true)
    }
    private suspend fun writeApiKey(apiKey: ApiKey, isUpdate: Boolean): DomainResult<ApiKey> {
        val notesEncrypted = ItemMapper.encryptNotesResult(apiKey, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(apiKey.metadata.id)?.item
                ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(apiKey.metadata.id.value, "ApiKey"))
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(apiKey, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(apiKey, notesEncrypted))
            }
            itemDao.insertApiKey(ItemMapper.toApiKeyEntity(apiKey))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(apiKey.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(apiKey.metadata.tagIds.map { ItemTagCrossRef(apiKey.metadata.id, it) })
        }
        return DomainResult.success(apiKey)
    }
    override suspend fun deleteApiKey(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<ApiKey> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class SshKeyRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : SshKeyRepository {
    override fun observeSshKey(itemId: ItemId): Flow<SSHKey?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SSHKey
        }
    override fun observeSshKeys(vaultId: VaultId): Flow<List<SSHKey>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.SSH_KEY)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? SSHKey }
            }
    override suspend fun getSshKey(itemId: ItemId): DomainResult<SSHKey> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "SSHKey"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SSHKey
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not an SSHKey"))
        return DomainResult.success(item)
    }
    override suspend fun createSshKey(sshKey: SSHKey): DomainResult<SSHKey> {
        return writeSshKey(sshKey, isUpdate = false)
    }
    override suspend fun updateSshKey(sshKey: SSHKey): DomainResult<SSHKey> {
        return writeSshKey(sshKey, isUpdate = true)
    }
    private suspend fun writeSshKey(sshKey: SSHKey, isUpdate: Boolean): DomainResult<SSHKey> {
        val notesEncrypted = ItemMapper.encryptNotesResult(sshKey, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(sshKey.metadata.id)?.item
                ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(sshKey.metadata.id.value, "SSHKey"))
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(sshKey, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(sshKey, notesEncrypted))
            }
            itemDao.insertSshKey(ItemMapper.toSshKeyEntity(sshKey))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(sshKey.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(sshKey.metadata.tagIds.map { ItemTagCrossRef(sshKey.metadata.id, it) })
        }
        return DomainResult.success(sshKey)
    }
    override suspend fun deleteSshKey(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<SSHKey> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class RecoveryCodeRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : RecoveryCodeRepository {
    override fun observeRecoveryCode(itemId: ItemId): Flow<RecoveryCode?> =
        itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? RecoveryCode
        }
    override fun observeRecoveryCodes(vaultId: VaultId): Flow<List<RecoveryCode>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.RECOVERY_CODE)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? RecoveryCode }
            }
    override suspend fun getRecoveryCode(itemId: ItemId): DomainResult<RecoveryCode> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "RecoveryCode"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? RecoveryCode
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a RecoveryCode"))
        return DomainResult.success(item)
    }
    override suspend fun createRecoveryCode(recoveryCode: RecoveryCode): DomainResult<RecoveryCode> {
        return writeRecoveryCode(recoveryCode, isUpdate = false)
    }
    override suspend fun updateRecoveryCode(recoveryCode: RecoveryCode): DomainResult<RecoveryCode> {
        return writeRecoveryCode(recoveryCode, isUpdate = true)
    }
    private suspend fun writeRecoveryCode(recoveryCode: RecoveryCode, isUpdate: Boolean): DomainResult<RecoveryCode> {
        val notesEncrypted = ItemMapper.encryptNotesResult(recoveryCode, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(recoveryCode.metadata.id)?.item
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.NotFound(recoveryCode.metadata.id.value, "RecoveryCode"),
                )
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(recoveryCode, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(recoveryCode, notesEncrypted))
            }
            itemDao.insertRecoveryCode(ItemMapper.toRecoveryCodeEntity(recoveryCode))
            if (isUpdate) {
                itemDao.deleteRecoveryCodeValues(recoveryCode.metadata.id)
            }
            itemDao.insertRecoveryCodeValues(ItemMapper.toRecoveryCodeValues(recoveryCode))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(recoveryCode.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(
                recoveryCode.metadata.tagIds.map { ItemTagCrossRef(recoveryCode.metadata.id, it) },
            )
        }
        return DomainResult.success(recoveryCode)
    }
    override suspend fun deleteRecoveryCode(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<RecoveryCode> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

@Singleton
class TotpRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : TotpRepository {
    override fun observeTotp(itemId: ItemId): Flow<TOTPSeed?> = itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? TOTPSeed
        }
    override fun observeTotps(vaultId: VaultId): Flow<List<TOTPSeed>> =
        itemDao.observeFullItemsByType(vaultId, ItemType.TOTP_SEED)
            .map { list ->
                ItemMapper.toDomainItems(list, encryptionEngine, itemDao)
                    .mapNotNull { it as? TOTPSeed }
            }
    override suspend fun getTotp(itemId: ItemId): DomainResult<TOTPSeed> {
        val pojo = itemDao.getFullItem(itemId) ?: return DomainResult.failure(
            DomainError.RepositoryError.NotFound(itemId.value, "TOTPSeed"),
        )
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? TOTPSeed
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a TOTPSeed"))
        return DomainResult.success(item)
    }
    override suspend fun createTotp(totp: TOTPSeed): DomainResult<TOTPSeed> {
        return writeTotp(totp, isUpdate = false)
    }
    override suspend fun updateTotp(totp: TOTPSeed): DomainResult<TOTPSeed> {
        return writeTotp(totp, isUpdate = true)
    }
    private suspend fun writeTotp(totp: TOTPSeed, isUpdate: Boolean): DomainResult<TOTPSeed> {
        val notesEncrypted = ItemMapper.encryptNotesResult(totp, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = if (isUpdate) {
            itemDao.getFullItem(totp.metadata.id)?.item
                ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(totp.metadata.id.value, "TOTPSeed"))
        } else null
        txRunner.runInTransaction {
            if (isUpdate) {
                itemDao.updateItemMetadata(ItemMapper.toItemEntityForUpdate(totp, existing!!, notesEncrypted))
            } else {
                itemDao.insertItemMetadata(ItemMapper.toItemEntity(totp, notesEncrypted))
            }
            itemDao.insertTotpSeed(ItemMapper.toTotpSeedEntity(totp))
            if (isUpdate) {
                itemDao.deleteItemTagCrossRefs(totp.metadata.id)
            }
            itemDao.insertItemTagCrossRefs(totp.metadata.tagIds.map { ItemTagCrossRef(totp.metadata.id, it) })
        }
        return DomainResult.success(totp)
    }
    override suspend fun deleteTotp(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        softDelete(itemDao, txRunner, itemId, permanent)
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<TOTPSeed> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}

/**
 * Shared delete path: permanent deletes the metadata row (details cascade);
 * otherwise the item is soft-deleted with [deletedAt] stamped (H3).
 * Runs inside a transaction so the read-modify-write is atomic.
 */
private suspend fun softDelete(
    itemDao: ItemDao,
    txRunner: TransactionRunner,
    itemId: ItemId,
    permanent: Boolean,
) {
    txRunner.runInTransaction {
        if (permanent) {
            itemDao.deleteItemMetadata(itemId)
        } else {
            itemDao.getFullItem(itemId)?.let { pojo ->
                itemDao.updateItemMetadata(
                    pojo.item.copy(isDeleted = true, deletedAt = Instant.now())
                )
            }
        }
    }
}
