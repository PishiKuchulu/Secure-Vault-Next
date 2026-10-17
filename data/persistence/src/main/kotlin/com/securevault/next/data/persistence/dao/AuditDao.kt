package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.AuditResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [AuditResultEntity].
 */
@Dao
interface AuditDao {

    @Query("SELECT * FROM audit_results WHERE vault_id = :vaultId")
    fun observeAuditResult(vaultId: VaultId): Flow<AuditResultEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditResult(result: AuditResultEntity)

    @Query("DELETE FROM audit_results WHERE vault_id = :vaultId")
    suspend fun deleteAuditResult(vaultId: VaultId)
}
