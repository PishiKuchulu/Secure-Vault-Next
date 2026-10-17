package com.securevault.next.core.domain.model

import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.PasswordId
import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import java.time.Instant

/**
 * A previously used password value for a
 * [com.securevault.next.core.domain.model.item.PasswordItem], retained
 * so a user can recover an old password if a change turns out to have
 * been a mistake, and so
 * [com.securevault.next.core.domain.usecase.RunSecurityAuditUseCase]
 * can detect password reuse across an item's own history, not just
 * across different items.
 */
data class PasswordHistory(
    val id: PasswordId,
    val itemId: ItemId,
    val password: EncryptedField,
    val changedAt: Instant,
    val strength: PasswordStrength? = null,
)
