package com.securevault.next.core.security.session

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AutoLockEvaluatorTest {
    private class FakeClock(
        var current: Instant,
    ) : SessionClock {
        override fun now(): Instant = current
    }

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `NEVER policy never locks regardless of elapsed time`() {
        val clock = FakeClock(start.plus(Duration.ofDays(365)))
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.NEVER, start)).isFalse()
    }

    @Test
    fun `IMMEDIATELY policy locks as soon as any time has passed`() {
        val clock = FakeClock(start.plusSeconds(1))
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.IMMEDIATELY, start)).isTrue()
    }

    @Test
    fun `IMMEDIATELY policy locks even with exactly zero elapsed time`() {
        val clock = FakeClock(start)
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.IMMEDIATELY, start)).isTrue()
    }

    @Test
    fun `MINUTES_5 policy does not lock before 5 minutes have elapsed`() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(4)))
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.MINUTES_5, start)).isFalse()
    }

    @Test
    fun `MINUTES_5 policy locks once exactly 5 minutes have elapsed`() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(5)))
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.MINUTES_5, start)).isTrue()
    }

    @Test
    fun `MINUTES_5 policy locks well after 5 minutes have elapsed`() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(30)))
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.shouldLock(AutoLockPolicy.MINUTES_5, start)).isTrue()
    }

    @Test
    fun `remainingUntilLock counts down correctly and floors at zero`() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(3)))
        val evaluator = AutoLockEvaluator(clock)

        val remaining = evaluator.remainingUntilLock(AutoLockPolicy.MINUTES_5, start)

        assertThat(remaining).isEqualTo(Duration.ofMinutes(2))
    }

    @Test
    fun `remainingUntilLock never goes negative once the threshold has passed`() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(10)))
        val evaluator = AutoLockEvaluator(clock)

        val remaining = evaluator.remainingUntilLock(AutoLockPolicy.MINUTES_5, start)

        assertThat(remaining).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `remainingUntilLock is null for NEVER`() {
        val clock = FakeClock(start)
        val evaluator = AutoLockEvaluator(clock)

        assertThat(evaluator.remainingUntilLock(AutoLockPolicy.NEVER, start)).isNull()
    }
}
