package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * An active unlocked session for a vault.
 *
 * This is the payload attached to [VaultState.Unlocked] — it exists
 * separately from [VaultState] itself so that auto-lock timers and
 * activity tracking (updating [lastActivityAt]) can operate on a session
 * value without needing to reconstruct the entire state machine.
 */
data class VaultSession(
    val vaultId: VaultId,
    val sessionId: String,
    val authenticationMethod: AuthenticationMethod,
    val unlockedAt: Instant,
    val lastActivityAt: Instant,
    val expiresAt: Instant? = null
) {
    init {
        require(sessionId.isNotBlank()) { "VaultSession.sessionId must not be blank" }
    }
}
