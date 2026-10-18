package com.securevault.next.data.repository.mapper

import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.VaultSettings
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.Revision
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.model.valueobject.Version
import com.securevault.next.data.persistence.entity.VaultEntity

/**
 * Maps between [VaultEntity] and the domain models [VaultMetadata],
 * [VaultSecurityPolicy], and [VaultSettings].
 */
object VaultMapper {
    fun toEntity(
        metadata: VaultMetadata,
        policy: VaultSecurityPolicy,
        settings: VaultSettings,
        envelopeJson: String?
    ): VaultEntity = VaultEntity(
        id = metadata.id,
        name = metadata.name.value,
        status = metadata.status,
        version = metadata.version.value,
        // L12: Revision is Long in the domain model but the column is Int;
        // overflow would require 2^31 revisions of one row — unrealistic,
        // so the narrowing conversion is kept as-is.
        revision = metadata.revision.value.toInt(),
        createdAt = metadata.createdAt.value,
        updatedAt = metadata.updatedAt.value,
        autoLockPolicy = policy.autoLockPolicy,
        clipboardTimeout = policy.clipboardTimeout,
        requireBiometric = policy.requireBiometric,
        maxFailedUnlockAttempts = policy.maxFailedUnlockAttempts,
        authenticationMethods = policy.authenticationMethods.joinToString(",") { it.name },
        minMasterPasswordLength = policy.minMasterPasswordLength,
        themeMode = settings.themeMode,
        defaultSortField = settings.defaultSortField,
        defaultSortOrder = settings.defaultSortOrder,
        showPasswordStrengthIndicator = settings.showPasswordStrengthIndicator,
        showRecentlyUsed = settings.showRecentlyUsed,
        envelopeJson = envelopeJson
    )

    fun toMetadata(entity: VaultEntity): VaultMetadata = VaultMetadata(
        id = entity.id,
        name = VaultName(entity.name),
        status = entity.status,
        version = Version(entity.version),
        revision = Revision(entity.revision.toLong()),
        createdAt = CreatedAt(entity.createdAt),
        updatedAt = UpdatedAt(entity.updatedAt)
    )

    fun toSecurityPolicy(entity: VaultEntity): VaultSecurityPolicy = VaultSecurityPolicy(
        autoLockPolicy = entity.autoLockPolicy,
        clipboardTimeout = entity.clipboardTimeout,
        requireBiometric = entity.requireBiometric,
        maxFailedUnlockAttempts = entity.maxFailedUnlockAttempts,
        // M7: never throw on corrupt/unknown stored values — unparseable
        // entries are dropped (an empty set is a valid policy state) so a
        // single bad row cannot crash the vault-open path.
        authenticationMethods =
        entity.authenticationMethods
            .split(",")
            .mapNotNull { runCatching { AuthenticationMethod.valueOf(it) }.getOrNull() }
            .toSet(),
        minMasterPasswordLength = entity.minMasterPasswordLength
    )

    fun toSettings(entity: VaultEntity): VaultSettings = VaultSettings(
        themeMode = entity.themeMode,
        defaultSortField = entity.defaultSortField,
        defaultSortOrder = entity.defaultSortOrder,
        showPasswordStrengthIndicator = entity.showPasswordStrengthIndicator,
        showRecentlyUsed = entity.showRecentlyUsed
    )
}
