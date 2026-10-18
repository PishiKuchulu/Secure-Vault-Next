package com.securevault.next.presentation.splash

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.securevault.next.core.domain.model.VaultMetadata
import com.securevault.next.core.domain.model.enums.VaultStatus
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.Revision
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.model.valueobject.VaultName
import com.securevault.next.core.domain.model.valueobject.Version
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun metadata(id: String) =
        VaultMetadata(
            id = VaultId(id),
            name = VaultName("Test"),
            status = VaultStatus.ACTIVE,
            version = Version(1),
            revision = Revision.INITIAL,
            createdAt = CreatedAt(Instant.parse("2026-10-17T00:00:00Z")),
            updatedAt = UpdatedAt(Instant.parse("2026-10-17T00:00:00Z")),
        )

    @Test
    fun `emits NavigateToUnlock when a vault exists`() =
        runTest {
            val repository =
                mockk<VaultRepository> {
                    every { observeAllVaultMetadata() } returns flowOf(listOf(metadata("vault-1")))
                }
            val viewModel = SplashViewModel(repository)

            viewModel.events.test {
                assertThat(awaitItem()).isEqualTo(SplashUiEvent.NavigateToUnlock("vault-1"))
            }
        }

    @Test
    fun `emits NavigateToOnboarding when no vault exists`() =
        runTest {
            val repository =
                mockk<VaultRepository> {
                    every { observeAllVaultMetadata() } returns flowOf(emptyList())
                }
            val viewModel = SplashViewModel(repository)

            viewModel.events.test {
                assertThat(awaitItem()).isEqualTo(SplashUiEvent.NavigateToOnboarding)
            }
        }
}
