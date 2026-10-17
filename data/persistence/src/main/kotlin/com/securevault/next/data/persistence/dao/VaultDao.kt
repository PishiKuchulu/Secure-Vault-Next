package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [VaultEntity].
 */
@Dao
interface VaultDao {

    @Query("SELECT * FROM vaults WHERE id = :vaultId")
    fun observeVault(vaultId: VaultId): Flow<VaultEntity?>

    @Query("SELECT * FROM vaults")
    fun observeAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE id = :vaultId")
    suspend fun getVault(vaultId: VaultId): VaultEntity?

    @Query("SELECT envelope_json FROM vaults WHERE id = :vaultId")
    suspend fun getEnvelopeJson(vaultId: VaultId): String?

    @Query("UPDATE vaults SET envelope_json = :envelopeJson WHERE id = :vaultId")
    suspend fun updateEnvelopeJson(vaultId: VaultId, envelopeJson: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVault(vault: VaultEntity)

    @Update
    suspend fun updateVault(vault: VaultEntity)

    @Query("DELETE FROM vaults WHERE id = :vaultId")
    suspend fun deleteVault(vaultId: VaultId)
}
