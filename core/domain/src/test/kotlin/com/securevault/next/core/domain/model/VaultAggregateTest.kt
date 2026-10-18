package com.securevault.next.core.domain.model

import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.model.valueobject.Version
import java.time.Instant
import org.junit.Test

class VaultAggregateTest {

    @Test
    fun `a PasswordItem can be built and reports the correct ItemType`() {
        val now = Instant.now()
        val vaultId = VaultId("vault-1")

        val item = PasswordItem(
            metadata = VaultItemMetadata(
                id = ItemId("item-1"),
                vaultId = vaultId,
                title = "GitHub",
                createdAt = CreatedAt(now),
                updatedAt = UpdatedAt(now),
            ),
            username = "octocat",
            password = EncryptedField(ciphertext = "cipher", nonce = "nonce"),
        )

        assertThat(item.type).isEqualTo(ItemType.PASSWORD)
        assertThat(item.metadata.title).isEqualTo("GitHub")
    }

    @Test
    fun `a Vault aggregates metadata, items, categories, tags, settings and statistics together`() {
        val now = Instant.now()
        val vaultId = VaultId("vault-1")

        val metadata = VaultMetadata(
            id = vaultId,
            name = VaultName("Personal"),
            version = Version(1),
            createdAt = CreatedAt(now),
            updatedAt = UpdatedAt(now),
        )

        val statistics = VaultStatistics(
            itemCount = 0,
            categoryCount = 0,
            favoriteCount = 0,
            weakPasswordCount = 0,
            reusedPasswordCount = 0,
            oldPasswordCount = 0,
            breachedPasswordCount = 0,
            securityScore = SecurityScore(100),
        )

        val vault = Vault(
            metadata = metadata,
            items = emptyList(),
            categories = emptyList(),
            tags = emptyList(),
            settings = VaultSettings(),
            securityPolicy = VaultSecurityPolicy(),
            statistics = statistics,
        )

        assertThat(vault.metadata.name.value).isEqualTo("Personal")
        assertThat(vault.statistics.securityScore.value).isEqualTo(100)
    }
}
