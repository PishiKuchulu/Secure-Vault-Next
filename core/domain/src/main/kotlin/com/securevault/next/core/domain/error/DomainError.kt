package com.securevault.next.core.domain.error

import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.model.valueobject.VaultName

/**
 * The root of every business-facing error in SecureVault Next.
 *
 * Every use case and repository contract in `core:domain` returns
 * failures as a [DomainError] (via `DomainResult<T>`, see
 * `core.domain.result.DomainResult`) rather than throwing. This keeps
 * error handling exhaustive and compiler-checked at every call site: a
 * `when` over a sealed hierarchy fails to compile if a new case is added
 * and not handled.
 *
 * The hierarchy is organized by the *kind* of failure, not by which
 * layer produced it — [RepositoryError], for instance, is what any
 * repository implementation maps its own infrastructure failures
 * (Room exceptions, IO errors, etc.) onto, without domain callers ever
 * needing to know which infrastructure technology was involved.
 */
sealed class DomainError(open val message: String, open val cause: Throwable? = null) {
    /**
     * A value failed a validation rule before it was ever persisted or
     * acted upon. Produced by the validation framework
     * (`core.domain.validation`) and by value object invariants.
     */
    sealed class ValidationError(override val message: String) : DomainError(message) {
        data class BlankValue(val field: String) : ValidationError("$field must not be blank")

        data class TooShort(val field: String, val minLength: Int, val actualLength: Int) :
            ValidationError("$field must be at least $minLength characters (was $actualLength)")

        data class TooLong(val field: String, val maxLength: Int, val actualLength: Int) :
            ValidationError("$field must be at most $maxLength characters (was $actualLength)")

        data class WeakMasterPassword(val reasons: List<String>) :
            ValidationError("Master password is missing: ${reasons.joinToString()}")

        data class InvalidFormat(val field: String, val reason: String) :
            ValidationError("$field has an invalid format: $reason")

        data class DuplicateValue(val field: String) : ValidationError("$field must be unique")

        data class OutOfRange(val field: String, val min: Number, val max: Number) :
            ValidationError("$field must be between $min and $max")
    }

    /** A failure specific to a vault's identity or lifecycle. */
    sealed class VaultError(override val message: String) : DomainError(message) {
        data class NotFound(val vaultId: VaultId) : VaultError("Vault ${vaultId.value} was not found")

        data class AlreadyExists(val name: VaultName) : VaultError("A vault named '${name.value}' already exists")

        data class AlreadyLocked(val vaultId: VaultId) : VaultError("Vault ${vaultId.value} is already locked")

        data class AlreadyUnlocked(val vaultId: VaultId) : VaultError("Vault ${vaultId.value} is already unlocked")

        data class Corrupted(val vaultId: VaultId, val details: String) :
            VaultError("Vault ${vaultId.value} appears corrupted: $details")

        data class InvalidState(val vaultId: VaultId, val expected: String, val actual: String) :
            VaultError("Vault ${vaultId.value} expected state '$expected' but was '$actual'")
    }

    /**
     * The operation needed cryptographic material (a derived key, a
     * decrypted field) that was not available. `core:domain` never
     * performs cryptography itself — this error exists so domain-layer
     * code can express "this needed encryption/decryption" without
     * depending on `core:crypto`.
     */
    sealed class CryptoRequiredError(override val message: String) : DomainError(message) {
        data object EncryptionUnavailable :
            CryptoRequiredError("Encryption is required for this operation but is not currently available")

        data object DecryptionFailed :
            CryptoRequiredError("Decryption failed — the data may be corrupted, or the wrong key was used")

        data object KeyDerivationRequired :
            CryptoRequiredError("A derived key is required for this operation but was not provided")
    }

    /**
     * An infrastructure-layer failure (persistence, filesystem, network)
     * surfaced through the repository contract without leaking the
     * originating technology to domain callers.
     */
    sealed class RepositoryError(override val message: String, override val cause: Throwable? = null) :
        DomainError(message, cause) {
        data class NotFound(val entityId: String, val entityType: String) :
            RepositoryError("$entityType with id '$entityId' was not found")

        data class ReadFailed(val reason: String, override val cause: Throwable? = null) :
            RepositoryError("Failed to read: $reason", cause)

        data class WriteFailed(val reason: String, override val cause: Throwable? = null) :
            RepositoryError("Failed to write: $reason", cause)

        data class ConflictDetected(val entityId: String, val expectedRevision: Long, val actualRevision: Long) :
            RepositoryError(
                "Conflict on '$entityId': expected revision $expectedRevision but found $actualRevision"
            )
    }

    /** The vault or item import pipeline failed. */
    sealed class ImportError(override val message: String) : DomainError(message) {
        data class UnsupportedFormat(val format: String) : ImportError("Import format '$format' is not supported")

        data class MalformedSource(val reason: String) : ImportError("The import source is malformed: $reason")

        data class PartialFailure(val succeeded: Int, val failed: Int) :
            ImportError("$failed of ${succeeded + failed} items failed to import")
    }

    /** The vault or item export pipeline failed. */
    sealed class ExportError(override val message: String) : DomainError(message) {
        data class UnsupportedFormat(val format: String) : ExportError("Export format '$format' is not supported")

        data class DestinationUnwritable(val destination: String) :
            ExportError("Cannot write export to '$destination'")
    }

    /** A backup or restore operation failed. */
    sealed class BackupError(override val message: String) : DomainError(message) {
        data class NotFound(val backupId: String) : BackupError("Backup '$backupId' was not found")

        data class Incompatible(val backupVersion: String, val currentVersion: String) :
            BackupError("Backup version $backupVersion is incompatible with current version $currentVersion")

        data class VerificationFailed(val reason: String) : BackupError("Backup verification failed: $reason")
    }

    /** Authentication into a vault or session failed. */
    sealed class AuthenticationError(override val message: String) : DomainError(message) {
        data object InvalidCredentials :
            AuthenticationError("The provided credentials are invalid")

        data class TooManyAttempts(val retryAfterSeconds: Long) :
            AuthenticationError("Too many failed attempts; retry after $retryAfterSeconds seconds")

        data object BiometricUnavailable :
            AuthenticationError("Biometric authentication is not available on this device")

        data object BiometricFailed :
            AuthenticationError("Biometric authentication failed")

        data object SessionExpired :
            AuthenticationError("The current session has expired")
    }

    /** The caller lacked a required permission or capability. */
    sealed class PermissionError(override val message: String) : DomainError(message) {
        data class Denied(val permission: String) : PermissionError("Permission denied: $permission")

        data class NotGranted(val permission: String) : PermissionError("Permission not granted: $permission")
    }

    /**
     * A failure that does not fit any known category above — always
     * carries the original [cause] so it is never a dead end for
     * debugging. Use cases should prefer a specific error type; this
     * exists as an honest escape hatch rather than a excuse to skip
     * modeling a real error case.
     */
    data class UnexpectedError(override val message: String, override val cause: Throwable? = null) :
        DomainError(message, cause)
}
