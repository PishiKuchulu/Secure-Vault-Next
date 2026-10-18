package com.securevault.next.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.core.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot navigation events the Splash screen can emit.
 *
 * The splash resolves the app's entry state — an existing vault goes
 * straight to its unlock screen (no login flash before home, no
 * welcome screen for a returning user), a fresh install goes to
 * onboarding. There is deliberately no artificial delay: the check is a
 * single local database read.
 */
sealed interface SplashUiEvent {
    data class NavigateToUnlock(
        val vaultId: String,
    ) : SplashUiEvent

    data object NavigateToOnboarding : SplashUiEvent
}

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val vaultRepository: VaultRepository,
    ) : ViewModel() {
        private val eventChannel = Channel<SplashUiEvent>(capacity = Channel.BUFFERED)
        val events: Flow<SplashUiEvent> = eventChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                val firstVault =
                    runCatching {
                        vaultRepository.observeAllVaultMetadata().first().firstOrNull()
                    }.getOrNull()
                if (firstVault != null) {
                    eventChannel.send(SplashUiEvent.NavigateToUnlock(firstVault.id.value))
                } else {
                    eventChannel.send(SplashUiEvent.NavigateToOnboarding)
                }
            }
        }
    }
