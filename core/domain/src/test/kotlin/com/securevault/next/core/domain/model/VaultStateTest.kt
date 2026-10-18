package com.securevault.next.core.domain.model

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.VaultId
import org.junit.Test
import java.time.Instant

class VaultStateTest {
    private val vaultId = VaultId("vault-1")

    @Test
    fun `vaultId extension resolves correctly for every state variant`() {
        val session =
            VaultSession(
                vaultId = vaultId,
                sessionId = "session-1",
                authenticationMethod = AuthenticationMethod.MASTER_PASSWORD,
                unlockedAt = Instant.EPOCH,
                lastActivityAt = Instant.EPOCH,
            )

        val states: List<VaultState> =
            listOf(
                VaultState.Locked(vaultId),
                VaultState.Opening(vaultId),
                VaultState.Unlocked(session),
                VaultState.Closing(vaultId),
                VaultState.Importing(vaultId, progress = 0.5f),
                VaultState.Exporting(vaultId, progress = 0.5f),
                VaultState.BackingUp(vaultId, progress = 0.5f),
                VaultState.Restoring(vaultId, progress = 0.5f),
            )

        states.forEach { state ->
            assertThat(state.vaultId).isEqualTo(vaultId)
        }
    }

    @Test
    fun `progress-bearing states reject an out-of-range progress value`() {
        var threw = false
        try {
            VaultState.Importing(vaultId, progress = 1.5f)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }
}
