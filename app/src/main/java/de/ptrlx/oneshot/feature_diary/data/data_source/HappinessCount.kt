package de.ptrlx.oneshot.feature_diary.data.data_source

import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType

/**
 * One row of the happiness tally.
 *
 * A query projection rather than a table: it adds no entity, so the exported Room schema and the
 * data contract stay exactly as they were.
 */
data class HappinessCount(
    val happiness: HappinessType,
    val count: Int
)
