package com.securevault.next.core.security.usecase

import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import com.securevault.next.core.domain.usecase.ValidatePasswordUseCase
import com.securevault.next.core.domain.security.SecurityScoreEngine
import javax.inject.Inject

/**
 * Implementation of [ValidatePasswordUseCase] using [SecurityScoreEngine].
 */
class ValidatePasswordUseCaseImpl @Inject constructor() : ValidatePasswordUseCase {
    override fun invoke(password: CharArray): PasswordStrength {
        return SecurityScoreEngine.evaluateStrength(password)
    }
}
