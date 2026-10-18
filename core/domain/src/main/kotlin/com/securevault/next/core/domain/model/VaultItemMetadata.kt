package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.Notes
import com.securevault.next.core.domain.model.valueobject.Revision
import com.securevault.next.core.domain.model.valueobject.TagId
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId

/**
 * The metadata every [com.securevault.next.core.domain.model.item.VaultItem]
 * carries, regardless of its concrete type.
 *
 * Every one of the ten item types (`PasswordItem`, `SecureNote`,
 * `CreditCard`, ...) embeds one of these as its `metadata` property
 * rather than repeating `id`, `vaultId`, `title`, `categoryId`, `tagIds`,
 * `isFavorite`, `customFields`, `notes`, `createdAt`, `updatedAt` and
 * `revision` as ten separate properties on each class — the duplication
 * would make every future change to "what every item has" a ten-file
 * edit instead of a one-file edit.
 */
data class VaultItemMetadata(
    val id: ItemId,
    val vaultId: VaultId,
    val title: String,
    val categoryId: CategoryId? = null,
    val tagIds: Set<TagId> = emptySet(),
    val isFavorite: Boolean = false,
    val customFields: List<CustomField> = emptyList(),
    val notes: Notes? = null,
    val createdAt: CreatedAt,
    val updatedAt: UpdatedAt,
    val revision: Revision = Revision.INITIAL
) {
    init {
        require(title.isNotBlank()) { "VaultItemMetadata.title must not be blank" }
        require(title.length <= MAX_TITLE_LENGTH) {
            "VaultItemMetadata.title must be at most $MAX_TITLE_LENGTH characters"
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 200
    }
}
