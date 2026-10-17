package com.securevault.next.data.repository

import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.Vault
import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.VaultStatistics
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.data.persistence.dao.CategoryDao
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.dao.TagDao
import com.securevault.next.data.persistence.dao.VaultDao
import com.securevault.next.data.repository.mapper.CategoryTagMapper
import com.securevault.next.data.repository.mapper.ItemMapper
import com.securevault.next.data.repository.mapper.VaultMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultDao: VaultDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val itemDao: ItemDao,
    private val encryptionEngine: ItemEncryptionEngine,
) : VaultRepository {

    override fun observeVault(vaultId: VaultId): Flow<Vault?> {
        return combine(
            vaultDao.observeVault(vaultId),
            categoryDao.observeCategories(vaultId),
            tagDao.observeTags(vaultId),
            itemDao.observeFullItemsByType(vaultId, null) // All items
        ) { entity, categories, tags, itemPojos ->
            if (entity == null) {
                null
            } else {
                val metadata = VaultMapper.toMetadata(entity)
                val items = ItemMapper.toDomainItems(itemPojos, encryptionEngine, itemDao)
                Vault(
                    metadata = metadata,
                    items = items,
                    categories = categories.map { CategoryTagMapper.toCategoryDomain(it) },
                    tags = tags.map { CategoryTagMapper.toTagDomain(it) },
                    settings = VaultMapper.toSettings(entity),
                    securityPolicy = VaultMapper.toSecurityPolicy(entity),
                    statistics = VaultStatistics(
                        itemCount = items.size,
                        categoryCount = categories.size,
                        favoriteCount = 0,
                        weakPasswordCount = 0, // Requires audit engine
                        reusedPasswordCount = 0, // Requires audit engine
                        oldPasswordCount = 0,
                        breachedPasswordCount = 0,
                        securityScore = SecurityScore(100)
                    )
                )
            }
        }
    }

    override fun observeAllVaultMetadata(): Flow<List<VaultMetadata>> {
        return vaultDao.observeAllVaults().map { list ->
            list.map { VaultMapper.toMetadata(it) }
        }
    }

    override suspend fun getVault(vaultId: VaultId): DomainResult<Vault> {
        val entity = vaultDao.getVault(vaultId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))

        val categories = categoryDao.getCategories(vaultId)
        val tags = tagDao.getTags(vaultId)
        val itemPojos = itemDao.getFullItemsByType(vaultId, null)
        val items = ItemMapper.toDomainItems(itemPojos, encryptionEngine, itemDao)

        return DomainResult.success(
            Vault(
                metadata = VaultMapper.toMetadata(entity),
                items = items,
                categories = categories.map { CategoryTagMapper.toCategoryDomain(it) },
                tags = tags.map { CategoryTagMapper.toTagDomain(it) },
                settings = VaultMapper.toSettings(entity),
                securityPolicy = VaultMapper.toSecurityPolicy(entity),
                statistics = VaultStatistics(
                    itemCount = items.size,
                    categoryCount = categories.size,
                    favoriteCount = 0,
                    weakPasswordCount = 0,
                    reusedPasswordCount = 0,
                    oldPasswordCount = 0,
                    breachedPasswordCount = 0,
                    securityScore = SecurityScore(100)
                )
            )
        )
    }

    override suspend fun getVaultMetadata(vaultId: VaultId): DomainResult<VaultMetadata> {
        val entity = vaultDao.getVault(vaultId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))
        return DomainResult.success(VaultMapper.toMetadata(entity))
    }

    override suspend fun getEnvelopeJson(vaultId: VaultId): DomainResult<String?> {
        if (vaultDao.getVault(vaultId) == null) {
            return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))
        }
        return DomainResult.success(vaultDao.getEnvelopeJson(vaultId))
    }

    override suspend fun updateEnvelopeJson(vaultId: VaultId, envelopeJson: String): DomainResult<Unit> {
        val updated = vaultDao.updateEnvelopeJson(vaultId, envelopeJson)
        if (updated == 0) {
            return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))
        }
        return DomainResult.success(Unit)
    }

    override suspend fun createVault(
        metadata: VaultMetadata,
        securityPolicy: VaultSecurityPolicy,
        settings: VaultSettings,
        envelopeJson: String?,
    ): DomainResult<Vault> {
        val entity = VaultMapper.toEntity(metadata, securityPolicy, settings, envelopeJson)
        vaultDao.insertVault(entity)
        return DomainResult.success(
            Vault(
                metadata = metadata,
                items = emptyList(),
                categories = emptyList(),
                tags = emptyList(),
                settings = settings,
                securityPolicy = securityPolicy,
                statistics = VaultStatistics(
                    itemCount = 0,
                    categoryCount = 0,
                    favoriteCount = 0,
                    weakPasswordCount = 0,
                    reusedPasswordCount = 0,
                    oldPasswordCount = 0,
                    breachedPasswordCount = 0,
                    securityScore = SecurityScore(100)
                )
            )
        )
    }

    override suspend fun updateVaultSettings(vaultId: VaultId, settings: VaultSettings): DomainResult<Unit> {
        val entity = vaultDao.getVault(vaultId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))

        val updated = entity.copy(
            themeMode = settings.themeMode,
            defaultSortField = settings.defaultSortField,
            defaultSortOrder = settings.defaultSortOrder,
            showPasswordStrengthIndicator = settings.showPasswordStrengthIndicator,
            showRecentlyUsed = settings.showRecentlyUsed
        )
        vaultDao.updateVault(updated)
        return DomainResult.success(Unit)
    }

    override suspend fun updateVaultSecurityPolicy(vaultId: VaultId, policy: VaultSecurityPolicy): DomainResult<Unit> {
        val entity = vaultDao.getVault(vaultId)
            ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(vaultId.value, "Vault"))

        val updated = entity.copy(
            autoLockPolicy = policy.autoLockPolicy,
            clipboardTimeout = policy.clipboardTimeout,
            requireBiometric = policy.requireBiometric,
            maxFailedUnlockAttempts = policy.maxFailedUnlockAttempts,
            authenticationMethods = policy.authenticationMethods.joinToString(",") { it.name },
            minMasterPasswordLength = policy.minMasterPasswordLength
        )
        vaultDao.updateVault(updated)
        return DomainResult.success(Unit)
    }

    override suspend fun deleteVault(vaultId: VaultId): DomainResult<Unit> {
        vaultDao.deleteVault(vaultId)
        return DomainResult.success(Unit)
    }
}
