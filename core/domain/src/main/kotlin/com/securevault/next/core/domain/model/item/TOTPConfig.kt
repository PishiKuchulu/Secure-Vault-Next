package com.securevault.next.core.domain.model.item

import com.securevault.next.core.domain.model.valueobject.EncryptedField

/**
 * The configuration needed to generate Time-based One-Time Passwords
 * (RFC 6238).
 *
 * Deliberately a plain embeddable value object rather than its own
 * [VaultItem]-shaped entity: [PasswordItem] embeds one directly (the
 * common "password + 2FA" pairing), and [TOTPSeed] wraps one for the
 * case where a user stores a bare TOTP secret with no associated
 * password (e.g. a hardware token or an app-only 2FA entry).
 */
data class TOTPConfig(
    val secret: EncryptedField,
    val issuer: String? = null,
    val accountName: String? = null,
    val algorithm: String = DEFAULT_ALGORITHM,
    val digits: Int = DEFAULT_DIGITS,
    val periodSeconds: Int = DEFAULT_PERIOD_SECONDS,
) {
    init {
        require(digits in MIN_DIGITS..MAX_DIGITS) {
            "TOTPConfig.digits must be between $MIN_DIGITS and $MAX_DIGITS, was $digits"
        }
        require(periodSeconds > 0) { "TOTPConfig.periodSeconds must be positive, was $periodSeconds" }
    }

    companion object {
        const val DEFAULT_ALGORITHM = "SHA1"
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD_SECONDS = 30
        const val MIN_DIGITS = 6
        const val MAX_DIGITS = 8
    }
}
