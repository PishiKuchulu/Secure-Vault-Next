package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.EncryptedField
import kotlinx.serialization.Serializable

/**
 * A user-defined extra field attached to a
 * [com.securevault.next.core.domain.model.item.VaultItem] — e.g. a
 * "Security Question" field on a [com.securevault.next.core.domain.model.item.PasswordItem]
 * that doesn't fit any of the item's built-in fields.
 *
 * The value is always stored as an [EncryptedField]: custom fields are
 * exactly as likely to hold sensitive data as any built-in field, so
 * there is no "plaintext custom field" variant. [isSensitive] instead
 * controls presentation (e.g. masked-by-default vs. shown-by-default in
 * a future UI), not whether encryption is applied.
 *
 * Carries [Serializable] because `data:repository`'s item mapper
 * persists `List<CustomField>` as JSON via kotlinx.serialization.
 */
@Serializable
data class CustomField(
    val label: String,
    val value: EncryptedField,
    val isSensitive: Boolean = true,
) {
    init {
        require(label.isNotBlank()) { "CustomField.label must not be blank" }
    }
}
