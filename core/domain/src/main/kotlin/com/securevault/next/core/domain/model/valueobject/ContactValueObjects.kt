package com.securevault.next.core.domain.model.valueobject

/**
 * An email address associated with an item (e.g. a login username).
 *
 * Only a minimal structural check (must contain `@`, must not be blank)
 * is enforced here. Rich, user-facing format validation with specific
 * error messages belongs to `core.domain.validation.FieldValidator`,
 * which can report multiple problems at once for a form; this
 * constructor-time check exists only to make a completely nonsensical
 * [EmailAddress] (blank, or missing `@` entirely) unrepresentable.
 */
@JvmInline
value class EmailAddress(val value: String) {
    init {
        require(value.isNotBlank()) { "EmailAddress must not be blank" }
        require(value.contains("@")) { "EmailAddress must contain '@'" }
    }
}

/**
 * A URL associated with an item (e.g. the login page for a
 * [com.securevault.next.core.domain.model.item.PasswordItem]).
 */
@JvmInline
value class UrlValue(val value: String) {
    init {
        require(value.isNotBlank()) { "UrlValue must not be blank" }
    }
}

/** A phone number, stored as entered rather than normalized to a specific regional format. */
@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "PhoneNumber must not be blank" }
    }
}
