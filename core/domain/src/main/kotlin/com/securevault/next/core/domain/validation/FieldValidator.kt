package com.securevault.next.core.domain.validation

import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult

/**
 * A configurable validator for a generic text field — the building block
 * used wherever a screen collects free text that doesn't have its own
 * dedicated value object (e.g. a [com.securevault.next.core.domain.model.CustomField]
 * label, an item's username).
 *
 * Unlike [MasterPasswordValidator] or [VaultNameValidator], this is not
 * tied to one specific field; [fieldName] is supplied per instance so
 * the same class serves every free-text input in the app.
 */
class FieldValidator(
    private val fieldName: String,
    private val required: Boolean = false,
    private val maxLength: Int = DEFAULT_MAX_LENGTH,
) : Validator<String> {

    override fun validate(value: String): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (required && value.isBlank()) {
            errors += DomainError.ValidationError.BlankValue(fieldName)
        }
        if (value.length > maxLength) {
            errors += DomainError.ValidationError.TooLong(fieldName, maxLength, value.length)
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val DEFAULT_MAX_LENGTH = 1_000
    }
}
