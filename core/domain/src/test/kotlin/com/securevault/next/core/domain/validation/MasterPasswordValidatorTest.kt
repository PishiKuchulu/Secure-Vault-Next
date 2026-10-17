package com.securevault.next.core.domain.validation

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult
import org.junit.Test

class MasterPasswordValidatorTest {

    private val validator = MasterPasswordValidator(minLength = 12)

    @Test
    fun `accepts a password meeting length and character diversity`() {
        val result = validator.validate("Correct7Horse!".toCharArray())

        assertThat(result).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `rejects a password shorter than the minimum length`() {
        val result = validator.validate("Short1!".toCharArray())

        require(result is ValidationResult.Invalid)
        assertThat(result.errors).contains(
            DomainError.ValidationError.TooShort(field = "masterPassword", minLength = 12, actualLength = 7),
        )
    }

    @Test
    fun `rejects a password missing character class diversity`() {
        val result = validator.validate("alllowercase".toCharArray())

        require(result is ValidationResult.Invalid)
        val weaknessError = result.errors.filterIsInstance<DomainError.ValidationError.WeakMasterPassword>().single()
        assertThat(weaknessError.reasons).containsAtLeast("an uppercase letter", "a digit", "a symbol")
    }

    @Test
    fun `reports both length and diversity problems simultaneously`() {
        val result = validator.validate("weak".toCharArray())

        require(result is ValidationResult.Invalid)
        assertThat(result.errors).hasSize(2)
        assertThat(result.errors.any { it is DomainError.ValidationError.TooShort }).isTrue()
        assertThat(result.errors.any { it is DomainError.ValidationError.WeakMasterPassword }).isTrue()
    }
}
