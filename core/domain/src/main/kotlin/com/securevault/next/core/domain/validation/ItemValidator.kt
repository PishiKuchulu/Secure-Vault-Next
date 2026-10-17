package com.securevault.next.core.domain.validation

import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.ValidationResult
import com.securevault.next.core.domain.model.VaultItemMetadata

/**
 * Validates the title every [com.securevault.next.core.domain.model.item.VaultItem]
 * carries via its [VaultItemMetadata] — the pre-construction check a
 * form runs before attempting to build a [VaultItemMetadata] at all, the
 * same way [VaultNameValidator] relates to
 * [com.securevault.next.core.domain.model.valueobject.VaultName].
 * Every other [VaultItemMetadata] field is either optional or already
 * invariant-checked at construction by [VaultItemMetadata]'s own `init`
 * block.
 */
class ItemValidator : Validator<String> {

    override fun validate(value: String): ValidationResult {
        val errors = mutableListOf<DomainError.ValidationError>()

        if (value.isBlank()) {
            errors += DomainError.ValidationError.BlankValue(FIELD_NAME)
        } else if (value.length > VaultItemMetadata.MAX_TITLE_LENGTH) {
            errors += DomainError.ValidationError.TooLong(
                field = FIELD_NAME,
                maxLength = VaultItemMetadata.MAX_TITLE_LENGTH,
                actualLength = value.length,
            )
        }

        return ValidationResult.of(errors)
    }

    private companion object {
        const val FIELD_NAME = "itemTitle"
    }
}
