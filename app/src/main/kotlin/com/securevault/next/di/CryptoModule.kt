package com.securevault.next.di

import com.securevault.next.core.contracts.VaultKeyEncryptionService
import com.securevault.next.core.crypto.cipher.AesGcmCipherEngine
import com.securevault.next.core.crypto.cipher.CipherEngine
import com.securevault.next.core.crypto.integrity.HmacSha256IntegrityVerifier
import com.securevault.next.core.crypto.integrity.IntegrityVerifier
import com.securevault.next.core.crypto.kdf.Argon2idKeyDerivationEngine
import com.securevault.next.core.crypto.kdf.KeyDerivationEngine
import com.securevault.next.core.crypto.key.DefaultKeyRotationManager
import com.securevault.next.core.crypto.key.KeyRotationManager
import com.securevault.next.core.crypto.key.SessionKeyHolder
import com.securevault.next.core.crypto.key.SessionKeyHolderStore
import com.securevault.next.core.crypto.random.SecureRandomProvider
import com.securevault.next.core.crypto.random.SystemSecureRandomProvider
import com.securevault.next.core.crypto.vault.DefaultItemEncryptionEngine
import com.securevault.next.core.crypto.vault.DefaultVaultEncryptionEngine
import com.securevault.next.core.crypto.vault.DefaultVaultKeyEncryptionService
import com.securevault.next.core.crypto.vault.VaultEncryptionEngine
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.security.session.FailedAttemptTracker
import com.securevault.next.core.security.session.SessionClock
import com.securevault.next.core.security.session.SessionKeyStore
import com.securevault.next.core.security.session.SessionListener
import com.securevault.next.core.security.session.SystemSessionClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Completes the Hilt graph for the crypto layer (Finding F3).
 *
 * These bindings are the *only* providers of the three crypto
 * capabilities that item encryption consumes. The concrete
 * implementations are the existing, already-audited ones — nothing here
 * re-implements cryptography, it only tells Hilt how to construct and
 * scope them:
 *
 * - [SecureRandomProvider] → [SystemSecureRandomProvider] (the single,
 *   cryptographically-secure random source used across `core:crypto`).
 * - [CipherEngine] → [AesGcmCipherEngine], which itself receives the
 *   [SecureRandomProvider] above.
 * - [ItemEncryptionEngine] → [DefaultItemEncryptionEngine], which
 *   receives the [CipherEngine] and the shared [SessionKeyHolder].
 *
 * [SessionKeyHolder] is provided as a singleton here rather than relying
 * on its public no-arg constructor so there is exactly one in-memory
 * session-key holder in the whole application — the invariant
 * `DefaultItemEncryptionEngine` (and the session work in later
 * milestones) depends on.
 *
 * [KeyRotationManager] is provided via `@Provides` because
 * [DefaultKeyRotationManager] deliberately has no `@Inject` constructor
 * (Finding M10/16c); this module is the single place that binding lives.
 *
 * There are no duplicate providers: no other module binds any of these
 * types.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindSecureRandomProvider(impl: SystemSecureRandomProvider): SecureRandomProvider

    @Binds
    @Singleton
    abstract fun bindCipherEngine(impl: AesGcmCipherEngine): CipherEngine

    @Binds
    @Singleton
    abstract fun bindItemEncryptionEngine(impl: DefaultItemEncryptionEngine): ItemEncryptionEngine

    @Binds
    @Singleton
    abstract fun bindKeyDerivationEngine(impl: Argon2idKeyDerivationEngine): KeyDerivationEngine

    @Binds
    @Singleton
    abstract fun bindIntegrityVerifier(impl: HmacSha256IntegrityVerifier): IntegrityVerifier

    @Binds
    @Singleton
    abstract fun bindVaultEncryptionEngine(impl: DefaultVaultEncryptionEngine): VaultEncryptionEngine

    @Binds
    @Singleton
    abstract fun bindVaultKeyEncryptionService(impl: DefaultVaultKeyEncryptionService): VaultKeyEncryptionService

    @Binds
    @Singleton
    abstract fun bindSessionKeyStore(impl: SessionKeyHolderStore): SessionKeyStore

    @Binds
    @Singleton
    abstract fun bindSessionClock(impl: SystemSessionClock): SessionClock

    companion object {
        @Provides
        @JvmStatic
        @Singleton
        fun provideSessionKeyHolder(): SessionKeyHolder = SessionKeyHolder()

        @Provides
        @JvmStatic
        @Singleton
        fun provideSessionListeners(): Set<SessionListener> = emptySet()

        @Provides
        @JvmStatic
        @Singleton
        fun provideFailedAttemptTracker(clock: SessionClock): FailedAttemptTracker =
            FailedAttemptTracker(
                clock = clock,
                maxAttempts = VaultSecurityPolicy().maxFailedUnlockAttempts,
            )

        // Finding 16c: DefaultKeyRotationManager has no @Inject
        // constructor, so it is bound here — the crypto layer's Hilt
        // module — rather than by annotation.
        @Provides
        @JvmStatic
        @Singleton
        fun provideKeyRotationManager(
            keyDerivationEngine: KeyDerivationEngine,
            cipherEngine: CipherEngine,
            integrityVerifier: IntegrityVerifier,
            secureRandomProvider: SecureRandomProvider,
        ): KeyRotationManager = DefaultKeyRotationManager(
            keyDerivationEngine = keyDerivationEngine,
            cipherEngine = cipherEngine,
            integrityVerifier = integrityVerifier,
            secureRandomProvider = secureRandomProvider,
        )
    }
}
