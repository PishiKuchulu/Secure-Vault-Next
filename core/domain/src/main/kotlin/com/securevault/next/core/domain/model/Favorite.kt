package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * An entry in a vault's user-ordered favorites list.
 *
 * This is distinct from [com.securevault.next.core.domain.model.VaultItemMetadata.isFavorite]:
 * the boolean flag on an item answers "is this favorited at all?",
 * while [Favorite.position] additionally captures the user's chosen
 * display order for a dedicated Favorites screen (Part 5), which a
 * boolean alone cannot represent.
 */
data class Favorite(val itemId: ItemId, val vaultId: VaultId, val position: Int, val addedAt: Instant) {
    init {
        require(position >= 0) { "Favorite.position must not be negative, was $position" }
    }
}
