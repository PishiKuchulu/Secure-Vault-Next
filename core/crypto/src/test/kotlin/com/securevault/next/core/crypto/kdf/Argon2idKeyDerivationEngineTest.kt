package com.securevault.next.core.crypto.kdf

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import org.junit.Test

class Argon2idKeyDerivationEngineTest {
    private val engine = Argon2idKeyDerivationEngine()

    /**
     * Real Argon2id at OWASP-floor cost (19 MiB, 1 iteration) so the test
     * suite runs in reasonable time — never mocked, never a fake weaker
     * "test algorithm"; every test in this file exercises the actual
     * Bouncy Castle Argon2id implementation.
     */
    private val fastTestParameters =
        KdfParameters(
            memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
            iterations = 1,
            parallelism = 1,
            outputLengthBytes = 64
        )

    private fun bytesOf(text: String) = SecureBytes.wrap(text.toByteArray(Charsets.UTF_8))

    private fun <T> CryptoResult<T>.successValue(): T {
        check(this is com.securevault.next.core.common.result.Result.Success) { "Expected success but was $this" }
        return value
    }

    @Test
    fun `deriving twice with the same password, salt and parameters produces identical output`() {
        val password = { bytesOf("correct horse battery staple") }
        val salt = { bytesOf("fixed-salt-16by") }

        val first = engine.derive(password(), salt(), fastTestParameters).successValue().peek { it.copyOf() }
        val second = engine.derive(password(), salt(), fastTestParameters).successValue().peek { it.copyOf() }

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `different passwords produce different derived output`() {
        val a =
            engine
                .derive(bytesOf("password-one"), bytesOf("fixed-salt-16by"), fastTestParameters)
                .successValue()
                .peek { it.copyOf() }
        val b =
            engine
                .derive(bytesOf("password-two"), bytesOf("fixed-salt-16by"), fastTestParameters)
                .successValue()
                .peek { it.copyOf() }

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `different salts produce different derived output for the same password`() {
        val a =
            engine
                .derive(bytesOf("same-password"), bytesOf("salt-number-one!"), fastTestParameters)
                .successValue()
                .peek { it.copyOf() }
        val b =
            engine
                .derive(bytesOf("same-password"), bytesOf("salt-number-two!"), fastTestParameters)
                .successValue()
                .peek { it.copyOf() }

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `output length matches the requested parameter`() {
        val output =
            engine
                .derive(
                    bytesOf("password"),
                    bytesOf("fixed-salt-16by"),
                    fastTestParameters.copy(outputLengthBytes = 96)
                ).successValue()

        assertThat(output.size).isEqualTo(96)
    }

    @Test
    fun `upgrading KDF parameters changes the derived output`() {
        val weaker = fastTestParameters
        val stronger = fastTestParameters.copy(iterations = 2)

        val a =
            engine
                .derive(bytesOf("same-password"), bytesOf("fixed-salt-16by"), weaker)
                .successValue()
                .peek { it.copyOf() }
        val b =
            engine
                .derive(bytesOf("same-password"), bytesOf("fixed-salt-16by"), stronger)
                .successValue()
                .peek { it.copyOf() }

        assertThat(a).isNotEqualTo(b)
    }
}
