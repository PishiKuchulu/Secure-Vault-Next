package com.securevault.next.core.domain.validation

import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult
import com.securevault.next.core.domain.model.valueobject.VaultName

/**
 * Validates a raw candidate vault name string before it is wrapped as a
 * [VaultName].
 *
 * This exists alongside [VaultName]'s own constructor invariant for a
 * specific reason: a form field needs to show "name must not be blank"
 * *without* throwing an [IllegalArgumentException] the moment the user
 * types a blank string — [VaultNameValidator.validate] lets a caller
 * check first and construct the value object only once validation
 * passes.
 */
class VaultNameValidator : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (value.isBlank()) {
            errors += DomainError.ValidationError.BlankValue(FIELD_NAME)
        } else if (value.length > VaultName.MAX_LENGTH) {
            errors +=
                DomainError.ValidationError.TooLong(
                    field = FIELD_NAME,
                    maxLength = VaultName.MAX_LENGTH,
                    actualLength = value.length,
                )
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val FIELD_NAME = "vaultName"
    }
}

/** Validates a candidate [com.securevault.next.core.domain.model.Category] name. */
class CategoryValidator(
    private val maxLength: Int = DEFAULT_MAX_LENGTH,
) : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (value.isBlank()) {
            errors += DomainError.ValidationError.BlankValue(FIELD_NAME)
        } else if (value.length > maxLength) {
            errors += DomainError.ValidationError.TooLong(FIELD_NAME, maxLength, value.length)
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val DEFAULT_MAX_LENGTH = 50
        const val FIELD_NAME = "categoryName"
    }
}

/** Validates a candidate [com.securevault.next.core.domain.model.Tag] name. */
class TagValidator(
    private val maxLength: Int = DEFAULT_MAX_LENGTH,
) : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (value.isBlank()) {
            errors += DomainError.ValidationError.BlankValue(FIELD_NAME)
        } else if (value.length > maxLength) {
            errors += DomainError.ValidationError.TooLong(FIELD_NAME, maxLength, value.length)
        } else if (value.any { it.isWhitespace() }) {
            errors += DomainError.ValidationError.InvalidFormat(FIELD_NAME, "must not contain whitespace")
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val DEFAULT_MAX_LENGTH = 30
        const val FIELD_NAME = "tagName"
    }
}
