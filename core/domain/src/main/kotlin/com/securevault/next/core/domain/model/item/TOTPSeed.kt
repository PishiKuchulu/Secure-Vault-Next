package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType

/**
 * A standalone TOTP secret — for a two-factor entry with no associated
 * password (e.g. a hardware security key's companion app entry, or a
 * service where the password lives elsewhere). See [PasswordItem.totp]
 * for the more common case of a TOTP config attached to a password.
 */
data class TOTPSeed(override val metadata: VaultItemMetadata, val config: TOTPConfig) : VaultItem {
    override val type: ItemType get() = ItemType.TOTP_SEED
}
