package com.securevault.next.core.domain.repository

import com.securevault.next.core.domain.model.AuditResult
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/** Runs and retrieves security audits for a vault. */
interface AuditRepository {
    fun observeLatestAuditResult(vaultId: VaultId): Flow<AuditResult?>

    suspend fun runAudit(vaultId: VaultId): DomainResult<AuditResult>

    /**
     * Persists a completed [AuditResult] — computed by
     * [com.securevault.next.core.domain.usecase.RunSecurityAuditUseCase] —
     * so it survives the process and is returned by
     * [observeLatestAuditResult].
     *
     * Added for Finding H8: audit results were previously computed but
     * never stored anywhere.
     */
    suspend fun saveAuditResult(result: AuditResult): DomainResult<Unit>
}

/** Persists a vault's [VaultSettings] (user preferences, not security policy — see [VaultSettings]'s own docs). */
interface SettingsRepository {
    fun observeSettings(vaultId: VaultId): Flow<VaultSettings>

    suspend fun updateSettings(vaultId: VaultId, settings: VaultSettings): DomainResult<Unit>
}

/** Tracks the active [VaultSession] for whichever vaults are currently unlocked. */
interface SessionRepository {
    fun observeSession(vaultId: VaultId): Flow<VaultSession?>

    suspend fun startSession(session: VaultSession): DomainResult<Unit>

    suspend fun updateActivity(vaultId: VaultId): DomainResult<Unit>

    suspend fun endSession(vaultId: VaultId): DomainResult<Unit>
}
