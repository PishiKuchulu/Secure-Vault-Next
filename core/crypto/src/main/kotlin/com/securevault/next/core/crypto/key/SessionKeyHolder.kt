package com.securevault.next.core.crypto.key

/**
 * Holds the [SessionKey] for an unlocked vault session, and nowhere
 * else — every other piece of code that needs the session key gets it
 * from here rather than holding its own reference, so there is exactly
 * one place [clear] needs to be called to guarantee the key is gone from
 * memory.
 *
 * This directly implements the Milestone 3 "Session Key Management" and
 * "Secure Memory handling" requirements: locking a vault
 * (`core.domain.usecase.LockVaultUseCase`'s eventual implementation)
 * calls [clear] here, which wipes the underlying `SecureBytes`
 * immediately rather than waiting for garbage collection.
 *
 * Not thread-safe by accident — it is thread-safe *on purpose*, via
 * `@Synchronized`, because auto-lock (a background timer) and explicit
 * user actions (unlocking, manually locking) can race to read or clear
 * the held key from different threads.
 */
class SessionKeyHolder {

    @Volatile
    private var current: SessionKey? = null

    /**
     * Installs [key] as the active session key, destroying whatever key
     * was previously held.
     *
     * Re-installing the *identical* instance is a no-op (Finding L5):
     * destroying-then-reinstalling the same key would leave a destroyed
     * key installed.
     */
    @Synchronized
    fun set(key: SessionKey) {
        val existing = current
        if (existing != null && existing === key) return
        existing?.destroy()
        current = key
    }

    /** The currently active session key, or `null` if the vault is locked. */
    @Synchronized
    fun get(): SessionKey? = current

    /** True while a session key is held (i.e. the vault is unlocked from this holder's point of view). */
    val isActive: Boolean
        get() = current != null

    /** Destroys the held key, if any, and clears it — the operation a lock action performs. */
    @Synchronized
    fun clear() {
        current?.destroy()
        current = null
    }
}
