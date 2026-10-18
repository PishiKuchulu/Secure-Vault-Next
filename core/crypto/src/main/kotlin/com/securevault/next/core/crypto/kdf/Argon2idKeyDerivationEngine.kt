package com.securevault.next.core.crypto.kdf

import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.crypto.result.CryptoResult
import com.securevault.next.core.crypto.result.asCryptoFailure
import com.securevault.next.core.crypto.result.asCryptoSuccess
import kotlinx.coroutines.CancellationException
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import javax.inject.Inject

/**
 * The production [KeyDerivationEngine], implementing Argon2id via
 * Bouncy Castle's [Argon2BytesGenerator].
 *
 * This uses the Bouncy Castle classes *directly* — `Argon2BytesGenerator().init(...)`
 * / `generateBytes(...)` — rather than registering Bouncy Castle as a
 * JCA [java.security.Security] provider. Android ships its own
 * stripped-down BC provider under the same name, and registering a
 * second one is a well-known source of provider-priority conflicts on
 * Android; calling the generator class directly sidesteps that
 * entirely, since no `Cipher.getInstance("...", "BC")`-style provider
 * lookup is involved.
 */
class Argon2idKeyDerivationEngine
    @Inject
    constructor() : KeyDerivationEngine {
        override fun derive(
            password: SecureBytes,
            salt: SecureBytes,
            parameters: KdfParameters,
        ): CryptoResult<SecureBytes> {
            if (parameters.algorithm != KdfAlgorithm.ARGON2ID) {
                return CryptoError
                    .CryptoFailure(
                        "Argon2idKeyDerivationEngine only supports ${KdfAlgorithm.ARGON2ID}, was ${parameters.algorithm}",
                    ).asCryptoFailure()
            }

            return try {
                val output =
                    password.peek { passwordBytes ->
                        salt.peek { saltBytes ->
                            val builtParameters =
                                Argon2Parameters
                                    .Builder(Argon2Parameters.ARGON2_id)
                                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                                    .withIterations(parameters.iterations)
                                    .withMemoryAsKB(parameters.memoryCostKib)
                                    .withParallelism(parameters.parallelism)
                                    .withSalt(saltBytes)
                                    .build()

                            val generator = Argon2BytesGenerator()
                            generator.init(builtParameters)

                            val result = ByteArray(parameters.outputLengthBytes)
                            generator.generateBytes(passwordBytes, result)
                            result
                        }
                    }
                SecureBytes.wrap(output).asCryptoSuccess()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                CryptoError.CryptoFailure("Argon2id key derivation failed", t).asCryptoFailure()
            }
        }
    }
