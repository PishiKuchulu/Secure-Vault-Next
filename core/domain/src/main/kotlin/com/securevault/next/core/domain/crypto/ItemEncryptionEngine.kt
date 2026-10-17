package com.securevault.next.core.domain.crypto

import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.result.DomainResult

/**
 * Interface for encrypting and decrypting individual item fields.
 *
 * This allows the domain and vault layers to perform cryptographic
 * operations without depending on the concrete implementation in
 * `core:crypto`.
 */
interface ItemEncryptionEngine {

    /** Decrypts an [EncryptedField] into a plaintext [CharArray]. */
    suspend fun decryptField(field: EncryptedField): DomainResult<CharArray>

    /** Encrypts a plaintext [CharArray] into an [EncryptedField]. */
    suspend fun encryptField(plaintext: CharArray): DomainResult<EncryptedField>
}
