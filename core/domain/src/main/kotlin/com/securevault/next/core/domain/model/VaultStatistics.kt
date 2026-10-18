package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.SecurityScore
import java.time.Instant

/**
 * A computed summary of a vault's contents and security posture, meant
 * for a dashboard-style overview (Part 5: Home Dashboard) rather than
 * for driving business logic — it is a read model, derived from the
 * vault's actual items and its most recent
 * [com.securevault.next.core.domain.model.AuditResult], not a source of
 * truth in its own right.
 */
data class VaultStatistics(
    val itemCount: Int,
    val categoryCount: Int,
    val favoriteCount: Int,
    val weakPasswordCount: Int,
    val reusedPasswordCount: Int,
    val oldPasswordCount: Int,
    val breachedPasswordCount: Int,
    val securityScore: SecurityScore,
    val lastAuditAt: Instant? = null
) {
    init {
        require(itemCount >= 0) { "VaultStatistics.itemCount must not be negative" }
        require(categoryCount >= 0) { "VaultStatistics.categoryCount must not be negative" }
        require(favoriteCount >= 0) { "VaultStatistics.favoriteCount must not be negative" }
        require(weakPasswordCount >= 0) { "VaultStatistics.weakPasswordCount must not be negative" }
        require(reusedPasswordCount >= 0) { "VaultStatistics.reusedPasswordCount must not be negative" }
        require(oldPasswordCount >= 0) { "VaultStatistics.oldPasswordCount must not be negative" }
        require(breachedPasswordCount >= 0) { "VaultStatistics.breachedPasswordCount must not be negative" }
    }
}
