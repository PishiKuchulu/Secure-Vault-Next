package com.securevault.next.core.domain.model.valueobject

/**
 * A vault's display name.
 *
 * The bounds enforced here (non-blank, ≤100 characters) are the bare
 * structural invariant that makes an invalid [VaultName] unrepresentable
 * at the type level. This is deliberately narrower than what
 * `core.domain.validation.VaultNameValidator` checks — the validator
 * exists to produce rich, user-facing [com.securevault.next.core.domain.model.ValidationResult]
 * feedback (e.g. for a form with multiple simultaneous errors), while
 * this constructor-time check is the last-resort guarantee that a
 * [VaultName] can never exist in an invalid state, no matter which code
 * path constructed it.
 */
@JvmInline
value class VaultName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "VaultName must not be blank" }
        require(value.length <= MAX_LENGTH) { "VaultName must be at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 100
    }
}
