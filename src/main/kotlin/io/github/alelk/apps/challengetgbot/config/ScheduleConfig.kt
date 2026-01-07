package io.github.alelk.apps.challengetgbot.config

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Schedule configuration for posting challenges
 */
data class ScheduleConfig(
    val frequency: Frequency = Frequency.DAILY,
    val time: LocalTime,
    val daysOfWeek: List<DayOfWeek>? = null,
    val timezone: String = "UTC"
)