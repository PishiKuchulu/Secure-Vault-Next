package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.RecoveryCode].
 *
 * Unlike other item types, [RecoveryCode] stores a list of encrypted
 * codes. This is handled by storing the codes in a separate table
 * [RecoveryCodeValueEntity] to maintain normalization and avoid
 * large blob serialization issues.
 */
@Entity(
    tableName = "recovery_codes",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecoveryCodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    val service: String,
    /** CSV of indices that have been used. */
    @ColumnInfo(name = "used_indices_csv")
    val usedIndicesCsv: String
)
