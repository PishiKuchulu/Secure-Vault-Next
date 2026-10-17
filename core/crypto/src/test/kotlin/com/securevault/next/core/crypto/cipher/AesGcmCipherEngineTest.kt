package com.securevault.next.core.crypto.cipher

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.isFailure
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import org.junit.Test
import java.util.Base64

class AesGcmCipherEngineTest {

    private val engine = AesGcmCipherEngine(SystemSecureRandomProvider())

    private fun randomKey(): SecureBytes = SystemSecureRandomProvider().nextBytes(32)

    private fun <T> CryptoResult<T>.successValue(): T {
        check(this is Result.Success) { "Expected success but was $this" }
        return value
    }

    private fun <T> CryptoResult<T>.failureError(): CryptoError {
        check(this is Result.Failure) { "Expected failure but was $this" }
        return error
    }

    @Test
    fun `encrypting then decrypting returns the original plaintext`() {
        val key = randomKey()
        val plaintext = "the quick brown fox".toByteArray()

        val encrypted = engine.encrypt(SecureBytes.wrap(plaintext.copyOf()), key.copy()).successValue()
        val decrypted = engine.decrypt(encrypted, key.copy()).successValue()

        assertThat(decrypted.peek { it.toString(Charsets.UTF_8) }).isEqualTo("the quick brown fox")
    }

    @Test
    fun `each encryption uses a unique nonce, even for identical plaintext and key`() {
        val key = randomKey()
        val plaintext = "same plaintext every time"

        val first = engine.encrypt(SecureBytes.wrap(plaintext.toByteArray()), key.copy()).successValue()
        val second = engine.encrypt(SecureBytes.wrap(plaintext.toByteArray()), key.copy()).successValue()

        assertThat(first.nonce).isNotEqualTo(second.nonce)
        // A different nonce with the same key and plaintext must also produce different ciphertext.
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
    }

    @Test
    fun `decrypting with the wrong key fails as InvalidCiphertext`() {
        val correctKey = randomKey()
        val wrongKey = randomKey()

        val encrypted = engine.encrypt(SecureBytes.wrap("secret".toByteArray()), correctKey).successValue()
        val result = engine.decrypt(encrypted, wrongKey)

        assertThat(result.failureError()).isInstanceOf(CryptoError.InvalidCiphertext::class.java)
    }

    @Test
    fun `decrypting tampered ciphertext fails as InvalidCiphertext`() {
        val key = randomKey()
        val encrypted = engine.encrypt(SecureBytes.wrap("secret value".toByteArray()), key.copy()).successValue()

        val tamperedBytes = Base64.getDecoder().decode(encrypted.ciphertext)
        tamperedBytes[0] = (tamperedBytes[0].toInt() xor 0xFF).toByte()
        val tampered = encrypted.copy(ciphertext = Base64.getEncoder().encodeToString(tamperedBytes))

        val result = engine.decrypt(tampered, key)

        assertThat(result.failureError()).isInstanceOf(CryptoError.InvalidCiphertext::class.java)
    }

    @Test
    fun `decrypting with a tampered nonce fails as InvalidCiphertext`() {
        val key = randomKey()
        val encrypted = engine.encrypt(SecureBytes.wrap("secret value".toByteArray()), key.copy()).successValue()

        val tamperedNonceBytes = Base64.getDecoder().decode(encrypted.nonce)
        tamperedNonceBytes[0] = (tamperedNonceBytes[0].toInt() xor 0xFF).toByte()
        val tampered = encrypted.copy(nonce = Base64.getEncoder().encodeToString(tamperedNonceBytes))

        val result = engine.decrypt(tampered, key)

        assertThat(result.failureError()).isInstanceOf(CryptoError.InvalidCiphertext::class.java)
    }

    @Test
    fun `associated data must match at decrypt time or decryption fails`() {
        val key = randomKey()
        val aad = "item-id-123".toByteArray()
        val encrypted = engine.encrypt(SecureBytes.wrap("secret".toByteArray()), key.copy(), aad).successValue()

        val wrongAad = "item-id-456".toByteArray()
        val result = engine.decrypt(encrypted, key, wrongAad)

        assertThat(result.failureError()).isInstanceOf(CryptoError.InvalidCiphertext::class.java)
    }

    @Test
    fun `associated data that matches at decrypt time succeeds`() {
        val key = randomKey()
        val aad = "item-id-123".toByteArray()
        val encrypted = engine.encrypt(SecureBytes.wrap("secret".toByteArray()), key.copy(), aad).successValue()

        val result = engine.decrypt(encrypted, key, aad)

        assertThat(result.successValue().peek { it.toString(Charsets.UTF_8) }).isEqualTo("secret")
    }

    @Test
    fun `rejects a key that is not 32 bytes`() {
        val shortKey = SecureBytes.wrap(ByteArray(16))

        val result = engine.encrypt(SecureBytes.wrap("data".toByteArray()), shortKey)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `rejects an unsupported algorithm on decrypt`() {
        val bogusField = EncryptedField(ciphertext = "AAAA", nonce = "AAAA", algorithm = "DES-ECB")

        val result = engine.decrypt(bogusField, randomKey())

        assertThat(result.isFailure).isTrue()
    }
}
