package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import java.time.Instant

/** A purchased software license key. */
data class SoftwareLicense(
    override val metadata: VaultItemMetadata,
    val productName: String,
    val licenseKey: EncryptedField,
    val licensedTo: String? = null,
    val purchaseDate: Instant? = null,
    val expiryDate: Instant? = null,
    val vendor: String? = null,
) : VaultItem {
    override val type: ItemType get() = ItemType.SOFTWARE_LICENSE

    init {
        require(productName.isNotBlank()) { "SoftwareLicense.productName must not be blank" }
    }
}
