package com.securevault.next.core.security.session

import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Duration
import java.time.Instant

/** The result of checking or recording against a [FailedAttemptTracker]. */
sealed interface LockoutState {
    /**
     * Unlock attempts are currently permitted. [attemptsRemaining] is
     * informational, for a UI to warn the user before lockout occurs.
     */
    data class Allowed(val attemptsRemaining: Int) : LockoutState

    /** Unlock attempts are currently blocked until [retryAt]. */
    data class LockedOut(val retryAt: Instant) : LockoutState
}

/**
 * Tracks consecutive failed unlock attempts per vault and enforces a
 * temporary lockout once a policy's maximum failed attempt count is
 * exceeded.
 *
 * Finding M1: attempt state is keyed by [VaultId] internally, so failed
 * attempts against vault A never consume vault B's budget. (Previously a
 * single global counter meant one vault's failures could lock out every
 * other vault — a cross-vault denial of service.)
 *
 * This does not itself decide *whether* a password was correct (that is
 * `core.crypto.vault.VaultEncryptionEngine.unlockVault`'s job) — it only
 * tracks the *count and timing* of failures a caller reports to it via
 * [recordFailure], and answers "is an attempt currently allowed" via
 * [currentState]. State is intentionally in-memory only rather than
 * persisted, since a device restart resetting a lockout is an accepted
 * tradeoff for not persisting security-sensitive counters in a form an
 * attacker with filesystem access could simply delete anyway.
 */
class FailedAttemptTracker(
    private val clock: SessionClock,
    private val maxAttempts: Int,
    private val lockoutDuration: Duration = Duration.ofMinutes(5),
) {
    init {
        require(maxAttempts > 0) { "FailedAttemptTracker.maxAttempts must be positive, was $maxAttempts" }
    }

    private data class VaultAttemptState(
        var consecutiveFailures: Int = 0,
        var lockedOutUntil: Instant? = null,
    )

    // Guarded by the @Synchronized methods below. Entries accumulate one
    // per vault ever attempted in this process, which is bounded by the
    // user's vault count.
    private val states = mutableMapOf<VaultId, VaultAttemptState>()

    private fun stateFor(vaultId: VaultId): VaultAttemptState =
        states.getOrPut(vaultId) { VaultAttemptState() }

    /** Records a failed unlock attempt against [vaultId], returning the resulting [LockoutState]. */
    @Synchronized
    fun recordFailure(vaultId: VaultId): LockoutState {
        val state = stateFor(vaultId)
        state.consecutiveFailures += 1
        if (state.consecutiveFailures >= maxAttempts) {
            state.lockedOutUntil = clock.now().plus(lockoutDuration)
        }
        return currentState(vaultId)
    }

    /** Records a successful unlock of [vaultId], resetting its failure count and clearing any lockout. */
    @Synchronized
    fun recordSuccess(vaultId: VaultId) {
        val state = stateFor(vaultId)
        state.consecutiveFailures = 0
        state.lockedOutUntil = null
    }

    /**
     * The current [LockoutState] for [vaultId], accounting for whether a
     * previously imposed lockout has since expired.
     */
    @Synchronized
    fun currentState(vaultId: VaultId): LockoutState {
        val state = stateFor(vaultId)
        val lockoutExpiry = state.lockedOutUntil
        if (lockoutExpiry != null) {
            if (clock.now().isBefore(lockoutExpiry)) {
                return LockoutState.LockedOut(lockoutExpiry)
            }
            // The lockout window has elapsed: clear it and give the caller
            // one fresh attempt rather than staying locked out forever.
            state.lockedOutUntil = null
            state.consecutiveFailures = 0
        }
        return LockoutState.Allowed(attemptsRemaining = maxAttempts - state.consecutiveFailures)
    }
}
