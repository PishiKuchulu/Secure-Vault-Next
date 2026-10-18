package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.CreditCard].
 */
@Entity(
    tableName = "credit_cards",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CreditCardEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    @ColumnInfo(name = "cardholder_name")
    val cardholderName: String,
    @Embedded(prefix = "card_number_")
    val cardNumber: EncryptedFieldEntity,
    @ColumnInfo(name = "expiry_month")
    val expiryMonth: Int,
    @ColumnInfo(name = "expiry_year")
    val expiryYear: Int,
    @Embedded(prefix = "cvv_")
    val cvv: EncryptedFieldEntity,
    @Embedded(prefix = "pin_")
    val pin: EncryptedFieldEntity? = null
)
