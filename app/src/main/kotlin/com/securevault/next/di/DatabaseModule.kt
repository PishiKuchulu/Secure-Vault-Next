package com.securevault.next.di

import android.content.Context
import androidx.room.Room
import com.securevault.next.data.persistence.SecureVaultDatabase
import com.securevault.next.data.persistence.dao.AuditDao
import com.securevault.next.data.persistence.dao.BackupDao
import com.securevault.next.data.persistence.dao.CategoryDao
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.dao.SearchDao
import com.securevault.next.data.persistence.dao.TagDao
import com.securevault.next.data.persistence.dao.VaultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SecureVaultDatabase {
        return Room.databaseBuilder(
            context,
            SecureVaultDatabase::class.java,
            "secure_vault.db"
        )
            .addMigrations(
                SecureVaultDatabase.MIGRATION_1_2,
                SecureVaultDatabase.MIGRATION_2_3
            )
            // C6: installs the triggers that keep the FTS4 search index
            // in sync with the items table (plus a one-time backfill).
            .addCallback(SecureVaultDatabase.FTS_SYNC_CALLBACK)
            .build()
    }

    @Provides
    fun provideVaultDao(db: SecureVaultDatabase): VaultDao = db.vaultDao()

    @Provides
    fun provideCategoryDao(db: SecureVaultDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTagDao(db: SecureVaultDatabase): TagDao = db.tagDao()

    @Provides
    fun provideItemDao(db: SecureVaultDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideSearchDao(db: SecureVaultDatabase): SearchDao = db.searchDao()

    @Provides
    fun provideAuditDao(db: SecureVaultDatabase): AuditDao = db.auditDao()

    @Provides
    fun provideBackupDao(db: SecureVaultDatabase): BackupDao = db.backupDao()
}
