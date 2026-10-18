package com.securevault.next.core.security.session

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VaultSessionManagerTest {
    private class FakeClock(var current: Instant) : SessionClock {
        override fun now(): Instant = current
    }

    private class FakeSessionKeyStore : SessionKeyStore {
        var installed: ByteArray? = null
            private set
        var destroyCalls = 0
            private set

        override fun install(keyBytes: ByteArray) {
            installed = keyBytes
        }

        override fun destroy() {
            destroyCalls++
            installed = null
        }

        override val isActive: Boolean
            get() = installed != null
    }

    private val vaultId = VaultId("vault-1")
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val keyBytes = ByteArray(32) { it.toByte() }

    private fun session(now: Instant = start) = VaultSession(
        vaultId = vaultId,
        sessionId = "session-1",
        authenticationMethod = AuthenticationMethod.MASTER_PASSWORD,
        unlockedAt = now,
        lastActivityAt = now,
        expiresAt = null
    )

    private fun manager(
        clock: SessionClock = FakeClock(start),
        keyStore: SessionKeyStore = FakeSessionKeyStore(),
        listeners: Set<SessionListener> = emptySet()
    ) = VaultSessionManager(clock, keyStore, listeners)

    // --- Unlock ---

    @Test
    fun `unlock installs key and reaches Unlocked only after installation`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)

        val success = manager.unlock(session(), keyBytes)

        assertThat(success).isTrue()
        assertThat(store.isActive).isTrue()
        assertThat(manager.state.first()).isEqualTo(SessionState.Unlocked(session()))
    }

    @Test
    fun `unlock with null key stays locked and never installs`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)

        val success = manager.unlock(session(), null)

        assertThat(success).isFalse()
        assertThat(store.isActive).isFalse()
        assertThat(manager.state.first()).isEqualTo(SessionState.Locked)
    }

    @Test
    fun `unlock while already unlocked is rejected`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)
        manager.unlock(session(), keyBytes)

        val second = manager.unlock(session(), ByteArray(32))

        assertThat(second).isFalse()
        assertThat(manager.state.first()).isEqualTo(SessionState.Unlocked(session()))
    }

    // --- Lock ---

    @Test
    fun `lock destroys key before publishing locked state`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)
        manager.unlock(session(), keyBytes)

        val locked = manager.lock()

        assertThat(locked).isTrue()
        assertThat(store.destroyCalls).isEqualTo(1)
        assertThat(store.isActive).isFalse()
        assertThat(manager.state.first()).isEqualTo(SessionState.Locked)
        assertThat(manager.currentSession()).isNull()
    }

    @Test
    fun `lock with timeout reason emits AutoLocked`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)
        manager.unlock(session(), keyBytes)

        manager.lock(reason = LockTrigger.TIMEOUT)

        assertThat(manager.state.first()).isEqualTo(SessionState.AutoLocked)
        assertThat(store.isActive).isFalse()
    }

    @Test
    fun `lock on an already locked vault is a no-op`() = runTest {
        val store = FakeSessionKeyStore()
        val manager = manager(keyStore = store)

        val locked = manager.lock()

        assertThat(locked).isFalse()
        assertThat(store.destroyCalls).isEqualTo(0)
        assertThat(manager.state.first()).isEqualTo(SessionState.Locked)
    }

    // --- Listeners ---

    @Test
    fun `listeners notified on unlock and lock with reasons`() = runTest {
        val store = FakeSessionKeyStore()
        var unlockedSession: VaultSession? = null
        var lockReasons = mutableListOf<LockTrigger>()
        val manager =
            manager(
                keyStore = store,
                listeners =
                setOf(
                    SessionListener.Adapter(
                        unlocked = { unlockedSession = it },
                        locked = { lockReasons.add(it) }
                    )
                )
            )

        manager.unlock(session(), keyBytes)
        assertThat(unlockedSession).isEqualTo(session())

        manager.lock(reason = LockTrigger.EXPLICIT)
        assertThat(lockReasons).containsExactly(LockTrigger.EXPLICIT)
    }

    // --- touch() ---

    @Test
    fun `touch updates lastActivityAt while unlocked`() = runTest {
        val clock = FakeClock(start)
        val manager = manager(clock = clock)
        manager.unlock(session(clock.now()), keyBytes)

        val advanced = start.plusSeconds(60)
        clock.current = advanced
        manager.touch()

        val state = manager.state.first() as SessionState.Unlocked
        assertThat(state.session.lastActivityAt).isEqualTo(advanced)
        assertThat(state.session.unlockedAt).isEqualTo(start)
    }

    @Test
    fun `touch while locked is a no-op`() = runTest {
        val manager = manager()

        val previous = manager.touch()

        assertThat(previous).isNull()
        assertThat(manager.state.first()).isEqualTo(SessionState.Locked)
    }
}
