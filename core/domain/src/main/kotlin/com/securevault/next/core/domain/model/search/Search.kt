package com.securevault.next.core.domain.model.search

import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.enums.SortField
import com.securevault.next.core.domain.model.enums.SortOrder
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.TagId

/**
 * The parameters of a search or filter operation over a vault's items.
 * Every field is optional/empty-by-default so a "no filter, just sort"
 * search is `SearchFilter(sortField = ...)` rather than needing a
 * separate query shape.
 */
data class SearchFilter(
    val query: String? = null,
    val itemTypes: Set<ItemType> = emptySet(),
    val categoryIds: Set<CategoryId> = emptySet(),
    val tagIds: Set<TagId> = emptySet(),
    val favoritesOnly: Boolean = false,
    val includeTrash: Boolean = false,
    val sortField: SortField = SortField.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
)

/** The result of running a [SearchFilter] against a vault's items. */
data class SearchResult(
    val items: List<VaultItem>,
    val totalCount: Int,
    val filter: SearchFilter,
) {
    init {
        require(totalCount >= 0) { "SearchResult.totalCount must not be negative" }
        require(totalCount >= items.size) {
            "SearchResult.totalCount ($totalCount) must be at least the number of returned items (${items.size})"
        }
    }
}
