package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId

/**
 * A user-defined grouping for items (e.g. "Work", "Banking",
 * "Streaming"). [parentCategoryId] allows a single level or many levels
 * of nesting (e.g. "Work" → "Work / Engineering"); it is left as a
 * plain [CategoryId] rather than a closed depth limit, since enforcing a
 * maximum nesting depth is a UI/UX decision, not a domain invariant.
 */
data class Category(
    val id: CategoryId,
    val vaultId: VaultId,
    val name: String,
    val icon: String? = null,
    val colorHex: String? = null,
    val parentCategoryId: CategoryId? = null,
    val createdAt: CreatedAt,
    val updatedAt: UpdatedAt
) {
    init {
        require(name.isNotBlank()) { "Category.name must not be blank" }
        require(parentCategoryId != id) { "Category cannot be its own parent" }
    }
}
