package com.securevault.next.core.domain.repository

import com.securevault.next.core.domain.model.Category
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.search.SearchResult
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/** Persists and retrieves a vault's [Category] tree. */
interface CategoryRepository {
    fun observeCategories(vaultId: VaultId): Flow<List<Category>>

    suspend fun getCategory(categoryId: CategoryId): DomainResult<Category>

    suspend fun createCategory(category: Category): DomainResult<Category>

    suspend fun updateCategory(category: Category): DomainResult<Category>

    suspend fun deleteCategory(categoryId: CategoryId): DomainResult<Unit>
}

/**
 * Executes search queries over a vault's items.
 *
 * Kept as its own repository, distinct from item-type repositories,
 * because search spans every item type at once — it is the backing
 * contract for [com.securevault.next.core.domain.usecase.SearchItemsUseCase].
 */
interface SearchRepository {
    suspend fun search(
        vaultId: VaultId,
        filter: SearchFilter,
    ): DomainResult<SearchResult>
}
