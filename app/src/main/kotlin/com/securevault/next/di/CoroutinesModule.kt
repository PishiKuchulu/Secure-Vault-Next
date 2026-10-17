package com.securevault.next.di

import com.securevault.next.core.common.dispatcher.Dispatcher
import com.securevault.next.core.common.dispatcher.SecureVaultDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Binds the platform-agnostic [SecureVaultDispatcher] qualifier (defined
 * in pure-Kotlin `core:common`) to real [kotlinx.coroutines.Dispatchers]
 * instances.
 *
 * This is deliberately the *only* file in the entire codebase allowed to
 * reference `Dispatchers.Default` / `Dispatchers.IO` directly — every
 * other class receives a dispatcher via `@Dispatcher(...)` constructor
 * injection instead, which is what makes those classes swappable in
 * tests (Part 2: "Testability... Constructor injection preferred").
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Dispatcher(SecureVaultDispatcher.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Dispatcher(SecureVaultDispatcher.Io)
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
