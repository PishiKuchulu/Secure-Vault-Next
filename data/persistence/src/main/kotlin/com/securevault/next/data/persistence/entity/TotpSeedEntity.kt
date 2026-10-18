package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.TOTPSeed].
 */
@Entity(
    tableName = "totp_seeds",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TotpSeedEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    @Embedded(prefix = "config_")
    val config: TotpConfigEntity
)
