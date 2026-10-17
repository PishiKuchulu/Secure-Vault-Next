package com.securevault.next.core.domain.validation

import com.securevault.next.core.domain.model.ValidationResult

/**
 * Validates a value of type [T], producing a [ValidationResult] that can
 * carry every problem found at once rather than stopping at the first.
 *
 * A `fun interface` so simple validators can be written as a lambda
 * (`Validator<String> { ... }`) in tests, while the concrete validators
 * in this package are still full classes where the extra structure
 * (configurable limits, multiple checks) earns it.
 */
fun interface Validator<in T> {
    fun validate(value: T): ValidationResult
}
