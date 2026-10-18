package com.securevault.next.core.security.session

/**
 * Why a vault transitioned from unlocked to locked. Useful for audit
 * logging and for tailoring the re-authentication UI's messaging.
 */
enum class LockTrigger {
    /** Auto-lock fired because the vault's auto-lock policy timeout elapsed. */
    TIMEOUT,

    /** The user explicitly chose to lock the vault. */
    EXPLICIT,

    /** The app was backgrounded and the active auto-lock policy locks on backgrounding. */
    APP_BACKGROUNDED,

    /** Too many consecutive failed unlock attempts were made. */
    FAILED_ATTEMPTS_EXCEEDED
}
