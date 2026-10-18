package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.domain.model.enums.ClipboardTimeout

/**
 * The security rules enforced on a vault: how it locks, how many failed
 * unlock attempts are tolerated, which authentication methods are
 * accepted.
 *
 * Kept distinct from [VaultSettings], which holds pure user-facing
 * preferences (theme, sort order) with no security consequence — the
 * separation means a future "reset to secure defaults" action can reset
 * [VaultSecurityPolicy] alone without touching a user's cosmetic
 * preferences.
 */
data class VaultSecurityPolicy(
    val autoLockPolicy: AutoLockPolicy = AutoLockPolicy.MINUTES_5,
    val clipboardTimeout: ClipboardTimeout = ClipboardTimeout.SECONDS_30,
    val requireBiometric: Boolean = false,
    val maxFailedUnlockAttempts: Int = 5,
    val authenticationMethods: Set<AuthenticationMethod> = setOf(AuthenticationMethod.MASTER_PASSWORD),
    val minMasterPasswordLength: Int = 12
) {
    init {
        require(maxFailedUnlockAttempts > 0) {
            "VaultSecurityPolicy.maxFailedUnlockAttempts must be positive, was $maxFailedUnlockAttempts"
        }
        require(authenticationMethods.isNotEmpty()) {
            "VaultSecurityPolicy.authenticationMethods must not be empty"
        }
        require(AuthenticationMethod.MASTER_PASSWORD in authenticationMethods) {
            "VaultSecurityPolicy.authenticationMethods must always include MASTER_PASSWORD as a fallback"
        }
        require(minMasterPasswordLength >= 8) {
            "VaultSecurityPolicy.minMasterPasswordLength must be at least 8, was $minMasterPasswordLength"
        }
    }
}
