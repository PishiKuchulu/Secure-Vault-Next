package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.repository.ApiKeyRepository
import com.securevault.next.core.domain.repository.BankAccountRepository
import com.securevault.next.core.domain.repository.CreditCardRepository
import com.securevault.next.core.domain.repository.IdentityRepository
import com.securevault.next.core.domain.repository.NotesRepository
import com.securevault.next.core.domain.repository.PasswordRepository
import com.securevault.next.core.domain.repository.RecoveryCodeRepository
import com.securevault.next.core.domain.repository.SoftwareLicenseRepository
import com.securevault.next.core.domain.repository.SshKeyRepository
import com.securevault.next.core.domain.repository.TotpRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.RestoreItemUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The [RestoreItemUseCase] implementation (Finding M10).
 *
 * Item persistence is per-type (ten repositories, no generic one) while
 * the contract only carries an [ItemId], so the owning repository is
 * located by probing each type's `restoreItem` until one succeeds — the
 * same try-all shape [DeleteItemUseCaseImpl] documents for deletes.
 * Repositories that do not hold the item report a failure that is
 * skipped; the first success wins.
 */
class RestoreItemUseCaseImpl
    @Inject
    constructor(
        private val passwordRepository: PasswordRepository,
        private val notesRepository: NotesRepository,
        private val creditCardRepository: CreditCardRepository,
        private val bankAccountRepository: BankAccountRepository,
        private val identityRepository: IdentityRepository,
        private val softwareLicenseRepository: SoftwareLicenseRepository,
        private val apiKeyRepository: ApiKeyRepository,
        private val sshKeyRepository: SshKeyRepository,
        private val recoveryCodeRepository: RecoveryCodeRepository,
        private val totpRepository: TotpRepository,
        @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher,
    ) : RestoreItemUseCase {
        override suspend fun invoke(itemId: ItemId): DomainResult<VaultItem> =
            withContext(ioDispatcher) {
                val attempts: List<suspend () -> DomainResult<VaultItem>> =
                    listOf(
                        { passwordRepository.restoreItem(itemId) },
                        { notesRepository.restoreItem(itemId) },
                        { creditCardRepository.restoreItem(itemId) },
                        { bankAccountRepository.restoreItem(itemId) },
                        { identityRepository.restoreItem(itemId) },
                        { softwareLicenseRepository.restoreItem(itemId) },
                        { apiKeyRepository.restoreItem(itemId) },
                        { sshKeyRepository.restoreItem(itemId) },
                        { recoveryCodeRepository.restoreItem(itemId) },
                        { totpRepository.restoreItem(itemId) },
                    )

                var lastError: DomainError =
                    DomainError.RepositoryError.NotFound(entityId = itemId.value, entityType = "item")
                for (attempt in attempts) {
                    when (val result = attempt()) {
                        is Result.Success -> return@withContext result
                        is Result.Failure -> lastError = result.error
                    }
                }
                DomainResult.failure(lastError)
            }
    }
