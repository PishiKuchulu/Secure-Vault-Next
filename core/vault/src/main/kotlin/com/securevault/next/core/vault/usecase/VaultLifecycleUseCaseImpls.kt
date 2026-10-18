package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.enums.VaultStatus
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.usecase.CreateVaultUseCase
import com.securevault.next.core.domain.usecase.OpenVaultUseCase
import com.securevault.next.core.domain.validation.MasterPasswordValidator
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CreateVaultUseCaseImpl
@Inject
constructor(
    private val vaultRepository: VaultRepository,
    private val vaultKeyEncryptionService: VaultKeyEncryptionService,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : CreateVaultUseCase {
    // The master-password policy validator. Instantiated directly
    // rather than injected: it is a stateless pure-domain value
    // with fixed defaults (min 12, max 128), not a service.
    private val masterPasswordValidator = MasterPasswordValidator()

    override suspend fun invoke(
        name: VaultName,
        masterPassword: CharArray,
        securityPolicy: VaultSecurityPolicy,
        settings: VaultSettings
    ): DomainResult<Vault> = withContext(ioDispatcher) {
        // Policy gate (Finding C4): the master-password policy is
        // enforced here, BEFORE any key derivation runs. A weak or
        // over-long password (the length cap is an Argon2id DoS
        // guard) is rejected with a typed validation error instead
        // of being silently accepted into a new vault.
        val validationResult = masterPasswordValidator.validate(masterPassword)
        if (!validationResult.isValid) {
            return@withContext DomainResult.failure(validationResult.validationErrors.first())
        }

        val vaultId = VaultId(UUID.randomUUID().toString())
        val now = Instant.now()
        val metadata =
            VaultMetadata(
                id = vaultId,
                name = name,
                status = VaultStatus.ACTIVE,
                version = Version(1),
                revision = Revision.INITIAL,
                createdAt = CreatedAt(now),
                updatedAt = UpdatedAt(now)
            )

        // Derive the wrapped vault key and serialize the envelope. The
        // adapter takes ownership of masterPassword (wiping it) and hands
        // back only the serialized envelope plus a temporary key buffer.
        when (val envelopeResult = vaultKeyEncryptionService.createEnvelope(masterPassword)) {
            is com.securevault.next.core.common.result.Result.Success -> {
                val envelopeAndKey = envelopeResult.value
                val envelopeJson = envelopeAndKey.envelopeJson
                val temporaryKey = envelopeAndKey.vaultKeyBytes
                try {
                    // Persist ONLY the envelope. The raw vault key is never
                    // written to disk; its temporary byte buffer is wiped
                    // immediately after the (for create) no-op key handoff.
                    vaultRepository.createVault(metadata, securityPolicy, settings, envelopeJson)
                } finally {
                    MemoryWiper.wipe(temporaryKey)
                }
            }
            is com.securevault.next.core.common.result.Result.Failure -> {
                DomainResult.failure(envelopeResult.error)
            }
        }
    }
}

class OpenVaultUseCaseImpl
@Inject
constructor(
    private val vaultRepository: VaultRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : OpenVaultUseCase {
    override suspend fun invoke(vaultId: VaultId): DomainResult<VaultMetadata> = withContext(ioDispatcher) {
        vaultRepository.getVaultMetadata(vaultId)
    }
}
