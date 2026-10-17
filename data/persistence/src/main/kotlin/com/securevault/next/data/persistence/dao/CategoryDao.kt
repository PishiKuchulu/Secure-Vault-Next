package com.securevault.next.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.securevault.next.core.domain.model.valueobject.CategoryId
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.data.persistence.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [CategoryEntity].
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE vault_id = :vaultId ORDER BY name ASC")
    fun observeCategories(vaultId: VaultId): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategory(categoryId: CategoryId): CategoryEntity?

    @Query("SELECT * FROM categories WHERE vault_id = :vaultId ORDER BY name ASC")
    suspend fun getCategories(vaultId: VaultId): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: CategoryId)
}
