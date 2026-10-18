package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.VaultId

/**
 * The runtime lifecycle state of a single vault, mirroring the flow
 * described in Part 9 (Application Flow): a vault starts [Locked],
 * transitions through [Opening] while its metadata loads, becomes
 * [Unlocked] once authentication succeeds, and can independently be
 * [Importing], [Exporting], [BackingUp] or [Restoring] — those four are
 * long-running operations a vault can be in, tracked with a
 * [Importing.progress]-style fraction, rather than states the vault is
 * otherwise stuck in.
 *
 * This is a sealed interface of data classes rather than an enum because
 * most states carry state-specific data (which vault, how far a
 * long-running operation has progressed, or the full [VaultSession] once
 * unlocked) that an enum constant cannot hold.
 */
sealed interface VaultState {
    data class Locked(
        val vaultId: VaultId,
    ) : VaultState

    data class Opening(
        val vaultId: VaultId,
    ) : VaultState

    data class Unlocked(
        val session: VaultSession,
    ) : VaultState

    data class Closing(
        val vaultId: VaultId,
    ) : VaultState

    data class Importing(
        val vaultId: VaultId,
        val progress: Float,
    ) : VaultState {
        init {
            require(progress in 0f..1f) { "Importing.progress must be between 0 and 1, was $progress" }
        }
    }

    data class Exporting(
        val vaultId: VaultId,
        val progress: Float,
    ) : VaultState {
        init {
            require(progress in 0f..1f) { "Exporting.progress must be between 0 and 1, was $progress" }
        }
    }

    data class BackingUp(
        val vaultId: VaultId,
        val progress: Float,
    ) : VaultState {
        init {
            require(progress in 0f..1f) { "BackingUp.progress must be between 0 and 1, was $progress" }
        }
    }

    data class Restoring(
        val vaultId: VaultId,
        val progress: Float,
    ) : VaultState {
        init {
            require(progress in 0f..1f) { "Restoring.progress must be between 0 and 1, was $progress" }
        }
    }
}

/** The [VaultId] a [VaultState] pertains to, regardless of which state it is. */
val VaultState.vaultId: VaultId
    get() =
        when (val state = this) {
            is VaultState.Locked -> state.vaultId
            is VaultState.Opening -> state.vaultId
            is VaultState.Unlocked -> state.session.vaultId
            is VaultState.Closing -> state.vaultId
            is VaultState.Importing -> state.vaultId
            is VaultState.Exporting -> state.vaultId
            is VaultState.BackingUp -> state.vaultId
            is VaultState.Restoring -> state.vaultId
        }
