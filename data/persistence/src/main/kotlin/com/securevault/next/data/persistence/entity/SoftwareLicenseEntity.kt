package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId
import java.time.Instant

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.SoftwareLicense].
 */
@Entity(
    tableName = "software_licenses",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class SoftwareLicenseEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @Embedded(prefix = "license_key_")
    val licenseKey: EncryptedFieldEntity,

    @ColumnInfo(name = "licensed_to")
    val licensedTo: String? = null,

    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Instant? = null,

    @ColumnInfo(name = "expiry_date")
    val expiryDate: Instant? = null,

    val vendor: String? = null,
)
