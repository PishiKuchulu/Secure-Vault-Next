package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.common.result.errorOrNull
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.UnlockVaultUseCase
import com.securevault.next.core.security.session.FailedAttemptTracker
import com.securevault.next.core.security.session.LockoutState
import com.securevault.next.core.security.session.SessionClock
import com.securevault.next.core.security.session.SessionKeyStore
import com.securevault.next.core.security.session.VaultSessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.UUID
import javax.inject.Inject

class UnlockVaultUseCaseImpl
    @Inject
    constructor(
        private val vaultRepository: VaultRepository,
        private val vaultKeyEncryptionService: VaultKeyEncryptionService,
        private val sessionKeyStore: SessionKeyStore,
        private val sessionManager: VaultSessionManager,
        private val clock: SessionClock,
        private val failedAttemptTracker: FailedAttemptTracker,
        @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher,
    ) : UnlockVaultUseCase {
        override suspend fun invoke(
            vaultId: VaultId,
            masterPassword: CharArray,
            method: AuthenticationMethod,
        ): DomainResult<VaultSession> {
            // Brute-force gate: if the tracker is currently locked out, refuse
            // the attempt BEFORE any cryptographic work — a locked-out attempt
            // must never run Argon2id (no crypto after lockout is known).
            when (val lockout = failedAttemptTracker.currentState(vaultId)) {
                is LockoutState.LockedOut -> {
                    val retryAfter = Duration.between(clock.now(), lockout.retryAt).seconds
                    return DomainResult.failure(
                        DomainError.AuthenticationError.TooManyAttempts(retryAfter.coerceAtLeast(0)),
                    )
                }
                is LockoutState.Allowed -> Unit
            }

            // Argon2id key unwrap is CPU/IO-bound; run it off the caller thread.
            val unwrapResult: DomainResult<ByteArray> =
                withContext(ioDispatcher) {
                    when (val envelopeResult = vaultRepository.getEnvelopeJson(vaultId)) {
                        is com.securevault.next.core.common.result.Result.Failure ->
                            DomainResult.failure(envelopeResult.error)
                        is com.securevault.next.core.common.result.Result.Success -> {
                            val envelopeJson =
                                envelopeResult.value
                                    ?: return@withContext DomainResult.failure(
                                        DomainError.RepositoryError.NotFound(
                                            entityId = "vault-${vaultId.value}",
                                            entityType = "vault envelope",
                                        ),
                                    )
                            vaultKeyEncryptionService.unwrapVaultKey(envelopeJson, masterPassword)
                        }
                    }
                }

            // Finding L1: the old `when` over unwrapResult had an unreachable
            // Success branch (getOrNull() is non-null on every
            // Success<ByteArray>); the failure path is handled directly now.
            val sessionKeyBytes = unwrapResult.getOrNull()
            if (sessionKeyBytes == null) {
                val error = unwrapResult.errorOrNull()!!
                if (error is DomainError.AuthenticationError.InvalidCredentials) {
                    failedAttemptTracker.recordFailure(vaultId)
                }
                return DomainResult.failure(error)
            }

            return try {
                // Session establishment runs on the caller thread: the manager
                // publishes state and fires listeners here.
                val session =
                    VaultSession(
                        vaultId = vaultId,
                        sessionId = UUID.randomUUID().toString(),
                        authenticationMethod = method,
                        unlockedAt = clock.now(),
                        lastActivityAt = clock.now(),
                    )
                val unlocked = sessionManager.unlock(session, sessionKeyBytes)
                if (unlocked) {
                    failedAttemptTracker.recordSuccess(vaultId)
                    DomainResult.success(session)
                } else if (sessionManager.isUnlocked()) {
                    // The manager refused because a session is already active:
                    // the credentials were valid, so reporting
                    // InvalidCredentials here would be a lie (Finding L1).
                    DomainResult.failure(DomainError.VaultError.AlreadyUnlocked(vaultId))
                } else {
                    // unlock() returned false with no active session: the key
                    // could not be installed (see VaultSessionManager.unlock).
                    DomainResult.failure(
                        DomainError.UnexpectedError("The session key could not be installed; the vault remains locked"),
                    )
                }
            } finally {
                MemoryWiper.wipe(sessionKeyBytes)
            }
        }
    }
