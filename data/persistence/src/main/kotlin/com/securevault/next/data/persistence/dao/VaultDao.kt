package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.securevault.next.data.persistence.entity.VaultEntity

/**
 * Data access object for [VaultEntity].
 */
@Dao
interface VaultDao {

    @Query("SELECT * FROM vaults")
    fun observeAllVaults(): List<VaultEntity>
}
