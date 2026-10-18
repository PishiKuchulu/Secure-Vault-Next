package com.securevault.next.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Part 6 (Design Principles / Premium Feel): shapes stay rounded but
 * restrained — clarity over visual effects, no gimmicks.
 *
 * SHAPE LOCK (stated rule, never violated):
 * - Buttons (every Vault*Button): pill — fully rounded ends
 * - Cards (VaultCard, panels): 16dp
 * - Inputs (every Vault*TextField): 12dp
 * - Bottom sheets: 28dp top corners only
 * - Chips, badges, small containers: 8dp
 * Nothing else gets a corner radius. Mixed radii without this rule
 * read as assembled-from-parts.
 */
internal val SecureVaultShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
