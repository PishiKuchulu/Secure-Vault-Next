package com.securevault.next.core.crypto.memory

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecureBytesTest {

    @Test
    fun `peek reads bytes without wiping them`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        val read = bytes.peek { it.copyOf() }

        assertThat(read).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(bytes.isWiped).isFalse()
    }

    @Test
    fun `consume reads bytes and wipes them afterward`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        val read = bytes.consume { it.copyOf() }

        assertThat(read).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(bytes.isWiped).isTrue()
    }

    @Test
    fun `consume wipes even if the block throws`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        var threw = false
        try {
            bytes.consume { error("boom") }
        } catch (_: IllegalStateException) {
            threw = true
        }

        assertThat(threw).isTrue()
        assertThat(bytes.isWiped).isTrue()
    }

    @Test
    fun `close overwrites the backing array with zeros`() {
        val original = byteArrayOf(1, 2, 3, 4)
        val bytes = SecureBytes.wrap(original)

        bytes.close()

        assertThat(original).isEqualTo(byteArrayOf(0, 0, 0, 0))
    }

    @Test
    fun `using bytes after wiping throws`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))
        bytes.close()

        var threw = false
        try {
            bytes.peek { it }
        } catch (_: IllegalStateException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }

    @Test
    fun `close is safe to call multiple times`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        bytes.close()
        bytes.close()

        assertThat(bytes.isWiped).isTrue()
    }

    @Test
    fun `copy produces an independent SecureBytes`() {
        val original = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        val copy = original.copy()
        original.close()

        assertThat(original.isWiped).isTrue()
        assertThat(copy.isWiped).isFalse()
        assertThat(copy.peek { it.copyOf() }).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `use from the standard library also wipes on completion`() {
        val bytes = SecureBytes.wrap(byteArrayOf(1, 2, 3))

        bytes.use { secureBytes ->
            secureBytes.peek { it }
        }

        assertThat(bytes.isWiped).isTrue()
    }
}
