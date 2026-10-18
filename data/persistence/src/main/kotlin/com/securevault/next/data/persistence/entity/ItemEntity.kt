package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists the common metadata for every [com.securevault.next.core.domain.model.item.VaultItem].
 *
 * This table serves as the central registry for all items, regardless of
 * their concrete type. Type-specific sensitive data (e.g. the password
 * itself, card numbers) is stored in specialized tables linked to this
 * one via [itemId] as a foreign key.
 *
 * This split allows for highly optimized listing and searching across
 * all item types without loading the opaque encrypted blobs into memory
 * until a specific item is requested for detail view.
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vault_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("vault_id"),
        Index("category_id"),
        Index("item_type"),
        Index("is_favorite"),
        Index("is_deleted"),
    ],
)
data class ItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: ItemId,
    @ColumnInfo(name = "vault_id")
    val vaultId: VaultId,
    @ColumnInfo(name = "item_type")
    val type: ItemType,
    val title: String,
    @ColumnInfo(name = "category_id")
    val categoryId: CategoryId? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    /**
     * C5: item notes encrypted at rest, stored exactly like every other
     * sensitive field ([EncryptedFieldEntity] flattened with the `notes_`
     * prefix → `notes_ciphertext` / `notes_nonce` / `notes_algorithm`).
     *
     * Writers must always populate this column and leave [notes] NULL.
     * [notes] is kept only so rows written before v3 can be lazily
     * migrated on read (see `ItemMapper.resolveNotes`); it must never
     * receive new plaintext.
     */
    @Embedded(prefix = "notes_")
    val notesEncrypted: EncryptedFieldEntity? = null,
    /**
     * Serialized list of [com.securevault.next.core.domain.model.CustomField]
     * JSON. Kept in the metadata table because custom fields are
     * metadata-shaped and often displayed in preview/listing contexts.
     */
    @ColumnInfo(name = "custom_fields_json")
    val customFieldsJson: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    val revision: Int,
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Instant? = null,
    @ColumnInfo(name = "use_count", defaultValue = "0")
    val useCount: Int = 0,
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
    // Soft-deletion / Trash support
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Instant? = null,
)
