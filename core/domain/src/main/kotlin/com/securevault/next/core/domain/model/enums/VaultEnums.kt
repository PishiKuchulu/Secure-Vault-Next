package com.securevault.next.core.domain.model.enums

/**
 * A vault's coarse, persisted status — distinct from
 * [com.securevault.next.core.domain.model.VaultState], which models the
 * detailed *runtime* lifecycle (locked/unlocked/opening/...) of a single
 * active session. [VaultStatus] is what a vault list screen would show
 * for every vault on the device, including ones that are not currently
 * open at all.
 */
enum class VaultStatus {
    ACTIVE,
    ARCHIVED,
    LOCKED_OUT,
    CORRUPTED,
    PENDING_DELETION,
}

/** The synchronization status of an entity, for a future multi-device sync feature. */
enum class SyncState {
    NOT_SYNCED,
    SYNCING,
    SYNCED,
    CONFLICT,
    FAILED,
}
