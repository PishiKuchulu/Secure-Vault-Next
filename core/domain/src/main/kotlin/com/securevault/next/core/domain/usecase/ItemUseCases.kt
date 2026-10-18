package com.securevault.next.core.domain.usecase

import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.search.SearchResult
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult

/**
 * Creates a new [VaultItem]. Accepts any concrete item type through the
 * sealed [VaultItem] interface — a single use case contract serves all
 * ten item types rather than needing one per type, since creation
 * doesn't need type-specific behavior the way
 * [com.securevault.next.core.domain.repository.PasswordRepository]'s
 * history tracking does.
 */
fun interface CreateItemUseCase {
    suspend operator fun invoke(item: VaultItem): DomainResult<VaultItem>
}

/** Updates an existing [VaultItem], identified by the [VaultItem.metadata]'s [ItemId]. */
fun interface UpdateItemUseCase {
    suspend operator fun invoke(item: VaultItem): DomainResult<VaultItem>
}

/**
 * Deletes an item. [permanent] defaults to `false`, meaning the item
 * moves to trash (see [com.securevault.next.core.domain.model.TrashItem])
 * rather than being immediately and irreversibly removed.
 */
fun interface DeleteItemUseCase {
    suspend operator fun invoke(itemId: ItemId, permanent: Boolean): DomainResult<Unit>
}

/** Restores a previously soft-deleted item out of trash. */
fun interface RestoreItemUseCase {
    suspend operator fun invoke(itemId: ItemId): DomainResult<VaultItem>
}

/** Runs a [SearchFilter] against a specific vault's items. */
fun interface SearchItemsUseCase {
    suspend operator fun invoke(vaultId: VaultId, filter: SearchFilter): DomainResult<SearchResult>
}
