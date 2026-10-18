package com.securevault.next.core.domain.validation

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult
import org.junit.Test

class NameValidatorsTest {
    @Test
    fun `VaultNameValidator rejects a blank name`() {
        val result = VaultNameValidator().validate("   ")

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single()).isEqualTo(DomainError.ValidationError.BlankValue("vaultName"))
    }

    @Test
    fun `VaultNameValidator rejects a name over the max length`() {
        val tooLong = "x".repeat(101)

        val result = VaultNameValidator().validate(tooLong)

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single())
            .isEqualTo(DomainError.ValidationError.TooLong("vaultName", 100, 101))
    }

    @Test
    fun `VaultNameValidator accepts a reasonable name`() {
        assertThat(VaultNameValidator().validate("Personal Vault")).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `CategoryValidator rejects a blank name`() {
        val result = CategoryValidator().validate("")

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single()).isEqualTo(DomainError.ValidationError.BlankValue("categoryName"))
    }

    @Test
    fun `TagValidator rejects a name containing whitespace`() {
        val result = TagValidator().validate("work stuff")

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single())
            .isEqualTo(DomainError.ValidationError.InvalidFormat("tagName", "must not contain whitespace"))
    }

    @Test
    fun `TagValidator accepts a single-word name`() {
        assertThat(TagValidator().validate("work")).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `FieldValidator only enforces blank check when required`() {
        val optional = FieldValidator(fieldName = "notes", required = false)
        val required = FieldValidator(fieldName = "username", required = true)

        assertThat(optional.validate("")).isEqualTo(ValidationResult.Valid)
        require(required.validate("") is ValidationResult.Invalid)
    }

    @Test
    fun `FieldValidator enforces its max length`() {
        val validator = FieldValidator(fieldName = "label", maxLength = 5)

        val result = validator.validate("toolong")

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single()).isEqualTo(DomainError.ValidationError.TooLong("label", 5, 7))
    }

    @Test
    fun `ItemValidator rejects a blank title`() {
        val result = ItemValidator().validate(" ")

        require(result is ValidationResult.Invalid)
        assertThat(result.errors.single()).isEqualTo(DomainError.ValidationError.BlankValue("itemTitle"))
    }

    @Test
    fun `ItemValidator accepts a normal title`() {
        assertThat(ItemValidator().validate("My Bank Login")).isEqualTo(ValidationResult.Valid)
    }
}
