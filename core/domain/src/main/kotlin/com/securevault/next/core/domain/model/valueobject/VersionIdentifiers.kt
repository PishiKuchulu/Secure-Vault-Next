package com.securevault.next.core.domain.model.valueobject

/**
 * A monotonically increasing counter incremented on every write to an
 * entity, used for optimistic-concurrency conflict detection (see
 * `DomainError.RepositoryError.ConflictDetected`). Never negative: a
 * brand-new entity starts at revision 0.
 */
@JvmInline
value class Revision(
    val value: Long,
) {
    init {
        require(value >= 0) { "Revision must not be negative, was $value" }
    }

    /** The revision after this write. */
    fun next(): Revision = Revision(value + 1)

    companion object {
        val INITIAL = Revision(0)
    }
}

/**
 * The schema/format version of a persisted structure (a vault, a
 * backup). Distinct from [Revision]: version changes when the *shape* of
 * the data changes and may require migration; revision changes on every
 * ordinary write and never requires migration.
 */
@JvmInline
value class Version(
    val value: Int,
) {
    init {
        require(value >= 1) { "Version must be at least 1, was $value" }
    }
}
