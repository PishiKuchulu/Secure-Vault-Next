package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.PasswordItem].
 */
@Entity(
    tableName = "passwords",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PasswordEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    val username: String? = null,
    @Embedded(prefix = "password_")
    val password: EncryptedFieldEntity,
    val url: String? = null,
    // TOTP Config (Flattened)
    @Embedded(prefix = "totp_")
    val totp: TotpConfigEntity? = null,
)

/** Opaque encrypted field representation for Room. */
data class EncryptedFieldEntity(
    val ciphertext: String,
    val nonce: String,
    val algorithm: String,
)

/** TOTP configuration details. */
data class TotpConfigEntity(
    @Embedded(prefix = "secret_")
    val secret: EncryptedFieldEntity,
    val issuer: String? = null,
    val accountName: String? = null,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
)
