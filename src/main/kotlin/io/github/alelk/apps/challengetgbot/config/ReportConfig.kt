package io.github.alelk.apps.challengetgbot.config

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Report configuration
 */
data class ReportConfig(
    val frequency: ReportFrequency = ReportFrequency.WEEKLY,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val time: LocalTime,
    val timezone: String = "UTC",
    val includeCompletionStats: Boolean = true,
    val includePointsStats: Boolean = true,
    val sortBy: SortBy = SortBy.POINTS_DESC
)