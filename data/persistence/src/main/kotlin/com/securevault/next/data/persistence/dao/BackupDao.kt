package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.BackupMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [BackupMetadataEntity].
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM backups WHERE vault_id = :vaultId ORDER BY created_at DESC")
    fun observeBackups(vaultId: VaultId): Flow<List<BackupMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupMetadata(metadata: BackupMetadataEntity)

    @Query("SELECT * FROM backups WHERE id = :backupId")
    suspend fun getBackupMetadata(backupId: String): BackupMetadataEntity?

    @Query("DELETE FROM backups WHERE id = :backupId")
    suspend fun deleteBackupMetadata(backupId: String)
}
