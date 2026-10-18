package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.error.DomainError

/**
 * The outcome of running a [com.securevault.next.core.domain.validation.Validator].
 *
 * Unlike [com.securevault.next.core.domain.result.DomainResult], which
 * short-circuits on the first failure, [ValidationResult.Invalid] can
 * carry *multiple* [DomainError.ValidationError]s at once — exactly what
 * a form with several simultaneously-invalid fields needs to show the
 * user every problem in one pass rather than one at a time.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<DomainError.ValidationError>) : ValidationResult {
        init {
            require(errors.isNotEmpty()) { "ValidationResult.Invalid must carry at least one error" }
        }
    }

    companion object {
        /** Builds a [Valid] result if [errors] is empty, otherwise an [Invalid] carrying them. */
        fun of(errors: List<DomainError.ValidationError>): ValidationResult =
            if (errors.isEmpty()) Valid else Invalid(errors)
    }
}

/** True for [ValidationResult.Valid]. */
val ValidationResult.isValid: Boolean
    get() = this is ValidationResult.Valid

/** The errors carried by a [ValidationResult.Invalid], or an empty list for [ValidationResult.Valid]. */
val ValidationResult.validationErrors: List<DomainError.ValidationError>
    get() =
        when (this) {
            is ValidationResult.Valid -> emptyList()
            is ValidationResult.Invalid -> errors
        }
