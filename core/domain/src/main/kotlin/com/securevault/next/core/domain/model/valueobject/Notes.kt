package com.securevault.next.core.domain.model.valueobject

/**
 * Free-text notes attached to a vault item. A separate type from a bare
 * `String` mainly for consistency with every other user-entered field in
 * the domain model, and to give notes a single place to enforce a length
 * ceiling.
 */
@JvmInline
value class Notes(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { "Notes must be at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 10_000
    }
}
