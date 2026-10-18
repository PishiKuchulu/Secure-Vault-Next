package com.securevault.next.core.domain.usecase

import com.securevault.next.core.domain.model.valueobject.PasswordStrength

/**
 * The parameters controlling generated password composition. All
 * character-class toggles default to `true` so the out-of-the-box
 * behavior is "generate the strongest reasonable password", with the
 * caller opting *out* of a class rather than needing to opt into
 * everything.
 */
data class PasswordGenerationOptions(
    val length: Int = DEFAULT_LENGTH,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguousCharacters: Boolean = false
) {
    init {
        require(length in MIN_LENGTH..MAX_LENGTH) {
            "PasswordGenerationOptions.length must be between $MIN_LENGTH and $MAX_LENGTH, was $length"
        }
        require(includeUppercase || includeLowercase || includeDigits || includeSymbols) {
            "PasswordGenerationOptions must include at least one character class"
        }
    }

    companion object {
        const val DEFAULT_LENGTH = 20
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 128
    }
}

/** A freshly generated password value together with its evaluated strength. */
data class GeneratedPassword(val value: String, val strength: PasswordStrength)

/**
 * Generates a new random password.
 *
 * `core:domain` never performs the actual random generation here — this
 * is a contract only; a secure implementation backed by
 * `java.security.SecureRandom` (or the Android-specific equivalent)
 * lands in `core:crypto` starting Milestone 3. Deliberately *not*
 * `suspend`: password generation is expected to be fast and CPU-bound,
 * with no IO or long-running work involved.
 */
fun interface GeneratePasswordUseCase {
    operator fun invoke(options: PasswordGenerationOptions): GeneratedPassword
}

/**
 * Evaluates the strength of an arbitrary candidate password (not
 * necessarily a master password — see
 * [com.securevault.next.core.domain.validation.MasterPasswordValidator]
 * for that specific, policy-driven check). Accepts a [CharArray] for the
 * same zeroing reasons documented on the vault lifecycle use cases.
 */
fun interface ValidatePasswordUseCase {
    operator fun invoke(password: CharArray): PasswordStrength
}
