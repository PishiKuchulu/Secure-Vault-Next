package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.ApiKey].
 */
@Entity(
    tableName = "api_keys",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ApiKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,

    val service: String,

    @Embedded(prefix = "key_value_")
    val keyValue: EncryptedFieldEntity,

    @Embedded(prefix = "key_secret_")
    val keySecret: EncryptedFieldEntity? = null,

    val environment: String? = null,
)
