package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.domain.model.enums.ClipboardTimeout
import com.securevault.next.core.domain.model.enums.SortField
import com.securevault.next.core.domain.model.enums.SortOrder
import com.securevault.next.core.domain.model.enums.ThemeMode
import com.securevault.next.core.domain.model.enums.VaultStatus
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists the core configuration and metadata of a vault.
 *
 * This entity flattens the [com.securevault.next.core.domain.model.VaultMetadata],
 * [com.securevault.next.core.domain.model.VaultSecurityPolicy], and
 * [com.securevault.next.core.domain.model.VaultSettings] domain models
 * into a single table for performance — a vault is the root of the
 * aggregate, and its configuration is always loaded together.
 */
@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey
    val id: VaultId,

    val name: String,

    val status: VaultStatus,

    val version: Int,

    val revision: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,

    // Security Policy (Flattened)

    @ColumnInfo(name = "auto_lock_policy")
    val autoLockPolicy: AutoLockPolicy,

    @ColumnInfo(name = "clipboard_timeout")
    val clipboardTimeout: ClipboardTimeout,

    @ColumnInfo(name = "require_biometric")
    val requireBiometric: Boolean,

    @ColumnInfo(name = "max_failed_unlock_attempts")
    val maxFailedUnlockAttempts: Int,

    @ColumnInfo(name = "authentication_methods")
    val authenticationMethods: String, // CSV of enum names

    @ColumnInfo(name = "min_master_password_length")
    val minMasterPasswordLength: Int,

    // Settings (Flattened)

    @ColumnInfo(name = "theme_mode")
    val themeMode: ThemeMode,

    @ColumnInfo(name = "default_sort_field")
    val defaultSortField: SortField,

    @ColumnInfo(name = "default_sort_order")
    val defaultSortOrder: SortOrder,

    @ColumnInfo(name = "show_password_strength_indicator")
    val showPasswordStrengthIndicator: Boolean,

    @ColumnInfo(name = "show_recently_used")
    val showRecentlyUsed: Boolean,

    // The serialized EncryptedVaultEnvelope protecting the vault's wrapped
    // VaultKey. Null for vaults created before v1.6/M3 (no envelope existed),
    // which are therefore not unlockable and must be re-created. Non-null for
    // every vault created after this schema version.
    @ColumnInfo(name = "envelope_json")
    val envelopeJson: String?,
)
