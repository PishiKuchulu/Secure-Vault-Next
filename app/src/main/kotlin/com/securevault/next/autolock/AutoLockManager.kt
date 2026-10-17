package com.securevault.next.autolock

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.domain.model.VaultSecurityPolicy
import com.securevault.next.core.domain.model.enums.AutoLockPolicy
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.usecase.LockVaultUseCase
import com.securevault.next.core.security.session.VaultSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** Events the auto-lock manager publishes for navigation to react to. */
sealed interface AutoLockEvent {
    /** The vault was auto-locked after the policy timeout; show its unlock screen. */
    data class AutoLocked(val vaultId: String) : AutoLockEvent
}

/**
 * Hilt entry point so [com.securevault.next.SecureVaultApplication]
 * can obtain the [AutoLockManager] without the `di/` modules changing:
 * the manager's own constructor is `@Inject`, so no module binding is
 * needed — only this entry point interface.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoLockEntryPoint {
    fun autoLockManager(): AutoLockManager
}

/**
 * Locks the vault when the app has been in the background longer than
 * the vault's auto-lock policy allows.
 *
 * Design:
 * - Background detection uses [Application.ActivityLifecycleCallbacks]
 *   with a started-activity counter — no extra dependency, and it
 *   survives configuration changes (a rotation stops then starts an
 *   activity, the counter never hits zero, no spurious lock).
 * - On background, a cancellable timer starts with the policy's delay.
 *   Any foregrounding cancels it: returning before the timeout never
 *   locks.
 * - [AutoLockPolicy.IMMEDIATELY] locks with no delay;
 *   [AutoLockPolicy.NEVER] starts no timer at all.
 * - On fire, the vault is locked through [LockVaultUseCase] (the same
 *   path as an explicit lock) and [AutoLockEvent.AutoLocked] is emitted
 *   so the NavHost can land on the unlock screen with the home cleared.
 * - The policy is read at background time from [VaultRepository], so a
 *   change in Settings takes effect on the next backgrounding.
 *
 * Lifecycle-clean: the callbacks are registered exactly once from the
 * Application, the timer job is always cancelled before a new one
 * starts, and the manager holds no Activity reference — only the
 * application-scoped callbacks.
 */
@Singleton
class AutoLockManager @Inject constructor(
    private val lockVaultUseCase: LockVaultUseCase,
    private val sessionManager: VaultSessionManager,
    private val vaultRepository: VaultRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val registered = AtomicBoolean(false)

    private val _events = MutableSharedFlow<AutoLockEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AutoLockEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var startedActivities = 0

    /** Registers the background detection; idempotent, call once from the Application. */
    fun register(application: Application) {
        if (!registered.compareAndSet(false, true)) return
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                timerJob?.cancel()
                timerJob = null
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) {
                    scheduleLock()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun scheduleLock() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val vaultId = sessionManager.currentSession()?.vaultId ?: return@launch
            val policy = vaultRepository.getVault(vaultId).getOrNull()?.securityPolicy
                ?: VaultSecurityPolicy()
            if (policy.autoLockPolicy == AutoLockPolicy.NEVER) return@launch

            val delayMs = policy.autoLockPolicy.minutes.coerceAtLeast(0) * MILLIS_PER_MINUTE
            if (delayMs > 0) {
                delay(delayMs)
            }
            lockVaultUseCase(vaultId)
            _events.emit(AutoLockEvent.AutoLocked(vaultId.value))
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
