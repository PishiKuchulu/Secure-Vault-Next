package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.TagId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists a user-defined tag for labeling vault items.
 */
@Entity(
    tableName = "tags",
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
data class TagEntity(
    @PrimaryKey
    val id: TagId,
    @ColumnInfo(name = "vault_id")
    val vaultId: VaultId,
    val name: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
