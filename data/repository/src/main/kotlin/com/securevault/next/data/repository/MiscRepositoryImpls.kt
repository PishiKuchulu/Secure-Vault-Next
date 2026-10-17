package com.securevault.next.data.repository

import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.AuditResult
import com.securevault.next.core.domain.model.BackupMetadata
import com.securevault.next.core.domain.model.Category
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.search.SearchResult
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.AuditRepository
import com.securevault.next.core.domain.repository.BackupRepository
import com.securevault.next.core.domain.repository.CategoryRepository
import com.securevault.next.core.domain.repository.SearchRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.data.persistence.dao.AuditDao
import com.securevault.next.data.persistence.dao.BackupDao
import com.securevault.next.data.persistence.dao.CategoryDao
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.dao.SearchDao
import com.securevault.next.data.persistence.entity.AuditResultEntity
import com.securevault.next.data.repository.mapper.CategoryTagMapper
import com.securevault.next.data.repository.mapper.ItemMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun observeCategories(vaultId: VaultId): Flow<List<Category>> =
        categoryDao.observeCategories(vaultId).map { list -> list.map { CategoryTagMapper.toCategoryDomain(it) } }

    override suspend fun getCategory(categoryId: CategoryId): DomainResult<Category> {
        val entity = categoryDao.getCategory(categoryId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(categoryId.value, "Category"))
        return DomainResult.success(CategoryTagMapper.toCategoryDomain(entity))
    }

    override suspend fun createCategory(category: Category): DomainResult<Category> {
        // H4: the domain Category carries its own vaultId, so this persists
        // for real — the previous fake-success stub is gone.
        categoryDao.insertCategory(CategoryTagMapper.toCategoryEntity(category))
        return DomainResult.success(category)
    }

    override suspend fun updateCategory(category: Category): DomainResult<Category> {
        val existing = categoryDao.getCategory(category.id)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(category.id.value, "Category"))
        // Load-then-update: preserve createdAt from the stored row.
        categoryDao.updateCategory(
            CategoryTagMapper.toCategoryEntity(category).copy(createdAt = existing.createdAt)
        )
        return DomainResult.success(category)
    }

    override suspend fun deleteCategory(categoryId: CategoryId): DomainResult<Unit> {
        categoryDao.deleteCategory(categoryId)
        return DomainResult.success(Unit)
    }
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchDao: SearchDao,
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
) : SearchRepository {
    override suspend fun search(vaultId: VaultId, filter: SearchFilter): DomainResult<SearchResult> {
        // H5: the raw user query is sanitized into safe FTS5 syntax before
        // it ever reaches MATCH. A null result means "nothing searchable".
        val ftsQuery = buildFtsMatchQuery(filter.query ?: "")
            ?: return DomainResult.success(SearchResult(emptyList(), 0, filter))
        val pojos = searchDao.searchItems(
            vaultId = vaultId,
            query = ftsQuery,
            favoritesOnly = filter.favoritesOnly
        )

        // Fetch full items to satisfy the domain model requirement.
        val fullItems = mutableListOf<VaultItem>()
        for (summary in pojos) {
            val pojo = itemDao.getFullItem(summary.item.id) ?: continue
            fullItems.add(ItemMapper.toDomain(pojo, encryptionEngine, itemDao))
        }

        // Filtering by item types, categories, and tags in memory for now.
        val filteredItems = fullItems.filter { item ->
            (filter.itemTypes.isEmpty() || item.type in filter.itemTypes) &&
            (filter.categoryIds.isEmpty() || item.metadata.categoryId in filter.categoryIds) &&
            (filter.tagIds.isEmpty() || item.metadata.tagIds.any { it in filter.tagIds })
        }

        return DomainResult.success(SearchResult(filteredItems, filteredItems.size, filter))
    }
}

