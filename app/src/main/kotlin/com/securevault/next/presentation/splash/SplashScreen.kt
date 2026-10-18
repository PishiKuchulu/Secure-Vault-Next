package com.securevault.next.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.next.R
import com.securevault.next.core.ui.theme.SECURE_VAULT_STATE_CHANGE_DURATION_MS
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultEaseOut
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Branded launch screen: the app mark fading and settling in over
 * 250ms with the app easing, then handing off the moment the entry
 * state resolves. Fast by design — no artificial delays, no progress
 * theater.
 */
@Composable
fun SplashScreen(
    onUnlockExisting: (vaultId: String) -> Unit,
    onOnboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    SplashScreenContent(
        events = viewModel.events,
        onUnlockExisting = onUnlockExisting,
        onOnboard = onOnboard,
    )
}

@Composable
private fun SplashScreenContent(
    events: Flow<SplashUiEvent>,
    onUnlockExisting: (vaultId: String) -> Unit,
    onOnboard: () -> Unit,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is SplashUiEvent.NavigateToUnlock -> onUnlockExisting(event.vaultId)
                SplashUiEvent.NavigateToOnboarding -> onOnboard()
            }
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = SECURE_VAULT_STATE_CHANGE_DURATION_MS,
                easing = SecureVaultEaseOut,
            ),
        label = "splashAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec =
            tween(
                durationMillis = SECURE_VAULT_STATE_CHANGE_DURATION_MS,
                easing = SecureVaultEaseOut,
            ),
        label = "splashScale",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.baseBackground,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .background(colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.splash_logo_description),
                    modifier = Modifier.size(44.dp),
                    tint = colors.accent,
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.label,
                modifier = Modifier.padding(top = spacing.large),
            )
        }
    }
}
