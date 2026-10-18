package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists the result of a security audit for a vault.
 */
@Entity(
    tableName = "audit_results",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vault_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AuditResultEntity(
    @PrimaryKey
    @ColumnInfo(name = "vault_id")
    val vaultId: VaultId,
    @ColumnInfo(name = "performed_at")
    val performedAt: Instant,
    @ColumnInfo(name = "overall_score")
    val overallScore: Double,
    /** JSON list of ItemIds. */
    @ColumnInfo(name = "weak_items_json")
    val weakItemsJson: String,
    /** JSON list of lists of ItemIds. */
    @ColumnInfo(name = "reused_password_groups_json")
    val reusedPasswordGroupsJson: String,
    /** JSON list of ItemIds. */
    @ColumnInfo(name = "old_items_json")
    val oldItemsJson: String,
    /** JSON list of ItemIds. */
    @ColumnInfo(name = "breached_items_json")
    val breachedItemsJson: String,
    /** JSON list of recommendation strings. */
    @ColumnInfo(name = "recommendations_json")
    val recommendationsJson: String
)
