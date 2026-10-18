package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.SSHKey].
 */
@Entity(
    tableName = "ssh_keys",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SshKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    @ColumnInfo(name = "key_type")
    val keyType: String,
    @Embedded(prefix = "private_key_")
    val privateKey: EncryptedFieldEntity,
    @ColumnInfo(name = "public_key")
    val publicKey: String? = null,
    @Embedded(prefix = "passphrase_")
    val passphrase: EncryptedFieldEntity? = null,
    @ColumnInfo(name = "host_address")
    val hostAddress: String? = null
)
