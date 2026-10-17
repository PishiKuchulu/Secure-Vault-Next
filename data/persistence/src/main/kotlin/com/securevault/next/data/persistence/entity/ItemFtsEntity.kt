package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts5
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * An FTS5 virtual table for high-performance search across vault items.
 *
 * [itemId] is the join key to [ItemEntity.id]. FTS5 rowids are integers
 * and can never hold the TEXT item ids, so the search queries join on
 * `items_fts.item_id` instead of `rowid`.
 *
 * This table is kept in sync with the `items` table by the triggers in
 * `SecureVaultDatabase.FTS_SYNC_CALLBACK` (C6): every INSERT / UPDATE /
 * DELETE on `items` is mirrored here. Only already-plaintext metadata is
 * indexed — the item title. Item notes are encrypted at rest and the
 * triggers deliberately insert NULL for [notes], so decrypted secrets
 * never enter the search index. The [notes] column is retained for
 * schema compatibility only.
 *
 * Using FTS5 allows for extremely fast full-text queries, prefix
 * matching, and ranking, even in vaults with 50,000+ items.
 */
@Entity(tableName = "items_fts")
@Fts5
data class ItemFtsEntity(
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "notes")
    val notes: String?,
)
