package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId
import java.time.Instant

/**
 * Persists the sensitive details of a [com.securevault.next.core.domain.model.item.IdentityDocument].
 */
@Entity(
    tableName = "identity_documents",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IdentityDocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    @ColumnInfo(name = "document_type")
    val documentType: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @Embedded(prefix = "document_number_")
    val documentNumber: EncryptedFieldEntity,
    @ColumnInfo(name = "issue_date")
    val issueDate: Instant? = null,
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Instant? = null,
    @ColumnInfo(name = "issuing_country")
    val issuingCountry: String? = null,
)
