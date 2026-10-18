package com.securevault.next.core.security.session

import java.time.Instant
import javax.inject.Inject

/**
 * The current time, as an injectable dependency rather than a direct
 * call to [Instant.now].
 *
 * Every piece of session-timing logic in `core:security`
 * ([AutoLockEvaluator]) takes a [SessionClock] instead of reading the
 * system clock itself, so tests can supply a fixed or step-controlled
 * clock instead of racing against real wall-clock time (Milestone 3
 * testing requirement: "Session lifecycle" must be testable).
 */
fun interface SessionClock {
    fun now(): Instant
}

/** The production [SessionClock], backed by [Instant.now]. */
class SystemSessionClock
    @Inject
    constructor() : SessionClock {
        override fun now(): Instant = Instant.now()
    }
