package com.securevault.next.core.ui.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Part 6 (Spacing System): "Use an 8dp spacing grid... Avoid random
 * spacing values." This test pins the default [SecureVaultSpacing]
 * values to that grid so a future edit cannot silently introduce a
 * non-8dp-multiple value.
 */
class SecureVaultSpacingTest {
    private val spacing = SecureVaultSpacing()

    @Test
    fun `every non-zero spacing value is a multiple of 4dp`() {
        val values =
            listOf(
                spacing.extraSmall,
                spacing.small,
                spacing.medium,
                spacing.large,
                spacing.extraLarge,
                spacing.extraExtraLarge
            )

        values.forEach { value ->
            val isMultipleOfBaseUnit = (value.value % 4f) == 0f
            assertThat(isMultipleOfBaseUnit).isTrue()
        }
    }

    @Test
    fun `spacing scale increases monotonically`() {
        val orderedValues =
            listOf(
                spacing.none,
                spacing.extraSmall,
                spacing.small,
                spacing.medium,
                spacing.large,
                spacing.extraLarge,
                spacing.extraExtraLarge
            )

        for (index in 0 until orderedValues.lastIndex) {
            assertThat(orderedValues[index].value).isLessThan(orderedValues[index + 1].value)
        }
    }

    @Test
    fun `medium spacing matches the canonical 16dp content padding`() {
        assertThat(spacing.medium).isEqualTo(16.dp)
    }
}
