package com.securevault.next.core.domain.result

import com.securevault.next.core.common.result.Result
import com.securevault.next.core.domain.error.DomainError

/**
 * The [Result] specialization used everywhere in `core:domain`: success
 * carries a domain value, failure always carries a [DomainError].
 *
 * Every repository interface (`core.domain.repository`) and use case
 * contract (`core.domain.usecase`) that can fail returns a
 * [DomainResult] rather than throwing, so that callers are forced by the
 * compiler to handle failure explicitly.
 */
typealias DomainResult<T> = Result<T, DomainError>

/** Wraps [value] as a successful [DomainResult]. */
fun <T> T.asDomainSuccess(): DomainResult<T> = Result.success(this)

/** Wraps [error] as a failed [DomainResult]. */
fun <T> DomainError.asDomainFailure(): DomainResult<T> = Result.failure(this)
