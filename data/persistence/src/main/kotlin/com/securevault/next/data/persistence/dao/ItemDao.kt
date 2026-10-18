package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.ApiKeyEntity
import com.securevault.next.data.persistence.entity.BankAccountEntity
import com.securevault.next.data.persistence.entity.CreditCardEntity
import com.securevault.next.data.persistence.entity.IdentityDocumentEntity
import com.securevault.next.data.persistence.entity.ItemEntity
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.persistence.entity.PasswordEntity
import com.securevault.next.data.persistence.entity.PasswordHistoryEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeValueEntity
import com.securevault.next.data.persistence.entity.SecureNoteEntity
import com.securevault.next.data.persistence.entity.SoftwareLicenseEntity
import com.securevault.next.data.persistence.entity.SshKeyEntity
import com.securevault.next.data.persistence.entity.TotpSeedEntity
import com.securevault.next.data.persistence.pojo.FullItemPojo
import com.securevault.next.data.persistence.pojo.ItemSummaryPojo
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [ItemEntity] and all type-specific item details.
 */
@Dao
interface ItemDao {
    // --- Metadata and Summaries ---

    @Transaction
    @Query("SELECT * FROM items WHERE vault_id = :vaultId AND is_deleted = 0 ORDER BY title ASC")
    fun observeItemSummaries(vaultId: VaultId): Flow<List<ItemSummaryPojo>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeFullItem(itemId: ItemId): Flow<FullItemPojo?>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getFullItem(itemId: ItemId): FullItemPojo?

    @Transaction
    @Query("SELECT * FROM items WHERE vault_id = :vaultId AND (:type IS NULL OR item_type = :type) AND is_deleted = 0")
    fun observeFullItemsByType(
        vaultId: VaultId,
        type: ItemType?,
    ): Flow<List<FullItemPojo>>

    @Transaction
    @Query("SELECT * FROM items WHERE vault_id = :vaultId AND (:type IS NULL OR item_type = :type) AND is_deleted = 0")
    suspend fun getFullItemsByType(
        vaultId: VaultId,
        type: ItemType?,
    ): List<FullItemPojo>

    // --- Core Item Operations ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemMetadata(item: ItemEntity)

    @Update
    suspend fun updateItemMetadata(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemMetadata(itemId: ItemId)

    // --- Tag Relationships ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemTagCrossRefs(refs: List<ItemTagCrossRef>)

    @Query("DELETE FROM item_tag_cross_ref WHERE item_id = :itemId")
    suspend fun deleteItemTagCrossRefs(itemId: ItemId)

    // --- Type-Specific Insertions ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecureNote(note: SecureNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentityDocument(doc: IdentityDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoftwareLicense(license: SoftwareLicenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSshKey(sshKey: SshKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecoveryCode(recoveryCode: RecoveryCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecoveryCodeValues(values: List<RecoveryCodeValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTotpSeed(totp: TotpSeedEntity)

    // --- Type-Specific Deletions ---

    @Query("DELETE FROM password_history WHERE item_id = :itemId")
    suspend fun deletePasswordHistory(itemId: ItemId)

    @Query("DELETE FROM recovery_code_values WHERE recovery_code_item_id = :itemId")
    suspend fun deleteRecoveryCodeValues(itemId: ItemId)

    // --- Password History ---

    @Transaction
    @Query(
        "SELECT * FROM items WHERE vault_id = :vaultId AND is_deleted = 0 AND is_archived = 0 " +
            "ORDER BY last_accessed_at DESC LIMIT :limit",
    )
    fun observeRecentItems(
        vaultId: VaultId,
        limit: Int = 10,
    ): Flow<List<ItemSummaryPojo>>

    @Transaction
    @Query(
        "SELECT * FROM items WHERE vault_id = :vaultId AND is_deleted = 0 AND is_archived = 0 " +
            "ORDER BY use_count DESC LIMIT :limit",
    )
    fun observeFrequentlyUsedItems(
        vaultId: VaultId,
        limit: Int = 10,
    ): Flow<List<ItemSummaryPojo>>

    @Transaction
    @Query("SELECT * FROM items WHERE vault_id = :vaultId AND is_pinned = 1 AND is_deleted = 0")
    fun observePinnedItems(vaultId: VaultId): Flow<List<ItemSummaryPojo>>

    @Query("UPDATE items SET last_accessed_at = :timestamp, use_count = use_count + 1 WHERE id = :itemId")
    suspend fun recordItemAccess(
        itemId: ItemId,
        timestamp: java.time.Instant,
    )

    // --- Bulk Operations ---

    @Query("UPDATE items SET is_deleted = 1, deleted_at = :timestamp WHERE id IN (:itemIds)")
    suspend fun bulkMoveToTrash(
        itemIds: List<ItemId>,
        timestamp: java.time.Instant,
    )

    @Query("UPDATE items SET is_favorite = :isFavorite WHERE id IN (:itemIds)")
    suspend fun bulkSetFavorite(
        itemIds: List<ItemId>,
        isFavorite: Boolean,
    )

    @Query("UPDATE items SET category_id = :categoryId WHERE id IN (:itemIds)")
    suspend fun bulkMoveToCategory(
        itemIds: List<ItemId>,
        categoryId: com.securevault.next.core.domain.model.valueobject.CategoryId?,
    )

    // --- Password History ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordHistoryEntry(entry: PasswordHistoryEntity)

    @Query("SELECT * FROM password_history WHERE item_id = :itemId ORDER BY changed_at DESC")
    suspend fun getPasswordHistory(itemId: ItemId): List<PasswordHistoryEntity>

    // --- Encrypted Notes (C5) ---

    /**
     * Lazy-migration write for C5: stores the encrypted notes produced
     * from a legacy plaintext row and clears the plaintext column in the
     * same statement. Targeted UPDATE (rather than a full @Update) so a
     * concurrent modification of other columns cannot be clobbered.
     */
    @Query(
        """
        UPDATE items
        SET notes_ciphertext = :ciphertext,
            notes_nonce = :nonce,
            notes_algorithm = :algorithm,
            notes = NULL
        WHERE id = :itemId
        """,
    )
    suspend fun migrateNotesToEncrypted(
        itemId: ItemId,
        ciphertext: String,
        nonce: String,
        algorithm: String,
    )
}
