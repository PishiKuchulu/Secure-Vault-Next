package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.ItemId

/**
 * A single recovery code value belonging to a [RecoveryCodeEntity].
 *
 * Declared in its own file: it is a distinct top-level `@Entity`
 * registered in `SecureVaultDatabase`, and Room's KSP processor resolves
 * multi-`@Entity`-per-file declarations unreliably for `@Database`
 * class references — this file keeps the table resolvable.
 */
@Entity(
    tableName = "recovery_code_values",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryCodeEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["recovery_code_item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class RecoveryCodeValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "recovery_code_item_id")
    val recoveryCodeItemId: ItemId,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @Embedded(prefix = "code_")
    val code: EncryptedFieldEntity,
)