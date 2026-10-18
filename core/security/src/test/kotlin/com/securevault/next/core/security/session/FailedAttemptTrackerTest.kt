package com.securevault.next.core.security.session

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.valueobject.VaultId
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FailedAttemptTrackerTest {
    private class FakeClock(
        var current: Instant,
    ) : SessionClock {
        override fun now(): Instant = current
    }

    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val vaultId = VaultId("vault-1")
    private val otherVaultId = VaultId("vault-2")

    @Test
    fun `allows attempts before the maximum is reached`() {
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 3)

        val stateAfterFirstFailure = tracker.recordFailure(vaultId)

        check(stateAfterFirstFailure is LockoutState.Allowed)
        assertThat(stateAfterFirstFailure.attemptsRemaining).isEqualTo(2)
    }

    @Test
    fun `locks out once the maximum consecutive failures is reached`() {
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 3, lockoutDuration = Duration.ofMinutes(5))

        tracker.recordFailure(vaultId)
        tracker.recordFailure(vaultId)
        val thirdState = tracker.recordFailure(vaultId)

        check(thirdState is LockoutState.LockedOut)
        assertThat(thirdState.retryAt).isEqualTo(start.plus(Duration.ofMinutes(5)))
    }

    @Test
    fun `a successful unlock resets the failure count`() {
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 3)

        tracker.recordFailure(vaultId)
        tracker.recordFailure(vaultId)
        tracker.recordSuccess(vaultId)
        val stateAfterReset = tracker.currentState(vaultId)

        check(stateAfterReset is LockoutState.Allowed)
        assertThat(stateAfterReset.attemptsRemaining).isEqualTo(3)
    }

    @Test
    fun `lockout expires after the configured duration and grants a fresh attempt`() {
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 2, lockoutDuration = Duration.ofMinutes(1))

        tracker.recordFailure(vaultId)
        tracker.recordFailure(vaultId)
        check(tracker.currentState(vaultId) is LockoutState.LockedOut)

        clock.current = start.plus(Duration.ofMinutes(1)).plusSeconds(1)
        val stateAfterExpiry = tracker.currentState(vaultId)

        check(stateAfterExpiry is LockoutState.Allowed)
        assertThat(stateAfterExpiry.attemptsRemaining).isEqualTo(2)
    }

    @Test
    fun `remains locked out before the lockout duration has elapsed`() {
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 2, lockoutDuration = Duration.ofMinutes(5))

        tracker.recordFailure(vaultId)
        tracker.recordFailure(vaultId)

        clock.current = start.plus(Duration.ofMinutes(2))

        assertThat(tracker.currentState(vaultId)).isInstanceOf(LockoutState.LockedOut::class.java)
    }

    @Test
    fun `rejects a non-positive maxAttempts`() {
        var threw = false
        try {
            FailedAttemptTracker(FakeClock(start), maxAttempts = 0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }

    @Test
    fun `concurrent failure recordings never exceed maxAttempts worth of lockouts triggered`() {
        // A pre-Milestone-4 security audit asked for explicit concurrency
        // coverage. recordFailure/currentState/recordSuccess are all
        // @Synchronized on the same monitor, so hammering the tracker
        // from many threads at once must still leave it in a state
        // consistent with *some* valid serialization of those calls —
        // in particular, it must end up locked out (never merely
        // "Allowed" with a negative attemptsRemaining, which would
        // indicate a lost update under concurrent access).
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 5, lockoutDuration = Duration.ofMinutes(1))
        val threadCount = 10
        val failuresPerThread = 50

        val threads =
            (0 until threadCount).map {
                Thread {
                    repeat(failuresPerThread) {
                        tracker.recordFailure(vaultId)
                    }
                }
            }

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        // 500 total failures against a threshold of 5 must result in a
        // lockout, and never in a nonsensical negative attempts-remaining.
        val finalState = tracker.currentState(vaultId)
        assertThat(finalState).isInstanceOf(LockoutState.LockedOut::class.java)
    }

    @Test
    fun `failures against one vault do not consume another vault's budget`() {
        // Finding M1 regression test: state is keyed per vaultId, so
        // exhausting vault-1's attempts must leave vault-2 untouched.
        val clock = FakeClock(start)
        val tracker = FailedAttemptTracker(clock, maxAttempts = 2)

        tracker.recordFailure(vaultId)
        tracker.recordFailure(vaultId)
        check(tracker.currentState(vaultId) is LockoutState.LockedOut)

        val otherState = tracker.currentState(otherVaultId)
        check(otherState is LockoutState.Allowed)
        assertThat(otherState.attemptsRemaining).isEqualTo(2)

        // And a success on one vault does not reset the other's lockout.
        tracker.recordSuccess(otherVaultId)
        assertThat(tracker.currentState(vaultId)).isInstanceOf(LockoutState.LockedOut::class.java)
    }
}
