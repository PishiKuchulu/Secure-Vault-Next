package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists a user-defined category for organizing vault items.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vault_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("vault_id"),
    ],
)
data class CategoryEntity(
    @PrimaryKey
    val id: CategoryId,
    @ColumnInfo(name = "vault_id")
    val vaultId: VaultId,
    val name: String,
    @ColumnInfo(name = "icon_alias")
    val iconAlias: String? = null,
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null,
    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: CategoryId? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
