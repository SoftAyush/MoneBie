package com.aurex.app.domain.model

/**
 * How soon an obligation needs attention.
 *
 * Ordered by urgency, so grouping by ordinal puts the things that matter most
 * at the top without a separate sort key.
 */
enum class DueBucket {
    OVERDUE,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LATER;

    val label: String
        get() = when (this) {
            OVERDUE -> "Overdue"
            TODAY -> "Due today"
            THIS_WEEK -> "Within a week"
            THIS_MONTH -> "Within a month"
            LATER -> "Later"
        }

    /** True for buckets that warrant a warning colour rather than a neutral one. */
    val isUrgent: Boolean get() = this == OVERDUE || this == TODAY

    companion object {
        fun forDaysUntilDue(days: Long): DueBucket = when {
            days < 0L -> OVERDUE
            days == 0L -> TODAY
            days <= 7L -> THIS_WEEK
            days <= 31L -> THIS_MONTH
            else -> LATER
        }
    }
}
