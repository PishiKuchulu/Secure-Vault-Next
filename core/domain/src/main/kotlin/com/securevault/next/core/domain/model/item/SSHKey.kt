package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/**
 * An SSH key pair. [publicKey] is stored in plaintext — a public key is
 * not sensitive by definition — while [privateKey] and an optional
 * [passphrase] are always encrypted.
 */
data class SSHKey(
    override val metadata: VaultItemMetadata,
    val keyType: String,
    val privateKey: EncryptedField,
    val publicKey: String? = null,
    val passphrase: EncryptedField? = null,
    val hostAddress: String? = null
) : VaultItem {
    override val type: ItemType get() = ItemType.SSH_KEY

    init {
        require(keyType.isNotBlank()) { "SSHKey.keyType must not be blank" }
    }
}
