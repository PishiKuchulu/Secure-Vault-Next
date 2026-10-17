package com.securevault.next.core.security.usecase

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.enums.PasswordStrengthLevel
import com.securevault.next.core.domain.security.SecurityScoreEngine
import org.junit.Test

class ValidatePasswordUseCaseTest {

    private val useCase = ValidatePasswordUseCaseImpl()

    @Test
    fun `weak password returns VERY_WEAK level`() {
        val result = useCase("123456".toCharArray())
        assertThat(result.level).isEqualTo(PasswordStrengthLevel.VERY_WEAK)
    }

    @Test
    fun `strong password returns VERY_STRONG level`() {
        val result = useCase("P@ssw0rd_With_High_Entropy_123!".toCharArray())
        assertThat(result.level).isEqualTo(PasswordStrengthLevel.VERY_STRONG)
    }

    @Test
    fun `entropy calculation is consistent`() {
        val entropy1 = SecurityScoreEngine.calculateEntropy("abc".toCharArray())
        val entropy2 = SecurityScoreEngine.calculateEntropy("ABC".toCharArray())
        assertThat(entropy1).isEqualTo(entropy2)

        val entropy3 = SecurityScoreEngine.calculateEntropy("abc1".toCharArray())
        assertThat(entropy3).isGreaterThan(entropy1)
    }
}
