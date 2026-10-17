package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.BankAccount].
 */
@Entity(
    tableName = "bank_accounts",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class BankAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "account_holder_name")
    val accountHolderName: String,

    @Embedded(prefix = "account_number_")
    val accountNumber: EncryptedFieldEntity,

    @Embedded(prefix = "routing_number_")
    val routingNumber: EncryptedFieldEntity? = null,

    @Embedded(prefix = "iban_")
    val iban: EncryptedFieldEntity? = null,

    @ColumnInfo(name = "swift_code")
    val swiftCode: String? = null,
)
