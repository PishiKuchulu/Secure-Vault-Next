package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/**
 * A set of single-use account recovery codes (e.g. the 10 backup codes
 * many services issue alongside 2FA setup). Each code is encrypted
 * individually — rather than encrypting the whole list as one blob — so
 * that [usedCodeIndices] can track which specific codes have already
 * been consumed without needing to decrypt the others.
 */
data class RecoveryCode(
    override val metadata: VaultItemMetadata,
    val service: String,
    val codes: List<EncryptedField>,
    val usedCodeIndices: Set<Int> = emptySet()
) : VaultItem {
    override val type: ItemType get() = ItemType.RECOVERY_CODE

    init {
        require(service.isNotBlank()) { "RecoveryCode.service must not be blank" }
        require(codes.isNotEmpty()) { "RecoveryCode.codes must not be empty" }
        require(usedCodeIndices.all { it in codes.indices }) {
            "RecoveryCode.usedCodeIndices must only reference valid indices into codes"
        }
    }

    /** How many codes have not yet been used. */
    val remainingCount: Int get() = codes.size - usedCodeIndices.size
}
