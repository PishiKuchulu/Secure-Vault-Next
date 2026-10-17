package com.securevault.next.data.persistence

/**
 * Runs persistence operations inside a single SQLite transaction.
 *
 * Implemented by [SecureVaultDatabase]. This interface exists so that
 * modules without a direct Room dependency (e.g. `data:repository`)
 * can still request atomic multi-statement writes without referencing
 * Room types — keeping the repository layer free of persistence
 * framework imports while guaranteeing all-or-nothing writes
 * (create/update = metadata + detail + tags).
 */
interface TransactionRunner {
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}
