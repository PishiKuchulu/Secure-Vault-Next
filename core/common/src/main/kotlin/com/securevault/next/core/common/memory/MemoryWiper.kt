package com.securevault.next.core.common.memory

/**
 * Utility for zeroing out sensitive data in memory.
 *
 * Part 7 (Production Hardening): "Assume millions of users... reliability,
 * performance, maintainability and robustness."
 *
 * Sensitive data like master passwords (held as [CharArray]) or
 * cryptographic keys (held as [ByteArray]) should be overwritten with
 * zeros as soon as they are no longer needed, rather than waiting for
 * garbage collection, to minimize the time they remain vulnerable to
 * memory scrapers.
 */
object MemoryWiper {

    /** Overwrites every element of [array] with the null character. */
    fun wipe(array: CharArray?) {
        array?.fill('\u0000')
    }

    /** Overwrites every element of [array] with zero. */
    fun wipe(array: ByteArray?) {
        array?.fill(0)
    }

    /**
     * Executes [block] with [sensitiveData], then wipes [sensitiveData]
     * immediately after, regardless of whether [block] succeeded.
     */
    inline fun <T> useAndWipe(sensitiveData: CharArray, block: (CharArray) -> T): T {
        return try {
            block(sensitiveData)
        } finally {
            wipe(sensitiveData)
        }
    }

    /**
     * Executes [block] with [sensitiveData], then wipes [sensitiveData]
     * immediately after, regardless of whether [block] succeeded.
     */
    inline fun <T> useAndWipe(sensitiveData: ByteArray, block: (ByteArray) -> T): T {
        return try {
            block(sensitiveData)
        } finally {
            wipe(sensitiveData)
        }
    }
}
