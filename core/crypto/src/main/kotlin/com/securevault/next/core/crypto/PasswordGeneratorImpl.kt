package com.securevault.next.core.crypto

import com.securevault.next.core.crypto.random.SecureRandomProvider
import com.securevault.next.core.domain.security.SecurityScoreEngine
import com.securevault.next.core.domain.usecase.GeneratePasswordUseCase
import com.securevault.next.core.domain.usecase.GeneratedPassword
import com.securevault.next.core.domain.usecase.PasswordGenerationOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordGeneratorImpl @Inject constructor(
    private val randomProvider: SecureRandomProvider
) : GeneratePasswordUseCase {

    private val lower = "abcdefghijklmnopqrstuvwxyz"
    private val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val digits = "0123456789"
    private val symbols = "!@#$%^&*_+-="
    private val ambiguous = "il1Lo0O"

    override fun invoke(options: PasswordGenerationOptions): GeneratedPassword {
        val classes = buildList {
            if (options.includeLowercase) add(lower)
            if (options.includeUppercase) add(upper)
            if (options.includeDigits) add(digits)
            if (options.includeSymbols) add(symbols)
        }.map { chars ->
            if (options.excludeAmbiguousCharacters) chars.filterNot { it in ambiguous } else chars
        }
        // PasswordGenerationOptions already requires at least one class;
        // this guards against a class left empty by ambiguous filtering
        // (defensive — none of the built-in classes become empty).
        require(classes.isNotEmpty() && classes.all { it.isNotEmpty() }) {
            "PasswordGeneratorImpl: a requested character class is empty after filtering"
        }
        val charset = classes.joinToString("")

        val password = CharArray(options.length) { charset[nextIndex(charset.length)] }

        // Finding M4 (class coverage): guarantee every requested class
        // appears at least once. Missing classes are repaired at distinct
        // random positions, so a repair can never evict another class's
        // only representative (length >= 4 >= number of classes, enforced
        // by PasswordGenerationOptions).
        val missingClasses = classes.filter { cls -> password.none { it in cls } }
        if (missingClasses.isNotEmpty()) {
            val positions = distinctRandomPositions(missingClasses.size, options.length)
            missingClasses.forEachIndexed { index, cls ->
                password[positions[index]] = cls[nextIndex(cls.length)]
            }
        }

        val passwordStr = password.concatToString()
        password.fill('\u0000')
        val strength = SecurityScoreEngine.evaluateStrength(passwordStr.toCharArray())

        return GeneratedPassword(passwordStr, strength)
    }

    /**
     * A uniform index in `[0, bound)` drawn from [SecureRandomProvider]
     * via rejection sampling (Finding M4): a raw byte is accepted only
     * when it falls below the largest multiple of [bound] that fits in a
     * byte, which eliminates the modulo bias of `byte % bound` (with a
     * 75-character charset the low indices otherwise come up ~33% more
     * often).
     */
    private fun nextIndex(bound: Int): Int {
        require(bound > 0) { "PasswordGeneratorImpl.nextIndex bound must be positive, was $bound" }
        val limit = 256 - (256 % bound)
        while (true) {
            val candidate = randomProvider.nextBytes(1).consume { it[0].toInt() and 0xFF }
            if (candidate < limit) return candidate % bound
        }
    }

    /** [count] distinct positions in `[0, length)`, via a partial Fisher-Yates shuffle. */
    private fun distinctRandomPositions(count: Int, length: Int): IntArray {
        val positions = IntArray(length) { it }
        repeat(count) { i ->
            val j = i + nextIndex(length - i)
            val tmp = positions[i]
            positions[i] = positions[j]
            positions[j] = tmp
        }
        return positions.copyOf(count)
    }
}
