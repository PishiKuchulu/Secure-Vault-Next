package com.securevault.next.core.domain.model.enums

/** Sort direction for any sortable list. */
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

/** The field a list of items can be sorted by. */
enum class SortField {
    TITLE,
    CREATED_AT,
    UPDATED_AT,
    LAST_USED,
    SECURITY_SCORE
}

/** The app's color scheme preference. */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * How long a value copied to the system clipboard remains before
 * SecureVault Next clears it. [seconds] of `0` means "clear
 * immediately on the next clipboard change"; there is no "immediately"
 * synchronous option because the OS clipboard write itself is
 * asynchronous.
 */
enum class ClipboardTimeout(val seconds: Int) {
    SECONDS_15(15),
    SECONDS_30(30),
    SECONDS_60(60),
    MINUTES_2(120),
    NEVER(0)
}

/**
 * How long a vault may sit idle before it is automatically locked.
 *
 * [minutes] of `0` ([IMMEDIATELY]) does *not* mean "lock on
 * backgrounding": the evaluator
 * (`core.security.session.AutoLockEvaluator`) only compares elapsed idle
 * time against this threshold and knows nothing about the app lifecycle,
 * so a zero-minute threshold reports "should lock" on the very next
 * check after any activity — even in the foreground. `-1` ([NEVER])
 * disables auto-lock entirely (strongly discouraged, but the policy type
 * still needs to be able to represent it since it is a legitimate, if
 * risky, user choice).
 *
 * Note: *when* the evaluator runs is the app layer's decision (e.g. on
 * backgrounding or on a timer); this type only carries the threshold.
 */
enum class AutoLockPolicy(val minutes: Int) {
    IMMEDIATELY(0),
    MINUTE_1(1),
    MINUTES_5(5),
    MINUTES_15(15),
    MINUTES_30(30),
    NEVER(-1)
}

/** A method by which a user can authenticate into a vault. */
enum class AuthenticationMethod {
    MASTER_PASSWORD,
    BIOMETRIC,
    PIN,
    HARDWARE_KEY
}
