package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.pojo.ItemSummaryPojo

/**
 * Data access object for high-performance search using FTS4.
 *
 * The `items_fts` table is kept in sync with `items` by the triggers in
 * `SecureVaultDatabase.FTS_SYNC_CALLBACK` (C6) — this DAO only reads.
 * Only item titles are indexed; notes are encrypted at rest and never
 * enter the search index.
 */
@Dao
interface SearchDao {
    @Transaction
    @Query(
        """
        SELECT items.*
        FROM items
        JOIN items_fts ON items.id = items_fts.item_id
        WHERE items.vault_id = :vaultId
          AND items.is_deleted = 0
          AND items_fts MATCH :query
          AND (:favoritesOnly = 0 OR items.is_favorite = 1)
        ORDER BY items.title ASC
    """
    )
    suspend fun searchItems(vaultId: VaultId, query: String, favoritesOnly: Boolean = false): List<ItemSummaryPojo>

    @Transaction
    @Query(
        """
        SELECT items.*
        FROM items
        JOIN items_fts ON items.id = items_fts.item_id
        WHERE items.vault_id = :vaultId
          AND items.is_deleted = 0
          AND items_fts MATCH :query
          AND (:favoritesOnly = 0 OR items.is_favorite = 1)
        ORDER BY items.updated_at DESC
    """
    )
    suspend fun searchItemsSortedByDate(
        vaultId: VaultId,
        query: String,
        favoritesOnly: Boolean = false
    ): List<ItemSummaryPojo>

    /**
     * Rebuilds the FTS index from the source table. Useful for
     * maintenance or data recovery. Call [clearFtsIndex] then
     * [populateFtsIndex] inside a single transaction.
     */
    @Query("DELETE FROM items_fts")
    suspend fun clearFtsIndex()

    /**
     * Re-indexes every item title. See [clearFtsIndex].
     */
    @Query("INSERT INTO items_fts(item_id, title, notes) SELECT id, title, NULL FROM items")
    suspend fun populateFtsIndex()
}
