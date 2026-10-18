package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.search.SearchResult
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.SearchRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.SearchItemsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The [SearchItemsUseCase] implementation (Finding M10): a thin
 * dispatcher-hop wrapper delegating to [SearchRepository], matching the
 * shape of the other single-repository use cases.
 */
class SearchItemsUseCaseImpl @Inject constructor(
    private val searchRepository: SearchRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : SearchItemsUseCase {
    override suspend fun invoke(vaultId: VaultId, filter: SearchFilter): DomainResult<SearchResult> =
        withContext(ioDispatcher) {
            searchRepository.search(vaultId, filter)
        }
}
