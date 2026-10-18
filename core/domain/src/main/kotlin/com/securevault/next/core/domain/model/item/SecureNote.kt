package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/** A free-form encrypted note — the simplest item type. */
data class SecureNote(override val metadata: VaultItemMetadata, val content: EncryptedField) : VaultItem {
    override val type: ItemType get() = ItemType.SECURE_NOTE
}
