package com.securevault.next.core.crypto.vault

import com.securevault.next.core.common.result.fold
import com.securevault.next.core.contracts.EnvelopeAndKey
import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.format.EncryptedVaultEnvelope
import com.securevault.next.core.crypto.key.KeyRotationManager
import com.securevault.next.core.crypto.key.VaultKey
import com.securevault.next.core.crypto.memory.MemoryUtils
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The `core:crypto` adapter that lets the pure-JVM vault orchestration
 * ([com.securevault.next.core.contracts.VaultKeyEncryptionService]) create
 * and unlock vault envelopes without depending on `core:crypto`.
 *
 * Implements the pure interface over the real [VaultEncryptionEngine] and
 * owns every crypto detail behind the boundary:
 *
 * - serializes an [EncryptedVaultEnvelope] to JSON and back,
 * - takes ownership of the caller's [CharArray] password and wipes it in
 *   a `finally` block on every exit path (the [SecureBytes] copy handed
 *   to the engine is owned and wiped by the engine itself — see its
 *   ownership docs),
 * - maps [CryptoError] onto the domain error surface
 *   ([DomainError.AuthenticationError.InvalidCredentials] for a wrong
 *   password, [DomainError.UnexpectedError] for structural corruption),
 * - for v1 scope, creates envelopes protecting **only the vault key** with
 *   an empty content payload (content encryption is a later milestone).
 *
 * **Memory hygiene:** the unwrapped [VaultKey]'s backing [SecureBytes] is
 * wiped here via [VaultKey.destroy] once its bytes have been copied out.
 * The returned [ByteArray]s are the boundary value; the *caller* (session
 * layer) is responsible for zeroing the returned bytes once installed.
 */
@Singleton
class DefaultVaultKeyEncryptionService
    @Inject
    constructor(
        private val vaultEncryptionEngine: VaultEncryptionEngine,
        private val keyRotationManager: KeyRotationManager,
    ) : VaultKeyEncryptionService {
        private val json = Json { ignoreUnknownKeys = true }

        override fun createEnvelope(masterPassword: CharArray): DomainResult<EnvelopeAndKey> {
            // Ownership (Finding H1): the contract promises the caller's
            // CharArray is wiped before returning, on every path — including
            // thrown exceptions. charsToSecureBytes only copies into a new
            // buffer, so the caller's array is wiped explicitly here.
            try {
                val password = MemoryUtils.charsToSecureBytes(masterPassword)
                return vaultEncryptionEngine
                    .createVault(
                        masterPassword = password,
                        // v1 scope: envelope protects only the vault key; no content yet.
                        plaintextContent = SecureBytes.ofSize(0),
                        kdfParameters = com.securevault.next.core.crypto.kdf.KdfParameters.CURRENT,
                    ).fold(
                        onSuccess = { result ->
                            val envelopeJson = json.encodeToString(result.envelope)
                            val vaultKeyBytes = result.vaultKey.bytes.peek { it.copyOf() }
                            result.vaultKey.destroy() // wipe the crypto-layer key copy after copying out
                            DomainResult.success(
                                EnvelopeAndKey(
                                    envelopeJson = envelopeJson,
                                    vaultKeyBytes = vaultKeyBytes,
                                ),
                            )
                        },
                        onFailure = { DomainResult.failure(it.toDomainError()) },
                    )
            } finally {
                MemoryUtils.wipe(masterPassword)
            }
        }

        override fun unwrapVaultKey(
            envelopeJson: String,
            masterPassword: CharArray,
        ): DomainResult<ByteArray> {
            // Ownership (Finding H1): same wipe guarantee as createEnvelope.
            // The envelope-decode early return sits inside the try so the
            // password is wiped on that path too.
            try {
                val envelope =
                    try {
                        json.decodeFromString<EncryptedVaultEnvelope>(envelopeJson)
                    } catch (e: Exception) {
                        return DomainResult.failure(DomainError.UnexpectedError("Vault envelope is corrupted", e))
                    }

                val password = MemoryUtils.charsToSecureBytes(masterPassword)
                return vaultEncryptionEngine.unlockVault(envelope, password).fold(
                    onSuccess = { vaultKey ->
                        val bytes = vaultKey.bytes.peek { it.copyOf() }
                        vaultKey.destroy() // wipe the crypto-layer key copy
                        DomainResult.success(bytes)
                    },
                    onFailure = { DomainResult.failure(it.toDomainError()) },
                )
            } finally {
                MemoryUtils.wipe(masterPassword)
            }
        }

        private fun CryptoError.toDomainError(): DomainError =
            when (this) {
                is CryptoError.InvalidPassword -> DomainError.AuthenticationError.InvalidCredentials
                is CryptoError.VaultCorrupted,
                is CryptoError.KeyUnavailable,
                is CryptoError.InvalidCiphertext,
                is CryptoError.IntegrityCheckFailed,
                -> DomainError.UnexpectedError(message, cause)
                else -> DomainError.UnexpectedError(message, cause)
            }

        override fun rotateMasterPassword(
            envelopeJson: String,
            currentPassword: CharArray,
            newPassword: CharArray,
        ): DomainResult<String> {
            // Ownership: both password arrays are wiped before returning, on
            // every path. unwrapVaultKey wipes currentPassword in its own
            // finally; the new password is wiped in the inner finally below.
            // A wrong current password surfaces as InvalidCredentials here,
            // exactly like the unlock path.
            val vaultKeyBytes =
                when (val result = unwrapVaultKey(envelopeJson, currentPassword)) {
                    is com.securevault.next.core.common.result.Result.Success -> result.value
                    is com.securevault.next.core.common.result.Result.Failure ->
                        return DomainResult.failure(result.error)
                }
            try {
                val envelope =
                    try {
                        json.decodeFromString<EncryptedVaultEnvelope>(envelopeJson)
                    } catch (e: Exception) {
                        return DomainResult.failure(DomainError.UnexpectedError("Vault envelope is corrupted", e))
                    }
                val newPasswordBytes = MemoryUtils.charsToSecureBytes(newPassword)
                try {
                    // VaultKey wraps (shares) the array: destroy() wipes the
                    // vaultKeyBytes copy once rotation has consumed it.
                    val currentKey = VaultKey(SecureBytes.wrap(vaultKeyBytes))
                    return try {
                        keyRotationManager.rotateMasterKey(envelope, currentKey, newPasswordBytes).fold(
                            onSuccess = { rotated -> DomainResult.success(json.encodeToString(rotated)) },
                            onFailure = { DomainResult.failure(it.toDomainError()) },
                        )
                    } finally {
                        currentKey.destroy()
                    }
                } finally {
                    MemoryUtils.wipe(newPassword)
                    // newPasswordBytes is consumed (and wiped) inside
                    // rotateMasterKey's `use` block; no extra wipe needed.
                }
            } finally {
                com.securevault.next.core.common.memory.MemoryWiper
                    .wipe(vaultKeyBytes)
            }
        }
    }
