package com.securevault.next.core.common.result

/**
 * A generic success/failure wrapper used across every layer of
 * SecureVault Next in place of throwing exceptions for expected failure
 * cases.
 *
 * This type is intentionally generic over both the success type [T] and
 * the error type [E] — it carries no dependency on any particular error
 * hierarchy. `core:domain` defines its own `DomainError` hierarchy and
 * pairs it with this type via a `DomainResult<T>` type alias; a future
 * crypto- or platform-specific error hierarchy could reuse this same
 * [Result] type without core:common needing to know anything about
 * domain concepts. That separation is what keeps this module a pure,
 * reusable primitive (Part 1: "a shared platform-independent business
 * core").
 */
sealed interface Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>

    data class Failure<out E>(val error: E) : Result<Nothing, E>

    companion object {
        fun <T> success(value: T): Result<T, Nothing> = Success(value)

        fun <E> failure(error: E): Result<Nothing, E> = Failure(error)
    }
}

/** True when this [Result] is a [Result.Success]. */
val Result<*, *>.isSuccess: Boolean
    get() = this is Result.Success

/** True when this [Result] is a [Result.Failure]. */
val Result<*, *>.isFailure: Boolean
    get() = this is Result.Failure

/** Returns the success value, or `null` if this is a [Result.Failure]. */
fun <T, E> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

/** Returns the failure error, or `null` if this is a [Result.Success]. */
fun <T, E> Result<T, E>.errorOrNull(): E? = when (this) {
    is Result.Success -> null
    is Result.Failure -> error
}

/** Returns the success value, or [default] if this is a [Result.Failure]. */
fun <T, E> Result<T, E>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> default
}

/** Returns the success value, or the result of [onFailure] applied to the error. */
inline fun <T, E> Result<T, E>.getOrElse(onFailure: (E) -> T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> onFailure(error)
}

/** Transforms the success value, leaving a failure untouched. */
inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Failure -> this
}

/** Transforms the failure error, leaving a success untouched. */
inline fun <T, E, F> Result<T, E>.mapError(transform: (E) -> F): Result<T, F> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
}

/**
 * Chains another [Result]-returning operation onto a success, short
 * circuiting on failure. This is the monadic bind used to sequence
 * multiple fallible operations without nested `when` blocks.
 */
inline fun <T, E, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> this
}

/** Runs [action] for its side effect when this is a success, then returns the original result unchanged. */
inline fun <T, E> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(value)
    return this
}

/** Runs [action] for its side effect when this is a failure, then returns the original result unchanged. */
inline fun <T, E> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Failure) action(error)
    return this
}

/** Collapses a [Result] to a single value by handling both branches. */
inline fun <T, E, R> Result<T, E>.fold(onSuccess: (T) -> R, onFailure: (E) -> R): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(error)
}

/**
 * Runs [block], catching any thrown [Throwable] and mapping it through
 * [onException] into a [Result.Failure] instead of propagating it.
 *
 * This is the sanctioned way to bridge exception-throwing code (e.g. a
 * third-party library call in a future repository implementation) into
 * the [Result] model, rather than letting exceptions cross layer
 * boundaries uncaught.
 *
 * [kotlinx.coroutines.CancellationException] is deliberately rethrown
 * rather than converted to a [Result.Failure]: swallowing it would break
 * structured concurrency for any caller running inside a coroutine.
 */
inline fun <T, E> resultOf(onException: (Throwable) -> E, block: () -> T): Result<T, E> = try {
    Result.Success(block())
} catch (c: kotlinx.coroutines.CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.Failure(onException(t))
}
