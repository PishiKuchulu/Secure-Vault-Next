package com.securevault.next.data.repository.mapper

import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.CustomField
import com.securevault.next.core.domain.model.PasswordHistory
import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.item.ApiKey
import com.securevault.next.core.domain.model.item.BankAccount
import com.securevault.next.core.domain.model.item.CreditCard
import com.securevault.next.core.domain.model.item.IdentityDocument
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.RecoveryCode
import com.securevault.next.core.domain.model.item.SSHKey
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.item.SoftwareLicense
import com.securevault.next.core.domain.model.item.TOTPConfig
import com.securevault.next.core.domain.model.item.TOTPSeed
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.Notes
import com.securevault.next.core.domain.model.valueobject.Revision
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.UrlValue
import com.securevault.next.core.domain.result.DomainResult
import com.securevault.next.data.persistence.TransactionRunner
import com.securevault.next.data.persistence.dao.ItemDao
import com.securevault.next.data.persistence.entity.ApiKeyEntity
import com.securevault.next.data.persistence.entity.BankAccountEntity
import com.securevault.next.data.persistence.entity.CreditCardEntity
import com.securevault.next.data.persistence.entity.EncryptedFieldEntity
import com.securevault.next.data.persistence.entity.IdentityDocumentEntity
import com.securevault.next.data.persistence.entity.ItemEntity
import com.securevault.next.data.persistence.entity.PasswordEntity
import com.securevault.next.data.persistence.entity.PasswordHistoryEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeValueEntity
import com.securevault.next.data.persistence.entity.SecureNoteEntity
import com.securevault.next.data.persistence.entity.SoftwareLicenseEntity
import com.securevault.next.data.persistence.entity.SshKeyEntity
import com.securevault.next.data.persistence.entity.TotpConfigEntity
import com.securevault.next.data.persistence.entity.TotpSeedEntity
import com.securevault.next.data.persistence.pojo.FullItemPojo
import com.securevault.next.data.persistence.pojo.ItemSummaryPojo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maps between VaultItem domain models and persistence entities/POJOs.
 *
 * Item notes are encrypted at rest (C5): [toItemEntity] never writes the
 * legacy plaintext `notes` column — callers pass the encrypted form
 * produced by [encryptNotesForStorage]. [toDomain] decrypts via
 * [resolveNotes], which also lazily migrates pre-v3 plaintext rows.
 */
object ItemMapper {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Base Metadata ---

    fun toMetadata(summary: ItemSummaryPojo, notes: Notes?): VaultItemMetadata = VaultItemMetadata(
        id = summary.item.id,
        vaultId = summary.item.vaultId,
        title = summary.item.title,
        categoryId = summary.item.categoryId,
        tagIds = summary.tags.map { it.id }.toSet(),
        isFavorite = summary.item.isFavorite,
        customFields =
        summary.item.customFieldsJson?.let {
            json.decodeFromString<List<CustomField>>(it)
        } ?: emptyList(),
        notes = notes,
        createdAt = CreatedAt(summary.item.createdAt),
        updatedAt = UpdatedAt(summary.item.updatedAt),
        revision = Revision(summary.item.revision.toLong())
    )

    /**
     * Builds the [ItemEntity] for an INSERT.
     *
     * C5: the legacy plaintext `notes` column is always written as NULL;
     * [notesEncrypted] carries the encrypted notes (or null when the item
     * has none). Callers must fail the write when notes were present but
     * encryption failed — a null [notesEncrypted] with non-null notes
     * would silently drop them.
     *
     * L12: [Revision] is a Long in the domain model but the column is an
     * Int; `toInt()` truncation is kept as-is because overflowing 2^31
     * revisions of a single row is unrealistic, and widening the column
     * is not worth a second migration.
     */
    fun toItemEntity(domain: VaultItem, notesEncrypted: EncryptedFieldEntity? = null): ItemEntity = ItemEntity(
        id = domain.metadata.id,
        vaultId = domain.metadata.vaultId,
        type = domain.type,
        title = domain.metadata.title,
        categoryId = domain.metadata.categoryId,
        isFavorite = domain.metadata.isFavorite,
        notes = null,
        notesEncrypted = notesEncrypted,
        customFieldsJson =
        if (domain.metadata.customFields.isEmpty()) {
            null
        } else {
            json.encodeToString(domain.metadata.customFields)
        },
        createdAt = domain.metadata.createdAt.value,
        updatedAt = domain.metadata.updatedAt.value,
        revision =
        domain.metadata.revision.value
            .toInt()
    )

