package com.securevault.next.autolock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Exposes the app-scoped [AutoLockManager]'s events to the Compose
 * navigation graph. The manager itself is a `@Singleton` obtained from
 * the Hilt graph, so this ViewModel is only a thin bridge the NavHost
 * can collect from.
 */
@HiltViewModel
class AutoLockViewModel
    @Inject
    constructor(
        autoLockManager: AutoLockManager,
    ) : ViewModel() {
        val events: SharedFlow<AutoLockEvent> = autoLockManager.events
    }
