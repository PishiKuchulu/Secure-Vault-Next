package com.securevault.next.core.crypto.random

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SystemSecureRandomProviderTest {

    private val provider = SystemSecureRandomProvider()

    @Test
    fun `nextBytes returns the requested number of bytes`() {
        val bytes = provider.nextBytes(32)

        assertThat(bytes.size).isEqualTo(32)
    }

    @Test
    fun `consecutive calls produce different output`() {
        val first = provider.nextBytes(32).peek { it.copyOf() }
        val second = provider.nextBytes(32).peek { it.copyOf() }

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `nonces are never reused across many calls`() {
        val seen = HashSet<String>()
        repeat(200) {
            val nonce = provider.nextNonce(12).peek { it.joinToString(",") }
            assertThat(seen.add(nonce)).isTrue()
        }
    }

    @Test
    fun `rejects a non-positive size`() {
        var threw = false
        try {
            provider.nextBytes(0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }

    @Test
    fun `output is not all zero bytes`() {
        // Not a rigorous randomness test — just a sanity check that the
        // provider isn't accidentally returning an unfilled buffer.
        val bytes = provider.nextBytes(32).peek { it.copyOf() }

        assertThat(bytes.all { it == 0.toByte() }).isFalse()
    }
}