    /**
     * Builds the [ItemEntity] for an UPDATE (H3).
     *
     * The domain model does not carry `lastAccessedAt`, `useCount`,
     * `isPinned`, `isArchived`, `isDeleted`, `deletedAt` or `expiresAt`,
     * so they are preserved from the existing row instead of being reset
     * to entity defaults. In particular a trashed item stays trashed
     * across edits, and soft-delete bookkeeping survives updates.
     */
    fun toItemEntityForUpdate(
        domain: VaultItem,
        existing: ItemEntity,
        notesEncrypted: EncryptedFieldEntity? = null
    ): ItemEntity = toItemEntity(domain, notesEncrypted).copy(
        lastAccessedAt = existing.lastAccessedAt,
        useCount = existing.useCount,
        isPinned = existing.isPinned,
        isArchived = existing.isArchived,
        isDeleted = existing.isDeleted,
        deletedAt = existing.deletedAt,
        expiresAt = existing.expiresAt
    )

    // --- Encrypted Notes (C5) ---

    /**
     * Encrypts [notes] for storage via [engine].
     *
     * Returns null when [notes] is null, or when encryption fails (the
     * caller must then fail the write if notes were present — see
     * [toItemEntity]). The intermediate CharArray is wiped before return.
     */
    suspend fun encryptNotesForStorage(notes: Notes?, engine: ItemEncryptionEngine): EncryptedFieldEntity? {
        if (notes == null) return null
        val chars = notes.value.toCharArray()
        return try {
            engine.encryptField(chars).getOrNull()?.let { toEncryptedEntity(it) }
        } finally {
            chars.fill('\u0000')
        }
    }

    /**
     * Resolves the domain [Notes] for [entity], preferring the encrypted
     * column (C5).
     *
     * - `notes_encrypted` present → decrypted via [engine].
     * - only legacy plaintext `notes` present → encrypted and written back
     *   through [itemDao] (lazy migration), then the plaintext is used.
     * - neither → null.
     *
     * A decryption failure yields null (notes unavailable) rather than
     * failing the whole item read; a failed lazy migration still returns
     * the legacy plaintext so the read succeeds.
     */
    suspend fun resolveNotes(entity: ItemEntity, engine: ItemEncryptionEngine, itemDao: ItemDao): Notes? {
        entity.notesEncrypted?.let { encrypted ->
            val chars =
                engine.decryptField(toEncryptedDomain(encrypted)).getOrNull()
                    ?: return null
            return try {
                notesOrNull(String(chars))
            } finally {
                chars.fill('\u0000')
            }
        }
        val legacy = entity.notes ?: return null
        // Lazy migration of a pre-v3 plaintext row: encrypt once, clear
        // the plaintext column, keep serving the read.
        encryptNotesForStorage(notesOrNull(legacy), engine)?.let { migrated ->
            itemDao.migrateNotesToEncrypted(
                itemId = entity.id,
                ciphertext = migrated.ciphertext,
                nonce = migrated.nonce,
                algorithm = migrated.algorithm
            )
        }
        return notesOrNull(legacy)
    }

    private fun notesOrNull(value: String): Notes? = runCatching { Notes(value) }.getOrNull()

