package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.domain.model.isValid
import com.securevault.next.core.domain.model.validationErrors
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.ChangeMasterPasswordUseCase
import com.securevault.next.core.domain.validation.MasterPasswordValidator
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Changes a vault's master password.
 *
 * Flow: validate the new password against the policy (Finding C4 gate —
 * min 12, four classes, max 128 as an Argon2id DoS guard), then delegate
 * to [VaultKeyEncryptionService.rotateMasterPassword], which verifies the
 * current password, re-wraps the unchanged vault key and returns the new
 * envelope; the rotated envelope is persisted via
 * [VaultRepository.updateEnvelopeJson].
 *
 * The vault key itself never changes, so an active session (and any
 * biometric enrollment wrapping the session key) stays valid across the
 * rotation. Both password arrays are wiped by the crypto adapter on every
 * path; this use case never retains them.
 */
class ChangeMasterPasswordUseCaseImpl @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val vaultKeyEncryptionService: VaultKeyEncryptionService,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher,
) : ChangeMasterPasswordUseCase {

    // Instantiated directly, like CreateVaultUseCaseImpl: a stateless
    // pure-domain value with fixed defaults, not a service.
    private val masterPasswordValidator = MasterPasswordValidator()

    override suspend fun invoke(
        vaultId: VaultId,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): DomainResult<Unit> = withContext(ioDispatcher) {
        val validation = masterPasswordValidator.validate(newPassword)
        if (!validation.isValid) {
            return@withContext DomainResult.failure(validation.validationErrors.first())
        }

        val envelopeJson = when (val result = vaultRepository.getEnvelopeJson(vaultId)) {
            is Result.Success ->
                result.value ?: return@withContext DomainResult.failure(
                    com.securevault.next.core.domain.error.DomainError.RepositoryError.NotFound(
                        entityId = vaultId.value,
                        entityType = "vault envelope",
                    ),
                )
            is Result.Failure -> return@withContext DomainResult.failure(result.error)
        }

        // rotateMasterPassword verifies the current password
        // (InvalidCredentials on mismatch) and wipes both arrays.
        return@withContext when (
            val rotated = vaultKeyEncryptionService.rotateMasterPassword(
                envelopeJson = envelopeJson,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        ) {
            is Result.Success ->
                when (val updated = vaultRepository.updateEnvelopeJson(vaultId, rotated.value)) {
                    is Result.Success -> DomainResult.success(Unit)
                    is Result.Failure -> DomainResult.failure(updated.error)
                }
            is Result.Failure -> DomainResult.failure(rotated.error)
        }
    }
}
