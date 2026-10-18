package com.securevault.next.data.repository.mapper

import com.securevault.next.core.domain.model.Category
import com.securevault.next.core.domain.model.Tag
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.data.persistence.entity.CategoryEntity
import com.securevault.next.data.persistence.entity.TagEntity

/**
 * Maps between Category/Tag domain models and persistence entities.
 *
 * The domain models carry their own [com.securevault.next.core.domain.model.valueobject.VaultId],
 * so persistence can round-trip them without caller-supplied context.
 */
object CategoryTagMapper {
    fun toCategoryEntity(domain: Category): CategoryEntity =
        CategoryEntity(
            id = domain.id,
            vaultId = domain.vaultId,
            name = domain.name,
            iconAlias = domain.icon,
            colorHex = domain.colorHex,
            parentCategoryId = domain.parentCategoryId,
            createdAt = domain.createdAt.value,
            updatedAt = domain.updatedAt.value,
        )

    fun toCategoryDomain(entity: CategoryEntity): Category =
        Category(
            id = entity.id,
            vaultId = entity.vaultId,
            name = entity.name,
            icon = entity.iconAlias,
            colorHex = entity.colorHex,
            parentCategoryId = entity.parentCategoryId,
            createdAt = CreatedAt(entity.createdAt),
            updatedAt = UpdatedAt(entity.updatedAt),
        )

    /**
     * The domain [Tag] does not track `updatedAt`; the entity column
     * reuses `createdAt` on insert. Tag updates are not modeled in the
     * domain, so no information is lost in practice.
     */
    fun toTagEntity(domain: Tag): TagEntity =
        TagEntity(
            id = domain.id,
            vaultId = domain.vaultId,
            name = domain.name,
            colorHex = domain.colorHex,
            createdAt = domain.createdAt.value,
            updatedAt = domain.createdAt.value,
        )

    fun toTagDomain(entity: TagEntity): Tag =
        Tag(
            id = entity.id,
            vaultId = entity.vaultId,
            name = entity.name,
            colorHex = entity.colorHex,
            createdAt = CreatedAt(entity.createdAt),
        )
}
