package com.securevault.next.core.crypto.vault

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.crypto.cipher.AesGcmCipherEngine
import com.securevault.next.core.crypto.key.SessionKey
import com.securevault.next.core.crypto.key.SessionKeyHolder
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DefaultItemEncryptionEngineTest {
    private val holder = SessionKeyHolder()
    private val engine =
        DefaultItemEncryptionEngine(
            sessionKeyHolder = holder,
            cipherEngine = AesGcmCipherEngine(SystemSecureRandomProvider())
        )

    private fun installSessionKey() {
        holder.set(SessionKey(SystemSecureRandomProvider().nextBytes(32)))
    }

    private fun EncryptedField.roundTrip(): CharArray {
        val result = runBlocking { engine.decryptField(this@roundTrip) }
        check(result is Result.Success) { "Expected decryption success but was $result" }
        return result.value
    }

    @Test
    fun `round-trip encryption and decryption returns the original plaintext`() {
        installSessionKey()
        val plaintext = "correct horse battery staple".toCharArray()

        val encrypted = runBlocking { engine.encryptField(plaintext) } as Result.Success
        val decrypted = encrypted.value.roundTrip()

        assertThat(String(decrypted)).isEqualTo("correct horse battery staple")
    }

    @Test
    fun `identical plaintext encrypted twice produces different ciphertext and nonce`() {
        installSessionKey()
        val plaintext = "same secret".toCharArray()

        val first = runBlocking { engine.encryptField(plaintext) } as Result.Success
        val second = runBlocking { engine.encryptField(plaintext) } as Result.Success

        assertThat(first.value.ciphertext).isNotEqualTo(second.value.ciphertext)
        assertThat(first.value.nonce).isNotEqualTo(second.value.nonce)
    }

    @Test
    fun `empty string round-trips`() {
        installSessionKey()

        val encrypted = runBlocking { engine.encryptField(charArrayOf()) } as Result.Success
        val decrypted = encrypted.value.roundTrip()

        assertThat(decrypted).isEmpty()
    }

    @Test
    fun `unicode text round-trips without corruption`() {
        installSessionKey()
        val plaintext = "münchen 東京 🔐 ünïcödé".toCharArray()

        val encrypted = runBlocking { engine.encryptField(plaintext) } as Result.Success
        val decrypted = encrypted.value.roundTrip()

        assertThat(String(decrypted)).isEqualTo("münchen 東京 🔐 ünïcödé")
    }

    @Test
    fun `large payload round-trips`() {
        installSessionKey()
        val plaintext = CharArray(64 * 1024) { ('a'.code + (it % 26)).toChar() }

        val encrypted = runBlocking { engine.encryptField(plaintext) } as Result.Success
        val decrypted = encrypted.value.roundTrip()

        assertThat(String(decrypted)).isEqualTo(String(plaintext))
    }

    @Test
    fun `encrypting with no session key installed fails with KeyDerivationRequired`() {
        holder.clear()
        val result = runBlocking { engine.encryptField("secret".toCharArray()) }

        assertThat(result is Result.Failure).isTrue()
        assertThat((result as Result.Failure).error)
            .isInstanceOf(DomainError.CryptoRequiredError.KeyDerivationRequired::class.java)
    }

    @Test
    fun `decrypting with no session key installed fails with KeyDerivationRequired`() {
        holder.clear()
        val result = runBlocking { engine.decryptField(EncryptedField(ciphertext = "AAAA", nonce = "AAAA")) }

        assertThat(result is Result.Failure).isTrue()
        assertThat((result as Result.Failure).error)
            .isInstanceOf(DomainError.CryptoRequiredError.KeyDerivationRequired::class.java)
    }

    @Test
    fun `decrypting with a different session key fails`() {
        installSessionKey()
        val encrypted = runBlocking { engine.encryptField("a secret".toCharArray()) } as Result.Success

        // Install a different session key, then attempt to decrypt the old field.
        holder.set(SessionKey(SystemSecureRandomProvider().nextBytes(32)))
        val result = runBlocking { engine.decryptField(encrypted.value) }

        assertThat(result is Result.Failure).isTrue()
        assertThat((result as Result.Failure).error)
            .isInstanceOf(DomainError.CryptoRequiredError.DecryptionFailed::class.java)
    }
}
