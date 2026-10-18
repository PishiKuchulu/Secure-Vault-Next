package com.securevault.next.core.domain.usecase

import com.securevault.next.core.domain.model.ExportMetadata
import com.securevault.next.core.domain.model.ImportMetadata
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.result.DomainResult

/** Imports items into a vault from an external source (a file path, a URI, ...) in a given [format]. */
fun interface ImportVaultUseCase {
    suspend operator fun invoke(
        vaultId: VaultId,
        source: String,
        format: String,
    ): DomainResult<ImportMetadata>
}

/** Exports a vault's items to an external destination in a given [format]. */
fun interface ExportVaultUseCase {
    suspend operator fun invoke(
        vaultId: VaultId,
        destination: String,
        format: String,
    ): DomainResult<ExportMetadata>
}
