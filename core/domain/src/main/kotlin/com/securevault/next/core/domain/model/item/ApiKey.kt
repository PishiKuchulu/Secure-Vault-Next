package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/** A developer-facing API key or key/secret pair for a third-party service. */
data class ApiKey(
    override val metadata: VaultItemMetadata,
    val service: String,
    val keyValue: EncryptedField,
    val keySecret: EncryptedField? = null,
    val environment: String? = null,
) : VaultItem {
    override val type: ItemType get() = ItemType.API_KEY

    init {
        require(service.isNotBlank()) { "ApiKey.service must not be blank" }
    }
}
