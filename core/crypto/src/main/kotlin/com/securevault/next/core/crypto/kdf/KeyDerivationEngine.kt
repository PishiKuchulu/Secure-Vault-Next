package com.securevault.next.core.crypto.kdf

import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult

/**
 * Derives cryptographic key material from a password.
 *
 * Deliberately synchronous, not `suspend`: Argon2id is intentionally
 * CPU/memory-bound and slow (that is its entire purpose as a KDF), which
 * makes it a poor fit for a coroutine dispatcher's cooperative
 * scheduling assumptions. Callers invoke this from a background thread
 * dispatcher explicitly (e.g. `withContext(ioOrDefaultDispatcher)` at
 * the call site) rather than this interface trying to manage that
 * itself.
 */
interface KeyDerivationEngine {
    /**
     * Derives [KdfParameters.outputLengthBytes] bytes of key material
     * from [password] and [salt] using [parameters].
     *
     * The output is intended to be split by the caller into a cipher key
     * and a separate integrity/MAC key (see `core.crypto.vault.VaultEncryptionEngine`)
     * — this function itself is agnostic to how its output bytes are used.
     */
    fun derive(password: SecureBytes, salt: SecureBytes, parameters: KdfParameters): CryptoResult<SecureBytes>
}
