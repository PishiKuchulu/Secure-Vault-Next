package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.ItemType

/**
 * Anything that can be stored inside a vault.
 *
 * This is a `sealed interface` rather than a `sealed class` because
 * every concrete item type ([PasswordItem], [SecureNote], [CreditCard],
 * ...) needs to be a `data class` embedding its own [VaultItemMetadata]
 * — Kotlin data classes can implement an interface's properties but
 * cannot extend another class's constructor parameters as cleanly, so an
 * interface keeps every item type free to declare `metadata` as its own
 * constructor parameter.
 *
 * A `when` over [type] (or over a sealed `when (this)` on [VaultItem]
 * itself) is exhaustive: the compiler flags any code that doesn't handle
 * a newly added item type.
 */
sealed interface VaultItem {
    val metadata: VaultItemMetadata
    val type: ItemType
}
