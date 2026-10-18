package com.securevault.next.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.securevault.next.core.common.memory.MemoryWiper
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.crypto.error.CryptoError
import com.securevault.next.core.crypto.key.SessionKeyHolder
import com.securevault.next.core.crypto.keystore.AndroidKeystoreKeyProvider
import com.securevault.next.core.crypto.keystore.SystemAndroidKeystoreKeyProvider
import com.securevault.next.core.crypto.memory.SecureBytes
import com.securevault.next.core.domain.model.VaultSession
import com.securevault.next.core.domain.model.enums.AuthenticationMethod
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.security.session.FailedAttemptTracker
import com.securevault.next.core.security.session.SessionClock
import com.securevault.next.core.security.session.VaultSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Biometric quick-unlock for SecureVault Next.
 *
 * Key design (envelope wrapping, not password storage):
 * - After a successful *password* unlock, while the session key is in
 *   memory, [enable] generates a biometric-bound AES-256 key in the
 *   Android Keystore under a per-vault alias and wraps (encrypts) the
 *   vault's session key with it. Only the wrapped blob — ciphertext,
 *   never key material — is persisted in private SharedPreferences.
 * - The Keystore key is created with
 *   `setUserAuthenticationRequired(true)`,
 *   `AUTH_BIOMETRIC_STRONG` (API 30+), and
 *   `setInvalidatedByBiometricEnrollment(true)`: the hardware refuses
 *   to use it unless a strong biometric was presented recently, and a
 *   newly enrolled biometric permanently invalidates it.
 * - On biometric unlock, [BiometricPrompt] performs the user
 *   authentication; within the key's validity window the manager then
 *   unwraps the vault key and establishes a [VaultSession] with
 *   [AuthenticationMethod.BIOMETRIC] through [VaultSessionManager] —
 *   the same session-establishment path a password unlock uses, so the
 *   rest of the app cannot tell the methods apart.
 * - The master password is never stored, and the unwrapped vault key
 *   exists only in memory for the unlock call, wiped immediately after
 *   the session key store installs its own copy.
 *
 * Ordering constraint: a biometric-bound key requires a *recent*
 * authentication for every use, including the wrapping encrypt during
 * [enable]. Callers must therefore run [authenticate] (the Biometric
 * prompt) first and call [enable] from its success callback.
 *
 * [SystemAndroidKeystoreKeyProvider] is instantiated directly: it is a
 * concrete, no-arg class and the app module owns this manager, so no
 * `di/` module change is needed. If the engine later binds
 * [AndroidKeystoreKeyProvider] in the Hilt graph, this constructor
 * parameter should switch to the injected interface.
 */
