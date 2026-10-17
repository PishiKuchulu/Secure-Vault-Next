package com.securevault.next.core.domain.model.valueobject

import com.securevault.next.core.domain.model.enums.PasswordStrengthLevel

/**
 * The result of evaluating how strong a password is.
 *
 * This is a plain data holder — the actual strength-scoring algorithm is
 * an implementation behind `core.domain.usecase.ValidatePasswordUseCase`
 * (a contract only as of Milestone 2; the entropy/scoring implementation
 * lands in a later milestone). [feedback] carries human-readable
 * improvement suggestions (e.g. "add a symbol"), kept separate from
 * [level] so a UI can render the categorical strength meter and the
 * suggestion list independently.
 */
data class PasswordStrength(
    val score: SecurityScore,
    val level: PasswordStrengthLevel,
    val feedback: List<String> = emptyList(),
)
