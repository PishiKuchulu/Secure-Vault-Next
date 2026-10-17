package com.securevault.next.core.crypto.result

import com.securevault.next.core.common.result.Result
import com.securevault.next.core.crypto.error.CryptoError

/**
 * The [Result] specialization used throughout `core:crypto`: success
 * carries a value, failure always carries a [CryptoError].
 */
typealias CryptoResult<T> = Result<T, CryptoError>

/** Wraps [value] as a successful [CryptoResult]. */
fun <T> T.asCryptoSuccess(): CryptoResult<T> = Result.success(this)

/** Wraps [error] as a failed [CryptoResult]. */
fun <T> CryptoError.asCryptoFailure(): CryptoResult<T> = Result.failure(this)
