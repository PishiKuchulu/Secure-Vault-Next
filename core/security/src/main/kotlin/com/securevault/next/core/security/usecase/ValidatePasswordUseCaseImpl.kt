package com.securevault.next.core.security.usecase

import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import com.securevault.next.core.domain.security.SecurityScoreEngine
import com.securevault.next.core.domain.usecase.ValidatePasswordUseCase
import javax.inject.Inject

/**
 * Implementation of [ValidatePasswordUseCase] using [SecurityScoreEngine].
 */
class ValidatePasswordUseCaseImpl
    @Inject
    constructor() : ValidatePasswordUseCase {
        override fun invoke(password: CharArray): PasswordStrength = SecurityScoreEngine.evaluateStrength(password)
    }
