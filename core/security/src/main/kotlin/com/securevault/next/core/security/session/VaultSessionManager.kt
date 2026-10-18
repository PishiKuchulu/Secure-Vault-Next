package com.securevault.next.core.security.session

import com.securevault.next.core.domain.model.VaultSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The runtime lifecycle state of the active vault session, observable via
 * [VaultSessionManager.state].
 *
 * Deliberately session-scoped rather than a mirror of the whole
 * [com.securevault.next.core.domain.model.VaultState] machine: the
 * manager owns exactly the states that carry a security consequence —
 * whether the vault's secrets are reachable ([Unlocked]) or not
 * ([Locked]/[AutoLocked]) — and the in-between [Unlocking] transition.
 */
sealed interface SessionState {

    /** No session key is installed; secrets are unreachable. */
    data object Locked : SessionState

    /** An unlock is in progress; the vault is not yet usable. */
    data object Unlocking : SessionState

    /** The vault is unlocked and its session key is installed. */
    data class Unlocked(val session: VaultSession) : SessionState

    /** The session was terminated by an auto-lock timeout rather than an explicit lock. */
    data object AutoLocked : SessionState
}

/** Receives notifications when the active session transitions in or out of the unlocked state. */
interface SessionListener {
    /** Invoked after the session becomes [SessionState.Unlocked]. */
    fun onUnlocked(session: VaultSession)

    /** Invoked after the session leaves [SessionState.Unlocked]. */
    fun onLocked(reason: LockTrigger)

    /** An adapter that makes a listener of only one of the two events convenient. */
    class Adapter(
        private val unlocked: (VaultSession) -> Unit = {},
        private val locked: (LockTrigger) -> Unit = {},
    ) : SessionListener {
        override fun onUnlocked(session: VaultSession) = unlocked(session)

        override fun onLocked(reason: LockTrigger) = locked(reason)
    }
}

/**
 * The single source of truth for the active vault session's lifecycle.
 *
 * Owns the state machine (locked → unlocking → unlocked → locked) and the
 * invariant that a session key may exist **only** while the state is
 * [SessionState.Unlocked]. The key itself lives behind [SessionKeyStore]
 * — an abstraction over the Android `SessionKeyHolder` — so this class
 * stays pure Kotlin/JVM and the key-wipe-before-state-change rule is
 * enforced in one place.
 *
 * Thread-safety: the transition methods are mutually exclusive via a
 * monitor, matching the existing `@Synchronized` discipline in
 * `core:crypto.key.SessionKeyHolder`; auto-lock (a later milestone's
 * timer) and explicit user actions can race, and must not interleave a
 * lock inside an unlock.
 */
@Singleton
class VaultSessionManager @Inject constructor(
    private val clock: SessionClock,
    private val sessionKeyStore: SessionKeyStore,
) {
    private val listeners: Set<SessionListener> = emptySet()

    private val monitor = Any()

    private val _state = MutableStateFlow<SessionState>(SessionState.Locked)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var session: VaultSession? = null

    /** The currently active session, or `null` when not unlocked. */
    fun currentSession(): VaultSession? = synchronized(monitor) { session }

    /**
     * Unlocks the vault for [session]. The transition to [SessionState.Unlocked]
     * happens **only after** the session key is successfully installed: if
     * [keyBytes] is `null` the request is rejected and the state remains
     * locked, because an unlocked session with no key would let callers
     * believe secrets are reachable when the key store never accepted one.
     *
     * Returns `true` on success, `false` if the vault was already unlocked
     * or the key could not be installed. A [SessionKeyStore.install] throw
     * never leaves the machine wedged in [SessionState.Unlocking]: the
     * pre-unlock state is restored so a later [lock]/[unlock] can proceed
     * (Finding L2).
     */
    fun unlock(session: VaultSession, keyBytes: ByteArray?): Boolean = synchronized(monitor) {
        if (_state.value is SessionState.Unlocked) return false

        val previousState = _state.value
        _state.value = SessionState.Unlocking
        keyBytes ?: run {
            _state.value = previousState
            return false
        }

        try {
            sessionKeyStore.install(keyBytes)
        } catch (c: CancellationException) {
            _state.value = previousState
            throw c
        } catch (t: Throwable) {
            // Recoverable: restore the state from before Unlocking rather
            // than stranding the machine mid-transition.
            _state.value = previousState
            return false
        }
        this.session = session
        _state.value = SessionState.Unlocked(session)
        listeners.forEach { it.onUnlocked(session) }
        true
    }

    /**
     * Locks the vault immediately. The session key is wiped **before** the
     * state is published as locked, so no observer can ever observe
     * "locked" while the key still exists in memory. Idempotent: locking an
     * already-locked vault is a no-op.
     */
    fun lock(reason: LockTrigger = LockTrigger.EXPLICIT): Boolean = synchronized(monitor) {
        val current = _state.value
        if (current !is SessionState.Unlocked && current != SessionState.Unlocking) return false

        sessionKeyStore.destroy()
        session = null
        _state.value = if (reason == LockTrigger.TIMEOUT) SessionState.AutoLocked else SessionState.Locked
        listeners.forEach { it.onLocked(reason) }
        true
    }

    /**
     * Records user activity on the active session; no-op when locked. Returns the previous [VaultSession] or `null`.
     */
    fun touch(): VaultSession? = synchronized(monitor) {
        val active = session ?: return null
        val updated = active.copy(lastActivityAt = clock.now())
        this.session = updated
        val state = _state.value
        if (state is SessionState.Unlocked) {
            _state.value = SessionState.Unlocked(updated)
        }
        active
    }

    /** True while the vault is unlocked (session key installed). */
    fun isUnlocked(): Boolean = synchronized(monitor) {
        _state.value is SessionState.Unlocked
    }
}
