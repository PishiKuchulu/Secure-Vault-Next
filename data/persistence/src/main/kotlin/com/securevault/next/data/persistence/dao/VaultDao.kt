package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.VaultEntity

/**
 * Data access object for [VaultEntity].
 */
@Dao
interface VaultDao {

    @Query("SELECT * FROM vaults")
    fun observeAllVaults(): List<VaultEntity>
}
