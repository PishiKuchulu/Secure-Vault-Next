package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.PasswordId
import java.time.Instant

/**
 * Persists a historical password value for a [com.securevault.next.core.domain.model.item.PasswordItem].
 */
@Entity(
    tableName = "password_history",
    foreignKeys = [
        ForeignKey(
            entity = PasswordEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("item_id")
    ]
)
data class PasswordHistoryEntity(
    @PrimaryKey
    val id: PasswordId,

    @ColumnInfo(name = "item_id")
    val itemId: ItemId,

    @Embedded(prefix = "password_")
    val password: EncryptedFieldEntity,

    @ColumnInfo(name = "changed_at")
    val changedAt: Instant,
)
