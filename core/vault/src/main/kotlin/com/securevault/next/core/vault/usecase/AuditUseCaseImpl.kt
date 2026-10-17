package com.securevault.next.core.vault.usecase

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import com.securevault.next.core.common.result.errorOrNull
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.common.result.isFailure
import com.securevault.next.core.common.result.isSuccess
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.model.AuditResult
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.AuditRepository
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.core.domain.security.SecurityScoreEngine
import com.securevault.next.core.domain.usecase.RunSecurityAuditUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class AuditUseCaseImpl @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val encryptionEngine: ItemEncryptionEngine,
    private val auditRepository: AuditRepository,
    @Dispatcher(SecureVaultDispatcher.Io) private val ioDispatcher: CoroutineDispatcher
) : RunSecurityAuditUseCase {

    override suspend fun invoke(vaultId: VaultId): DomainResult<AuditResult> = withContext(ioDispatcher) {
        val vaultResult = vaultRepository.getVault(vaultId)
        if (vaultResult.isFailure) return@withContext DomainResult.failure(vaultResult.errorOrNull()!!)

        val vault = vaultResult.getOrNull()!!
        val passwordItems: List<PasswordItem> = vault.items.filterIsInstance<PasswordItem>()

        val weakItems = mutableListOf<ItemId>()
        val oldItems = mutableListOf<ItemId>()

        val now = Instant.now()
        val sixMonthsAgo = now.minusSeconds(180 * 24 * 60 * 60L)

        // Finding H8: decrypted passwords are held as CharArray (never as
        // an immutable String) and every one is wiped in the finally block
        // below, whether the audit succeeds or fails partway through.
        val decrypted = mutableListOf<Pair<ItemId, CharArray>>()
        try {
            passwordItems.forEach { item ->
                // 1. Weak detection
                val decryptedResult = encryptionEngine.decryptField(item.password)
                if (decryptedResult.isSuccess) {
                    val plaintext = decryptedResult.getOrNull()!!
                    val strength = SecurityScoreEngine.evaluateStrength(plaintext)
                    if (strength.score.value < 60) {
                        weakItems.add(item.metadata.id)
                    }
                    decrypted.add(item.metadata.id to plaintext)
                }

                // 2. Age detection
                if (item.metadata.updatedAt.value.isBefore(sixMonthsAgo)) {
                    oldItems.add(item.metadata.id)
                }
            }

            // 3. Reuse detection: group items whose passwords compare
            // equal in constant time, without materializing Strings.
            val reusedPasswordGroups = findReuseGroups(decrypted)

            // Simplified score calculation
            val totalPasswordCount = passwordItems.size
            val badCount = weakItems.size + reusedPasswordGroups.flatten().distinct().size
            val scoreValue = if (totalPasswordCount == 0) 100
                            else (100 - (badCount.toDouble() / totalPasswordCount * 100)).toInt().coerceIn(0, 100)

            val auditResult = AuditResult(
                vaultId = vaultId,
                performedAt = now,
                overallScore = SecurityScore(scoreValue),
                weakItems = weakItems,
                reusedPasswordGroups = reusedPasswordGroups,
                oldItems = oldItems,
                recommendations = generateRecommendations(weakItems.size, reusedPasswordGroups.size)
            )

            // Finding H8: the computed result is persisted through the
            // audit repository so it survives the process and is visible
            // to observeLatestAuditResult. A persistence failure is
            // reported as this use case's failure.
            val saveError = auditRepository.saveAuditResult(auditResult).errorOrNull()
            if (saveError != null) {
                return@withContext DomainResult.failure(saveError)
            }
            DomainResult.success(auditResult)
        } finally {
            decrypted.forEach { (_, password) -> password.fill('\u0000') }
        }
    }

    /**
     * Groups item ids whose passwords are equal, comparing the CharArrays
     * in constant time (Finding H8) so no password is ever materialized
     * as an immutable, un-wipeable String for use as a map key.
     */
    private fun findReuseGroups(decrypted: List<Pair<ItemId, CharArray>>): List<List<ItemId>> {
        val groups = mutableListOf<MutableList<ItemId>>()
        val grouped = BooleanArray(decrypted.size)
        for (i in decrypted.indices) {
            if (grouped[i]) continue
            val group = mutableListOf(decrypted[i].first)
            for (j in i + 1 until decrypted.size) {
                if (!grouped[j] && constantTimeEquals(decrypted[i].second, decrypted[j].second)) {
                    group.add(decrypted[j].first)
                    grouped[j] = true
                }
            }
            if (group.size >= 2) {
                grouped[i] = true
                groups.add(group)
            }
        }
        return groups
    }

    /**
     * Constant-time CharArray equality: the loop always runs to
     * completion over the full length, accumulating differences, so the
     * comparison time does not reveal where two passwords first differ.
     * A length mismatch short-circuits — length is not treated as secret
     * here, matching `MessageDigest.isEqual` semantics.
     */
    private fun constantTimeEquals(a: CharArray, b: CharArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    private fun generateRecommendations(weakCount: Int, reusedCount: Int): List<String> {
        val recs = mutableListOf<String>()
        if (weakCount > 0) recs.add("You have $weakCount weak passwords. Use the generator to create stronger ones.")
        if (reusedCount > 0) recs.add("You have $reusedCount reused passwords. Unique passwords prevent cascade breaches.")
        return recs
    }
}
