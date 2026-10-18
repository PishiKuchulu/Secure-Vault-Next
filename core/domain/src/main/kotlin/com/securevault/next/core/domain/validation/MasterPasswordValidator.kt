package com.securevault.next.core.domain.validation

import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult

/**
 * Validates a candidate master password against SecureVault Next's
 * minimum strength bar: length and character-class diversity.
 *
 * This performs character-class *inspection* only — it never hashes,
 * derives a key from, or otherwise cryptographically processes the
 * password (that is `core:crypto`'s job, starting Milestone 3). Judging
 * "does this string contain a digit" is plain text analysis, not
 * cryptography.
 *
 * The password is accepted as a [CharArray] rather than a [String] so
 * callers can zero it out after use; this validator never copies it into
 * a new `String` (which, once created, cannot be zeroed and lives in
 * memory until garbage collected).
 *
 * [maxLength] is a denial-of-service guard, not a strength rule: an
 * unbounded password would let a caller force an arbitrarily expensive
 * Argon2id derivation on the create/unlock path, so over-long
 * candidates are rejected before any key derivation runs.
 */
class MasterPasswordValidator(
    private val minLength: Int = DEFAULT_MIN_LENGTH,
    private val maxLength: Int = DEFAULT_MAX_LENGTH,
) : Validator<CharArray> {
    init {
        require(minLength >= 1) { "MasterPasswordValidator.minLength must be positive, was $minLength" }
        require(maxLength >= minLength) {
            "MasterPasswordValidator.maxLength ($maxLength) must not be below minLength ($minLength)"
        }
    }

    override fun validate(value: CharArray): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (value.size < minLength) {
            errors +=
                DomainError.ValidationError.TooShort(
                    field = FIELD_NAME,
                    minLength = minLength,
                    actualLength = value.size,
                )
        }

        if (value.size > maxLength) {
            errors +=
                DomainError.ValidationError.TooLong(
                    field = FIELD_NAME,
                    maxLength = maxLength,
                    actualLength = value.size,
                )
        }

        val missingCharacterClasses =
            buildList {
                if (value.none { it.isUpperCase() }) add("an uppercase letter")
                if (value.none { it.isLowerCase() }) add("a lowercase letter")
                if (value.none { it.isDigit() }) add("a digit")
                if (value.none { it.isLetterOrDigit().not() }) add("a symbol")
            }
        if (missingCharacterClasses.isNotEmpty()) {
            errors += DomainError.ValidationError.WeakMasterPassword(missingCharacterClasses)
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val DEFAULT_MIN_LENGTH = 12
        const val DEFAULT_MAX_LENGTH = 128
        const val FIELD_NAME = "masterPassword"
    }
}
