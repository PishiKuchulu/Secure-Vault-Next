package com.securevault.next

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.securevault.next.core.ui.theme.SecureVaultTheme
import com.securevault.next.navigation.SecureVaultNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * The application's single [FragmentActivity].
 *
 * Part 2 (Presentation Layer): "Jetpack Compose only." There is
 * deliberately no XML layout, no Fragment and no second Activity —
 * every screen is a composable reached through [SecureVaultNavHost].
 * This class itself contains no business logic; it only installs the
 * system splash screen, enables edge-to-edge rendering and hosts the
 * Compose tree.
 *
 * It extends [FragmentActivity] (rather than ComponentActivity) solely
 * because androidx.biometric's BiometricPrompt requires a
 * FragmentActivity host for the biometric unlock flow.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() and before setContent
        // per the androidx.core.splashscreen contract.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SecureVaultTheme {
                SecureVaultNavHost()
            }
        }
    }
}
