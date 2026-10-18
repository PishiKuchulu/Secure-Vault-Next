package com.securevault.next.core.crypto.memory

import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Free functions for handling password material that arrives as a
 * `CharArray` (see the `masterPassword: CharArray` parameters throughout
 * `core.domain.usecase`) without ever materializing it as a `String`.
 *
 * A `String` is immutable and interned/cached in ways the JVM does not
 * expose control over — once a password touches a `String`, there is no
 * reliable way to remove it from memory before garbage collection, and
 * no guarantee GC runs promptly. Every function here works directly on
 * `CharArray`/`ByteArray`, which callers can wipe deterministically via
 * [wipe] or [SecureBytes.close].
 */
object MemoryUtils {
    /** Overwrites [chars] with `'\u0000'` in place. Safe to call on an already-wiped or empty array. */
    fun wipe(chars: CharArray) {
        Arrays.fill(chars, '\u0000')
    }

    /**
     * Encodes [chars] as UTF-8 bytes without allocating a `String`,
     * wrapping the result in a [SecureBytes] so the encoded copy can be
     * wiped independently of the original [chars] (which the caller
     * remains responsible for wiping).
     */
    fun charsToSecureBytes(chars: CharArray): SecureBytes {
        val charBuffer = CharBuffer.wrap(chars)
        val byteBuffer = StandardCharsets.UTF_8.encode(charBuffer)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        // encode() may allocate an internal buffer larger than needed; clear
        // whatever it produced beyond what we copied out.
        if (byteBuffer.hasArray()) {
            Arrays.fill(byteBuffer.array(), 0)
        } else {
            // Finding L11: a direct (off-heap) buffer has no accessible
            // array — overwrite every byte in place via absolute puts so
            // the encoded password does not linger off-heap until GC.
            // Absolute puts do not disturb the buffer's position.
            for (i in 0 until byteBuffer.capacity()) {
                byteBuffer.put(i, 0.toByte())
            }
        }
        return SecureBytes.wrap(bytes)
    }
}
