package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.item.VaultItem

/**
 * A fully loaded vault: its metadata, every item, category and tag it
 * contains, its settings and security policy, and a computed statistics
 * snapshot.
 *
 * This can only meaningfully exist while a vault is
 * [VaultState.Unlocked] — items are held here as already-decrypted
 * [VaultItem] instances with their [com.securevault.next.core.domain.model.valueobject.EncryptedField]
 * payloads still opaque (decryption of the *field values themselves*
 * happens on demand, one field at a time, not by loading the whole
 * vault plaintext into memory at once). See [VaultMetadata] for the
 * subset of this data that is available before unlock.
 */
data class Vault(
    val metadata: VaultMetadata,
    val items: List<VaultItem>,
    val categories: List<Category>,
    val tags: List<Tag>,
    val settings: VaultSettings,
    val securityPolicy: VaultSecurityPolicy,
    val statistics: VaultStatistics
)
