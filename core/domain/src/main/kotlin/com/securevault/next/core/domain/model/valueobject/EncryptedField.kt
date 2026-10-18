package com.securevault.next.core.domain.model.valueobject

import kotlinx.serialization.Serializable

/**
 * The at-rest representation of a sensitive value (a password, a card
 * number, a private key).
 *
 * `core:domain` never performs cryptography (Part 2: Domain Layer rules)
 * and therefore never knows how to turn an [EncryptedField] back into
 * plaintext — it only ever carries the opaque ciphertext, nonce and
 * algorithm identifier produced by `core:crypto` (Milestone 3) as an
 * inert value. Decryption happens entirely outside this module; domain
 * models simply hold the encrypted payload as data in transit.
 *
 * [ciphertext] and [nonce] are stored Base64-encoded so this type stays
 * trivially serializable without depending on any particular encoding
 * library.
 *
 * Carries [Serializable] because it is nested inside [CustomField],
 * which `data:repository` persists as JSON via kotlinx.serialization.
 */
@Serializable
data class EncryptedField(val ciphertext: String, val nonce: String, val algorithm: String = DEFAULT_ALGORITHM) {
    init {
        require(ciphertext.isNotBlank()) { "EncryptedField.ciphertext must not be blank" }
        require(nonce.isNotBlank()) { "EncryptedField.nonce must not be blank" }
        require(algorithm.isNotBlank()) { "EncryptedField.algorithm must not be blank" }
    }

    companion object {
        const val DEFAULT_ALGORITHM = "AES-256-GCM"
    }
}
