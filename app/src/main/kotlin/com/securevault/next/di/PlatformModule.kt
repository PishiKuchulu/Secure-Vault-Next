package com.securevault.next.di

import android.content.Context
import com.securevault.next.core.platform.clipboard.SecureClipboardManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Platform-layer bindings: Android system-service wrappers that have no
 * `@Inject` constructor of their own.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideSecureClipboardManager(
        @ApplicationContext context: Context,
    ): SecureClipboardManager = SecureClipboardManager(context)
}
