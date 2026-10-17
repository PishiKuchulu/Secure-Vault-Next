package com.securevault.next.data.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Persists metadata about a local vault backup.
 *
 * The actual backup file lives on the filesystem; this entity allows
 * the app to list and manage available backups without scanning the
 * disk repeatedly.
 *
 * M9: [formatVersion] identifies the backup file format so a future
 * restore can reject formats it cannot read; [vaultRevision] is the
 * vault revision the backup was taken at, so a restore can refuse to
 * overwrite newer data with an older backup; [sha256] is the hex digest
 * of the backup file for integrity verification. All three are nullable
 * because backups recorded before v3 predate them.
 */
@Entity(
    tableName = "backups",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vault_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("vault_id")
    ]
)
data class BackupMetadataEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "vault_id")
    val vaultId: VaultId,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "item_count")
    val itemCount: Int,

    @ColumnInfo(name = "app_version")
    val appVersion: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    /**
     * Backup file format version. Written by the backup pipeline when it
     * creates the row; defaults to 1 for rows predating v3.
     */
    @ColumnInfo(name = "format_version", defaultValue = "1")
    val formatVersion: Int = 1,

    /**
     * The vault revision the backup was taken at, when known. A restore
     * implementation must compare this against the live vault revision
     * and refuse to overwrite newer data with an older backup.
     */
    @ColumnInfo(name = "vault_revision")
    val vaultRevision: Long? = null,

    /**
     * Hex-encoded SHA-256 digest of the backup file, when computed at
     * backup time. Verified before any restore.
     */
    @ColumnInfo(name = "sha256")
    val sha256: String? = null,
)
