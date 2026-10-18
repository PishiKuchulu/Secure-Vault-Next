package com.securevault.next.core.common.result

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import org.junit.Test

class ResultTest {
    @Test
    fun `success carries its value`() {
        val result: Result<Int, String> = Result.success(42)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo(42)
        assertThat(result.errorOrNull()).isNull()
    }

    @Test
    fun `failure carries its error`() {
        val result: Result<Int, String> = Result.failure("boom")

        assertThat(result.isFailure).isTrue()
        assertThat(result.isSuccess).isFalse()
        assertThat(result.errorOrNull()).isEqualTo("boom")
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `getOrDefault returns the fallback on failure`() {
        val result: Result<Int, String> = Result.failure("boom")

        assertThat(result.getOrDefault(-1)).isEqualTo(-1)
    }

    @Test
    fun `getOrElse computes a value from the error`() {
        val result: Result<Int, String> = Result.failure("boom")

        assertThat(result.getOrElse { it.length }).isEqualTo(4)
    }

    @Test
    fun `map transforms a success value and leaves failure untouched`() {
        val success: Result<Int, String> = Result.success(2)
        val failure: Result<Int, String> = Result.failure("boom")

        assertThat(success.map { it * 10 }.getOrNull()).isEqualTo(20)
        assertThat(failure.map { it * 10 }.errorOrNull()).isEqualTo("boom")
    }

    @Test
    fun `mapError transforms a failure error and leaves success untouched`() {
        val success: Result<Int, String> = Result.success(2)
        val failure: Result<Int, String> = Result.failure("boom")

        assertThat(success.mapError { it.length }.getOrNull()).isEqualTo(2)
        assertThat(failure.mapError { it.length }.errorOrNull()).isEqualTo(4)
    }

    @Test
    fun `flatMap chains fallible operations and short-circuits on the first failure`() {
        fun half(value: Int): Result<Int, String> =
            if (value % 2 == 0) Result.success(value / 2) else Result.failure("odd: $value")

        val bothSucceed = Result.success(8).flatMap(::half).flatMap(::half)
        val secondFails =
            Result
                .success(8)
                .flatMap(::half)
                .flatMap(::half)
                .flatMap(::half)

        assertThat(bothSucceed.getOrNull()).isEqualTo(2)
        assertThat(secondFails.errorOrNull()).isEqualTo("odd: 1")
    }

    @Test
    fun `onSuccess and onFailure run only their matching side effect`() {
        var successRan = false
        var failureRan = false

        Result.success(1).onSuccess { successRan = true }.onFailure { failureRan = true }

        assertThat(successRan).isTrue()
        assertThat(failureRan).isFalse()
    }

    @Test
    fun `fold collapses both branches to the same return type`() {
        val success: Result<Int, String> = Result.success(5)
        val failure: Result<Int, String> = Result.failure("boom")

        assertThat(success.fold(onSuccess = { "ok:$it" }, onFailure = { "err:$it" })).isEqualTo("ok:5")
        assertThat(failure.fold(onSuccess = { "ok:$it" }, onFailure = { "err:$it" })).isEqualTo("err:boom")
    }

    @Test
    fun `resultOf wraps a thrown exception into a Failure`() {
        val result =
            resultOf<Int, String>(onException = { it.message ?: "unknown" }) {
                error("kaboom")
            }

        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull()).isEqualTo("kaboom")
    }

    @Test
    fun `resultOf returns a Success when block does not throw`() {
        val result = resultOf<Int, String>(onException = { "unused" }) { 7 }

        assertThat(result.getOrNull()).isEqualTo(7)
    }

    @Test
    fun `resultOf rethrows CancellationException instead of swallowing it`() {
        var thrown: CancellationException? = null
        try {
            resultOf<Int, String>(onException = { "unused" }) {
                throw CancellationException("cancelled")
            }
        } catch (c: CancellationException) {
            thrown = c
        }

        assertThat(thrown).isNotNull()
    }
}