@Singleton
class BiometricKeyManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val sessionKeyHolder: SessionKeyHolder,
        private val sessionManager: VaultSessionManager,
        private val failedAttemptTracker: FailedAttemptTracker,
        private val clock: SessionClock,
    ) {
        private val keystore: AndroidKeystoreKeyProvider = SystemAndroidKeystoreKeyProvider()

        private val prefs by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        /** True when the device can perform strong biometric authentication right now. */
        fun canAuthenticate(): Boolean =
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

        /** True when biometric unlock is enrolled for [vaultId] (flag set and a wrapped blob stored). */
        fun isEnabled(vaultId: VaultId): Boolean =
            prefs.getBoolean(enabledKey(vaultId), false) && wrappedBlob(vaultId) != null

        /**
         * Shows the system biometric prompt. Must be called on the main
         * thread (e.g. from a click handler). [onSuccess] fires after the
         * user authenticates; [onError] carries a human-readable message.
         */
        fun authenticate(
            activity: FragmentActivity,
            title: String,
            subtitle: String,
            negativeButtonText: String,
            onSuccess: () -> Unit,
            onError: (message: String) -> Unit,
        ) {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            onError(errString.toString())
                        }

                        override fun onAuthenticationFailed() {
                            // Deliberately silent: the system already shows its
                            // own "not recognized" feedback; a second message
                            // would double-report the same event.
                        }
                    },
                )
            val promptInfo =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setNegativeButtonText(negativeButtonText)
                    .build()
            prompt.authenticate(promptInfo)
        }

        /**
         * Enrolls biometric unlock for [vaultId]. Call only while the vault
         * is unlocked (the session key is read from memory) and only after
         * [authenticate] succeeded, so the wrapping encrypt is inside the
         * key's authentication validity window.
         */
        suspend fun enable(vaultId: VaultId): EnableResult =
            withContext(Dispatchers.IO) {
                val alias = aliasFor(vaultId)
                val sessionKey =
                    sessionKeyHolder.get()
                        ?: return@withContext EnableResult.NoActiveSession

                when (val generated = keystore.generateKey(alias, requireUserAuthentication = true)) {
                    is Result.Failure ->
                        return@withContext EnableResult.KeystoreError(generated.error.message)
                    is Result.Success -> Unit
                }

                val wrapped: EncryptedField =
                    try {
                        sessionKey.bytes.copy().use { keyCopy ->
                            keystore.encrypt(alias, keyCopy).getOrNull()
                                ?: return@withContext EnableResult.KeystoreError("wrapping the vault key failed")
                        }
                    } catch (t: Throwable) {
                        return@withContext EnableResult.KeystoreError(t.message ?: "wrapping the vault key failed")
                    }

                prefs
                    .edit()
                    .putString(blobKey(vaultId), blobToJson(wrapped))
                    .putBoolean(enabledKey(vaultId), true)
                    .apply()
                EnableResult.Success
            }

        /** Removes the biometric enrollment for [vaultId]: deletes the Keystore key and the wrapped blob. */
        suspend fun disable(vaultId: VaultId): Unit =
            withContext(Dispatchers.IO) {
                keystore.deleteKey(aliasFor(vaultId))
                prefs
                    .edit()
                    .remove(blobKey(vaultId))
                    .putBoolean(enabledKey(vaultId), false)
                    .apply()
            }

        /**
         * Unlocks [vaultId] with the biometric-wrapped vault key. Call only
         * after [authenticate] succeeded.
         */
        suspend fun unlockWithBiometrics(vaultId: VaultId): BiometricUnlockResult =
            withContext(Dispatchers.IO) {
                if (sessionManager.isUnlocked()) {
                    return@withContext BiometricUnlockResult.AlreadyUnlocked
                }
                val blob =
                    wrappedBlob(vaultId)
                        ?: return@withContext BiometricUnlockResult.KeyInvalidated

                val decrypted: SecureBytes =
                    when (
                        val result = keystore.decrypt(aliasFor(vaultId), blob)
                    ) {
                        is Result.Success -> result.value
                        is Result.Failure -> {
                            return@withContext when (result.error) {
                                // The key is gone or permanently invalidated (e.g.
                                // a new biometric was enrolled): the enrollment is
                                // dead, clear it so the UI falls back to password.
                                is CryptoError.KeyUnavailable,
                                is CryptoError.KeystoreFailure,
                                -> {
                                    disable(vaultId)
                                    BiometricUnlockResult.KeyInvalidated
                                }
                                else ->
                                    BiometricUnlockResult.Failed(
                                        result.error.message.ifBlank { "biometric unlock failed" },
                                    )
                            }
                        }
                    }

                val now = clock.now()
                val session =
                    VaultSession(
                        vaultId = vaultId,
                        sessionId = UUID.randomUUID().toString(),
                        authenticationMethod = AuthenticationMethod.BIOMETRIC,
                        unlockedAt = now,
                        lastActivityAt = now,
                    )
                val keyBytes = decrypted.use { it.peek { bytes -> bytes.copyOf() } }
                try {
                    val unlocked = sessionManager.unlock(session, keyBytes)
                    if (unlocked) {
                        failedAttemptTracker.recordSuccess(vaultId)
                        BiometricUnlockResult.Success
                    } else {
                        BiometricUnlockResult.Failed("could not establish the vault session")
                    }
                } finally {
                    MemoryWiper.wipe(keyBytes)
                }
            }

        /** Whether the "enable biometric unlock" offer was already shown/dismissed for [vaultId]. */
        fun isOfferDismissed(vaultId: VaultId): Boolean = prefs.getBoolean(offerDismissedKey(vaultId), false)

        fun markOfferDismissed(vaultId: VaultId) {
            prefs.edit().putBoolean(offerDismissedKey(vaultId), true).apply()
        }

        private fun aliasFor(vaultId: VaultId): String = "sv_biometric_${vaultId.value}"

        private fun enabledKey(vaultId: VaultId): String = "bio_enabled_${vaultId.value}"

        private fun blobKey(vaultId: VaultId): String = "bio_blob_${vaultId.value}"

        private fun offerDismissedKey(vaultId: VaultId): String = "bio_offer_dismissed_${vaultId.value}"

        private fun wrappedBlob(vaultId: VaultId): EncryptedField? {
            val json = prefs.getString(blobKey(vaultId), null) ?: return null
            return runCatching {
                val obj = JSONObject(json)
                EncryptedField(
                    ciphertext = obj.getString("ciphertext"),
                    nonce = obj.getString("nonce"),
                    algorithm = obj.optString("algorithm", EncryptedField.DEFAULT_ALGORITHM),
                )
            }.getOrNull()
        }

        private fun blobToJson(field: EncryptedField): String =
            JSONObject()
                .put("ciphertext", field.ciphertext)
                .put("nonce", field.nonce)
                .put("algorithm", field.algorithm)
                .toString()

        private companion object {
            const val PREFS_NAME = "securevault_biometric"
        }
    }

/** The outcome of enrolling biometric unlock. */
sealed interface EnableResult {
    data object Success : EnableResult

    /** Called while no vault session is active, so there is no key to wrap. */
    data object NoActiveSession : EnableResult

    data class KeystoreError(
        val message: String,
    ) : EnableResult
}

/** The outcome of a biometric unlock attempt. */
sealed interface BiometricUnlockResult {
    data object Success : BiometricUnlockResult

    /** The vault was already unlocked; nothing to do. */
    data object AlreadyUnlocked : BiometricUnlockResult

    /** The Keystore key is gone or invalidated; the enrollment was cleared, use the password. */
    data object KeyInvalidated : BiometricUnlockResult

    data class Failed(
        val message: String,
    ) : BiometricUnlockResult
}