    /**
     * Restores a soft-deleted item out of trash (M10): clears
     * [ItemEntity.isDeleted] / [ItemEntity.deletedAt] inside a transaction,
     * then returns the re-read domain item.
     *
     * Idempotent — restoring an item that is not in trash simply returns
     * it. The FTS index needs no repair: the sync triggers kept the row
     * indexed while it was deleted, and the search queries filter on
     * `is_deleted` themselves.
     */
    suspend inline fun <reified T : VaultItem> restoreItem(
        itemId: ItemId,
        itemDao: ItemDao,
        engine: ItemEncryptionEngine,
        txRunner: TransactionRunner
    ): DomainResult<T> {
        val typeName = T::class.simpleName ?: "VaultItem"
        val pojo =
            itemDao.getFullItem(itemId)
                ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(itemId.value, typeName))
        if (pojo.item.isDeleted) {
            txRunner.runInTransaction {
                itemDao.updateItemMetadata(pojo.item.copy(isDeleted = false, deletedAt = null))
            }
        }
        val reread =
            itemDao.getFullItem(itemId)
                ?: return DomainResult.failure(DomainError.RepositoryError.NotFound(itemId.value, typeName))
        val item =
            toDomain(reread, engine, itemDao) as? T
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.ReadFailed("Item $itemId is not a $typeName")
                )
        return DomainResult.success(item)
    }

    /**
     * Encrypts the notes of [domain] for storage (C5), wrapped as a
     * [DomainResult] for the repository write paths.
     *
     * Success(null) when the item has no notes; failure when notes are
     * present but encryption failed — so callers never silently drop them.
     */
    suspend fun encryptNotesResult(
        domain: VaultItem,
        engine: ItemEncryptionEngine
    ): DomainResult<EncryptedFieldEntity?> {
        val notes = domain.metadata.notes ?: return DomainResult.success(null)
        val encrypted =
            encryptNotesForStorage(notes, engine)
                ?: return DomainResult.failure(
                    DomainError.RepositoryError.WriteFailed(
                        "Failed to encrypt notes for item ${domain.metadata.id}"
                    )
                )
        return DomainResult.success(encrypted)
    }

    // --- Encrypted Fields ---

    private fun toEncryptedEntity(domain: EncryptedField): EncryptedFieldEntity = EncryptedFieldEntity(
        ciphertext = domain.ciphertext,
        nonce = domain.nonce,
        algorithm = domain.algorithm
    )

    private fun toEncryptedDomain(entity: EncryptedFieldEntity): EncryptedField = EncryptedField(
        ciphertext = entity.ciphertext,
        nonce = entity.nonce,
        algorithm = entity.algorithm
    )

    // --- TOTP ---

    private fun toTotpEntity(domain: TOTPConfig): TotpConfigEntity = TotpConfigEntity(
        secret = toEncryptedEntity(domain.secret),
        issuer = domain.issuer,
        accountName = domain.accountName,
        algorithm = domain.algorithm,
        digits = domain.digits,
        periodSeconds = domain.periodSeconds
    )

    private fun toTotpDomain(entity: TotpConfigEntity): TOTPConfig = TOTPConfig(
        secret = toEncryptedDomain(entity.secret),
        issuer = entity.issuer,
        accountName = entity.accountName,
        algorithm = entity.algorithm,
        digits = entity.digits,
        periodSeconds = entity.periodSeconds
    )

    // --- Type-Specific Mappings ---

    fun toPasswordEntity(domain: PasswordItem): PasswordEntity = PasswordEntity(
        itemId = domain.metadata.id,
        username = domain.username,
        password = toEncryptedEntity(domain.password),
        url = domain.url?.value,
        totp = domain.totp?.let { toTotpEntity(it) }
    )

    fun toCreditCardEntity(domain: CreditCard): CreditCardEntity = CreditCardEntity(
        itemId = domain.metadata.id,
        cardholderName = domain.cardholderName,
        cardNumber = toEncryptedEntity(domain.cardNumber),
        expiryMonth = domain.expiryMonth,
        expiryYear = domain.expiryYear,
        cvv = toEncryptedEntity(domain.cvv),
        pin = domain.pin?.let { toEncryptedEntity(it) }
    )

    fun toSecureNoteEntity(domain: SecureNote): SecureNoteEntity = SecureNoteEntity(
        itemId = domain.metadata.id,
        content = toEncryptedEntity(domain.content)
    )

    fun toBankAccountEntity(domain: BankAccount): BankAccountEntity = BankAccountEntity(
        itemId = domain.metadata.id,
        bankName = domain.bankName,
        accountHolderName = domain.accountHolderName,
        accountNumber = toEncryptedEntity(domain.accountNumber),
        routingNumber = domain.routingNumber?.let { toEncryptedEntity(it) },
        iban = domain.iban?.let { toEncryptedEntity(it) },
        swiftCode = domain.swiftCode
    )

    fun toIdentityDocumentEntity(domain: IdentityDocument): IdentityDocumentEntity = IdentityDocumentEntity(
        itemId = domain.metadata.id,
        documentType = domain.documentType,
        fullName = domain.fullName,
        documentNumber = toEncryptedEntity(domain.documentNumber),
        issueDate = domain.issueDate,
        expiryDate = domain.expiryDate,
        issuingCountry = domain.issuingCountry
    )

    fun toSoftwareLicenseEntity(domain: SoftwareLicense): SoftwareLicenseEntity = SoftwareLicenseEntity(
        itemId = domain.metadata.id,
        productName = domain.productName,
        licenseKey = toEncryptedEntity(domain.licenseKey),
        licensedTo = domain.licensedTo,
        purchaseDate = domain.purchaseDate,
        expiryDate = domain.expiryDate,
        vendor = domain.vendor
    )

    fun toApiKeyEntity(domain: ApiKey): ApiKeyEntity = ApiKeyEntity(
        itemId = domain.metadata.id,
        service = domain.service,
        keyValue = toEncryptedEntity(domain.keyValue),
        keySecret = domain.keySecret?.let { toEncryptedEntity(it) },
        environment = domain.environment
    )

    fun toSshKeyEntity(domain: SSHKey): SshKeyEntity = SshKeyEntity(
        itemId = domain.metadata.id,
        keyType = domain.keyType,
        privateKey = toEncryptedEntity(domain.privateKey),
        publicKey = domain.publicKey,
        passphrase = domain.passphrase?.let { toEncryptedEntity(it) },
        hostAddress = domain.hostAddress
    )

    fun toRecoveryCodeEntity(domain: RecoveryCode): RecoveryCodeEntity = RecoveryCodeEntity(
        itemId = domain.metadata.id,
        service = domain.service,
        usedIndicesCsv = domain.usedCodeIndices.joinToString(",")
    )

    fun toRecoveryCodeValues(domain: RecoveryCode): List<RecoveryCodeValueEntity> =
        domain.codes.mapIndexed { index, code ->
            RecoveryCodeValueEntity(
                recoveryCodeItemId = domain.metadata.id,
                sortOrder = index,
                code = toEncryptedEntity(code)
            )
        }

    fun toTotpSeedEntity(domain: TOTPSeed): TotpSeedEntity = TotpSeedEntity(
        itemId = domain.metadata.id,
        config = toTotpEntity(domain.config)
    )

    // --- Full Aggregate Mapping ---

    /**
     * Maps a list of POJOs, resolving notes for each. Exists so callers can
     * stay in a single suspend context instead of nesting suspend calls
     * inside non-suspend `map`/`mapNotNull` lambdas.
     */
    suspend fun toDomainItems(
        pojos: List<FullItemPojo>,
        engine: ItemEncryptionEngine,
        itemDao: ItemDao
    ): List<VaultItem> {
        val result = ArrayList<VaultItem>(pojos.size)
        for (pojo in pojos) result.add(toDomain(pojo, engine, itemDao))
        return result
    }

    suspend fun toDomain(pojo: FullItemPojo, engine: ItemEncryptionEngine, itemDao: ItemDao): VaultItem {
        val summary = ItemSummaryPojo(pojo.item, pojo.tags)
        val metadata = toMetadata(summary, resolveNotes(pojo.item, engine, itemDao))

        val password = pojo.password
        val creditCard = pojo.creditCard
        val secureNote = pojo.secureNote
        val identityDocument = pojo.identityDocument
        val sshKey = pojo.sshKey
        val apiKey = pojo.apiKey
        val bankAccount = pojo.bankAccount
        val softwareLicense = pojo.softwareLicense
        val totpSeed = pojo.totpSeed
        val recoveryCode = pojo.recoveryCode
        return when {
            password != null ->
                PasswordItem(
                    metadata = metadata,
                    username = password.username,
                    password = toEncryptedDomain(password.password),
                    url = password.url?.let { UrlValue(it) },
                    totp = password.totp?.let { toTotpDomain(it) }
                )
            creditCard != null ->
                CreditCard(
                    metadata = metadata,
                    cardholderName = creditCard.cardholderName,
                    cardNumber = toEncryptedDomain(creditCard.cardNumber),
                    expiryMonth = creditCard.expiryMonth,
                    expiryYear = creditCard.expiryYear,
                    cvv = toEncryptedDomain(creditCard.cvv),
                    pin = creditCard.pin?.let { toEncryptedDomain(it) }
                )
            secureNote != null ->
                SecureNote(
                    metadata = metadata,
                    content = toEncryptedDomain(secureNote.content)
                )
            bankAccount != null ->
                BankAccount(
                    metadata = metadata,
                    bankName = bankAccount.bankName,
                    accountHolderName = bankAccount.accountHolderName,
                    accountNumber = toEncryptedDomain(bankAccount.accountNumber),
                    routingNumber = bankAccount.routingNumber?.let { toEncryptedDomain(it) },
                    iban = bankAccount.iban?.let { toEncryptedDomain(it) },
                    swiftCode = bankAccount.swiftCode
                )
            identityDocument != null ->
                IdentityDocument(
                    metadata = metadata,
                    documentType = identityDocument.documentType,
                    fullName = identityDocument.fullName,
                    documentNumber = toEncryptedDomain(identityDocument.documentNumber),
                    issueDate = identityDocument.issueDate,
                    expiryDate = identityDocument.expiryDate,
                    issuingCountry = identityDocument.issuingCountry
                )
            softwareLicense != null ->
                SoftwareLicense(
                    metadata = metadata,
                    productName = softwareLicense.productName,
                    licenseKey = toEncryptedDomain(softwareLicense.licenseKey),
                    licensedTo = softwareLicense.licensedTo,
                    purchaseDate = softwareLicense.purchaseDate,
                    expiryDate = softwareLicense.expiryDate,
                    vendor = softwareLicense.vendor
                )
            apiKey != null ->
                ApiKey(
                    metadata = metadata,
                    service = apiKey.service,
                    keyValue = toEncryptedDomain(apiKey.keyValue),
                    keySecret = apiKey.keySecret?.let { toEncryptedDomain(it) },
                    environment = apiKey.environment
                )
            sshKey != null ->
                SSHKey(
                    metadata = metadata,
                    keyType = sshKey.keyType,
                    privateKey = toEncryptedDomain(sshKey.privateKey),
                    publicKey = sshKey.publicKey,
                    passphrase = sshKey.passphrase?.let { toEncryptedDomain(it) },
                    hostAddress = sshKey.hostAddress
                )
            recoveryCode != null ->
                RecoveryCode(
                    metadata = metadata,
                    service = recoveryCode.service,
                    codes =
                    pojo.recoveryCodeValues
                        .sortedBy { it.sortOrder }
                        .map { toEncryptedDomain(it.code) },
                    // L13: never throw on malformed CSV — drop unparseable tokens.
                    usedCodeIndices =
                    if (recoveryCode.usedIndicesCsv.isEmpty()) {
                        emptySet()
                    } else {
                        recoveryCode.usedIndicesCsv
                            .split(",")
                            .mapNotNull { it.toIntOrNull() }
                            .toSet()
                    }
                )
            totpSeed != null ->
                TOTPSeed(
                    metadata = metadata,
                    config = toTotpDomain(totpSeed.config)
                )
            else -> throw IllegalStateException("FullItemPojo with ID ${pojo.item.id} has no type-specific details")
        }
    }

    // --- Password History ---

    fun toHistoryDomain(entity: PasswordHistoryEntity): PasswordHistory = PasswordHistory(
        id = entity.id,
        itemId = entity.itemId,
        password = toEncryptedDomain(entity.password),
        changedAt = entity.changedAt
    )

    fun toHistoryEntity(domain: PasswordHistory, itemId: ItemId): PasswordHistoryEntity = PasswordHistoryEntity(
        id = domain.id,
        itemId = itemId,
        password = toEncryptedEntity(domain.password),
        changedAt = domain.changedAt
    )
}
