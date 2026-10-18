package com.securevault.next.core.crypto.cipher

import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.domain.model.valueobject.EncryptedField

/**
 * Performs authenticated encryption and decryption, producing and
 * consuming the domain layer's own [EncryptedField] directly — the
 * on-disk/at-rest shape and the crypto-layer output are the same type,
 * so nothing downstream needs to translate between a "crypto result" and
 * a "domain field".
 *
 * [associatedData] is AEAD Associated Authenticated Data (AAD): bytes
 * that are authenticated (tamper-detected) but *not* encrypted and *not*
 * stored in the resulting [EncryptedField]. Binding a field to context
 * that must match at decrypt time — e.g. its owning item's ID — prevents
 * an attacker who can rearrange ciphertexts in storage from swapping one
 * item's encrypted password onto a different item without detection:
 * decryption fails unless the caller supplies the *same* AAD it was
 * encrypted with.
 */
interface CipherEngine {
    fun encrypt(
        plaintext: SecureBytes,
        key: SecureBytes,
        associatedData: ByteArray = ByteArray(0),
    ): CryptoResult<EncryptedField>

    fun decrypt(
        encryptedField: EncryptedField,
        key: SecureBytes,
        associatedData: ByteArray = ByteArray(0),
    ): CryptoResult<SecureBytes>
}
