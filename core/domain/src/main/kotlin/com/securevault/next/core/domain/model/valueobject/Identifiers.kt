package com.securevault.next.core.domain.model.valueobject

import kotlinx.serialization.Serializable

/**
 * Strongly-typed identifiers.
 *
 * Part 2 (Value Objects): "Avoid primitive obsession." Without these,
 * every function signature dealing with entity identity would take a
 * bare `String`, and nothing would stop a caller from accidentally
 * passing a [TagId] where a [CategoryId] was expected — both compile
 * fine as `String`, but only one is correct. Each of these wraps a
 * single `String` value and is a distinct, non-interchangeable type.
 *
 * All of them require a non-blank value: an identifier that is blank is
 * never valid, so that invariant is enforced structurally rather than by
 * convention at every call site.
 *
 * [ItemId] carries [Serializable] (as its underlying `String`, the
 * standard kotlinx.serialization representation of an inline value
 * class) because `data:repository`'s audit persistence decodes
 * `List<ItemId>` from JSON.
 */
data class VaultId(val value: String) {
    init {
        require(value.isNotBlank()) { "VaultId must not be blank" }
    }
}

@Serializable
data class ItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ItemId must not be blank" }
    }
}

data class CategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryId must not be blank" }
    }
}

data class TagId(val value: String) {
    init {
        require(value.isNotBlank()) { "TagId must not be blank" }
    }
}

/**
 * Identifies a single historical password value stored in
 * [com.securevault.next.core.domain.model.PasswordHistory] — distinct
 * from [ItemId], which identifies the [com.securevault.next.core.domain.model.item.PasswordItem]
 * the history entry belongs to.
 */
data class PasswordId(val value: String) {
    init {
        require(value.isNotBlank()) { "PasswordId must not be blank" }
    }
}
