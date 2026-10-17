package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.UrlValue

/**
 * A website or application login: username, encrypted password, and
 * optionally a login URL and an attached [TOTPConfig] for two-factor
 * authentication.
 */
data class PasswordItem(
    override val metadata: VaultItemMetadata,
    val username: String? = null,
    val password: EncryptedField,
    val url: UrlValue? = null,
    val totp: TOTPConfig? = null,
) : VaultItem {
    override val type: ItemType get() = ItemType.PASSWORD
}
