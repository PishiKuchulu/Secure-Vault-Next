package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * A soft-deleted item awaiting either restoration
 * ([com.securevault.next.core.domain.usecase.RestoreItemUseCase]) or
 * permanent purge once [expiresAt] passes. [originalCategoryId] is
 * retained so restoring the item can put it back where it came from,
 * even if the category itself is later renamed or removed.
 */
data class TrashItem(
    val itemId: ItemId,
    val vaultId: VaultId,
    val itemType: ItemType,
    val deletedAt: Instant,
    val originalCategoryId: CategoryId? = null,
    val expiresAt: Instant? = null,
)
