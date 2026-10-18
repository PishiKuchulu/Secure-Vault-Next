package com.securevault.next.core.crypto.kdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KdfParametersTest {
    @Test
    fun `the production default parameters meet the OWASP recommended floor`() {
        // This is the regression guard the audit's finding asked for:
        // whatever DEFAULT_MEMORY_COST_KIB and DEFAULT_ITERATIONS get
        // tuned to in the future, CURRENT must never silently regress
        // below OWASP's combined recommendation.
        assertThat(KdfParameters.CURRENT.meetsOwaspMinimumRecommendation()).isTrue()
    }

    @Test
    fun `a configuration at the memory floor with only 1 iteration does not meet the recommendation`() {
        val belowRecommendation =
            KdfParameters(
                memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
                iterations = 1,
            )

        assertThat(belowRecommendation.meetsOwaspMinimumRecommendation()).isFalse()
    }

    @Test
    fun `a configuration at the memory floor with 2 or more iterations meets the recommendation`() {
        val atRecommendation =
            KdfParameters(
                memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB,
                iterations = 2,
            )

        assertThat(atRecommendation.meetsOwaspMinimumRecommendation()).isTrue()
    }

    @Test
    fun `above-floor memory meets the recommendation regardless of iteration count`() {
        val highMemoryLowIterations =
            KdfParameters(
                memoryCostKib = KdfParameters.DEFAULT_MEMORY_COST_KIB,
                iterations = 1,
            )

        assertThat(highMemoryLowIterations.meetsOwaspMinimumRecommendation()).isTrue()
    }

    @Test
    fun `rejects memory cost below the absolute floor`() {
        var threw = false
        try {
            KdfParameters(memoryCostKib = KdfParameters.MIN_MEMORY_COST_KIB - 1)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }

    @Test
    fun `rejects an output length shorter than the master key plus MAC key requirement`() {
        var threw = false
        try {
            KdfParameters(outputLengthBytes = 32)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }
}
