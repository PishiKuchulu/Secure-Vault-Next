package com.securevault.next.data.repository

import com.securevault.next.core.common.result.getOrElse
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.PasswordHistory
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.PasswordId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.NotesRepository
import com.securevault.next.core.domain.repository.PasswordRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.data.persistence.TransactionRunner
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.entity.EncryptedFieldEntity
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.persistence.entity.PasswordHistoryEntity
import com.securevault.next.data.repository.mapper.ItemMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : PasswordRepository {

    override fun observePasswordItem(itemId: ItemId): Flow<PasswordItem?> {
        return itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? PasswordItem
        }
    }

    override fun observePasswordItems(vaultId: VaultId): Flow<List<PasswordItem>> {
        return itemDao.observeFullItemsByType(vaultId, ItemType.PASSWORD).map { list ->
            ItemMapper.toDomainItems(list, encryptionEngine, itemDao).mapNotNull { it as? PasswordItem }
        }
    }

    override suspend fun getPasswordItem(itemId: ItemId): DomainResult<PasswordItem> {
        val pojo = itemDao.getFullItem(itemId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(itemId.value, "PasswordItem"))
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? PasswordItem
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a PasswordItem"))
        return DomainResult.success(item)
    }

    override suspend fun createPasswordItem(item: PasswordItem): DomainResult<PasswordItem> {
        val notesEncrypted = ItemMapper.encryptNotesResult(item, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        txRunner.runInTransaction {
            itemDao.insertItemMetadata(ItemMapper.toItemEntity(item, notesEncrypted))
            itemDao.insertPassword(ItemMapper.toPasswordEntity(item))
            itemDao.insertItemTagCrossRefs(item.metadata.tagIds.map {
                ItemTagCrossRef(item.metadata.id, it)
            })
            // Wire the (previously dead) password-history write path: the
            // initial password becomes the first history entry.
            recordPasswordHistory(item.metadata.id, ItemMapper.toPasswordEntity(item).password)
        }
        return DomainResult.success(item)
    }

    override suspend fun updatePasswordItem(item: PasswordItem): DomainResult<PasswordItem> {
        val notesEncrypted = ItemMapper.encryptNotesResult(item, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = itemDao.getFullItem(item.metadata.id)
            ?: return DomainResult.failure(
                DomainError.RepositoryError.NotFound(item.metadata.id.value, "PasswordItem")
            )
        txRunner.runInTransaction {
            // H3: load-then-update preserves lastAccessedAt, useCount,
            // isPinned, isArchived, isDeleted, deletedAt, expiresAt.
            itemDao.updateItemMetadata(
                ItemMapper.toItemEntityForUpdate(item, existing.item, notesEncrypted)
            )
            itemDao.insertPassword(ItemMapper.toPasswordEntity(item))
            // Record history only when the password actually changed —
            // ciphertext comparison alone is not enough because every
            // encryption uses a fresh nonce.
            if (passwordChanged(existing.password?.password, item.password)) {
                existing.password?.password?.let { oldPassword ->
                    recordPasswordHistory(item.metadata.id, oldPassword)
                }
            }
            itemDao.deleteItemTagCrossRefs(item.metadata.id)
            itemDao.insertItemTagCrossRefs(item.metadata.tagIds.map {
                ItemTagCrossRef(item.metadata.id, it)
            })
        }
        return DomainResult.success(item)
    }

    override suspend fun deletePasswordItem(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        txRunner.runInTransaction {
            if (permanent) {
                itemDao.deleteItemMetadata(itemId)
            } else {
                // H3: soft-delete stamps deletedAt instead of leaving it NULL.
                itemDao.getFullItem(itemId)?.let { pojo ->
                    itemDao.updateItemMetadata(
                        pojo.item.copy(isDeleted = true, deletedAt = Instant.now())
                    )
                }
            }
        }
        return DomainResult.success(Unit)
    }

    override suspend fun getPasswordHistory(itemId: ItemId): DomainResult<List<PasswordHistory>> {
        val history = itemDao.getPasswordHistory(itemId)
        return DomainResult.success(history.map { ItemMapper.toHistoryDomain(it) })
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<PasswordItem> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)

    private suspend fun recordPasswordHistory(itemId: ItemId, password: EncryptedFieldEntity) {
        itemDao.insertPasswordHistoryEntry(
            PasswordHistoryEntity(
                id = PasswordId(UUID.randomUUID().toString()),
                itemId = itemId,
                password = password,
                changedAt = Instant.now(),
            )
        )
    }

    /**
     * True when the stored password was really replaced. A cheap
     * ciphertext/nonce comparison short-circuits the identical case;
     * otherwise both sides are decrypted and compared as plaintext
     * (a fresh nonce makes equal passwords encrypt differently).
     * Undecryptable values err on the side of recording history.
     */
    private suspend fun passwordChanged(old: EncryptedFieldEntity?, new: EncryptedField): Boolean {
        if (old == null) return true
        if (old.ciphertext == new.ciphertext && old.nonce == new.nonce) return false
        val oldDomain = runCatching {
            EncryptedField(old.ciphertext, old.nonce, old.algorithm)
        }.getOrNull() ?: return true
        val oldPlain = encryptionEngine.decryptField(oldDomain).getOrNull()
        val newPlain = encryptionEngine.decryptField(new).getOrNull()
        return try {
            if (oldPlain == null || newPlain == null) true
            else !oldPlain.contentEquals(newPlain)
        } finally {
            oldPlain?.fill('\u0000')
            newPlain?.fill('\u0000')
        }
    }
}

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
    private val txRunner: TransactionRunner,
) : NotesRepository {

    override fun observeNote(itemId: ItemId): Flow<SecureNote?> {
        return itemDao.observeFullItem(itemId).map { pojo ->
            if (pojo == null) null
            else ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SecureNote
        }
    }

    override fun observeNotes(vaultId: VaultId): Flow<List<SecureNote>> {
        return itemDao.observeFullItemsByType(vaultId, ItemType.SECURE_NOTE).map { list ->
            ItemMapper.toDomainItems(list, encryptionEngine, itemDao).mapNotNull { it as? SecureNote }
        }
    }

    override suspend fun getNote(itemId: ItemId): DomainResult<SecureNote> {
        val pojo = itemDao.getFullItem(itemId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(itemId.value, "SecureNote"))
        val item = ItemMapper.toDomain(pojo, encryptionEngine, itemDao) as? SecureNote
            ?: return DomainResult.failure(DomainError.RepositoryError.ReadFailed("Item $itemId is not a SecureNote"))
        return DomainResult.success(item)
    }

    override suspend fun createNote(note: SecureNote): DomainResult<SecureNote> {
        val notesEncrypted = ItemMapper.encryptNotesResult(note, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        txRunner.runInTransaction {
            itemDao.insertItemMetadata(ItemMapper.toItemEntity(note, notesEncrypted))
            itemDao.insertSecureNote(ItemMapper.toSecureNoteEntity(note))
            itemDao.insertItemTagCrossRefs(note.metadata.tagIds.map {
                ItemTagCrossRef(note.metadata.id, it)
            })
        }
        return DomainResult.success(note)
    }

    override suspend fun updateNote(note: SecureNote): DomainResult<SecureNote> {
        val notesEncrypted = ItemMapper.encryptNotesResult(note, encryptionEngine)
            .getOrElse { return DomainResult.failure(it) }
        val existing = itemDao.getFullItem(note.metadata.id)?.item
            ?: return DomainResult.failure(
                DomainError.RepositoryError.NotFound(note.metadata.id.value, "SecureNote")
            )
        txRunner.runInTransaction {
            // H3: load-then-update preserves lastAccessedAt, useCount,
            // isPinned, isArchived, isDeleted, deletedAt, expiresAt.
            itemDao.updateItemMetadata(
                ItemMapper.toItemEntityForUpdate(note, existing, notesEncrypted)
            )
            itemDao.insertSecureNote(ItemMapper.toSecureNoteEntity(note))
            itemDao.deleteItemTagCrossRefs(note.metadata.id)
            itemDao.insertItemTagCrossRefs(note.metadata.tagIds.map {
                ItemTagCrossRef(note.metadata.id, it)
            })
        }
        return DomainResult.success(note)
    }

    override suspend fun deleteNote(itemId: ItemId, permanent: Boolean): DomainResult<Unit> {
        txRunner.runInTransaction {
            if (permanent) {
                itemDao.deleteItemMetadata(itemId)
            } else {
                // H3: soft-delete stamps deletedAt instead of leaving it NULL.
                itemDao.getFullItem(itemId)?.let { pojo ->
                    itemDao.updateItemMetadata(
                        pojo.item.copy(isDeleted = true, deletedAt = Instant.now())
                    )
                }
            }
        }
        return DomainResult.success(Unit)
    }

    override suspend fun restoreItem(itemId: ItemId): DomainResult<SecureNote> =
        ItemMapper.restoreItem(itemId, itemDao, encryptionEngine, txRunner)
}
