package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.TagId
import com.securevault.next.core.domain.model.valueobject.VaultId

/**
 * A lightweight, non-hierarchical label an item can carry any number of
 * (an item has a single [com.securevault.next.core.domain.model.VaultItemMetadata.categoryId]
 * but any number of tag IDs) — the many-to-many counterpart to
 * [Category]'s strict one-parent-per-item grouping.
 */
data class Tag(
    val id: TagId,
    val vaultId: VaultId,
    val name: String,
    val colorHex: String? = null,
    val createdAt: CreatedAt,
) {
    init {
        require(name.isNotBlank()) { "Tag.name must not be blank" }
    }
}
