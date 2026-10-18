package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.TagId

/**
 * A many-to-many relationship mapping between [ItemEntity] and [TagEntity].
 */
@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["item_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("item_id"),
        Index("tag_id")
    ]
)
data class ItemTagCrossRef(
    @ColumnInfo(name = "item_id")
    val itemId: ItemId,
    @ColumnInfo(name = "tag_id")
    val tagId: TagId
)
