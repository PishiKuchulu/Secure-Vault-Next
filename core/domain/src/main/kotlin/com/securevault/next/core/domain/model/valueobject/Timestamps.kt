package com.securevault.next.core.domain.model.valueobject

import java.time.Instant

/**
 * When an entity was first created / most recently modified.
 *
 * These wrap [java.time.Instant] rather than a raw epoch [Long] or
 * platform-specific date type: `java.time` is part of the JDK standard
 * library (not `android.*`), so it compiles equally well in this pure
 * Kotlin/JVM module and on a future desktop JVM client, and it is
 * natively available at runtime on this project's minSdk 29 (Android
 * 8.0 / API 26 introduced `java.time` support).
 */
@JvmInline
value class CreatedAt(val value: Instant)

@JvmInline
value class UpdatedAt(val value: Instant)
