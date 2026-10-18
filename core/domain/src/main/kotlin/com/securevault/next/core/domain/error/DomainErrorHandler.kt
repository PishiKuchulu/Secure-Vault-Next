package com.securevault.next.core.domain.error

import com.securevault.next.core.domain.result.DomainResult

/**
 * Centrally manages the transformation of exceptions into [DomainError]s.
 *
 * Part 7: "Error Handling... Review every error path... improve recovery".
 */
object DomainErrorHandler {

    /** Maps any [Throwable] into a standard [DomainError.UnexpectedError]. */
    fun fromThrowable(t: Throwable): DomainError {
        return when (t) {
            is IllegalArgumentException -> DomainError.ValidationError.InvalidFormat(
                "input",
                t.message ?: "Invalid data",
            )
            is java.io.IOException -> DomainError.RepositoryError.ReadFailed("IO failure", t)
            else -> DomainError.UnexpectedError(t.message ?: "An unknown error occurred", t)
        }
    }

    /** Wraps a block in a [DomainResult], catching and mapping exceptions. */
    inline fun <T> runCatchingDomain(block: () -> T): DomainResult<T> {
        return try {
            DomainResult.success(block())
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            DomainResult.failure(fromThrowable(t))
        }
    }
}
