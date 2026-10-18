package com.securevault.next.core.domain.model.valueobject

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValueObjectInvariantsTest {
    @Test
    fun `VaultId rejects a blank value`() {
        assertThrows { VaultId("") }
    }

    @Test
    fun `VaultId accepts a non-blank value`() {
        assertThat(VaultId("vault-1").value).isEqualTo("vault-1")
    }

    @Test
    fun `VaultName rejects a blank value`() {
        assertThrows { VaultName("   ") }
    }

    @Test
    fun `VaultName rejects a value over the max length`() {
        assertThrows { VaultName("x".repeat(VaultName.MAX_LENGTH + 1)) }
    }

    @Test
    fun `SecurityScore rejects values outside 0 to 100`() {
        assertThrows { SecurityScore(-1) }
        assertThrows { SecurityScore(101) }
    }

    @Test
    fun `SecurityScore accepts boundary values`() {
        assertThat(SecurityScore(0).value).isEqualTo(0)
        assertThat(SecurityScore(100).value).isEqualTo(100)
    }

    @Test
    fun `Revision starts at zero and increments`() {
        val initial = Revision.INITIAL

        assertThat(initial.value).isEqualTo(0)
        assertThat(initial.next().value).isEqualTo(1)
    }

    @Test
    fun `Revision rejects a negative value`() {
        assertThrows { Revision(-1) }
    }

    @Test
    fun `EncryptedField rejects a blank ciphertext or nonce`() {
        assertThrows { EncryptedField(ciphertext = "", nonce = "nonce") }
        assertThrows { EncryptedField(ciphertext = "cipher", nonce = "") }
    }

    @Test
    fun `EncryptedField defaults to AES-256-GCM`() {
        val field = EncryptedField(ciphertext = "cipher", nonce = "nonce")

        assertThat(field.algorithm).isEqualTo("AES-256-GCM")
    }

    @Test
    fun `EmailAddress requires an at sign`() {
        assertThrows { EmailAddress("not-an-email") }
        assertThat(EmailAddress("a@b.com").value).isEqualTo("a@b.com")
    }

    private inline fun assertThrows(block: () -> Unit) {
        var threw = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }
}
