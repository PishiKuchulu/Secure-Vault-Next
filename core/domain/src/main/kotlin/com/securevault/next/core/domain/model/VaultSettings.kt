package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.enums.SortField
import com.securevault.next.core.domain.model.enums.SortOrder
import com.securevault.next.core.domain.model.enums.ThemeMode

/**
 * User-facing display and UX preferences for a vault. See
 * [VaultSecurityPolicy] for the enforced-security counterpart to this
 * class — nothing in here changes what the app will or won't allow, only
 * how it looks and how lists default to being ordered.
 */
data class VaultSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultSortField: SortField = SortField.TITLE,
    val defaultSortOrder: SortOrder = SortOrder.ASCENDING,
    val showPasswordStrengthIndicator: Boolean = true,
    val showRecentlyUsed: Boolean = true,
)
