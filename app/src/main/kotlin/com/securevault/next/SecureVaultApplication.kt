package com.securevault.next

import android.app.Application
import com.securevault.next.autolock.AutoLockEntryPoint
import com.securevault.next.autolock.AutoLockManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Beyond enabling Hilt, this class registers the [AutoLockManager]'s
 * background detection exactly once. The manager itself is obtained
 * through [AutoLockEntryPoint] so no `di/` module needs to change; all
 * of its constructor dependencies are already bound in the graph.
 *
 * Business logic, cryptography and vault lifecycle management never
 * live here — they are owned by the Application layer use cases and
 * the Security module, wired in through Hilt and invoked from
 * ViewModels.
 */
@HiltAndroidApp
class SecureVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val autoLockManager =
            EntryPointAccessors
                .fromApplication(this, AutoLockEntryPoint::class.java)
                .autoLockManager()
        autoLockManager.register(this)
    }
}
