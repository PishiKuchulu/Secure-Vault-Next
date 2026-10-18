package com.securevault.next.navigation

/**
 * Every top-level destination the app-level [androidx.navigation.NavHost]
 * can navigate to.
 *
 * The vault feature screens ([VaultHome], [ItemDetail], [ItemEditor],
 * [PasswordGenerator]) are implemented in
 * `com.securevault.next.presentation.vault` against the exact composable
 * signatures in the navigation contract; this file only names their
 * routes.
 */
sealed class SecureVaultDestination(val route: String) {
    // --- Onboarding & Auth ---
    data object Splash : SecureVaultDestination("splash")

    data object Welcome : SecureVaultDestination("welcome")

    data object CreateVault : SecureVaultDestination("create_vault")

    data object VaultUnlock : SecureVaultDestination("vault_unlock/{vaultId}") {
        fun createRoute(vaultId: String) = "vault_unlock/$vaultId"
    }

    // --- Main Vault ---
    data object VaultHome : SecureVaultDestination("vault_home")

    /**
     * Back-compat alias for [VaultHome]: the Milestone 1 route string
     * "dashboard" still renders the vault home screen, so any persisted
     * deep link or old navigation call keeps working.
     */
    data object Dashboard : SecureVaultDestination("dashboard")

    // --- Items (implemented in presentation.vault) ---
    data object ItemDetail : SecureVaultDestination("item_detail/{itemId}") {
        fun createRoute(itemId: String) = "item_detail/$itemId"
    }

    data object ItemEditor : SecureVaultDestination("item_editor?itemId={itemId}") {
        fun createRoute(itemId: String?) = if (itemId == null) "item_editor" else "item_editor?itemId=$itemId"
    }

    data object PasswordGenerator : SecureVaultDestination("password_generator")

    // --- Settings ---
    data object Settings : SecureVaultDestination("settings")
}
