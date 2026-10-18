package com.securevault.next.data.persistence.pojo

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.securevault.next.data.persistence.entity.ItemEntity
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.persistence.entity.TagEntity

/**
 * A summary of a vault item used for list displays.
 *
 * It includes the base metadata and the list of associated tags, but
 * omits the sensitive type-specific encrypted fields.
 */
data class ItemSummaryPojo(
    @Embedded
    val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
        Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
