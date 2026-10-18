package com.securevault.next.core.crypto.key

import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.security.session.SessionKeyStore
import javax.inject.Inject

/**
 * The Android-side adapter that lets the pure-JVM session manager
 * ([com.securevault.next.core.security.session.VaultSessionManager])
 * install and destroy the in-memory session key without depending on
 * `core:crypto` itself.
 *
 * Implements [SessionKeyStore] over the real [SessionKeyHolder]: the bytes
 * handed over by the session layer are wrapped in [SecureBytes], wrapped
 * again as a [SessionKey], and installed via [SessionKeyHolder.set]. The
 * holder is the single in-memory owner of the key material, and [destroy]
 * delegates to its wipe-on-clear, so the key is zeroed the moment the
 * session locks.
 *
 * **Ownership:** [install] copies [keyBytes] into an owned buffer — the
 * holder (and through it the wrapped [SecureBytes]) is responsible for
 * wiping its own copy on [destroy]. The caller remains responsible for
 * wiping the array it passed, and must not retain or reuse it after
 * calling this.
 *
 * Does not modify [SessionKeyHolder]'s behavior; it only adapts it behind
 * the session layer's abstraction.
 */
class SessionKeyHolderStore
    @Inject
    constructor(
        private val holder: SessionKeyHolder,
    ) : SessionKeyStore {
        override fun install(keyBytes: ByteArray) {
            // SecureBytes.wrap aliases the caller's array; the caller (unlock use
            // case) wipes its temporary buffer in a finally block. Copy the bytes
            // into a fresh owned buffer so the holder's key is a distinct array
            // that survives the caller's wipe. Exactly one live key buffer is held.
            holder.set(SessionKey(SecureBytes.wrap(keyBytes.copyOf())))
        }

        override fun destroy() {
            holder.clear()
        }

        override val isActive: Boolean
            get() = holder.isActive
    }
