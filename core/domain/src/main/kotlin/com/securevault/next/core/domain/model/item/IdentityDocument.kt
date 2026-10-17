package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import java.time.Instant

/**
 * A government-issued identity document (passport, driver's license,
 * national ID, ...). [documentType] is a free-text field rather than an
 * enum: the set of document types varies by country and issuing
 * authority in ways that would make a closed enum either incomplete or
 * constantly churning.
 */
data class IdentityDocument(
    override val metadata: VaultItemMetadata,
    val documentType: String,
    val fullName: String,
    val documentNumber: EncryptedField,
    val issueDate: Instant? = null,
    val expiryDate: Instant? = null,
    val issuingCountry: String? = null,
) : VaultItem {
    override val type: ItemType get() = ItemType.IDENTITY_DOCUMENT

    init {
        require(documentType.isNotBlank()) { "IdentityDocument.documentType must not be blank" }
        require(fullName.isNotBlank()) { "IdentityDocument.fullName must not be blank" }
    }
}