/**
 * Builds a safe FTS5 MATCH query from raw user input (H5).
 *
 * FTS5 rejects a leading `*` (syntax error) and treats `"`, `*`, `(`, `)`,
 * `:`, `AND`/`OR`/`NOT`/`NEAR` and `column:` filters as query syntax —
 * feeding raw user input to MATCH therefore crashes on ordinary input.
 * This sanitizer:
 * - splits on whitespace into tokens,
 * - strips every character that is not a letter, digit, `_`, `-`, `.` or `@`,
 * - drops tokens with no alphanumeric content,
 * - caps the token count to bound query size,
 * - emits each token double-quoted (neutralizing keywords and filters)
 *   with a single trailing `*` for prefix matching.
 *
 * The value is still passed as a bound parameter, so there is no SQL
 * injection vector; this is purely about FTS5 syntax safety.
 *
 * Returns null when nothing searchable remains.
 */
private fun buildFtsMatchQuery(rawQuery: String): String? {
    val tokens = rawQuery.trim()
        .split(Regex("\\s+"))
        .map { token ->
            token.filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' || it == '@' }
        }
        .filter { it.isNotEmpty() && it.any(Char::isLetterOrDigit) }
        .take(MAX_FTS_QUERY_TOKENS)
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" ") { "\"$it\"*" }
}

private const val MAX_FTS_QUERY_TOKENS = 10

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditDao: AuditDao
) : AuditRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeLatestAuditResult(vaultId: VaultId): Flow<AuditResult?> =
        auditDao.observeAuditResult(vaultId).map { entity ->
            entity?.let {
                AuditResult(
                    vaultId = it.vaultId,
                    performedAt = it.performedAt,
                    // The column stores the score as Double while the domain
                    // score is an Int in 0..100 — coerce defensively so a
                    // corrupt row cannot crash the read path.
                    overallScore = SecurityScore(
                        it.overallScore.toInt().coerceIn(SecurityScore.MIN, SecurityScore.MAX)
                    ),
                    weakItems = json.decodeFromString(it.weakItemsJson),
                    reusedPasswordGroups = json.decodeFromString(it.reusedPasswordGroupsJson),
                    oldItems = json.decodeFromString(it.oldItemsJson),
                    breachedItems = json.decodeFromString(it.breachedItemsJson),
                    recommendations = json.decodeFromString(it.recommendationsJson)
                )
            }
        }

    override suspend fun saveAuditResult(result: AuditResult): DomainResult<Unit> {
        // H8: audit results are now actually persisted (previously computed
        // but never stored). vault_id is the primary key, so REPLACE keeps
        // exactly one — the latest — result per vault.
        auditDao.insertAuditResult(
            AuditResultEntity(
                vaultId = result.vaultId,
                performedAt = result.performedAt,
                overallScore = result.overallScore.value.toDouble(),
                weakItemsJson = json.encodeToString(result.weakItems),
                reusedPasswordGroupsJson = json.encodeToString(result.reusedPasswordGroups),
                oldItemsJson = json.encodeToString(result.oldItems),
                breachedItemsJson = json.encodeToString(result.breachedItems),
                recommendationsJson = json.encodeToString(result.recommendations),
            )
        )
        return DomainResult.success(Unit)
    }

    override suspend fun runAudit(vaultId: VaultId): DomainResult<AuditResult> {
        // Implementation of audit logic is outside Milestone 4 scope (Audit is a feature).
        // Here we just provide the persistence bridge.
        return DomainResult.failure(DomainError.UnexpectedError("Audit not implemented"))
    }
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupDao: BackupDao
) : BackupRepository {
    override fun observeBackups(vaultId: VaultId): Flow<List<BackupMetadata>> =
        backupDao.observeBackups(vaultId).map { list ->
            list.map {
                BackupMetadata(
                    id = it.id,
                    vaultId = it.vaultId,
                    createdAt = it.createdAt,
                    sizeBytes = it.sizeBytes,
                    itemCount = it.itemCount,
                    appVersion = it.appVersion
                )
            }
        }

    override suspend fun createBackup(vaultId: VaultId): DomainResult<BackupMetadata> {
        return DomainResult.failure(DomainError.UnexpectedError("Backup creation not implemented"))
    }

    override suspend fun restoreBackup(backupId: String, masterPassword: CharArray): DomainResult<com.securevault.next.core.domain.model.Vault> {
        return DomainResult.failure(DomainError.UnexpectedError("Backup restoration not implemented"))
    }

    override suspend fun deleteBackup(backupId: String): DomainResult<Unit> {
        backupDao.deleteBackupMetadata(backupId)
        return DomainResult.success(Unit)
    }
}
