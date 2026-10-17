package com.securevault.next.core.crypto.memory

import java.util.Arrays

/**
 * A wrapper around a `ByteArray` holding sensitive material (a derived
 * key, a decrypted field, raw password bytes) that can be explicitly
 * wiped and refuses to be used again afterward.
 *
 * Every cryptographic primitive in this module that produces or
 * consumes raw key/plaintext bytes uses [SecureBytes] instead of a bare
 * `ByteArray` specifically so that:
 *
 * 1. Callers are reminded, at the type level, that this data is
 *    sensitive and its lifetime matters (Milestone 3 requirement:
 *    "Reduce lifetime of sensitive data").
 * 2. [close] can overwrite the backing array with zeros
 *    ([Arrays.fill]) the moment the data is no longer needed, rather
 *    than waiting for garbage collection — a `ByteArray` left to the GC
 *    may remain readable in memory for an unpredictable length of time.
 * 3. [SecureBytes] implements [AutoCloseable], so wrapping a whole
 *    operation in the Kotlin standard library's `someSecureBytes.use { ... }`
 *    guarantees the wipe happens even if an exception is thrown.
 *
 * Read access is deliberately split into two differently-named methods
 * rather than one, so a call site's intent is unambiguous and neither
 * is confusable with the stdlib's `AutoCloseable.use`:
 *
 * - [peek] reads the bytes without wiping them — for keys and other
 *   values the *caller* owns and may need again (a session key used to
 *   encrypt several fields must not be wiped after the first one).
 * - [consume] reads the bytes and immediately wipes them afterward —
 *   for genuinely one-shot values this instance's *creator* has no
 *   further use for (e.g. a freshly generated nonce, once it has been
 *   copied into a cipher parameter spec).
 *
 * This is *not* a perfect guarantee against all memory-disclosure
 * attacks — the JVM/ART can still copy array contents during GC
 * compaction before [close] runs, and `String`s derived from this data
 * before wiping are immutable and cannot be wiped at all (which is
 * exactly why this codebase uses `CharArray`/`ByteArray` for secrets
 * end-to-end and never converts them to `String`). What [SecureBytes]
 * *does* guarantee is that the moment a caller is done with the data, it
 * takes an explicit, auditable action to reduce that data's remaining
 * lifetime in memory to as close to zero as the JVM allows.
 */
class SecureBytes private constructor(
    private var backing: ByteArray?,
) : AutoCloseable {

    /** The size of the wrapped array. Throws [IllegalStateException] once wiped. */
    val size: Int
        get() = current().size

    /** True once [close] has been called and this instance can no longer be used. */
    val isWiped: Boolean
        get() = backing == null

    /**
     * Reads the backing bytes without wiping them afterward. The array
     * reference passed to [block] must not be retained beyond the
     * lifetime of the call, and must not be wiped directly by the
     * caller — wipe this [SecureBytes] itself (via [close] or
     * [consume]) so wiping happens in exactly one place.
     */
    fun <R> peek(block: (ByteArray) -> R): R = block(current())

    /**
     * Reads the backing bytes and immediately wipes this [SecureBytes]
     * afterward (even if [block] throws) — the "I am done with this the
     * moment [block] returns" access pattern.
     */
    fun <R> consume(block: (ByteArray) -> R): R {
        try {
            return block(current())
        } finally {
            close()
        }
    }

    /** A defensive copy of the current bytes, wrapped in a new [SecureBytes]. Does not affect this instance. */
    fun copy(): SecureBytes = peek { SecureBytes(it.copyOf()) }

    /** Overwrites the backing array with zeros. Safe to call multiple times. */
    override fun close() {
        backing?.let { Arrays.fill(it, 0) }
        backing = null
    }

    private fun current(): ByteArray =
        backing ?: error("SecureBytes has already been wiped and cannot be used again")

    companion object {
        /**
         * Wraps an existing [ByteArray]. Ownership of [bytes] transfers to
         * the returned [SecureBytes] — the caller must not continue to
         * hold or mutate the original reference independently, since
         * [close] will zero it out from under them.
         */
        fun wrap(bytes: ByteArray): SecureBytes = SecureBytes(bytes)

        /** Allocates a new zero-filled [SecureBytes] of the given [size]. */
        fun ofSize(size: Int): SecureBytes = SecureBytes(ByteArray(size))
    }
}
