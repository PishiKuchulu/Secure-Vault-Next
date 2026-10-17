package com.securevault.next.core.vault.usecase

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.common.result.errorOrNull
import com.securevault.next.core.common.result.isFailure
import com.securevault.next.core.common.result.isSuccess
import com.securevault.next.core.contracts.EnvelopeAndKey
import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.security.session.FailedAttemptTracker
import com.securevault.next.core.security.session.SessionClock
import com.securevault.next.core.security.session.SessionKeyStore
import com.securevault.next.core.security.session.SessionState
import com.securevault.next.core.security.session.VaultSessionManager
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class UnlockLockUseCaseTest {

    // --- Fakes ---

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

    private class FakeVaultRepository(
        private val envelopeJson: String?,
    ) : VaultRepository {
        override fun observeVault(vaultId: VaultId): Flow<com.securevault.next.core.domain.model.Vault?> =
            kotlinx.coroutines.flow.emptyFlow()

        override fun observeAllVaultMetadata(): Flow<List<com.securevault.next.core.domain.model.VaultMetadata>> =
            kotlinx.coroutines.flow.emptyFlow()

        override suspend fun getVault(vaultId: VaultId): DomainResult<com.securevault.next.core.domain.model.Vault> =
            DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))

        override suspend fun getVaultMetadata(vaultId: VaultId): DomainResult<com.securevault.next.core.domain.model.VaultMetadata> =
            DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))

        override suspend fun createVault(
            metadata: com.securevault.next.core.domain.model.VaultMetadata,
            securityPolicy: com.securevault.next.core.domain.model.VaultSecurityPolicy,
            settings: com.securevault.next.core.domain.model.VaultSettings,
            envelopeJson: String?,
        ): DomainResult<com.securevault.next.core.domain.model.Vault> =
            DomainResult.success(
                com.securevault.next.core.domain.model.Vault(
                    metadata = metadata,
                    items = emptyList(),
                    categories = emptyList(),
                    tags = emptyList(),
                    settings = settings,
                    securityPolicy = securityPolicy,
                    statistics = com.securevault.next.core.domain.model.VaultStatistics(
                        itemCount = 0,
                        categoryCount = 0,
                        favoriteCount = 0,
                        weakPasswordCount = 0,
                        reusedPasswordCount = 0,
                        oldPasswordCount = 0,
                        breachedPasswordCount = 0,
                        securityScore = com.securevault.next.core.domain.model.valueobject.SecurityScore(0),
                    ),
                ),
            )

        override suspend fun getEnvelopeJson(vaultId: VaultId): DomainResult<String?> =
            DomainResult.success(envelopeJson)

        override suspend fun updateEnvelopeJson(vaultId: VaultId, envelopeJson: String): DomainResult<Unit> =
            DomainResult.success(Unit)

        override suspend fun updateVaultSettings(vaultId: VaultId, settings: com.securevault.next.core.domain.model.VaultSettings): DomainResult<Unit> =
            DomainResult.success(Unit)

        override suspend fun updateVaultSecurityPolicy(vaultId: VaultId, policy: com.securevault.next.core.domain.model.VaultSecurityPolicy): DomainResult<Unit> =
            DomainResult.success(Unit)

        override suspend fun deleteVault(vaultId: VaultId): DomainResult<Unit> =
            DomainResult.success(Unit)
    }

    private class FakeVaultKeyEncryptionService(
        private val envelopeJson: String?,
        private val keyBytes: ByteArray,
        private val failWith: DomainError? = null,
    ) : VaultKeyEncryptionService {
        var unwrapCalls = 0
            private set

        override fun createEnvelope(masterPassword: CharArray): DomainResult<EnvelopeAndKey> =
            DomainResult.success(EnvelopeAndKey(envelopeJson = envelopeJson ?: "", vaultKeyBytes = keyBytes))

        override fun unwrapVaultKey(envelopeJson: String, masterPassword: CharArray): DomainResult<ByteArray> {
            unwrapCalls++
            return failWith?.let { DomainResult.failure(it) }
                ?: DomainResult.success(keyBytes)
        }

        override fun rotateMasterPassword(
            envelopeJson: String,
            currentPassword: CharArray,
            newPassword: CharArray,
        ): DomainResult<String> =
            failWith?.let { DomainResult.failure(it) }
                ?: DomainResult.success("rotated-envelope-json")
    }

    private val vaultId = VaultId("vault-1")
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val keyBytes = ByteArray(32) { it.toByte() }
    private val password = "correct-password".toCharArray()

    private fun manager(store: SessionKeyStore) =
        VaultSessionManager(FakeClock(start), store)

    private fun tracker(maxAttempts: Int, clock: FakeClock = FakeClock(start)) =
        FailedAttemptTracker(clock, maxAttempts, Duration.ofMinutes(5))

    // --- Unlock ---

    @Test
    fun `successful unlock installs key and reaches Unlocked`() = runTest {
        val store = FakeSessionKeyStore()
        val repo = FakeVaultRepository("envelope-json")
        val returned = ByteArray(32) { it.toByte() }
        val svc = FakeVaultKeyEncryptionService("envelope-json", returned)
        val m = manager(store)
        val useCase = UnlockVaultUseCaseImpl(repo, svc, store, m, FakeClock(start), tracker(5), kotlinx.coroutines.Dispatchers.Unconfined)

        val result = useCase(vaultId, password, AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isSuccess).isTrue()
        assertThat(store.isActive).isTrue()
        // The store holds the installed key; the returned temporary buffer is wiped.
        assertThat(returned.all { it == 0.toByte() }).isTrue()
        assertThat(m.state.first()).isInstanceOf(SessionState.Unlocked::class.java)
    }

    @Test
    fun `wrong password leaves session locked and installs no key`() = runTest {
        val store = FakeSessionKeyStore()
        val repo = FakeVaultRepository("envelope-json")
        val svc = FakeVaultKeyEncryptionService(
            envelopeJson = "envelope-json",
            keyBytes = keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(store)
        val useCase = UnlockVaultUseCaseImpl(repo, svc, store, m, FakeClock(start), tracker(5), kotlinx.coroutines.Dispatchers.Unconfined)

        val result = useCase(vaultId, "wrong-password".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isFailure).isTrue()
        assertThat(store.isActive).isFalse()
        assertThat(store.installed).isNull()
        assertThat(m.state.first()).isEqualTo(SessionState.Locked)
    }

    @Test
    fun `missing envelope rejects unlock without installing a key`() = runTest {
        val store = FakeSessionKeyStore()
        val repo = FakeVaultRepository(null) // no envelope stored
        val svc = FakeVaultKeyEncryptionService(null, keyBytes)
        val m = manager(store)
        val useCase = UnlockVaultUseCaseImpl(repo, svc, store, m, FakeClock(start), tracker(5), kotlinx.coroutines.Dispatchers.Unconfined)

        val result = useCase(vaultId, password, AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isFailure).isTrue()
        assertThat(store.isActive).isFalse()
        assertThat(m.state.first()).isEqualTo(SessionState.Locked)
    }

    @Test
    fun `session key bytes are wiped after install`() = runTest {
        val store = FakeSessionKeyStore()
        val repo = FakeVaultRepository("envelope-json")
        // A mutable buffer: after the use case returns, it must be zeroed.
        val buffer = ByteArray(32) { 0x5A }
        val svc = FakeVaultKeyEncryptionService("envelope-json", buffer)
        val m = manager(store)
        val useCase = UnlockVaultUseCaseImpl(repo, svc, store, m, FakeClock(start), tracker(5), kotlinx.coroutines.Dispatchers.Unconfined)

        useCase(vaultId, password, AuthenticationMethod.MASTER_PASSWORD)

        assertThat(buffer.all { it == 0.toByte() }).isTrue()
        assertThat(m.state.first()).isInstanceOf(SessionState.Unlocked::class.java)
    }

    // --- Lockout (M4) ---

    @Test
    fun `first invalid password increments failure count`() = runTest {
        val store = FakeSessionKeyStore()
        val t = tracker(3)
        val scr = FakeSessionKeyStore()
        val svc = FakeVaultKeyEncryptionService(
            envelopeJson = "env",
            keyBytes = keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(scr)
        val useCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)

        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)

        assertThat(t.currentState(vaultId)).isEqualTo(
            com.securevault.next.core.security.session.LockoutState.Allowed(attemptsRemaining = 2),
        )
    }

    @Test
    fun `invalid password below threshold remains locked with no key`() = runTest {
        val scr = FakeSessionKeyStore()
        val t = tracker(3)
        val svc = FakeVaultKeyEncryptionService(
            "env",
            keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(scr)
        val useCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)

        val result = useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isFailure).isTrue()
        assertThat(scr.isActive).isFalse()
        assertThat(m.state.first()).isEqualTo(SessionState.Locked)
    }

    @Test
    fun `threshold reached triggers lockout`() = runTest {
        val scr = FakeSessionKeyStore()
        val t = tracker(2)
        val svc = FakeVaultKeyEncryptionService(
            "envelope",
            keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(scr)
        val useCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)

        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)
        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)

        assertThat(t.currentState(vaultId)).isInstanceOf(com.securevault.next.core.security.session.LockoutState.LockedOut::class.java)
    }

    @Test
    fun `locked out attempt performs no cryptographic work`() = runTest {
        val scr = FakeSessionKeyStore()
        val t = tracker(1) // lockout after first failure
        val svc = FakeVaultKeyEncryptionService(
            "envelope",
            keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )

        // cause lockout
        val m0 = manager(scr)
        val first = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m0, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)
        first(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)
        val callsAtLock = svc.unwrapCalls

        // a fresh invocation while locked out must NOT call crypto
        val m1 = manager(scr)
        val second = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m1, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)
        val result = second(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull()).isInstanceOf(DomainError.AuthenticationError.TooManyAttempts::class.java)
        assertThat(svc.unwrapCalls).isEqualTo(callsAtLock) // no additional crypto
    }

    @Test
    fun `lockout expires after lockoutDuration`() = runTest {
        val clock = FakeClock(start)
        val scr = FakeSessionKeyStore()
        val t = tracker(2, clock)
        val svc = FakeVaultKeyEncryptionService(
            "envelope",
            keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(scr)
        val useCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m, clock, t, kotlinx.coroutines.Dispatchers.Unconfined)

        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)
        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)
        assertThat(t.currentState(vaultId)).isInstanceOf(com.securevault.next.core.security.session.LockoutState.LockedOut::class.java)

        clock.current = start.plus(Duration.ofMinutes(6)) // past 5-min lockout
        assertThat(t.currentState(vaultId)).isEqualTo(com.securevault.next.core.security.session.LockoutState.Allowed(attemptsRemaining = 2))
    }

    @Test
    fun `successful unlock clears failure state`() = runTest {
        val scr = FakeSessionKeyStore()
        val t = tracker(3)
        val svc = FakeVaultKeyEncryptionService(
            "envelope",
            keyBytes,
            failWith = DomainError.AuthenticationError.InvalidCredentials,
        )
        val m = manager(scr)
        val useCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), svc, scr, m, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)

        useCase(vaultId, "wrong".toCharArray(), AuthenticationMethod.MASTER_PASSWORD)
        assertThat(t.currentState(vaultId)).isInstanceOf(com.securevault.next.core.security.session.LockoutState.Allowed::class.java)

        // switch service to succeed
        val okSvc = FakeVaultKeyEncryptionService("envelope", keyBytes)
        val okUseCase = UnlockVaultUseCaseImpl(FakeVaultRepository("env"), okSvc, scr, m, FakeClock(start), t, kotlinx.coroutines.Dispatchers.Unconfined)
        val result = okUseCase(vaultId, password, AuthenticationMethod.MASTER_PASSWORD)

        assertThat(result.isSuccess).isTrue()
        assertThat(t.currentState(vaultId)).isEqualTo(com.securevault.next.core.security.session.LockoutState.Allowed(attemptsRemaining = 3))
    }

    // --- Lock ---

    @Test
    fun `lock destroys the session key`() = runTest {
        val store = FakeSessionKeyStore()
        val m = manager(store)
        m.unlock(session(), keyBytes)
        assertThat(store.isActive).isTrue()

        val useCase = LockVaultUseCaseImpl(store, m)
        val result = useCase(vaultId)

        assertThat(result.isSuccess).isTrue()
        assertThat(store.isActive).isFalse()
        assertThat(m.state.first()).isEqualTo(SessionState.Locked)
    }

    @Test
    fun `repeated lock is safe and idempotent`() = runTest {
        val store = FakeSessionKeyStore()
        val m = manager(store)
        m.unlock(session(), keyBytes)
        val useCase = LockVaultUseCaseImpl(store, m)

        val first = useCase(vaultId)
        val second = useCase(vaultId)

        assertThat(first.isSuccess).isTrue()
        assertThat(second.isFailure).isTrue() // already locked
        assertThat(store.isActive).isFalse()
    }

    private fun session(now: Instant = start) = VaultSession(
        vaultId = vaultId,
        sessionId = "session-1",
        authenticationMethod = AuthenticationMethod.MASTER_PASSWORD,
        unlockedAt = now,
        lastActivityAt = now,
        expiresAt = null,
    )
}
