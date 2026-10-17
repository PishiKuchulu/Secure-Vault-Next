package com.securevault.next.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.R
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.ui.components.VaultButton
import com.securevault.next.core.ui.components.VaultSecondaryButton
import com.securevault.next.core.ui.components.VaultTextButton
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    vaultRepository: VaultRepository,
) : ViewModel() {
    /** The id of an existing vault, when the user has one to unlock instead of creating. */
    val existingVaultId: StateFlow<String?> = vaultRepository.observeAllVaultMetadata()
        .map { vaults -> vaults.firstOrNull()?.id?.value }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private data class OnboardingPage(
    val icon: ImageVector,
    val iconDescriptionRes: Int,
    val titleRes: Int,
    val bodyRes: Int,
)

/**
 * Three-page value introduction with skip. One idea per page, one
 * primary action at the end: create a vault, or unlock the existing
 * one when there is one.
 */
@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onUnlockExisting: (vaultId: String) -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val existingVaultId by viewModel.existingVaultId.collectAsState()

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.VpnKey,
            iconDescriptionRes = R.string.welcome_page1_title,
            titleRes = R.string.welcome_page1_title,
            bodyRes = R.string.welcome_page1_body,
        ),
        OnboardingPage(
            icon = Icons.Filled.Fingerprint,
            iconDescriptionRes = R.string.welcome_page2_title,
            titleRes = R.string.welcome_page2_title,
            bodyRes = R.string.welcome_page2_body,
        ),
        OnboardingPage(
            icon = Icons.Filled.Timer,
            iconDescriptionRes = R.string.welcome_page3_title,
            titleRes = R.string.welcome_page3_title,
            bodyRes = R.string.welcome_page3_body,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    val finishOnboarding = {
        val id = existingVaultId
        if (id != null) onUnlockExisting(id) else onCreateVault()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.baseBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                VaultTextButton(
                    text = stringResource(R.string.welcome_skip),
                    onClick = finishOnboarding,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) { index ->
                val page = pages[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium),
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(colors.accentSoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = stringResource(page.iconDescriptionRes),
                            modifier = Modifier.size(56.dp),
                            tint = colors.accent,
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.large))
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.label,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            PageIndicators(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(spacing.large))

            VaultButton(
                text = stringResource(R.string.welcome_create_vault),
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
            )

            if (existingVaultId != null) {
                Spacer(modifier = Modifier.height(spacing.small))
                VaultSecondaryButton(
                    text = stringResource(R.string.welcome_open_existing),
                    onClick = { onUnlockExisting(existingVaultId!!) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraLarge))
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val indicatorDescription = stringResource(
        R.string.welcome_page_indicator,
        currentPage + 1,
        pageCount,
    )

    Row(
        modifier = modifier.semantics { contentDescription = indicatorDescription },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .background(
                        color = if (active) colors.accent else colors.fill,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
