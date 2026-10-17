package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * The output of running a security audit over a vault
 * ([com.securevault.next.core.domain.usecase.RunSecurityAuditUseCase]):
 * an overall score plus which specific items triggered which specific
 * finding, so a UI can link straight from a finding to the offending
 * item rather than only showing an aggregate number.
 */
data class AuditResult(
    val vaultId: VaultId,
    val performedAt: Instant,
    val overallScore: SecurityScore,
    val weakItems: List<ItemId> = emptyList(),
    val reusedPasswordGroups: List<List<ItemId>> = emptyList(),
    val oldItems: List<ItemId> = emptyList(),
    val breachedItems: List<ItemId> = emptyList(),
    val recommendations: List<String> = emptyList(),
) {
    init {
        require(reusedPasswordGroups.all { it.size >= 2 }) {
            "AuditResult.reusedPasswordGroups entries must each contain at least 2 items to count as reuse"
        }
    }
}
