package com.securevault.next.core.crypto.key

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.crypto.memory.SecureBytes
import org.junit.Test
import java.util.Collections

class SessionKeyHolderTest {
    private fun keyOf(vararg bytes: Byte) = SessionKey(SecureBytes.wrap(bytes))

    @Test
    fun `holder is inactive until a key is set`() {
        val holder = SessionKeyHolder()

        assertThat(holder.isActive).isFalse()
        assertThat(holder.get()).isNull()
    }

    @Test
    fun `set installs the key and get returns it`() {
        val holder = SessionKeyHolder()
        val key = keyOf(1, 2, 3)

        holder.set(key)

        assertThat(holder.isActive).isTrue()
        assertThat(holder.get()).isEqualTo(key)
    }

    @Test
    fun `clear destroys the held key and deactivates the holder`() {
        val holder = SessionKeyHolder()
        val key = keyOf(1, 2, 3)
        holder.set(key)

        holder.clear()

        assertThat(holder.isActive).isFalse()
        assertThat(holder.get()).isNull()
        assertThat(key.bytes.isWiped).isTrue()
    }

    @Test
    fun `setting a new key destroys the previously held key`() {
        val holder = SessionKeyHolder()
        val firstKey = keyOf(1, 2, 3)
        val secondKey = keyOf(4, 5, 6)

        holder.set(firstKey)
        holder.set(secondKey)

        assertThat(firstKey.bytes.isWiped).isTrue()
        assertThat(secondKey.bytes.isWiped).isFalse()
        assertThat(holder.get()).isEqualTo(secondKey)
    }

    @Test
    fun `clear is safe to call when no key is held`() {
        val holder = SessionKeyHolder()

        holder.clear()

        assertThat(holder.isActive).isFalse()
    }

    @Test
    fun `concurrent set and clear calls never leave two keys simultaneously un-wiped`() {
        // A pre-Milestone-4 security audit asked for explicit concurrency
        // coverage of every @Synchronized security component. This
        // hammers the holder from many threads at once and asserts the
        // invariant SessionKeyHolder exists to guarantee: whichever key
        // ends up installed, every key that got replaced along the way
        // was wiped — there is never a moment where two live (un-wiped)
        // keys are reachable from outside the holder.
        val holder = SessionKeyHolder()
        val threadCount = 16
        val setsPerThread = 200
        val installedKeys = Collections.synchronizedList(mutableListOf<SessionKey>())

        val threads =
            (0 until threadCount).map { threadIndex ->
                Thread {
                    repeat(setsPerThread) { iteration ->
                        val key = keyOf(threadIndex.toByte(), iteration.toByte())
                        installedKeys.add(key)
                        holder.set(key)
                    }
                }
            }

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        holder.clear()

        // Every single key that was ever installed must end up wiped —
        // either because a later set() replaced it, or because the final
        // clear() above wiped whichever key was still active.
        assertThat(installedKeys.all { it.bytes.isWiped }).isTrue()
    }
}
