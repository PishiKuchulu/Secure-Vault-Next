package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.enums.VaultStatus
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.Revision
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.model.valueobject.Version

/**
 * Descriptive information about a vault that is meaningful *without*
 * decrypting it — its name, when it was created, its schema version.
 *
 * This is deliberately what a lock screen shows (Part 9: "Vault Locked
 * Screen" needs to display which vault the user is unlocking before
 * authentication succeeds) and what a vault-switcher list is built from,
 * as opposed to [Vault] itself, which represents the fully decrypted
 * aggregate and can only exist once a session is unlocked.
 */
data class VaultMetadata(
    val id: VaultId,
    val name: VaultName,
    val status: VaultStatus = VaultStatus.ACTIVE,
    val version: Version,
    val revision: Revision = Revision.INITIAL,
    val createdAt: CreatedAt,
    val updatedAt: UpdatedAt
)
