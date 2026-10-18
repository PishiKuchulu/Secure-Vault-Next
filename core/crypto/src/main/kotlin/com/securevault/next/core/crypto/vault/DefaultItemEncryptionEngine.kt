package com.securevault.next.core.crypto.vault

import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.common.result.isSuccess
import com.securevault.next.core.crypto.cipher.CipherEngine
import com.securevault.next.core.crypto.key.SessionKeyHolder
import com.securevault.next.core.crypto.memory.MemoryUtils
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.result.asDomainFailure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultItemEncryptionEngine
@Inject
constructor(
    private val sessionKeyHolder: SessionKeyHolder,
    private val cipherEngine: CipherEngine
) : ItemEncryptionEngine {
    override suspend fun decryptField(field: EncryptedField): DomainResult<CharArray> {
        val sessionKey =
            sessionKeyHolder.get()
                ?: return DomainResult.failure(DomainError.CryptoRequiredError.KeyDerivationRequired)

        return try {
            val decryptedResult =
                cipherEngine.decrypt(
                    encryptedField = field,
                    key = sessionKey.bytes
                )

            if (decryptedResult.isSuccess) {
                val decrypted = decryptedResult.getOrNull()!!
                // Finding L6: decode with the same explicit UTF-8 the
                // encrypt path uses (never the platform default), and
                // keep the transient String confined to this peek block
                // so it becomes unreachable as soon as the CharArray
                // copy is made.
                val result =
                    decrypted.peek { revealed ->
                        String(revealed, Charsets.UTF_8).toCharArray()
                    }

                // Production Hardening: Wipe sensitive raw bytes immediately
                decrypted.close()

                DomainResult.success(result)
            } else {
                DomainResult.failure(DomainError.CryptoRequiredError.DecryptionFailed)
            }
        } catch (e: Exception) {
            DomainResult.failure(DomainError.CryptoRequiredError.DecryptionFailed)
        }
    }

    override suspend fun encryptField(plaintext: CharArray): DomainResult<EncryptedField> {
        val sessionKey =
            sessionKeyHolder.get()
                ?: return DomainResult.failure(DomainError.CryptoRequiredError.KeyDerivationRequired)

        // plaintext is encrypted as a SecureBytes view so it is never
        // materialized as a String. MemoryUtils.charsToSecureBytes encodes
        // UTF-8 without allocating a String; the resulting SecureBytes is
        // wiped as soon as encryption has consumed it (see use {} below).
        MemoryUtils.charsToSecureBytes(plaintext).use { plaintextBytes ->
            val result =
                cipherEngine.encrypt(
                    plaintext = plaintextBytes,
                    key = sessionKey.bytes,
                    associatedData = ByteArray(0)
                )
            return when (result) {
                is com.securevault.next.core.common.result.Result.Success -> {
                    DomainResult.success(result.value)
                }
                is com.securevault.next.core.common.result.Result.Failure -> {
                    DomainError.CryptoRequiredError.EncryptionUnavailable.asDomainFailure()
                }
            }
        }
    }
}
