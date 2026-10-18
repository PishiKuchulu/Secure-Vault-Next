package com.securevault.next.data.persistence.converter

import androidx.room.TypeConverter
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.domain.model.enums.ClipboardTimeout
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.enums.SecurityLevel
import com.securevault.next.core.domain.model.enums.SortField
import com.securevault.next.core.domain.model.enums.SortOrder
import com.securevault.next.core.domain.model.enums.ThemeMode
import com.securevault.next.core.domain.model.enums.VaultStatus
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.PasswordId
import com.securevault.next.core.domain.model.valueobject.TagId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Type converters for Room to handle domain value objects, enums, and
 * standard library types that SQLite doesn't support natively.
 */
class VaultTypeConverters {
    // --- java.time ---

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    // --- Identifiers ---

    @TypeConverter
    fun fromVaultId(value: VaultId?): String? = value?.value

    @TypeConverter
    fun toVaultId(value: String?): VaultId? = value?.let { VaultId(it) }

    @TypeConverter
    fun fromItemId(value: ItemId?): String? = value?.value

    @TypeConverter
    fun toItemId(value: String?): ItemId? = value?.let { ItemId(it) }

    @TypeConverter
    fun fromCategoryId(value: CategoryId?): String? = value?.value

    @TypeConverter
    fun toCategoryId(value: String?): CategoryId? = value?.let { CategoryId(it) }

    @TypeConverter
    fun fromTagId(value: TagId?): String? = value?.value

    @TypeConverter
    fun toTagId(value: String?): TagId? = value?.let { TagId(it) }

    @TypeConverter
    fun fromPasswordId(value: PasswordId?): String? = value?.value

    @TypeConverter
    fun toPasswordId(value: String?): PasswordId? = value?.let { PasswordId(it) }

    // --- Enums ---
    //
    // M7: every to-enum converter is total — unknown, empty, or malformed
    // stored values never throw. Each has a documented fallback chosen so
    // that reading a vault (or a row written by a newer schema version)
    // degrades gracefully instead of crashing. Corrupt enum data is a
    // data problem, not a reason to brick the vault-open path.

    @TypeConverter
    fun fromItemType(value: ItemType?): String? = value?.name

    /**
     * Unknown item types (e.g. written by a newer app version) fall back
     * to PASSWORD. The concrete detail relations — not this column —
     * drive domain mapping, so the item still loads with the right shape;
     * only type-filtered queries may misclassify the row.
     */
    @TypeConverter
    fun toItemType(value: String?): ItemType? =
        value?.let { runCatching { ItemType.valueOf(it) }.getOrNull() ?: ItemType.PASSWORD }

    @TypeConverter
    fun fromSecurityLevel(value: SecurityLevel?): String? = value?.name

    /** Unknown values fall back to MEDIUM (neutral midpoint). */
    @TypeConverter
    fun toSecurityLevel(value: String?): SecurityLevel? =
        value?.let { runCatching { SecurityLevel.valueOf(it) }.getOrNull() ?: SecurityLevel.MEDIUM }

    @TypeConverter
    fun fromVaultStatus(value: VaultStatus?): String? = value?.name

    /**
     * Unknown values fall back to ACTIVE. Status is advisory — access is
     * gated cryptographically, not by this column — so availability wins
     * over fail-closed here.
     */
    @TypeConverter
    fun toVaultStatus(value: String?): VaultStatus? =
        value?.let { runCatching { VaultStatus.valueOf(it) }.getOrNull() ?: VaultStatus.ACTIVE }

    @TypeConverter
    fun fromAutoLockPolicy(value: AutoLockPolicy?): String? = value?.name

    /** Unknown values fall back to MINUTES_5 (neutral default). */
    @TypeConverter
    fun toAutoLockPolicy(value: String?): AutoLockPolicy? =
        value?.let { runCatching { AutoLockPolicy.valueOf(it) }.getOrNull() ?: AutoLockPolicy.MINUTES_5 }

    @TypeConverter
    fun fromClipboardTimeout(value: ClipboardTimeout?): String? = value?.name

    /** Unknown values fall back to SECONDS_30 (neutral default). */
    @TypeConverter
    fun toClipboardTimeout(value: String?): ClipboardTimeout? =
        value?.let { runCatching { ClipboardTimeout.valueOf(it) }.getOrNull() ?: ClipboardTimeout.SECONDS_30 }

    @TypeConverter
    fun fromThemeMode(value: ThemeMode?): String? = value?.name

    /** Unknown values fall back to SYSTEM (neutral default). */
    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? =
        value?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() ?: ThemeMode.SYSTEM }

    @TypeConverter
    fun fromSortField(value: SortField?): String? = value?.name

    /** Unknown values fall back to TITLE (neutral default). */
    @TypeConverter
    fun toSortField(value: String?): SortField? =
        value?.let { runCatching { SortField.valueOf(it) }.getOrNull() ?: SortField.TITLE }

    @TypeConverter
    fun fromSortOrder(value: SortOrder?): String? = value?.name

    /** Unknown values fall back to ASCENDING (neutral default). */
    @TypeConverter
    fun toSortOrder(value: String?): SortOrder? =
        value?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() ?: SortOrder.ASCENDING }

    @TypeConverter
    fun fromAuthenticationMethod(value: AuthenticationMethod?): String? = value?.name

    /**
     * Unknown values fall back to MASTER_PASSWORD — the baseline method
     * every vault supports.
     */
    @TypeConverter
    fun toAuthenticationMethod(value: String?): AuthenticationMethod? = value?.let {
        runCatching { AuthenticationMethod.valueOf(it) }.getOrNull() ?: AuthenticationMethod.MASTER_PASSWORD
    }
}
