package com.securevault.next.core.crypto.key

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.crypto.memory.SecureBytes
import org.junit.Test

class SessionKeyHolderStoreTest {
    @Test
    fun `install makes the store active and readable through the holder`() {
        val holder = SessionKeyHolder()
        val store = SessionKeyHolderStore(holder)
        val key = ByteArray(32) { it.toByte() }

        store.install(key)

        assertThat(store.isActive).isTrue()
        assertThat(holder.isActive).isTrue()
        // Read a defensive copy; do NOT wipe the key the holder still owns.
        val held = holder.get()!!.bytes.peek { it.copyOf() }
        assertThat(held).isEqualTo(key)
        store.destroy()
    }

    @Test
    fun `destroy wipes the held key and deactivates`() {
        val holder = SessionKeyHolder()
        val store = SessionKeyHolderStore(holder)
        store.install(ByteArray(32) { it.toByte() })

        store.destroy()

        assertThat(store.isActive).isFalse()
        assertThat(holder.isActive).isFalse()
        assertThat(holder.get()).isNull()
    }

    @Test
    fun `reinstall destroys the previously held key`() {
        val holder = SessionKeyHolder()
        val store = SessionKeyHolderStore(holder)
        val first = SecureBytes.wrap(ByteArray(32) { it.toByte() })
        holder.set(SessionKey(first))

        store.install(ByteArray(32) { 7 })

        assertThat(first.isWiped).isTrue()
        assertThat(store.isActive).isTrue()
    }

    @Test
    fun `destroy on an empty store is safe and inactive`() {
        val store = SessionKeyHolderStore(SessionKeyHolder())

        store.destroy()

        assertThat(store.isActive).isFalse()
    }
}
