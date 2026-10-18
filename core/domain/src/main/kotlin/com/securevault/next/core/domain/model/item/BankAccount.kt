package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/** A bank account's access details. */
data class BankAccount(
    override val metadata: VaultItemMetadata,
    val bankName: String,
    val accountHolderName: String,
    val accountNumber: EncryptedField,
    val routingNumber: EncryptedField? = null,
    val iban: EncryptedField? = null,
    val swiftCode: String? = null
) : VaultItem {
    override val type: ItemType get() = ItemType.BANK_ACCOUNT

    init {
        require(bankName.isNotBlank()) { "BankAccount.bankName must not be blank" }
        require(accountHolderName.isNotBlank()) { "BankAccount.accountHolderName must not be blank" }
    }
}
