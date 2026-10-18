package com.securevault.next.data.persistence.pojo

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.securevault.next.data.persistence.entity.ApiKeyEntity
import com.securevault.next.data.persistence.entity.BankAccountEntity
import com.securevault.next.data.persistence.entity.CreditCardEntity
import com.securevault.next.data.persistence.entity.IdentityDocumentEntity
import com.securevault.next.data.persistence.entity.ItemEntity
import com.securevault.next.data.persistence.entity.ItemTagCrossRef
import com.securevault.next.data.persistence.entity.PasswordEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeEntity
import com.securevault.next.data.persistence.entity.RecoveryCodeValueEntity
import com.securevault.next.data.persistence.entity.SecureNoteEntity
import com.securevault.next.data.persistence.entity.SoftwareLicenseEntity
import com.securevault.next.data.persistence.entity.SshKeyEntity
import com.securevault.next.data.persistence.entity.TagEntity
import com.securevault.next.data.persistence.entity.TotpSeedEntity

/**
 * The complete aggregate of a vault item, including metadata, tags, and
 * its specific details (if any).
 */
data class FullItemPojo(
    @Embedded
    val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
        Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "item_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val password: PasswordEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val creditCard: CreditCardEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val secureNote: SecureNoteEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val bankAccount: BankAccountEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val identityDocument: IdentityDocumentEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val softwareLicense: SoftwareLicenseEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val apiKey: ApiKeyEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val sshKey: SshKeyEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val recoveryCode: RecoveryCodeEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "recovery_code_item_id")
    val recoveryCodeValues: List<RecoveryCodeValueEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val totpSeed: TotpSeedEntity? = null
)
