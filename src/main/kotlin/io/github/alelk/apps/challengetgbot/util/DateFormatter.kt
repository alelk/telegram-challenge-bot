package io.github.alelk.apps.challengetgbot.util

import io.github.alelk.apps.challengetgbot.config.GroupConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Utility object for formatting dates and times in Russian
 */
object DateFormatter {

    private val MONTH_NAMES = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val DAY_OF_WEEK_NAMES = mapOf(
        DayOfWeek.MONDAY to "понедельник",
        DayOfWeek.TUESDAY to "вторник",
        DayOfWeek.WEDNESDAY to "среда",
        DayOfWeek.THURSDAY to "четверг",
        DayOfWeek.FRIDAY to "пятница",
        DayOfWeek.SATURDAY to "суббота",
        DayOfWeek.SUNDAY to "воскресенье"
    )

    /**
     * Format date in Russian format (e.g., "7 января")
     */
    fun formatDate(date: LocalDate): String {
        return "${date.dayOfMonth} ${MONTH_NAMES[date.monthValue - 1]}"
    }

    /**
     * Get Russian month name
     */
    fun getMonthName(monthValue: Int): String {
        require(monthValue in 1..12) { "Month value must be between 1 and 12" }
        return MONTH_NAMES[monthValue - 1]
    }

    /**
     * Format day of week in Russian
     */
    fun formatDayOfWeek(dayOfWeek: DayOfWeek): String {
        return DAY_OF_WEEK_NAMES[dayOfWeek] ?: dayOfWeek.name
    }

    /**
     * Format question text with template variables
     */
    fun formatQuestionTemplate(template: String, date: LocalDate): String {
        return template
            .replace("{date}", formatDate(date))
            .replace("{day}", date.dayOfMonth.toString())
            .replace("{month}", getMonthName(date.monthValue))
            .replace("{year}", date.year.toString())
            .replace("{dayOfWeek}", formatDayOfWeek(date.dayOfWeek))
    }

    /**
     * Format question from group config using current date in group's timezone
     */
    fun formatQuestionFromConfig(groupConfig: GroupConfig): String {
        val zoneId = ZoneId.of(groupConfig.schedule.timezone)
        val localDate = LocalDate.now(zoneId)
        return formatQuestionTemplate(groupConfig.challenge.questionTemplate, localDate)
    }
}

