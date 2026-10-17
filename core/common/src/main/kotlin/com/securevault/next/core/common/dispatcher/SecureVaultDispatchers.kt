package com.securevault.next.core.common.dispatcher

import javax.inject.Qualifier

/**
 * Identifies which [kotlinx.coroutines.CoroutineDispatcher] should be
 * injected at a given call site.
 *
 * This lives in `core:common` — a pure Kotlin/JVM module — rather than
 * alongside the Hilt `@Module` that actually provides the dispatcher
 * instances. The qualifier itself is platform-agnostic (it is a plain
 * JSR-330 annotation, see [Qualifier]); only the *binding* of
 * [SecureVaultDispatcher.Default] to [kotlinx.coroutines.Dispatchers.Default]
 * is Android/Hilt-specific, and that binding lives in the `app` module's
 * DI graph. This keeps the shared business core (Part 1: "a shared
 * platform-independent business core") free of any Hilt dependency.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class Dispatcher(val secureVaultDispatcher: SecureVaultDispatcher)

/**
 * The set of coroutine dispatchers used across SecureVault Next.
 *
 * [Default] and [Io] map directly onto [kotlinx.coroutines.Dispatchers];
 * [Unconfined] is reserved for tests that need to advance a
 * [kotlinx.coroutines.test.TestDispatcher] synchronously and is never
 * bound to a real dispatcher in production code.
 */
enum class SecureVaultDispatcher {
    Default,
    Io,
    Unconfined,
}
