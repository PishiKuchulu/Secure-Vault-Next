package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.VaultId
import java.time.Instant

/**
 * Describes a single backup artifact produced by
 * [com.securevault.next.core.domain.usecase.CreateBackupUseCase]. The
 * backup's actual encrypted content is opaque to the domain layer — this
 * is only the catalog entry a Backup Center screen (Part 4) would list.
 */
data class BackupMetadata(
    val id: String,
    val vaultId: VaultId,
    val createdAt: Instant,
    val sizeBytes: Long,
    val itemCount: Int,
    val appVersion: String
) {
    init {
        require(id.isNotBlank()) { "BackupMetadata.id must not be blank" }
        require(sizeBytes >= 0) { "BackupMetadata.sizeBytes must not be negative" }
        require(itemCount >= 0) { "BackupMetadata.itemCount must not be negative" }
        require(appVersion.isNotBlank()) { "BackupMetadata.appVersion must not be blank" }
    }
}

/**
 * The outcome of an import operation
 * ([com.securevault.next.core.domain.usecase.ImportVaultUseCase]).
 * [format] is a plain string (e.g. `"csv"`, `"1password"`, `"bitwarden"`)
 * rather than a closed enum, since the set of supported third-party
 * formats is expected to grow without requiring a `core:domain` change
 * for every new one.
 */
data class ImportMetadata(
    val source: String,
    val format: String,
    val importedAt: Instant,
    val itemsImported: Int,
    val itemsSkipped: Int = 0,
    val itemsFailed: Int = 0,
    val warnings: List<String> = emptyList()
) {
    init {
        require(source.isNotBlank()) { "ImportMetadata.source must not be blank" }
        require(format.isNotBlank()) { "ImportMetadata.format must not be blank" }
        require(itemsImported >= 0) { "ImportMetadata.itemsImported must not be negative" }
        require(itemsSkipped >= 0) { "ImportMetadata.itemsSkipped must not be negative" }
        require(itemsFailed >= 0) { "ImportMetadata.itemsFailed must not be negative" }
    }
}

/** The outcome of an export operation ([com.securevault.next.core.domain.usecase.ExportVaultUseCase]). */
data class ExportMetadata(
    val destination: String,
    val format: String,
    val exportedAt: Instant,
    val itemsExported: Int
) {
    init {
        require(destination.isNotBlank()) { "ExportMetadata.destination must not be blank" }
        require(format.isNotBlank()) { "ExportMetadata.format must not be blank" }
        require(itemsExported >= 0) { "ExportMetadata.itemsExported must not be negative" }
    }
}
