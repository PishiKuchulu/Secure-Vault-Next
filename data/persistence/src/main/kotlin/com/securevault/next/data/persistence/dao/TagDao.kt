package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.securevault.next.core.domain.model.valueobject.TagId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [TagEntity].
 */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE vault_id = :vaultId ORDER BY name ASC")
    fun observeTags(vaultId: VaultId): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTag(tagId: TagId): TagEntity?

    @Query("SELECT * FROM tags WHERE vault_id = :vaultId ORDER BY name ASC")
    suspend fun getTags(vaultId: VaultId): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: TagId)
}
