package com.securevault.next.core.domain.security

import com.securevault.next.core.domain.model.enums.PasswordStrengthLevel
import com.securevault.next.core.domain.model.valueobject.PasswordStrength
import com.securevault.next.core.domain.model.valueobject.SecurityScore
import kotlin.math.log2

/**
 * The intelligence engine responsible for calculating password entropy,
 * strength levels, and overall security scores.
 */
object SecurityScoreEngine {
    fun calculateEntropy(password: CharArray): Double {
        if (password.isEmpty()) return 0.0

        var hasLower = false
        var hasUpper = false
        var hasDigit = false
        var hasSymbol = false

        for (char in password) {
            when {
                char.isLowerCase() -> hasLower = true
                char.isUpperCase() -> hasUpper = true
                char.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        var charsetSize = 0
        if (hasLower) charsetSize += 26
        if (hasUpper) charsetSize += 26
        if (hasDigit) charsetSize += 10
        if (hasSymbol) charsetSize += 32

        return if (charsetSize == 0) 0.0 else password.size * log2(charsetSize.toDouble())
    }

    fun evaluateStrength(password: CharArray): PasswordStrength {
        val entropy = calculateEntropy(password)
        val level =
            when {
                entropy < 40 -> PasswordStrengthLevel.VERY_WEAK
                entropy < 60 -> PasswordStrengthLevel.WEAK
                entropy < 80 -> PasswordStrengthLevel.FAIR
                entropy < 100 -> PasswordStrengthLevel.STRONG
                else -> PasswordStrengthLevel.VERY_STRONG
            }

        val scoreValue = (entropy.coerceIn(0.0, 120.0) / 120.0 * 100).toInt()
        val score = SecurityScore(scoreValue)

        val feedback = mutableListOf<String>()
        if (password.size < 12) feedback.add("Password is too short")

        return PasswordStrength(score, level, feedback)
    }
}
