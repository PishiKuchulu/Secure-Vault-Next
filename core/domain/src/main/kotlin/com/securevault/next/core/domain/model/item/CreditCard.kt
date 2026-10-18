package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/**
 * A payment card. [expiryMonth] and [expiryYear] are stored in plaintext
 * (they are not, by themselves, sufficient to authorize a charge and are
 * routinely printed on the physical card), while [cardNumber], [cvv] and
 * [pin] are always encrypted.
 */
data class CreditCard(
    override val metadata: VaultItemMetadata,
    val cardholderName: String,
    val cardNumber: EncryptedField,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: EncryptedField,
    val pin: EncryptedField? = null
) : VaultItem {
    override val type: ItemType get() = ItemType.CREDIT_CARD

    init {
        require(expiryMonth in 1..12) { "CreditCard.expiryMonth must be between 1 and 12, was $expiryMonth" }
        require(expiryYear >= MIN_EXPIRY_YEAR) {
            "CreditCard.expiryYear must be at least $MIN_EXPIRY_YEAR, was $expiryYear"
        }
    }

    private companion object {
        const val MIN_EXPIRY_YEAR = 1970
    }
}
