package com.securevault.next.core.domain.model.valueobject

/**
 * A normalized 0–100 security score. Used both for a single item's
 * password strength and for a whole vault's aggregate
 * [com.securevault.next.core.domain.model.VaultStatistics.securityScore].
 * Bounding it at the type level means no caller can accidentally compare
 * or display a score that has drifted out of range.
 */
@JvmInline
value class SecurityScore(
    val value: Int,
) {
    init {
        require(value in MIN..MAX) { "SecurityScore must be between $MIN and $MAX, was $value" }
    }

    companion object {
        const val MIN = 0
        const val MAX = 100
    }
}
