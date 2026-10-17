package com.securevault.next.core.security.session

import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import java.time.Duration
import java.time.Instant

/**
 * Decides whether a session has been idle long enough to auto-lock,
 * given a vault's [AutoLockPolicy] and when it was last active.
 *
 * This is deliberately pure logic with no timer, no coroutine, and no
 * knowledge of the app lifecycle — it answers one question
 * ("should this be locked *right now*, given this elapsed time?") and
 * leaves *when* to ask that question (a periodic check, an
 * `onStop`/`onStart` lifecycle callback) to the platform layer in a
 * later milestone. That split is what makes this pure Kotlin/JVM
 * testable without Android's `Handler`/`WorkManager`/lifecycle APIs.
 */
class AutoLockEvaluator(
    private val clock: SessionClock,
) {

    /**
     * True if, given [policy] and the session's [lastActivityAt], the
     * session should be locked as of [clock]'s current time.
     *
     * [AutoLockPolicy.NEVER] always returns `false`.
     * [AutoLockPolicy.IMMEDIATELY] (zero-minute threshold) returns `true`
     * for any non-negative elapsed duration, including exactly zero —
     * "immediately" means the very next check after activity stops locks
     * it.
     */
    fun shouldLock(policy: AutoLockPolicy, lastActivityAt: Instant): Boolean {
        if (policy == AutoLockPolicy.NEVER) return false

        val elapsed = Duration.between(lastActivityAt, clock.now())
        val threshold = Duration.ofMinutes(policy.minutes.toLong())
        return !elapsed.isNegative && elapsed >= threshold
    }

    /**
     * How much longer, from now, until [policy] would lock a session
     * last active at [lastActivityAt]. Never negative; zero once the
     * threshold has already passed. Returns `null` for
     * [AutoLockPolicy.NEVER], which has no such duration.
     */
    fun remainingUntilLock(policy: AutoLockPolicy, lastActivityAt: Instant): Duration? {
        if (policy == AutoLockPolicy.NEVER) return null

        val elapsed = Duration.between(lastActivityAt, clock.now())
        val threshold = Duration.ofMinutes(policy.minutes.toLong())
        val remaining = threshold.minus(elapsed)
        return if (remaining.isNegative) Duration.ZERO else remaining
    }
}
