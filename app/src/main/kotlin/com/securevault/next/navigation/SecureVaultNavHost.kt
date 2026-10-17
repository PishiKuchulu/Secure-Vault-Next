package com.securevault.next.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securevault.next.autolock.AutoLockEvent
import com.securevault.next.autolock.AutoLockViewModel
import com.securevault.next.presentation.createvault.CreateVaultScreen
import com.securevault.next.presentation.settings.SettingsScreen
import com.securevault.next.presentation.splash.SplashScreen
import com.securevault.next.presentation.unlock.UnlockScreen
import com.securevault.next.presentation.vault.GeneratorScreen
import com.securevault.next.presentation.vault.ItemDetailScreen
import com.securevault.next.presentation.vault.ItemEditorScreen
import com.securevault.next.presentation.vault.VaultHomeScreen
import com.securevault.next.presentation.welcome.WelcomeScreen

/**
 * The application's single top-level [NavHost].
 *
 * Navigation grammar:
 * - Onboarding is a one-way door: splash resolves to either the welcome
 *   flow or the unlock screen, and [androidx.navigation.NavController]
 *   never lets back re-enter a finished auth step.
 * - Unlock is a one-way door: every path that lands on the unlock screen
 *   (fresh launch, manual lock, auto-lock) clears the vault home from the
 *   back stack, so back from unlock exits the app instead of revealing a
 *   locked vault.
 * - Everything under the vault home (detail, editor, generator,
 *   settings) is a push; back returns to where the user came from.
 */
@Composable
fun SecureVaultNavHost(
    navController: NavHostController = rememberNavController(),
    autoLockViewModel: AutoLockViewModel = hiltViewModel(),
    lockActionViewModel: LockActionViewModel = hiltViewModel(),
) {
    // Auto-lock fires while the user is away: land on the unlock screen
    // with the home cleared, exactly like a manual lock.
    LaunchedEffect(autoLockViewModel) {
        autoLockViewModel.events.collect { event ->
            when (event) {
                is AutoLockEvent.AutoLocked -> {
                    // The timer can fire just after a manual lock landed
                    // here already; never stack two unlock screens.
                    if (navController.currentDestination?.route !=
                        SecureVaultDestination.VaultUnlock.route
                    ) {
                        navController.navigateToUnlockClearingHome(event.vaultId)
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SecureVaultDestination.Splash.route,
    ) {
        composable(route = SecureVaultDestination.Splash.route) {
            SplashScreen(
                onUnlockExisting = { vaultId ->
                    navController.navigate(SecureVaultDestination.VaultUnlock.createRoute(vaultId)) {
                        popUpTo(SecureVaultDestination.Splash.route) { inclusive = true }
                    }
                },
                onOnboard = {
                    navController.navigate(SecureVaultDestination.Welcome.route) {
                        popUpTo(SecureVaultDestination.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = SecureVaultDestination.Welcome.route) {
            WelcomeScreen(
                onCreateVault = {
                    navController.navigate(SecureVaultDestination.CreateVault.route)
                },
                onUnlockExisting = { vaultId ->
                    navController.navigate(SecureVaultDestination.VaultUnlock.createRoute(vaultId)) {
                        popUpTo(SecureVaultDestination.Welcome.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = SecureVaultDestination.CreateVault.route) {
            CreateVaultScreen(
                onNavigateBack = { navController.popBackStack() },
                onVaultCreated = {
                    // The home screen reads the fresh session; no id needed.
                    navController.navigate(SecureVaultDestination.VaultHome.route) {
                        popUpTo(SecureVaultDestination.Welcome.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = SecureVaultDestination.VaultUnlock.route,
            arguments = listOf(navArgument("vaultId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId").orEmpty()
            UnlockScreen(
                vaultId = vaultId,
                onUnlocked = {
                    navController.navigate(SecureVaultDestination.VaultHome.route) {
                        popUpTo(SecureVaultDestination.VaultUnlock.route) { inclusive = true }
                    }
                },
            )
        }

        // The contract vault home, reachable through the canonical route…
        composable(route = SecureVaultDestination.VaultHome.route) {
            VaultHomeContent(
                navController = navController,
                lockActionViewModel = lockActionViewModel,
                homeRoute = SecureVaultDestination.VaultHome.route,
            )
        }

        // …and through the Milestone 1 "dashboard" route string, kept for
        // back-compat. Both render the same screen.
        composable(route = SecureVaultDestination.Dashboard.route) {
            VaultHomeContent(
                navController = navController,
                lockActionViewModel = lockActionViewModel,
                homeRoute = SecureVaultDestination.Dashboard.route,
            )
        }

        composable(
            route = SecureVaultDestination.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
            ItemDetailScreen(
                itemId = itemId,
                onEdit = { id ->
                    navController.navigate(SecureVaultDestination.ItemEditor.createRoute(id))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = SecureVaultDestination.ItemEditor.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            ItemEditorScreen(
                itemId = backStackEntry.arguments?.getString("itemId"),
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = SecureVaultDestination.PasswordGenerator.route) {
            GeneratorScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = SecureVaultDestination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLockNow = {
                    lockActionViewModel.lockNow { vaultId ->
                        navController.navigateToUnlockClearingHome(vaultId)
                    }
                },
            )
        }
    }
}

/**
 * The vault home screen with its five navigation actions wired.
 *
 * [homeRoute] is which route string rendered this instance (the
 * canonical "vault_home" or the back-compat "dashboard" alias): the
 * lock path pops exactly that route so a locked vault is never left
 * behind unlock on the back stack.
 */
@Composable
private fun VaultHomeContent(
    navController: NavHostController,
    lockActionViewModel: LockActionViewModel,
    homeRoute: String,
) {
    VaultHomeScreen(
        onOpenItem = { itemId ->
            navController.navigate(SecureVaultDestination.ItemDetail.createRoute(itemId))
        },
        onAddItem = {
            navController.navigate(SecureVaultDestination.ItemEditor.createRoute(null))
        },
        onOpenGenerator = {
            navController.navigate(SecureVaultDestination.PasswordGenerator.route)
        },
        onOpenSettings = {
            navController.navigate(SecureVaultDestination.Settings.route)
        },
        onLock = {
            lockActionViewModel.lockNow { vaultId ->
                navController.navigate(SecureVaultDestination.VaultUnlock.createRoute(vaultId)) {
                    popUpTo(homeRoute) { inclusive = true }
                }
            }
        },
    )
}

/**
 * Navigates to the unlock screen for [vaultId] with any vault-home
 * instance cleared from the back stack — through the canonical route
 * and through the back-compat "dashboard" alias, since a popUpTo that
 * matches nothing is a no-op rather than an error.
 */
private fun NavHostController.navigateToUnlockClearingHome(vaultId: String) {
    navigate(SecureVaultDestination.VaultUnlock.createRoute(vaultId)) {
        popUpTo(SecureVaultDestination.VaultHome.route) { inclusive = true }
    }
    popBackStack(SecureVaultDestination.Dashboard.route, inclusive = true)
}
