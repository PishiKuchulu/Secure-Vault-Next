package com.securevault.next.core.security.session

/**
 * The narrow seam between session *policy* (owned by `core:security`,
 * pure Kotlin/JVM) and session *key material* (owned by `core:crypto`,
 * Android).
 *
 * `core:security` deliberately does not depend on `core:crypto`, so the
 * session manager never touches a concrete key holder. It depends only on
 * this interface; the Android-backed implementation
 * (`core:crypto.key.SessionKeyHolderStore`) adapts it over the real
 * `SessionKeyHolder`. That keeps the business rule — "the key exists
 * only while the session is unlocked" — testable on the JVM with a fake,
 * exactly the pattern the rest of the codebase uses for crypto seams
 * (e.g. `core.domain.crypto.ItemEncryptionEngine`).
 *
 * The installed value is deliberately a plain [ByteArray] rather than a
 * named key type: the pure-JVM core must not reference a `core:crypto`
 * type, and the caller (the unlock flow) is the one that knows which key
 * it is installing.
 *
 * Ownership (Finding L3): [install] copies [keyBytes] into the store's
 * own private buffer — the *caller* remains responsible for wiping the
 * array it passed, which must not be read or reused after the call. The
 * store owns and wipes its private copy when [destroy] is called.
 */
interface SessionKeyStore {
    /**
     * Installs the session key, destroying whatever key was previously
     * held. Copies [keyBytes]; the caller must wipe its own array
     * afterwards (see the ownership note above).
     */
    fun install(keyBytes: ByteArray)

    /** Wipes and removes any held key immediately — the lock operation's action. */
    fun destroy()

    /** True while a session key is held (i.e. the vault is unlocked from the key's point of view). */
    val isActive: Boolean
}
