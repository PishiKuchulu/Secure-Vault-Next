package com.securevault.next.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.securevault.next.data.persistence.converter.VaultTypeConverters
import com.securevault.next.data.persistence.entity.ApiKeyEntity
import com.securevault.next.data.persistence.entity.AuditResultEntity
import com.securevault.next.data.persistence.entity.BankAccountEntity
import com.securevault.next.data.persistence.entity.BackupMetadataEntity
import com.securevault.next.data.persistence.entity.CategoryEntity
import com.securevault.next.data.persistence.entity.CreditCardEntity
import com.securevault.next.data.persistence.entity.IdentityDocumentEntity
import com.securevault.next.data.persistence.entity.ItemEntity
import com.securevault.next.data.persistence.entity.ItemFtsEntity
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.persistence.entity.PasswordEntity
import com.securevault.next.data.persistence.entity.PasswordHistoryEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeValueEntity
import com.securevault.next.data.persistence.entity.SecureNoteEntity
import com.securevault.next.data.persistence.entity.SoftwareLicenseEntity
import com.securevault.next.data.persistence.entity.SshKeyEntity
import com.securevault.next.data.persistence.entity.TagEntity
import com.securevault.next.data.persistence.entity.TotpSeedEntity
import com.securevault.next.data.persistence.entity.VaultEntity

/**
 * The Room database for SecureVault Next.
 *
 * This database owns the entire persistence layer of the application.
 * It uses a normalized schema with 20 entities to support all 10 vault
 * item types, categories, tags, search, audits, and backups.
 */
@Database(
    entities = [
        VaultEntity::class,
        ItemEntity::class,
        ItemFtsEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        PasswordEntity::class,
        PasswordHistoryEntity::class,
        CreditCardEntity::class,
        SecureNoteEntity::class,
        BankAccountEntity::class,
        IdentityDocumentEntity::class,
        SoftwareLicenseEntity::class,
        ApiKeyEntity::class,
        SshKeyEntity::class,
        RecoveryCodeEntity::class,
        RecoveryCodeValueEntity::class,
        TotpSeedEntity::class,
        AuditResultEntity::class,
        BackupMetadataEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(VaultTypeConverters::class)
abstract class SecureVaultDatabase : RoomDatabase(), TransactionRunner {
    abstract fun vaultDao(): VaultDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun itemDao(): ItemDao
    abstract fun searchDao(): SearchDao
    abstract fun auditDao(): AuditDao
    abstract fun backupDao(): BackupDao

    override suspend fun <R> runInTransaction(block: suspend () -> R): R =
        withTransaction { block() }

    companion object {
        /**
         * v1 → v2: adds the `envelope_json` column to `vaults`, which holds
         * the serialized EncryptedVaultEnvelope protecting the wrapped
         * VaultKey. Existing rows keep every other column and gain a NULL
         * envelope (legacy vaults are not unlockable and must be re-created).
         * Never destructive.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE vaults ADD COLUMN envelope_json TEXT"
                )
            }
        }

        /**
         * v2 → v3:
         * - `items`: adds `notes_ciphertext` / `notes_nonce` / `notes_algorithm`
         *   (C5 — item notes are encrypted at rest like every other sensitive
         *   field). The legacy plaintext `notes` column is kept so existing
         *   rows can be lazily migrated on read; writers must leave it NULL.
         * - `items_fts`: rebuilt with an explicit `item_id` TEXT column (C6).
         *   FTS4 rowids are integers and can never hold the TEXT item ids the
         *   search queries join on, so the old table is dropped and rebuilt
         *   from item titles only — never from decrypted secrets. (SQLite
         *   cannot ADD COLUMN on a virtual table, hence the rebuild.)
         * - `categories`: adds `color_hex` / `parent_category_id` so the
         *   domain Category maps losslessly.
         * - `backups`: adds `format_version`, `vault_revision`, `sha256` (M9).
         *
         * Never destructive to user data: the only dropped table is the
         * derived FTS index, which is rebuilt from the source rows.
         *
         * Must be registered in DatabaseModule alongside [MIGRATION_1_2]:
         * `.addMigrations(SecureVaultDatabase.MIGRATION_1_2, SecureVaultDatabase.MIGRATION_2_3)`
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // C5: encrypted notes columns on items.
                db.execSQL("ALTER TABLE items ADD COLUMN notes_ciphertext TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN notes_nonce TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN notes_algorithm TEXT")

                // Lossless Category mapping.
                db.execSQL("ALTER TABLE categories ADD COLUMN color_hex TEXT")
                db.execSQL("ALTER TABLE categories ADD COLUMN parent_category_id TEXT")

                // M9: backup integrity metadata.
                db.execSQL("ALTER TABLE backups ADD COLUMN format_version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE backups ADD COLUMN vault_revision INTEGER")
                db.execSQL("ALTER TABLE backups ADD COLUMN sha256 TEXT")

                // C6: FTS4 virtual tables do not support ADD COLUMN — drop and
                // recreate with the item_id column, re-indexing titles only.
                // (Triggers do not exist yet at migration time; they are
                // created in FTS_SYNC_CALLBACK.onOpen afterwards.)
                db.execSQL("DROP TABLE items_fts")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS items_fts USING FTS4(item_id, title, notes)")
                db.execSQL("INSERT INTO items_fts(item_id, title, notes) SELECT id, title, NULL FROM items")
            }
        }

        /**
         * Keeps the FTS4 search index ([ItemFtsEntity]) in sync with the
         * `items` table (C6).
         *
         * Three triggers mirror every INSERT/UPDATE/DELETE into `items_fts`.
         * Only already-plaintext metadata is indexed (the item title) —
         * notes are encrypted at rest and the trigger deliberately inserts
         * NULL for the notes column, so decrypted secrets never enter the
         * search index.
         *
         * `onOpen` also performs a one-time backfill: if the FTS index is
         * completely empty while items exist (databases created before the
         * triggers existed), every item title is indexed. This is additive
         * only and never touches user data.
         *
         * Must be registered in DatabaseModule:
         * `.addCallback(SecureVaultDatabase.FTS_SYNC_CALLBACK)`
         */
        val FTS_SYNC_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createFtsTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                createFtsTriggers(db)
                backfillFtsIndex(db)
            }

            private fun createFtsTriggers(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_items_fts_insert AFTER INSERT ON items BEGIN
                        INSERT INTO items_fts(item_id, title, notes) VALUES (new.id, new.title, NULL);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_items_fts_update AFTER UPDATE ON items BEGIN
                        DELETE FROM items_fts WHERE item_id = old.id;
                        INSERT INTO items_fts(item_id, title, notes) VALUES (new.id, new.title, NULL);
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_items_fts_delete AFTER DELETE ON items BEGIN
                        DELETE FROM items_fts WHERE item_id = old.id;
                    END
                    """.trimIndent()
                )
            }

            private fun backfillFtsIndex(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT INTO items_fts(item_id, title, notes)
                    SELECT id, title, NULL FROM items
                    WHERE NOT EXISTS (SELECT 1 FROM items_fts LIMIT 1)
                    """.trimIndent()
                )
            }
        }
    }
}
