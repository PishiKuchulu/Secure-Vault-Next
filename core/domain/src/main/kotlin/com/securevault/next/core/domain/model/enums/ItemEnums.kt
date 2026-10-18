package com.securevault.next.core.domain.model.enums

/**
 * The concrete kind of a [com.securevault.next.core.domain.model.item.VaultItem].
 * Every item subtype sets its own [com.securevault.next.core.domain.model.item.VaultItem.type]
 * to exactly one of these; a `when` over this enum in a UI layer is
 * exhaustive without an `else` branch.
 */
enum class ItemType {
    PASSWORD,
    SECURE_NOTE,
    CREDIT_CARD,
    BANK_ACCOUNT,
    IDENTITY_DOCUMENT,
    SOFTWARE_LICENSE,
    API_KEY,
    SSH_KEY,
    RECOVERY_CODE,
    TOTP_SEED
}

/**
 * A coarse-grained sensitivity classification, independent of
 * [PasswordStrengthLevel] — this describes how damaging exposure of an
 * item would be (e.g. a bank account is [CRITICAL] regardless of how
 * "strong" any password on it is), whereas [PasswordStrengthLevel]
 * describes how hard a specific password is to guess or crack.
 */
enum class SecurityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/** A human-facing categorization of password strength. */
enum class PasswordStrengthLevel {
    VERY_WEAK,
    WEAK,
    FAIR,
    STRONG,
    VERY_STRONG
}
